package tr.com.hapicraft.client.performans;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.server.level.ParticleStatus;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.HapiClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dört oyun modu. Seçim <oyun dizini>/hapiclient/profil.json dosyasında durur; launcher da aynı
 * dosyayı okur ve mod listesini, gölgelendiriciyi ve öteki modların ayarlarını ona göre kurar.
 * Buradaki kısım oyunun kendi görüntü seçenekleridir ve anında uygulanır.
 */
public enum Profil {
    ULTRA_FPS("ultrafps", "Ultra FPS Boost", "Zayıf bilgisayarlar için en yüksek FPS",
            5, 5, GraphicsPreset.FAST, CloudStatus.OFF, 2, ParticleStatus.MINIMAL, 0, 0, false, 0.5,
            false, false, false, 3, 0.0, PrioritizeChunkUpdates.NONE, 0, TextureFilteringMethod.NONE, 260, 12, 16),
    FPS("fps", "FPS Boost", "Akıcı oyun, sade görüntü",
            7, 5, GraphicsPreset.FAST, CloudStatus.OFF, 2, ParticleStatus.DECREASED, 0, 1, false, 0.75,
            false, false, false, 5, 0.0, PrioritizeChunkUpdates.NONE, 0, TextureFilteringMethod.NONE, 260, 24, 32),
    NORMAL("normal", "Normal", "Görüntü ve akıcılık dengesi",
            10, 8, GraphicsPreset.FANCY, CloudStatus.FAST, 32, ParticleStatus.ALL, 2, 4, true, 1.0,
            true, true, true, 10, 0.75, PrioritizeChunkUpdates.PLAYER_AFFECTED, 5, TextureFilteringMethod.NONE, 240, 48, 96),
    KALITE("kalite", "Yüksek Kalite", "Gölgelendiricili en iyi görüntü, güçlü ekran kartı ister",
            12, 10, GraphicsPreset.FANCY, CloudStatus.FANCY, 64, ParticleStatus.ALL, 3, 4, true, 1.0,
            true, true, true, 10, 0.75, PrioritizeChunkUpdates.PLAYER_AFFECTED, 5, TextureFilteringMethod.NONE, 240, 48, 96);

    /** Launcher ile ortak kimlik (profil.json içindeki değer). */
    public final String kimlik;
    public final String ad, aciklama;
    /** Başka oyuncuların aurası ve takıları bu uzaklıktan (blok) sonra çizilmez. */
    public final int auraMesafesi, kozmetikMesafesi;
    private final int gorus, simulasyon, bulutMesafesi, biyom, mipmap, havaYaricapi, bulaniklik, fps;
    private final GraphicsPreset grafik;
    private final CloudStatus bulut;
    private final ParticleStatus parcacik;
    private final boolean golge, yumusakIsik, oyukYaprak, kenarKarartma;
    private final double varlikMesafesi, bolumBelirme;
    private final PrioritizeChunkUpdates bolumOnceligi;
    private final TextureFilteringMethod dokuSuzme;

    Profil(String kimlik, String ad, String aciklama, int gorus, int simulasyon, GraphicsPreset grafik,
           CloudStatus bulut, int bulutMesafesi, ParticleStatus parcacik, int biyom, int mipmap, boolean golge,
           double varlikMesafesi, boolean yumusakIsik, boolean oyukYaprak, boolean kenarKarartma, int havaYaricapi,
           double bolumBelirme, PrioritizeChunkUpdates bolumOnceligi, int bulaniklik,
           TextureFilteringMethod dokuSuzme, int fps, int auraMesafesi, int kozmetikMesafesi) {
        this.kimlik = kimlik; this.ad = ad; this.aciklama = aciklama; this.gorus = gorus;
        this.simulasyon = simulasyon; this.grafik = grafik; this.bulut = bulut; this.bulutMesafesi = bulutMesafesi;
        this.parcacik = parcacik; this.biyom = biyom; this.mipmap = mipmap; this.golge = golge;
        this.varlikMesafesi = varlikMesafesi; this.yumusakIsik = yumusakIsik; this.oyukYaprak = oyukYaprak;
        this.kenarKarartma = kenarKarartma; this.havaYaricapi = havaYaricapi; this.bolumBelirme = bolumBelirme;
        this.bolumOnceligi = bolumOnceligi; this.bulaniklik = bulaniklik; this.dokuSuzme = dokuSuzme;
        this.fps = fps; this.auraMesafesi = auraMesafesi; this.kozmetikMesafesi = kozmetikMesafesi;
    }

    /** Kimlikten ya da eski sürümlerin profil adından modu bulur; tanınmazsa null. */
    public static Profil bul(String deger) {
        if (deger == null) return null;
        for (Profil p : values()) if (p.kimlik.equalsIgnoreCase(deger) || p.name().equalsIgnoreCase(deger)) return p;
        return switch (deger.toUpperCase(java.util.Locale.ROOT)) {
            case "PATATES" -> ULTRA_FPS;
            case "DUSUK" -> FPS;
            case "DENGELI", "YUKSEK" -> NORMAL;
            case "ULTRA" -> KALITE;
            default -> null;
        };
    }

    /** Şu an geçerli mod; hiç seçilmemişse Normal. */
    public static Profil aktif() {
        Profil p = bul(Ayarlar.A.profil);
        return p == null ? NORMAL : p;
    }

    private static Path dosya() {
        return Ayarlar.dizin().resolve("profil.json");
    }

    /** profil.json içindeki seçim; dosya yoksa ya da okunamıyorsa null. */
    public static Profil secili() {
        try {
            Path d = dosya();
            if (!Files.exists(d)) return null;
            JsonObject j = JsonParser.parseString(Files.readString(d, StandardCharsets.UTF_8)).getAsJsonObject();
            return j.has("profil") ? bul(j.get("profil").getAsString()) : null;
        } catch (Throwable t) {
            HapiClient.LOG.warn("profil.json okunamadı", t);
            return null;
        }
    }

    private void seciliYaz() {
        try {
            Files.createDirectories(Ayarlar.dizin());
            JsonObject j = new JsonObject();
            j.addProperty("profil", kimlik);
            Files.writeString(dosya(), j + "\n", StandardCharsets.UTF_8);
        } catch (Throwable t) {
            HapiClient.LOG.warn("profil.json yazılamadı", t);
        }
    }

    /** Launcher'da (ya da ilk açılışta donanıma göre) seçilen mod henüz uygulanmadıysa uygular. */
    public static void baslangictaUygula(Minecraft mc) {
        Profil istenen = secili();
        if (istenen == null && bul(Ayarlar.A.profil) == null) istenen = Donanim.tara().onerilen();
        if (istenen != null && istenen != bul(Ayarlar.A.profil)) istenen.uygula(mc);
    }

    public void uygula(Minecraft mc) {
        Options o = mc.options;
        o.applyGraphicsPreset(grafik);
        o.renderDistance().set(gorus);
        o.simulationDistance().set(simulasyon);
        o.cloudStatus().set(bulut);
        o.cloudRange().set(bulutMesafesi);
        o.particles().set(parcacik);
        o.biomeBlendRadius().set(biyom);
        o.mipmapLevels().set(mipmap);
        o.entityShadows().set(golge);
        o.entityDistanceScaling().set(varlikMesafesi);
        o.ambientOcclusion().set(yumusakIsik);
        o.cutoutLeaves().set(oyukYaprak);
        o.vignette().set(kenarKarartma);
        o.improvedTransparency().set(false);
        o.weatherRadius().set(havaYaricapi);
        o.chunkSectionFadeInTime().set(bolumBelirme);
        o.prioritizeChunkUpdates().set(bolumOnceligi);
        o.menuBackgroundBlurriness().set(bulaniklik);
        o.textureFiltering().set(dokuSuzme);
        o.framerateLimit().set(fps);
        o.enableVsync().set(false);
        o.save();
        if (mc.levelRenderer != null && mc.level != null) mc.levelRenderer.allChanged();
        Ayarlar.A.profil = name();
        Ayarlar.kaydet();
        seciliYaz();
    }
}
