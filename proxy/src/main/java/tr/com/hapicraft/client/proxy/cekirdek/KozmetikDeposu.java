package tr.com.hapicraft.client.proxy.cekirdek;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Kozmetik seçimlerinin kalıcı deposu: UUID anahtarlı tek JSON dosyası.
 * Yazma atomiktir (geçici dosya + taşıma) ve geciktirilir: ilk değişiklikten {@code gecikmeMs}
 * sonra, o ana kadarki bütün değişiklikler tek seferde yazılır. Thread-güvenlidir.
 */
public final class KozmetikDeposu implements AutoCloseable {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private final Path dosya;
    private final long gecikmeMs;
    private final Consumer<String> hataGunlugu;
    private final ConcurrentMap<UUID, Map<String, String>> secimler = new ConcurrentHashMap<>();
    private final AtomicBoolean kirli = new AtomicBoolean();
    private final AtomicBoolean planlandi = new AtomicBoolean();
    private final Object yazmaKilidi = new Object();
    private final ScheduledExecutorService zamanlayici = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "HapiClientProxy-depo");
        t.setDaemon(true);
        return t;
    });

    public KozmetikDeposu(Path dosya, long gecikmeMs, Consumer<String> hataGunlugu) {
        this.dosya = dosya;
        this.gecikmeMs = gecikmeMs;
        this.hataGunlugu = hataGunlugu;
    }

    /**
     * Dosyayı okur. Dosya yoksa boş başlar. Dosya bozuksa üzerine yazılmasın diye
     * {@code .bozuk} uzantısıyla kenara alınır ve boş başlanır. Geçersiz kayıtlar atlanır.
     */
    public void yukle() {
        secimler.clear();
        if (!Files.exists(dosya)) {
            return;
        }
        JsonObject kok;
        try {
            JsonElement e = JsonParser.parseString(Files.readString(dosya, StandardCharsets.UTF_8));
            if (!e.isJsonObject()) {
                throw new IllegalStateException("kök bir nesne değil");
            }
            kok = e.getAsJsonObject();
        } catch (IOException | RuntimeException | StackOverflowError e) {
            Path yedek = dosya.resolveSibling(dosya.getFileName() + ".bozuk");
            try {
                Files.move(dosya, yedek, StandardCopyOption.REPLACE_EXISTING);
                hataGunlugu.accept("Kozmetik dosyası okunamadı, " + yedek + " olarak kenara alındı: " + e);
            } catch (IOException e2) {
                hataGunlugu.accept("Kozmetik dosyası okunamadı ve kenara alınamadı: " + e + " / " + e2);
            }
            return;
        }
        for (Map.Entry<String, JsonElement> giris : kok.entrySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(giris.getKey());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (!giris.getValue().isJsonObject()) {
                continue;
            }
            // Sahiplik burada bilinemez; yalnız biçim doğrulanır. Sahiplik MERHABA'da yeniden denetlenir.
            Map<String, String> secim = Kozmetikler.dogrula(giris.getValue().getAsJsonObject(), k -> true);
            if (!Kozmetikler.bosMu(secim)) {
                secimler.put(uuid, secim);
            }
        }
    }

    /** Oyuncunun seçimi; kaydı yoksa bütün yuvalar boş. */
    public Map<String, String> al(UUID uuid) {
        Map<String, String> secim = secimler.get(uuid);
        return secim != null ? secim : Kozmetikler.bos();
    }

    /** Seçimi değiştirir ve gecikmeli kaydı planlar. Tamamen boş seçim kaydı siler. */
    public void ayarla(UUID uuid, Map<String, String> secim) {
        Map<String, String> onceki;
        if (Kozmetikler.bosMu(secim)) {
            onceki = secimler.remove(uuid);
            if (onceki == null) {
                return;
            }
        } else {
            // Biçim doğrulaması aynı zamanda değiştirilemez, yuva sıralı bir kopya üretir.
            Map<String, String> kopya = Kozmetikler.dogrula(secim, k -> true);
            onceki = secimler.put(uuid, kopya);
            if (kopya.equals(onceki)) {
                return;
            }
        }
        kirli.set(true);
        if (planlandi.compareAndSet(false, true)) {
            try {
                zamanlayici.schedule(this::planliKaydet, gecikmeMs, TimeUnit.MILLISECONDS);
            } catch (RuntimeException e) {
                // Depo kapatılmış: kapat() son durumu zaten yazar.
                planlandi.set(false);
            }
        }
    }

    private void planliKaydet() {
        planlandi.set(false);
        kaydet();
    }

    /** Bekleyen değişiklik varsa hemen yazar. */
    public void kaydet() {
        synchronized (yazmaKilidi) {
            if (!kirli.compareAndSet(true, false)) {
                return;
            }
            try {
                yaz();
            } catch (IOException | RuntimeException e) {
                kirli.set(true);
                hataGunlugu.accept("Kozmetik dosyası yazılamadı: " + e);
            }
        }
    }

    private void yaz() throws IOException {
        // Sıralı anahtar: dosya farkları okunabilir kalsın.
        Map<String, Map<String, String>> sirali = new TreeMap<>();
        secimler.forEach((uuid, secim) -> sirali.put(uuid.toString(), secim));
        Path dizin = dosya.toAbsolutePath().getParent();
        Files.createDirectories(dizin);
        Path gecici = dizin.resolve(dosya.getFileName() + ".tmp");
        Files.writeString(gecici, GSON.toJson(sirali), StandardCharsets.UTF_8);
        try {
            Files.move(gecici, dosya, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(gecici, dosya, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public int kayitSayisi() {
        return secimler.size();
    }

    /** Zamanlayıcıyı durdurur ve bekleyen değişikliği yazar. */
    @Override
    public void close() {
        zamanlayici.shutdownNow();
        try {
            zamanlayici.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        kaydet();
    }
}
