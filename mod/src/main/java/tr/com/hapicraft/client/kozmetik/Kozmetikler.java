package tr.com.hapicraft.client.kozmetik;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.ag.Ag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Kozmetik kataloğu ve kimin ne taktığının tablosu. */
public final class Kozmetikler {
    public static final String[] YUVALAR = {"pelerin", "kanat", "sapka", "hale", "aura"};
    public static final String[] YUVA_ADLARI = {"Pelerin", "Kanat", "Şapka", "Hale", "Aura"};
    private static final Map<String, Kozmetik> KATALOG = new LinkedHashMap<>();
    private static final Map<UUID, Map<String, String>> DIGER = new ConcurrentHashMap<>();
    private static final java.util.Random RASTGELE = new java.util.Random();

    private Kozmetikler() {}

    private static void ekle(String kimlik, String ad, String yuva, String bicim, int r1, int r2) {
        KATALOG.put(kimlik, new Kozmetik(kimlik, ad, yuva, bicim, 0xFF000000 | r1, 0xFF000000 | r2));
    }

    static {
        ekle("p_hapi", "HapiCraft", "pelerin", "hapi", 0x2BD35A, 0xFFFFFF);
        ekle("p_bayrak", "Ay Yıldız", "pelerin", "bayrak", 0xE30A17, 0xFFFFFF);
        ekle("p_gece", "Gece Göğü", "pelerin", "gece", 0x101030, 0xFFFFFF);
        ekle("p_alev", "Alev", "pelerin", "alev", 0xFF6A00, 0xFFD23F);
        ekle("p_buz", "Buz", "pelerin", "buz", 0x7FD8FF, 0xFFFFFF);
        ekle("p_altin", "Altın", "pelerin", "altin", 0xF2C230, 0xFFF3B0);
        ekle("p_gokkusagi", "Gökkuşağı", "pelerin", "gokkusagi", 0xFF0000, 0x0000FF);
        ekle("p_kareli", "Kareli", "pelerin", "kareli", 0x145A28, 0x2BD35A);
        ekle("p_siyah", "Kara Pelerin", "pelerin", "siyah", 0x111111, 0x444444);
        ekle("p_kalp", "Kalpli", "pelerin", "kalp", 0xFF7BAC, 0xFFFFFF);

        ekle("k_melek", "Melek Kanadı", "kanat", "tuy", 0xFFFFFF, 0xCFE8FF);
        ekle("k_seytan", "Şeytan Kanadı", "kanat", "yarasa", 0x8B0000, 0x2A0000);
        ekle("k_ejderha", "Ejderha Kanadı", "kanat", "yarasa", 0x1E8F3E, 0x0B3D1A);
        ekle("k_kelebek", "Kelebek Kanadı", "kanat", "kelebek", 0xFF8A00, 0x8A2BE2);
        ekle("k_peri", "Peri Kanadı", "kanat", "kelebek", 0x9BF5FF, 0xE8FFFF);
        ekle("k_hapi", "Hapi Kanadı", "kanat", "tuy", 0x2BD35A, 0xFFFFFF);
        ekle("k_anka", "Anka Kanadı", "kanat", "tuy", 0xFF4500, 0xFFD23F);
        ekle("k_gece", "Gece Kanadı", "kanat", "tuy", 0x1B1B3A, 0x6C5CE7);

        ekle("s_silindir", "Silindir Şapka", "sapka", "silindir", 0x1A1A1A, 0x2BD35A);
        ekle("s_tac", "Kral Tacı", "sapka", "tac", 0xF2C230, 0xE0115F);
        ekle("s_fes", "Fes", "sapka", "fes", 0xB3001B, 0x111111);
        ekle("s_parti", "Parti Şapkası", "sapka", "parti", 0xFF4FA3, 0x4FC3FF);
        ekle("s_hasir", "Hasır Şapka", "sapka", "hasir", 0xE2C275, 0x8B5A2B);
        ekle("s_boynuz", "Boynuz", "sapka", "boynuz", 0xB30000, 0x400000);
        ekle("s_kedi", "Kedi Kulağı", "sapka", "kedi", 0x2B2B2B, 0xFF9EC4);
        ekle("s_tavsan", "Tavşan Kulağı", "sapka", "tavsan", 0xFFFFFF, 0xFF9EC4);
        ekle("s_asci", "Aşçı Şapkası", "sapka", "asci", 0xFFFFFF, 0xE8E8E8);
        ekle("s_buyucu", "Büyücü Şapkası", "sapka", "buyucu", 0x5B2A86, 0xF2C230);
        ekle("s_kep", "Hapi Kep", "sapka", "kep", 0x2BD35A, 0xFFFFFF);
        ekle("s_cicek", "Çiçek Tacı", "sapka", "cicek", 0x2E8B3D, 0xFF7BAC);

        ekle("h_altin", "Altın Hale", "hale", "halka", 0xFFE066, 0xFFF7C2);
        ekle("h_hapi", "Yeşil Hale", "hale", "halka", 0x2BD35A, 0xB6FFCB);
        ekle("h_ates", "Ateş Halesi", "hale", "halka", 0xFF4500, 0xFFD23F);
        ekle("h_gokkusagi", "Gökkuşağı Halesi", "hale", "gokkusagi", 0xFFFFFF, 0xFFFFFF);
        ekle("h_yildiz", "Yıldız Çemberi", "hale", "yorunge", 0xFFFFFF, 0x9BF5FF);

        ekle("a_kalp", "Kalpler", "aura", "kalp", 0xFF4F6D, 0);
        ekle("a_alev", "Alev", "aura", "alev", 0xFF6A00, 0);
        ekle("a_buyu", "Büyü Harfleri", "aura", "buyu", 0xB28DFF, 0);
        ekle("a_nota", "Notalar", "aura", "nota", 0x4FC3FF, 0);
        ekle("a_kiraz", "Kiraz Yaprakları", "aura", "kiraz", 0xFFB7D5, 0);
        ekle("a_ruh", "Ruh Ateşi", "aura", "ruh", 0x40E0D0, 0);
        ekle("a_yildiz", "Yıldız Tozu", "aura", "yildiz", 0xFFFFFF, 0);
        ekle("a_kar", "Kar Taneleri", "aura", "kar", 0xE8FFFF, 0);
        ekle("a_hapi", "Hapi Işıltısı", "aura", "hapi", 0x2BD35A, 0);
        ekle("a_simsek", "Kıvılcım", "aura", "simsek", 0xFFF066, 0);
    }

    public static void kur() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((tur, cizer, yardimci, baglam) -> {
            if (cizer instanceof AvatarRenderer<?> oyuncuCizer) yardimci.register(new KozmetikKatmani(oyuncuCizer));
        });
    }

    public static Kozmetik bul(String kimlik) { return kimlik == null ? null : KATALOG.get(kimlik); }

    public static List<Kozmetik> yuvadakiler(String yuva) {
        List<Kozmetik> sonuc = new ArrayList<>();
        for (Kozmetik k : KATALOG.values()) if (k.yuva().equals(yuva)) sonuc.add(k);
        return sonuc;
    }

    public static boolean sahip(String kimlik) {
        return Ag.sahip.contains("*") || Ag.sahip.contains(kimlik);
    }

    /** Verilen oyuncunun verilen yuvadaki kozmetiği; yoksa null. */
    public static Kozmetik takili(UUID uuid, String yuva) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, String> secim = mc.player != null && mc.player.getUUID().equals(uuid)
                ? Ayarlar.A.kozmetik : DIGER.get(uuid);
        if (secim == null) return null;
        Kozmetik k = bul(secim.get(yuva));
        return k != null && k.yuva().equals(yuva) ? k : null;
    }

    public static Kozmetik takili(int varlikNo, String yuva) {
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

    public static void digeriniAyarla(UUID uuid, Map<String, String> secim) { DIGER.put(uuid, secim); }
    public static void digeriniSil(UUID uuid) { DIGER.remove(uuid); }
    public static void digerleriniTemizle() { DIGER.clear(); }

    /** Aura parçacıkları. */
    public static void tik(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        for (AbstractClientPlayer o : mc.level.players()) {
            if (o.isInvisible() || o.distanceToSqr(mc.player) > 48 * 48) continue;
            Kozmetik k = takili(o.getUUID(), "aura");
            if (k == null) continue;
            if (o == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            if (RASTGELE.nextInt(4) != 0) continue;
            SimpleParticleType tur = switch (k.bicim()) {
                case "kalp" -> ParticleTypes.HEART;
                case "alev" -> ParticleTypes.FLAME;
                case "buyu" -> ParticleTypes.ENCHANT;
                case "nota" -> ParticleTypes.NOTE;
                case "kiraz" -> ParticleTypes.CHERRY_LEAVES;
                case "ruh" -> ParticleTypes.SOUL_FIRE_FLAME;
                case "yildiz" -> ParticleTypes.END_ROD;
                case "kar" -> ParticleTypes.SNOWFLAKE;
                case "simsek" -> ParticleTypes.ELECTRIC_SPARK;
                default -> ParticleTypes.HAPPY_VILLAGER;
            };
            double aci = RASTGELE.nextDouble() * Math.PI * 2, r = 0.45 + RASTGELE.nextDouble() * 0.35;
            mc.level.addParticle(tur, o.getX() + Math.cos(aci) * r, o.getY() + 0.2 + RASTGELE.nextDouble() * 1.8,
                    o.getZ() + Math.sin(aci) * r, 0, 0.02, 0);
        }
    }
}
