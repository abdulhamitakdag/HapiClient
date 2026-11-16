package tr.com.hapicraft.client.ozellik;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import tr.com.hapicraft.client.HapiClient;

/**
 * 360° bakış: tuş basılıyken kamera oyuncunun çevresinde döner, oyuncunun baktığı yön değişmez.
 * Sunucuya hiçbir şey gönderilmez; yalnız kameranın açısı değişir.
 */
public final class SerbestBakis {
    public static boolean etkin;
    public static float yaw, pitch;
    private static CameraType onceki;

    private SerbestBakis() {}

    public static void tik(Minecraft mc) {
        boolean istenen = mc.player != null && mc.screen == null && HapiClient.tusSerbest.isDown()
                && Ozellikler.acik("serbestbakis", true);
        if (istenen && !etkin) {
            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();
            onceki = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            etkin = true;
        } else if (!istenen && etkin) {
            etkin = false;
            mc.options.setCameraType(onceki);
        }
    }

    /** Fare hareketi. true dönerse hareket kameraya gitmiştir, oyuncu döndürülmez. */
    public static boolean cevir(double dx, double dy) {
        if (!etkin) return false;
        yaw += (float) dx * 0.15f;
        pitch = Mth.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
        return true;
    }
}
