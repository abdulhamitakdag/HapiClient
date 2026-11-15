package tr.com.hapicraft.client.performans;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;
import tr.com.hapicraft.client.Ayarlar;

/** Tek tuşla uygulanan görüntü profilleri. */
public enum Profil {
    PATATES("Patates", "En yüksek FPS, en sade görüntü", 4, 5, GraphicsPreset.FAST, CloudStatus.OFF,
            ParticleStatus.MINIMAL, 0, 0, false, 0.5, false, 60),
    DUSUK("Düşük", "Zayıf bilgisayarlar için", 6, 5, GraphicsPreset.FAST, CloudStatus.OFF,
            ParticleStatus.DECREASED, 0, 1, false, 0.75, false, 120),
    DENGELI("Dengeli", "Görüntü ve akıcılık dengesi", 10, 8, GraphicsPreset.FAST, CloudStatus.FAST,
            ParticleStatus.DECREASED, 2, 4, true, 1.0, true, 144),
    YUKSEK("Yüksek", "Güçlü ekran kartları için", 14, 10, GraphicsPreset.FANCY, CloudStatus.FANCY,
            ParticleStatus.ALL, 3, 4, true, 1.0, true, 240),
    ULTRA("Ultra", "Her şey açık", 20, 12, GraphicsPreset.FANCY, CloudStatus.FANCY,
            ParticleStatus.ALL, 5, 4, true, 1.5, true, 260);

    public final String ad, aciklama;
    private final int goruş, simulasyon, biyom, mipmap, fps;
    private final GraphicsPreset grafik;
    private final CloudStatus bulut;
    private final ParticleStatus parcacik;
    private final boolean golge, yumusakIsik;
    private final double varlikMesafesi;

    Profil(String ad, String aciklama, int gorus, int simulasyon, GraphicsPreset grafik, CloudStatus bulut,
           ParticleStatus parcacik, int biyom, int mipmap, boolean golge, double varlikMesafesi,
           boolean yumusakIsik, int fps) {
        this.ad = ad; this.aciklama = aciklama; this.goruş = gorus; this.simulasyon = simulasyon;
        this.grafik = grafik; this.bulut = bulut; this.parcacik = parcacik; this.biyom = biyom;
        this.mipmap = mipmap; this.golge = golge; this.varlikMesafesi = varlikMesafesi;
        this.yumusakIsik = yumusakIsik; this.fps = fps;
    }

    public void uygula(Minecraft mc) {
        Options o = mc.options;
        o.applyGraphicsPreset(grafik);
        o.renderDistance().set(goruş);
        o.simulationDistance().set(simulasyon);
        o.cloudStatus().set(bulut);
        o.particles().set(parcacik);
        o.biomeBlendRadius().set(biyom);
        o.mipmapLevels().set(mipmap);
        o.entityShadows().set(golge);
        o.entityDistanceScaling().set(varlikMesafesi);
        o.ambientOcclusion().set(yumusakIsik);
        o.framerateLimit().set(fps);
        o.enableVsync().set(false);
        o.save();
        if (mc.levelRenderer != null && mc.level != null) mc.levelRenderer.allChanged();
        Ayarlar.A.profil = name();
        Ayarlar.kaydet();
    }
}
