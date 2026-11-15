package tr.com.hapicraft.client.kozmetik;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import tr.com.hapicraft.client.HapiClient;

/** Şapka, hale ve kanatları oyuncu modelinin üstüne çizer. Pelerin vanilla katmanıyla çizilir. */
public final class KozmetikKatmani extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier BEYAZ = HapiClient.kimlik("textures/beyaz.png");
    private static final int PARLAK = LightTexture.FULL_BRIGHT;

    public KozmetikKatmani(RenderLayerParent<AvatarRenderState, PlayerModel> ust) {
        super(ust);
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector toplayici, int isik, AvatarRenderState d, float yRot, float xRot) {
        if (d.isInvisible) return;
        Kozmetik sapka = Kozmetikler.takili(d.id, "sapka");
        Kozmetik hale = Kozmetikler.takili(d.id, "hale");
        Kozmetik kanat = Kozmetikler.takili(d.id, "kanat");
        if (sapka == null && hale == null && kanat == null) return;
        PlayerModel model = getParentModel();
        RenderType tur = RenderTypes.entityCutoutNoCull(BEYAZ);
        float zaman = d.ageInTicks;

        if (sapka != null || hale != null) {
            ps.pushPose();
            model.root().translateAndRotate(ps);
            model.head.translateAndRotate(ps);
            if (sapka != null) toplayici.submitCustomGeometry(ps, tur, (p, vc) -> sapkaCiz(p, vc, sapka, isik));
            if (hale != null) {
                ps.translate(0, -11.5f / 16f + (float) Math.sin(zaman * 0.08f) * 0.02f, 0);
                ps.mulPose(Axis.YP.rotationDegrees(zaman * 3f));
                toplayici.submitCustomGeometry(ps, tur, (p, vc) -> haleCiz(p, vc, hale, zaman));
            }
            ps.popPose();
        }
        if (kanat != null && !d.isFallFlying) {
            float cirpma = (float) Math.sin(zaman * (d.isCrouching ? 0.08f : 0.16f)) * 0.3f + 0.45f;
            for (int yon = -1; yon <= 1; yon += 2) {
                ps.pushPose();
                model.root().translateAndRotate(ps);
                model.body.translateAndRotate(ps);
                ps.translate(yon * 1.2f / 16f, 2.5f / 16f, 2.1f / 16f);
                ps.mulPose(Axis.YP.rotation(-yon * cirpma));
                final int y = yon;
                toplayici.submitCustomGeometry(ps, tur, (p, vc) -> kanatCiz(p, vc, kanat, y, isik));
                ps.popPose();
            }
        }
    }

    private static void sapkaCiz(PoseStack.Pose p, VertexConsumer vc, Kozmetik k, int isik) {
        int a = k.renk1(), b = k.renk2();
        switch (k.bicim()) {
            case "silindir" -> {
                Sekil.kutu(p, vc, -5.5f, -8.8f, -5.5f, 5.5f, -8f, 5.5f, a, isik);
                Sekil.kutu(p, vc, -3.6f, -15f, -3.6f, 3.6f, -8.8f, 3.6f, a, isik);
                Sekil.kutu(p, vc, -3.8f, -10.4f, -3.8f, 3.8f, -8.8f, 3.8f, b, isik);
            }
            case "tac" -> {
                Sekil.kutu(p, vc, -4.3f, -10f, -4.3f, 4.3f, -8f, 4.3f, a, isik);
                for (int i = 0; i < 4; i++) {
                    float x = (i % 2 == 0 ? -4.3f : 2.7f), z = (i < 2 ? -4.3f : 2.7f);
                    Sekil.kutu(p, vc, x, -12.5f, z, x + 1.6f, -10f, z + 1.6f, a, isik);
                }
                Sekil.kutu(p, vc, -0.8f, -12f, -4.3f, 0.8f, -10f, -3.5f, a, isik);
                Sekil.kutu(p, vc, -0.7f, -9.7f, -4.6f, 0.7f, -8.4f, -4.2f, b, PARLAK);
            }
            case "fes" -> {
                Sekil.kutu(p, vc, -3.4f, -13.5f, -3.4f, 3.4f, -8f, 3.4f, a, isik);
                Sekil.kutu(p, vc, -3.0f, -14f, -3.0f, 3.0f, -13.5f, 3.0f, a, isik);
                Sekil.kutu(p, vc, -0.4f, -14.6f, -0.4f, 0.4f, -14f, 0.4f, b, isik);
                Sekil.kutu(p, vc, 0.2f, -14.5f, -0.3f, 4.2f, -14.1f, 0.3f, b, isik);
                Sekil.kutu(p, vc, 3.6f, -14.3f, -0.4f, 4.4f, -10.5f, 0.4f, b, isik);
            }
            case "parti" -> {
                for (int i = 0; i < 6; i++) {
                    float r = 3.2f - i * 0.5f;
                    Sekil.kutu(p, vc, -r, -9.3f - i * 1.3f, -r, r, -8f - i * 1.3f, r, i % 2 == 0 ? a : b, isik);
                }
                Sekil.kutu(p, vc, -0.8f, -17f, -0.8f, 0.8f, -15.6f, 0.8f, 0xFFFFFFFF, isik);
            }
            case "hasir" -> {
                Sekil.kutu(p, vc, -7f, -8.7f, -7f, 7f, -8f, 7f, a, isik);
                Sekil.kutu(p, vc, -4f, -11.5f, -4f, 4f, -8.7f, 4f, a, isik);
                Sekil.kutu(p, vc, -4.2f, -9.8f, -4.2f, 4.2f, -8.7f, 4.2f, b, isik);
            }
            case "boynuz" -> {
                for (int s = -1; s <= 1; s += 2) {
                    Sekil.kutu(p, vc, s * 3f - 0.9f, -10f, -0.9f, s * 3f + 0.9f, -8f, 0.9f, a, isik);
                    Sekil.kutu(p, vc, s * 3.8f - 0.7f, -12f, -0.7f, s * 3.8f + 0.7f, -10f, 0.7f, a, isik);
                    Sekil.kutu(p, vc, s * 4.4f - 0.5f, -13.6f, -0.5f, s * 4.4f + 0.5f, -12f, 0.5f, b, isik);
                }
            }
            case "kedi" -> {
                for (int s = -1; s <= 1; s += 2) {
                    Sekil.kutu(p, vc, s * 2.8f - 1.5f, -10f, -0.5f, s * 2.8f + 1.5f, -8f, 0.5f, a, isik);
                    Sekil.kutu(p, vc, s * 2.8f - 0.9f, -11.3f, -0.5f, s * 2.8f + 0.9f, -10f, 0.5f, a, isik);
                    Sekil.kutu(p, vc, s * 2.8f - 0.7f, -9.8f, -0.7f, s * 2.8f + 0.7f, -8.3f, -0.5f, b, isik);
                }
            }
            case "tavsan" -> {
                for (int s = -1; s <= 1; s += 2) {
                    Sekil.kutu(p, vc, s * 2.2f - 1f, -15f, -0.5f, s * 2.2f + 1f, -8f, 0.5f, a, isik);
                    Sekil.kutu(p, vc, s * 2.2f - 0.5f, -14.2f, -0.7f, s * 2.2f + 0.5f, -9f, -0.5f, b, isik);
                }
            }
            case "asci" -> {
                Sekil.kutu(p, vc, -3.6f, -11.5f, -3.6f, 3.6f, -8f, 3.6f, b, isik);
                Sekil.kutu(p, vc, -4.6f, -15.5f, -4.6f, 4.6f, -11.5f, 4.6f, a, isik);
            }
            case "buyucu" -> {
                Sekil.kutu(p, vc, -6.5f, -8.7f, -6.5f, 6.5f, -8f, 6.5f, a, isik);
                for (int i = 0; i < 6; i++) {
                    float r = 3.6f - i * 0.58f;
                    Sekil.kutu(p, vc, -r + i * 0.2f, -10.5f - i * 1.6f, -r, r + i * 0.2f, -8.7f - i * 1.6f, r, a, isik);
                }
                Sekil.kutu(p, vc, -3.8f, -10f, -3.8f, 3.8f, -8.7f, 3.8f, b, isik);
            }
            case "kep" -> {
                Sekil.kutu(p, vc, -4.3f, -10.5f, -4.3f, 4.3f, -8f, 4.3f, a, isik);
                Sekil.kutu(p, vc, -3.6f, -11.3f, -3.6f, 3.6f, -10.5f, 3.6f, a, isik);
                Sekil.kutu(p, vc, -4.3f, -8.7f, -8f, 4.3f, -8f, -4.3f, b, isik);
                Sekil.kutu(p, vc, -1.2f, -10f, -4.5f, 1.2f, -8.9f, -4.3f, b, isik);
            }
            case "cicek" -> {
                Sekil.kutu(p, vc, -4.4f, -8.9f, -4.4f, 4.4f, -8f, 4.4f, a, isik);
                int[] renkler = {b, 0xFFFFE066, 0xFFFFFFFF, 0xFF9B7BFF};
                for (int i = 0; i < 8; i++) {
                    double aci = i * Math.PI / 4;
                    float x = (float) Math.cos(aci) * 4.2f, z = (float) Math.sin(aci) * 4.2f;
                    Sekil.kutu(p, vc, x - 0.8f, -10.2f, z - 0.8f, x + 0.8f, -8.9f, z + 0.8f, renkler[i % 4], isik);
                }
            }
            default -> { }
        }
    }

    private static void haleCiz(PoseStack.Pose p, VertexConsumer vc, Kozmetik k, float zaman) {
        boolean yorunge = k.bicim().equals("yorunge");
        int parca = yorunge ? 5 : 12;
        float yaricap = yorunge ? 6.5f : 4.2f;
        for (int i = 0; i < parca; i++) {
            double aci = i * 2 * Math.PI / parca;
            float x = (float) Math.cos(aci) * yaricap, z = (float) Math.sin(aci) * yaricap;
            int renk = k.bicim().equals("gokkusagi") ? Sekil.gokkusagi(i / (float) parca + zaman * 0.01f)
                    : Sekil.karistir(k.renk1(), k.renk2(), (i % 3) / 2f);
            float b = yorunge ? 0.6f : 0.95f;
            float dy = yorunge ? (float) Math.sin(zaman * 0.15f + i) * 0.8f : 0;
            Sekil.kutu(p, vc, x - b, -0.45f + dy, z - b, x + b, 0.45f + dy, z + b, renk, PARLAK);
        }
    }

    /** Kanat menteşesi orijinde; yon=+1 oyuncunun soluna (model +x) uzanır. */
    private static void kanatCiz(PoseStack.Pose p, VertexConsumer vc, Kozmetik k, int yon, int isik) {
        int a = k.renk1(), b = k.renk2();
        switch (k.bicim()) {
            case "yarasa" -> {
                Sekil.kutu(p, vc, 0, -1f, 0, yon * 11f, 0.2f, 0.7f, b, isik);
                float[] uzun = {9f, 7.5f, 9.5f, 6.5f, 8f};
                for (int i = 0; i < 5; i++) {
                    float x0 = yon * (i * 2.2f), x1 = yon * (i * 2.2f + 2.2f);
                    Sekil.kutu(p, vc, Math.min(x0, x1), 0.2f, 0.1f, Math.max(x0, x1), uzun[i], 0.5f,
                            Sekil.karistir(a, b, i / 5f), isik);
                }
                Sekil.kutu(p, vc, yon * 10.2f - 0.5f, -3f, 0, yon * 10.2f + 0.5f, -1f, 0.7f, b, isik);
            }
            case "kelebek" -> {
                kanatParca(p, vc, yon, 0.5f, -7f, 9f, 0.5f, a, isik);
                kanatParca(p, vc, yon, 2f, -5.5f, 7f, -1f, b, PARLAK);
                kanatParca(p, vc, yon, 0.5f, 1f, 7f, 7f, Sekil.karistir(a, b, 0.5f), isik);
                kanatParca(p, vc, yon, 1.8f, 2.3f, 5.2f, 5.5f, b, PARLAK);
            }
            default -> {
                for (int i = 0; i < 6; i++) {
                    float uzunluk = 13f - i * 1.7f;
                    float ust = -5f + i * 2.4f;
                    float x1 = yon * uzunluk;
                    Sekil.kutu(p, vc, Math.min(0, x1), ust, 0.05f * i, Math.max(0, x1), ust + 2.6f, 0.6f + 0.05f * i,
                            Sekil.karistir(a, b, i / 5f), isik);
                }
                float uc = yon * 13.5f;
                Sekil.kutu(p, vc, Math.min(uc, uc - yon * 2f), -7f, 0, Math.max(uc, uc - yon * 2f), -5f, 0.6f, a, isik);
            }
        }
    }

    private static void kanatParca(PoseStack.Pose p, VertexConsumer vc, int yon, float x0, float y0, float x1, float y1,
                                   int renk, int isik) {
        float ax = yon * x0, bx = yon * x1;
        Sekil.kutu(p, vc, Math.min(ax, bx), y0, 0, Math.max(ax, bx), y1, 0.5f, renk, isik);
    }
}
