package tr.com.hapicraft.client.kozmetik;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Düz renkli kutu çizimi. Koordinatlar model pikseli (1/16 blok). */
public final class Sekil {
    private Sekil() {}

    public static void kutu(PoseStack.Pose p, VertexConsumer vc, float x0, float y0, float z0,
                            float x1, float y1, float z1, int renk, int isik) {
        x0 /= 16f; y0 /= 16f; z0 /= 16f; x1 /= 16f; y1 /= 16f; z1 /= 16f;
        yuz(p, vc, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1, renk, isik);
        yuz(p, vc, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1, renk, isik);
        yuz(p, vc, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0, renk, isik);
        yuz(p, vc, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0, renk, isik);
        yuz(p, vc, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0, renk, isik);
        yuz(p, vc, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, renk, isik);
    }

    private static void yuz(PoseStack.Pose p, VertexConsumer vc,
                            float ax, float ay, float az, float bx, float by, float bz,
                            float cx, float cy, float cz, float dx, float dy, float dz,
                            float nx, float ny, float nz, int renk, int isik) {
        nokta(p, vc, ax, ay, az, 0, 0, nx, ny, nz, renk, isik);
        nokta(p, vc, bx, by, bz, 1, 0, nx, ny, nz, renk, isik);
        nokta(p, vc, cx, cy, cz, 1, 1, nx, ny, nz, renk, isik);
        nokta(p, vc, dx, dy, dz, 0, 1, nx, ny, nz, renk, isik);
    }

    private static void nokta(PoseStack.Pose p, VertexConsumer vc, float x, float y, float z, float u, float v,
                              float nx, float ny, float nz, int renk, int isik) {
        vc.addVertex(p, x, y, z).setColor(renk).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(isik).setNormal(p, nx, ny, nz);
    }

    public static int karistir(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    public static int gokkusagi(float t) {
        return 0xFF000000 | java.awt.Color.HSBtoRGB(t - (float) Math.floor(t), 0.75f, 1f);
    }
}
