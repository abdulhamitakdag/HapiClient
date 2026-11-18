package tr.com.hapicraft.client.kozmetik;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Entity;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.HapiClient;
import tr.com.hapicraft.client.ag.Ag;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Kozmetik kataloğu ve kimin ne taktığının tablosu. Katalog kozmetik/uret.py ile üretilir. */
public final class Kozmetikler {
    public static final String[] YUVALAR = {"pelerin", "kanat", "sapka", "hale", "aura"};
    public static final String[] YUVA_ADLARI = {"Pelerin", "Kanat", "Şapka", "Hale", "Aura"};
    private static final Map<String, Kozmetik> KATALOG = new LinkedHashMap<>();
    private static final Map<UUID, Map<String, String>> DIGER = new ConcurrentHashMap<>();

    private Kozmetikler() {}

    private static final class Dosya {
        List<Kozmetik> kozmetikler;
    }

    static {
        try (InputStream in = Kozmetikler.class.getResourceAsStream("/assets/hapiclient/kozmetik/katalog.json")) {
            Dosya d = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Dosya.class);
            for (Kozmetik k : d.kozmetikler) KATALOG.put(k.kimlik(), k);
        } catch (Exception e) {
            HapiClient.LOG.error("Kozmetik kataloğu okunamadı", e);
        }
    }

    public static void kur() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((tur, cizer, yardimci, baglam) -> {
            if (cizer instanceof AvatarRenderer<?> oyuncuCizer) yardimci.register(new KozmetikKatmani(oyuncuCizer));
        });
    }

    public static Kozmetik bul(String kimlik) { return kimlik == null ? null : KATALOG.get(kimlik); }

    public static List<Kozmetik> hepsi() { return new ArrayList<>(KATALOG.values()); }

    public static List<Kozmetik> yuvadakiler(String yuva) {
        List<Kozmetik> sonuc = new ArrayList<>();
        for (Kozmetik k : KATALOG.values()) if (k.yuva().equals(yuva)) sonuc.add(k);
        return sonuc;
    }

    public static boolean sahip(String kimlik) {
        return Ag.sahip.contains("*") || Ag.sahip.contains(kimlik);
    }

    private static Kozmetik secimden(Map<String, String> secim, String yuva) {
        if (secim == null) return null;
        Kozmetik k = bul(secim.get(yuva));
        return k != null && k.yuva().equals(yuva) ? k : null;
    }

    /** Verilen oyuncunun verilen yuvadaki kozmetiği; yoksa null. */
    public static Kozmetik takili(UUID uuid, String yuva) {
        Minecraft mc = Minecraft.getInstance();
        return secimden(mc.player != null && mc.player.getUUID().equals(uuid) ? Ayarlar.A.kozmetik : DIGER.get(uuid), yuva);
    }

    /** Çizim durumundaki varlık numarasından; önizleme mankeni kendi seçim tablosunu kullanır. */
    public static Kozmetik takili(int varlikNo, String yuva) {
        if (varlikNo == Onizleme.NO) return secimden(Onizleme.SECIM, yuva);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Entity e = mc.level.getEntity(varlikNo);
        return e == null ? null : takili(e.getUUID(), yuva);
    }

    public static void sec(String yuva, String kimlik) {
        if (kimlik == null || kimlik.isEmpty()) Ayarlar.A.kozmetik.remove(yuva);
        else Ayarlar.A.kozmetik.put(yuva, kimlik);
        Ayarlar.kaydet();
        Ag.kozmetikGonder();
    }

    public static void hepsiniCikar() {
        Ayarlar.A.kozmetik.clear();
        Ayarlar.kaydet();
        Ag.kozmetikGonder();
    }

    public static void digeriniAyarla(UUID uuid, Map<String, String> secim) { DIGER.put(uuid, secim); }
    public static void digeriniSil(UUID uuid) { DIGER.remove(uuid); }
    public static void digerleriniTemizle() { DIGER.clear(); }
}
