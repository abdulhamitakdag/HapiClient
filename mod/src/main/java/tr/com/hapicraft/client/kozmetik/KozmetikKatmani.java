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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import tr.com.hapicraft.client.HapiClient;

/** Şapka, hale ve kanatları oyuncu modelinin üstüne çizer. Pelerin vanilla katmanıyla çizilir. */
public final class KozmetikKatmani extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier BEYAZ = HapiClient.kimlik("textures/beyaz.png");
    private static final Identifier DISLI = HapiClient.kimlik("textures/kozmetik/disli.png");
    private static final int PARLAK = LightTexture.FULL_BRIGHT;
    /** Kanadın kökten uca bölündüğü eklem sayısı. */
    public static final int EKLEM = 8;

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
        float zaman = d.ageInTicks;

        if (sapka != null || hale != null) {
            RenderType duz = RenderTypes.entityCutoutNoCull(BEYAZ);
            ps.pushPose();
            model.root().translateAndRotate(ps);
            model.head.translateAndRotate(ps);
            if (sapka != null) VokselModel.al(sapka.model()).ciz(ps, toplayici, duz, isik, zaman);
            if (hale != null) VokselModel.al(hale.model()).ciz(ps, toplayici, duz, isik, zaman);
            ps.popPose();
        }
        if (kanat != null && !d.isFallFlying) kanatCiz(ps, toplayici, model, kanat, d, isik);
    }

    private static void kanatCiz(PoseStack ps, SubmitNodeCollector toplayici, PlayerModel model, Kozmetik k,
                                 AvatarRenderState d, int isik) {
        float[] aci = kanatAcilari(k.hareket(), d.ageInTicks, d.isCrouching, Math.min(1f, d.walkAnimationSpeed));
        Identifier doku = HapiClient.kimlik("textures/kozmetik/kanat/" + k.doku() + ".png");
        RenderType tur = k.saydam() ? RenderTypes.entityTranslucent(doku) : RenderTypes.entityCutoutNoCull(doku);
        RenderType parlak = k.isik()
                ? RenderTypes.entityCutoutNoCull(HapiClient.kimlik("textures/kozmetik/kanat/" + k.doku() + "_isik.png")) : null;
        RenderType disli = k.disliler().length > 0 ? RenderTypes.entityCutoutNoCull(DISLI) : null;
        float zaman = d.ageInTicks;
        for (int yon = -1; yon <= 1; yon += 2) {
            final int y = yon;
            ps.pushPose();
            model.root().translateAndRotate(ps);
            model.body.translateAndRotate(ps);
            ps.translate(yon * 1.4f / 16f, 3.2f / 16f, 2.05f / 16f);
            ps.mulPose(Axis.ZP.rotation(-yon * 0.08f));
            toplayici.submitCustomGeometry(ps, tur, (p, vc) -> kanatYaz(p, vc, k, y, aci, isik));
            if (parlak != null) toplayici.submitCustomGeometry(ps, parlak, (p, vc) -> kanatYaz(p, vc, k, y, aci, PARLAK));
            if (disli != null) toplayici.submitCustomGeometry(ps, disli, (p, vc) -> disliYaz(p, vc, k, y, aci, isik, zaman));
            ps.popPose();
        }
    }

    /**
     * Kanadın her ekleminin, sırt düzlemine göre geriye doğru açısı (radyan, kökten uca birikimli).
     * JS karşılığı: launcher/app/assets/js/hapi/kozmetik3b.js içindeki kanatAcilari.
     */
    public static float[] kanatAcilari(String hareket, float t, boolean egik, float yurume) {
        float[] a = new float[EKLEM];
        float hiz, acilma, genlik, bukum, bukumGenlik, gecikme;
        switch (hareket) {
            case "zar" -> { hiz = 0.13f; acilma = 0.62f; genlik = 0.34f; bukum = 0.05f; bukumGenlik = 0.075f; gecikme = 0.42f; }
            case "kelebek" -> { hiz = 0.21f; acilma = 0.80f; genlik = 0.55f; bukum = 0.0f; bukumGenlik = 0.012f; gecikme = 0.2f; }
            case "bocek" -> { hiz = 1.7f; acilma = 0.55f; genlik = 0.13f; bukum = 0.0f; bukumGenlik = 0.0f; gecikme = 0f; }
            case "mekanik" -> { hiz = 0.11f; acilma = 0.58f; genlik = 0.32f; bukum = 0.03f; bukumGenlik = 0.06f; gecikme = 0.5f; }
            case "kristal" -> { hiz = 0.06f; acilma = 0.50f; genlik = 0.14f; bukum = 0.02f; bukumGenlik = 0.03f; gecikme = 0.7f; }
            default -> { hiz = 0.14f; acilma = 0.56f; genlik = 0.30f; bukum = 0.035f; bukumGenlik = 0.06f; gecikme = 0.38f; }
        }
        hiz *= 1f + 0.8f * yurume;
        if (egik) { acilma += 0.28f; genlik *= 0.45f; }
        float faz = t * hiz;
        float toplam = acilma + genlik * dalga(hareket, faz);
        if (hareket.equals("bocek")) toplam += 0.1f * (float) Math.sin(t * 0.09f);
        for (int i = 0; i < EKLEM; i++) {
            if (i > 0) toplam += bukum + bukumGenlik * dalga(hareket, faz - gecikme - i * 0.3f);
            a[i] = toplam;
        }
        return a;
    }

    /** Mekanik kanat dişli gibi kademeli hareket eder; diğerleri yumuşak salınır. */
    private static float dalga(String hareket, float faz) {
        float s = (float) Math.sin(faz);
        return hareket.equals("mekanik") ? Math.round(s * 5f) / 5f : s;
    }

    /** Kanat menteşesi orijinde; yon=+1 oyuncunun soluna (model +x) uzanır. */
    private static void kanatYaz(PoseStack.Pose p, VertexConsumer vc, Kozmetik k, int yon, float[] aci, int isik) {
        float seg = k.en() / EKLEM / 16f;
        float ust = -k.kok() * k.boy() / 16f, alt = (1f - k.kok()) * k.boy() / 16f;
        float x = 0, z = 0;
        for (int i = 0; i < EKLEM; i++) {
            float c = (float) Math.cos(aci[i]), s = (float) Math.sin(aci[i]);
            float x2 = x + yon * c * seg, z2 = z + s * seg;
            float nx = -yon * s, nz = c;
            float u0 = i / (float) EKLEM, u1 = (i + 1) / (float) EKLEM;
            nokta(p, vc, x, ust, z, u0, 0, nx, nz, isik);
            nokta(p, vc, x2, ust, z2, u1, 0, nx, nz, isik);
            nokta(p, vc, x2, alt, z2, u1, 1, nx, nz, isik);
            nokta(p, vc, x, alt, z, u0, 1, nx, nz, isik);
            x = x2;
            z = z2;
        }
    }

    private static void disliYaz(PoseStack.Pose p, VertexConsumer vc, Kozmetik k, int yon, float[] aci, int isik, float zaman) {
        float seg = k.en() / EKLEM / 16f;
        float ust = -k.kok() * k.boy() / 16f;
        for (float[] d : k.disliler()) {
            int hedef = Math.min(EKLEM - 1, (int) (d[0] * EKLEM));
            float x = 0, z = 0;
            for (int i = 0; i < hedef; i++) {
                x += yon * (float) Math.cos(aci[i]) * seg;
                z += (float) Math.sin(aci[i]) * seg;
            }
            float c = (float) Math.cos(aci[hedef]), s = (float) Math.sin(aci[hedef]);
            float kalan = d[0] * EKLEM - hedef;
            float ax = yon * c, az = s, nx = -yon * s, nz = c;
            float mx = x + ax * seg * kalan, mz = z + az * seg * kalan, my = ust + d[1] * k.boy() / 16f;
            float r = d[2] / 16f;
            double f = Math.toRadians(d[3] * zaman) * yon;
            float co = (float) Math.cos(f) * r, si = (float) Math.sin(f) * r;
            // Köşeler: merkez ± (co·A + si·B) ± (−si·A + co·B); A kanat yönü, B aşağı.
            float[][] kose = {{-co + si, -si - co}, {co + si, si - co}, {co - si, si + co}, {-co - si, -si + co}};
            float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
            for (int yuz = -1; yuz <= 1; yuz += 2) {
                float ox = nx * yuz * 0.35f / 16f, oz = nz * yuz * 0.35f / 16f;
                for (int q = 0; q < 4; q++)
                    nokta(p, vc, mx + ax * kose[q][0] + ox, my + kose[q][1], mz + az * kose[q][0] + oz,
                            uv[q][0], uv[q][1], nx, nz, isik);
            }
        }
    }

    private static void nokta(PoseStack.Pose p, VertexConsumer vc, float x, float y, float z, float u, float v,
                              float nx, float nz, int isik) {
        vc.addVertex(p, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(isik).setNormal(p, nx, 0, nz);
    }
}
