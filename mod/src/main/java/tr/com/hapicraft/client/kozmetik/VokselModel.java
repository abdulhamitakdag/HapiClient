package tr.com.hapicraft.client.kozmetik;

import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import tr.com.hapicraft.client.HapiClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Inflater;

/**
 * Küçük küplerden oluşan kozmetik modeli (şapka, hale). Dosya biçimi kozmetik/voksel.py içinde.
 * Koordinatlar kafa kemiği uzayında model pikselidir (1/16 blok, y aşağı).
 */
public final class VokselModel {
    private static final Map<String, VokselModel> ONBELLEK = new ConcurrentHashMap<>();
    private static final VokselModel BOS = new VokselModel(List.of());
    private static final int[][] YONLER = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /** JSON karşılığı. */
    static final class Ham {
        List<HamParca> parcalar;
    }

    static final class HamParca {
        String ad, veri;
        int olcek;
        int[] boyut;
        float[] koken, eksen;
        List<String> palet;
        boolean isik;
        Map<String, float[]> anim;
    }

    public static final class Parca {
        /** Dörtgen başına 12 konum (blok birimi) ve tek renk, tek normal. */
        final float[] konum;
        final int[] renk;
        final byte[] yon;
        final boolean isik;
        final float ex, ey, ez;
        final float[] don, sallan, sek, yorunge;

        Parca(HamParca h) {
            int nx = h.boyut[0], ny = h.boyut[1], nz = h.boyut[2];
            byte[] v = coz(h.veri, nx * ny * nz);
            int[] palet = new int[h.palet.size() + 1];
            for (int i = 0; i < h.palet.size(); i++) palet[i + 1] = 0xFF000000 | Integer.parseInt(h.palet.get(i), 16);
            List<float[]> k = new ArrayList<>();
            List<int[]> r = new ArrayList<>();
            int[] n = {nx, ny, nz};
            float adim = 1f / h.olcek / 16f;
            // Her yön için katman katman: aynı renkli açık yüzleri dikdörtgenlere birleştir.
            for (int y = 0; y < 6; y++) {
                int[] d = YONLER[y];
                int a = d[0] != 0 ? 0 : d[1] != 0 ? 1 : 2, b = (a + 1) % 3, c = (a + 2) % 3;
                int[] maske = new int[n[b] * n[c]];
                int[] p = new int[3];
                for (p[a] = 0; p[a] < n[a]; p[a]++) {
                    for (p[c] = 0; p[c] < n[c]; p[c]++) for (p[b] = 0; p[b] < n[b]; p[b]++) {
                        int kendi = v[p[0] + nx * (p[1] + ny * p[2])] & 255;
                        int kx = p[0] + d[0], ky = p[1] + d[1], kz = p[2] + d[2];
                        boolean kapali = kx >= 0 && ky >= 0 && kz >= 0 && kx < nx && ky < ny && kz < nz
                                && v[kx + nx * (ky + ny * kz)] != 0;
                        maske[p[b] + n[b] * p[c]] = kendi == 0 || kendi == 255 || kapali ? 0 : kendi;
                    }
                    for (int j = 0; j < n[c]; j++) for (int i = 0; i < n[b]; ) {
                        int renk = maske[i + n[b] * j];
                        if (renk == 0) { i++; continue; }
                        int g = 1;
                        while (i + g < n[b] && maske[i + g + n[b] * j] == renk) g++;
                        int yuk = 1;
                        dis:
                        for (; j + yuk < n[c]; yuk++)
                            for (int t = 0; t < g; t++) if (maske[i + t + n[b] * (j + yuk)] != renk) break dis;
                        for (int jj = 0; jj < yuk; jj++) for (int t = 0; t < g; t++) maske[i + t + n[b] * (j + jj)] = 0;
                        float[] q = new float[12];
                        int[][] kose = {{0, 0}, {g, 0}, {g, yuk}, {0, yuk}};
                        for (int s = 0; s < 4; s++) {
                            float[] ijk = new float[3];
                            ijk[a] = p[a] + (d[a] > 0 ? 1 : 0);
                            ijk[b] = i + kose[s][0];
                            ijk[c] = j + kose[s][1];
                            q[s * 3] = h.koken[0] / 16f + ijk[0] * adim;
                            q[s * 3 + 1] = h.koken[1] / 16f - ijk[1] * adim;
                            q[s * 3 + 2] = h.koken[2] / 16f + ijk[2] * adim;
                        }
                        k.add(q);
                        r.add(new int[]{palet[Math.min(renk, palet.length - 1)], y});
                        i += g;
                    }
                }
            }
            konum = new float[k.size() * 12];
            renk = new int[k.size()];
            yon = new byte[k.size()];
            for (int i = 0; i < k.size(); i++) {
                System.arraycopy(k.get(i), 0, konum, i * 12, 12);
                renk[i] = r.get(i)[0];
                yon[i] = (byte) r.get(i)[1];
            }
            isik = h.isik;
            ex = h.eksen[0] / 16f;
            ey = h.eksen[1] / 16f;
            ez = h.eksen[2] / 16f;
            don = h.anim == null ? null : h.anim.get("don");
            sallan = h.anim == null ? null : h.anim.get("sallan");
            sek = h.anim == null ? null : h.anim.get("sek");
            yorunge = h.anim == null ? null : h.anim.get("yorunge");
        }

        public int dortgenSayisi() { return renk.length; }

        /** Parçanın canlandırma dönüşümünü yığına uygular. JS karşılığı: launcher/.../kozmetik3b.js */
        void donustur(PoseStack ps, float t) {
            if (don == null && sallan == null && sek == null && yorunge == null) return;
            ps.translate(ex, ey, ez);
            if (yorunge != null) {
                ps.mulPose(Axis.YP.rotationDegrees(yorunge[1] * t + yorunge[2]));
                ps.translate(yorunge[0] / 16f, 0, 0);
            }
            if (sek != null) ps.translate(0, (float) Math.sin(t * sek[1] + sek[2]) * sek[0] / 16f, 0);
            if (don != null) {
                if (don[0] != 0) ps.mulPose(Axis.XP.rotationDegrees(don[0] * t));
                if (don[1] != 0) ps.mulPose(Axis.YP.rotationDegrees(don[1] * t));
                if (don[2] != 0) ps.mulPose(Axis.ZP.rotationDegrees(don[2] * t));
            }
            if (sallan != null) {
                float aci = (float) Math.sin(t * sallan[2] + sallan[3]) * sallan[1];
                Axis eksen = sallan[0] == 0 ? Axis.XP : sallan[0] == 1 ? Axis.YP : Axis.ZP;
                ps.mulPose(eksen.rotationDegrees(aci));
            }
            ps.translate(-ex, -ey, -ez);
        }

        void yaz(PoseStack.Pose p, VertexConsumer vc, int isikDegeri) {
            int l = isik ? LightTexture.FULL_BRIGHT : isikDegeri;
            for (int i = 0; i < renk.length; i++) {
                int[] d = YONLER[yon[i]];
                // Model y ekseni aşağı baktığı için voksel "yukarı" yönü -y'dir.
                float nx = d[0], ny = -d[1], nz = d[2];
                int c = renk[i], o = i * 12;
                for (int s = 0; s < 4; s++)
                    vc.addVertex(p, konum[o + s * 3], konum[o + s * 3 + 1], konum[o + s * 3 + 2]).setColor(c)
                            .setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(l).setNormal(p, nx, ny, nz);
            }
        }
    }

    private final List<Parca> parcalar;

    private VokselModel(List<Parca> parcalar) { this.parcalar = parcalar; }

    public List<Parca> parcalar() { return parcalar; }

    public static VokselModel al(String ad) {
        return ad == null ? BOS : ONBELLEK.computeIfAbsent(ad, VokselModel::yukle);
    }

    private static VokselModel yukle(String ad) {
        try (InputStream in = VokselModel.class.getResourceAsStream("/assets/hapiclient/kozmetik/model/" + ad + ".json")) {
            if (in == null) throw new IllegalStateException("dosya yok");
            Ham ham = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Ham.class);
            List<Parca> liste = new ArrayList<>();
            for (HamParca h : ham.parcalar) liste.add(new Parca(h));
            return new VokselModel(List.copyOf(liste));
        } catch (Exception e) {
            HapiClient.LOG.warn("Kozmetik modeli yüklenemedi: {}", ad, e);
            return BOS;
        }
    }

    private static byte[] coz(String b64, int boyut) {
        try {
            Inflater ac = new Inflater();
            ac.setInput(Base64.getDecoder().decode(b64));
            byte[] cikti = new byte[boyut];
            int okunan = 0;
            while (okunan < boyut && !ac.finished()) {
                int n = ac.inflate(cikti, okunan, boyut - okunan);
                if (n == 0 && (ac.needsInput() || ac.needsDictionary())) break;
                okunan += n;
            }
            ac.end();
            if (okunan != boyut) throw new IllegalStateException("eksik voksel verisi");
            return cikti;
        } catch (java.util.zip.DataFormatException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Modeli, yığının o anki (kafa kemiği) dönüşümüyle çizer. */
    public void ciz(PoseStack ps, SubmitNodeCollector toplayici, RenderType tur, int isik, float zaman) {
        for (Parca p : parcalar) {
            ps.pushPose();
            p.donustur(ps, zaman);
            toplayici.submitCustomGeometry(ps, tur, (poz, vc) -> p.yaz(poz, vc, isik));
            ps.popPose();
        }
    }
}
