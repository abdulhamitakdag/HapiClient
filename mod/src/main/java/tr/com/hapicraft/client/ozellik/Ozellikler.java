package tr.com.hapicraft.client.ozellik;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.HapiClient;
import tr.com.hapicraft.client.ag.Ag;

/** HUD dışı küçük özelliklerin ortak kapısı ve yakınlaştırma, tam parlaklık. Sunucu her birini uzaktan kapatabilir. */
public final class Ozellikler {
    private static final float ZOOM_VARSAYILAN = 4f;
    private static float zoomOrani = 1f, zoomSeviye = ZOOM_VARSAYILAN;
    private static long sonKare = System.nanoTime();

    private Ozellikler() {}

    public static boolean izinli(String kimlik) {
        return !Ag.kapali.contains(kimlik);
    }

    public static boolean acik(String kimlik, boolean varsayilan) {
        return izinli(kimlik) && Ayarlar.A.acik(kimlik, varsayilan);
    }

    /** Görüş açısı çarpanı; her karede yumuşakça hedefe yaklaşır. */
    public static float zoomCarpani() {
        long simdi = System.nanoTime();
        float dt = Math.min(0.1f, (simdi - sonKare) / 1_000_000_000f);
        sonKare = simdi;
        boolean basili = zoomBasili();
        if (!basili) zoomSeviye = ZOOM_VARSAYILAN;
        float hedef = basili ? 1f / zoomSeviye : 1f;
        zoomOrani += (hedef - zoomOrani) * Math.min(1f, dt * 14f);
        if (Math.abs(zoomOrani - hedef) < 0.002f) zoomOrani = hedef;
        return zoomOrani;
    }

    private static boolean zoomBasili() {
        return Minecraft.getInstance().screen == null && HapiClient.tusZoom.isDown() && acik("zoom", true);
    }

    /** Tekerleğin bir adımı: yukarı yaklaştırır, aşağı uzaklaştırır; 1,5x ile 16x arasında kalır. */
    public static float zoomSonraki(float seviye, double tekerlek) {
        return Math.max(1.5f, Math.min(16f, seviye * (tekerlek > 0 ? 1.25f : 0.8f)));
    }

    /** Fare tekerleği. true dönerse tekerlek yakınlaştırmaya gitmiştir, eşya çubuğu kaymaz. */
    public static boolean zoomKaydir(double tekerlek) {
        if (tekerlek == 0 || !zoomBasili()) return false;
        zoomSeviye = zoomSonraki(zoomSeviye, tekerlek);
        return true;
    }

    public static boolean tamParlaklik() {
        return Ayarlar.A.tamParlaklik && izinli("tamparlaklik");
    }

    public static void parlaklikDegistir() {
        Minecraft mc = Minecraft.getInstance();
        if (!izinli("tamparlaklik")) {
            bilgi("Tam parlaklık bu sunucuda kapalı.");
            return;
        }
        Ayarlar.A.tamParlaklik = !Ayarlar.A.tamParlaklik;
        Ayarlar.kaydet();
        bilgi("Tam parlaklık " + (Ayarlar.A.tamParlaklik ? "açık" : "kapalı"));
    }

    public static void bilgi(String metin) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(
                Component.literal("§a§lHapi§f§lClient §7» §f" + metin), true);
    }
}
