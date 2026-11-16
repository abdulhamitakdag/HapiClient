package tr.com.hapicraft.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Kalıcı istemci ayarları: <oyun dizini>/hapiclient/ayarlar.json */
public final class Ayarlar {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Ayarlar A = new Ayarlar();

    public Map<String, Boolean> modul = new HashMap<>();
    public Map<String, float[]> konum = new HashMap<>();
    public Map<String, String> kozmetik = new HashMap<>();
    /** Görünüm seçenekleri (renk, şekil, kalınlık): anahtar → seçili değerin sırası. */
    public Map<String, Integer> secenek = new HashMap<>();
    public List<String> favoriEmote = new ArrayList<>();
    public boolean sessiz = false;
    public boolean tamParlaklik = false;
    public String profil = "";
    public boolean ilkAcilis = true;

    public static Path dizin() {
        return FabricLoader.getInstance().getGameDir().resolve("hapiclient");
    }

    public static void yukle() {
        Path dosya = dizin().resolve("ayarlar.json");
        try {
            if (Files.exists(dosya)) {
                Ayarlar okunan = GSON.fromJson(Files.readString(dosya, StandardCharsets.UTF_8), Ayarlar.class);
                if (okunan != null) A = okunan.onar();
            }
        } catch (Exception e) {
            HapiClient.LOG.warn("Ayarlar okunamadı, varsayılanlar kullanılıyor", e);
        }
    }

    private Ayarlar onar() {
        if (modul == null) modul = new HashMap<>();
        if (konum == null) konum = new HashMap<>();
        if (kozmetik == null) kozmetik = new HashMap<>();
        if (secenek == null) secenek = new HashMap<>();
        if (favoriEmote == null) favoriEmote = new ArrayList<>();
        if (profil == null) profil = "";
        return this;
    }

    public static void kaydet() {
        try {
            Files.createDirectories(dizin());
            Path gecici = dizin().resolve("ayarlar.json.tmp");
            Files.writeString(gecici, GSON.toJson(A), StandardCharsets.UTF_8);
            Files.move(gecici, dizin().resolve("ayarlar.json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            HapiClient.LOG.warn("Ayarlar kaydedilemedi", e);
        }
    }

    public boolean acik(String kimlik, boolean varsayilan) {
        return modul.getOrDefault(kimlik, varsayilan);
    }
}
