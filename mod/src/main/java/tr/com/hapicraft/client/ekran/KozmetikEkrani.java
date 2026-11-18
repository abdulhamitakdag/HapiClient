package tr.com.hapicraft.client.ekran;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.HapiClient;
import tr.com.hapicraft.client.kozmetik.Kozmetik;
import tr.com.hapicraft.client.kozmetik.Kozmetikler;
import tr.com.hapicraft.client.kozmetik.Onizleme;

import java.util.ArrayList;
import java.util.List;

/**
 * Kozmetik seçimi: solda yuvalar, ortada ikonlu kartlar, sağda döndürülebilen 3B manken.
 * Karta gelince manken o kozmetiği dener, tıklayınca takılır. Ana menüde de (oyuna girmeden) çalışır.
 */
public final class KozmetikEkrani extends HapiEkran {
    private static final int KART_G = 58, KART_Y = 66, ARALIK = 4, UST = 36;
    private static int yuva = 0;

    private final List<Kozmetik> liste = new ArrayList<>();
    private int kaydirma;
    private float yaw = 20, hedefYaw = 20, pitch = 8, yakinlik = 1f;
    private boolean surukleniyor, elleDonduruldu;
    /** Fare altındaki kart: -1 yok, 0 "Yok" kartı, i+1 listedeki i. kozmetik. */
    private int ustundeki = -1;

    public KozmetikEkrani(Screen onceki) {
        super("Kozmetikler", onceki);
        hedefYaw = yaw = varsayilanYaw();
    }

    private static float varsayilanYaw() {
        // Pelerin ve kanat arkadan, diğerleri önden daha iyi görünür.
        return yuva <= 1 ? 205 : 20;
    }

    // ---- yerleşim
    private int panelG() { return Math.max(118, Math.min(230, (int) (width * 0.36f))); }
    private int panelX() { return width - panelG() - 8; }
    private int alt() { return height - 34; }
    private int izgaraX() { return 88; }
    private int izgaraG() { return panelX() - 8 - izgaraX(); }
    private int sutun() { return Math.max(1, (izgaraG() + ARALIK) / (KART_G + ARALIK)); }
    private int onizlemeAlt() { return alt() - 62; }

    @Override
    protected void init() {
        liste.clear();
        liste.addAll(Kozmetikler.yuvadakiler(Kozmetikler.YUVALAR[yuva]));
        for (int i = 0; i < Kozmetikler.YUVALAR.length; i++) {
            final int no = i;
            boolean takili = Ayarlar.A.kozmetik.containsKey(Kozmetikler.YUVALAR[i]);
            HapiDugme d = dugme(Kozmetikler.YUVA_ADLARI[i] + (takili ? " §a●" : ""), 8, UST + i * 24, 72, () -> {
                yuva = no;
                kaydirma = 0;
                elleDonduruldu = false;
                hedefYaw = varsayilanYaw();
                rebuildWidgets();
            });
            d.secili = i == yuva;
        }
        int y = height - 27;
        HapiDugme bitti = dugme("Bitti", width - 8 - 80, y, 80, this::onClose);
        bitti.vurgulu = true;
        dugme("Hepsini Çıkar", width - 8 - 80 - 4 - 92, y, 92, () -> { Kozmetikler.hepsiniCikar(); rebuildWidgets(); })
                .active = !Ayarlar.A.kozmetik.isEmpty();
        dugme("Çevir", panelX(), y, 50, () -> { elleDonduruldu = false; hedefYaw = hedefYaw > 110 ? 20 : 205; });
        kaydirma = Math.max(0, Math.min(kaydirma, azamiKaydirma()));
    }

    private int azamiKaydirma() {
        int satir = (liste.size() + 1 + sutun() - 1) / sutun();
        return Math.max(0, satir * (KART_Y + ARALIK) - ARALIK - (alt() - UST));
    }

    private int[] kartKonumu(int sira) {
        int s = sutun();
        return new int[]{izgaraX() + (sira % s) * (KART_G + ARALIK), UST + (sira / s) * (KART_Y + ARALIK) - kaydirma};
    }

    private int kartBul(double fx, double fy) {
        if (fx < izgaraX() || fx >= izgaraX() + izgaraG() || fy < UST || fy >= alt()) return -1;
        for (int i = 0; i <= liste.size(); i++) {
            int[] k = kartKonumu(i);
            if (fx >= k[0] && fx < k[0] + KART_G && fy >= k[1] && fy < k[1] + KART_Y) return i;
        }
        return -1;
    }

    private boolean onizlemede(double fx, double fy) {
        return fx >= panelX() && fx < panelX() + panelG() && fy >= UST && fy < onizlemeAlt();
    }

    // ---- çizim
    @Override
    public void render(GuiGraphics g, int fx, int fy, float kismi) {
        super.render(g, fx, fy, kismi);
        String yuvaAdi = Kozmetikler.YUVALAR[yuva];
        String secili = Ayarlar.A.kozmetik.getOrDefault(yuvaAdi, "");
        ustundeki = kartBul(fx, fy);
        Kozmetik denenen = ustundeki > 0 ? liste.get(ustundeki - 1) : null;
        Onizleme.ayarla(yuvaAdi, ustundeki < 0 ? null : denenen == null ? "" : denenen.kimlik());

        g.enableScissor(izgaraX(), UST, izgaraX() + izgaraG(), alt());
        for (int i = 0; i <= liste.size(); i++) {
            int[] k = kartKonumu(i);
            if (k[1] + KART_Y < UST || k[1] > alt()) continue;
            Kozmetik koz = i == 0 ? null : liste.get(i - 1);
            kartCiz(g, k[0], k[1], koz, koz == null ? secili.isEmpty() : koz.kimlik().equals(secili), i == ustundeki);
        }
        g.disableScissor();
        if (azamiKaydirma() > 0) {
            int yol = alt() - UST, boy = Math.max(12, yol * yol / (yol + azamiKaydirma()));
            int ky = UST + (yol - boy) * kaydirma / azamiKaydirma();
            g.fill(izgaraX() + izgaraG() + 2, UST, izgaraX() + izgaraG() + 4, alt(), 0x60000000);
            g.fill(izgaraX() + izgaraG() + 2, ky, izgaraX() + izgaraG() + 4, ky + boy, 0xFF2BD35A);
        }

        // Sağ panel: manken ve bilgi.
        int px = panelX(), pg = panelG(), oa = onizlemeAlt();
        g.fill(px, UST, px + pg, oa, 0x90060A08);
        g.fillGradient(px, oa - 40, px + pg, oa, 0x00143220, 0x70143220);
        g.renderOutline(px, UST, pg, oa - UST, 0xFF26402F);
        if (!surukleniyor) {
            if (!elleDonduruldu) yaw += (hedefYaw - yaw) * 0.12f;
        }
        float sallanti = surukleniyor || elleDonduruldu ? 0 : (float) Math.sin(System.nanoTime() / 1.4e9) * 10f;
        float olcek = Math.min((oa - UST) / 3.1f, pg / 2.7f) * yakinlik;
        g.enableScissor(px + 1, UST + 1, px + pg - 1, oa - 1);
        Onizleme.ciz(g, px + 1, UST + 1, px + pg - 1, oa - 1, olcek, yaw + sallanti, pitch);
        Kozmetik aura = Kozmetikler.takili(Onizleme.NO, "aura");
        if (aura != null) auraCiz(g, aura, px + pg / 2, UST + (oa - UST) / 2, olcek);
        g.disableScissor();
        g.drawString(font, "§8Sürükle: döndür · Tekerlek: yakınlaştır", px + 4, oa - 11, 0xFFFFFFFF, false);

        Kozmetik bilgi = denenen != null ? denenen : ustundeki == 0 ? null : Kozmetikler.bul(secili);
        int by = oa + 6;
        if (bilgi != null) {
            g.drawString(font, bilgi.ad(), px, by, bilgi.nadirlikRengi(), true);
            String durum = !Kozmetikler.sahip(bilgi.kimlik()) ? "§cKilitli" : bilgi.kimlik().equals(secili) ? "§aTakılı" : "§7Takmak için tıkla";
            g.drawString(font, "§7" + bilgi.nadirlikAdi() + " §8· " + durum, px, by + 11, 0xFFFFFFFF, false);
            g.drawWordWrap(font, Component.literal(bilgi.aciklama()), px, by + 24, pg, 0xFFB8C4BC);
        } else {
            g.drawString(font, ustundeki == 0 ? "Yok" : Kozmetikler.YUVA_ADLARI[yuva], px, by, 0xFFFFFFFF, true);
            g.drawWordWrap(font, Component.literal(ustundeki == 0 ? "Bu yuvadaki kozmetiği çıkarır."
                    : "Bir karta gel: mankende dene. Tıkla: tak."), px, by + 13, pg, 0xFFB8C4BC);
        }
        if (minecraft.player == null)
            g.drawString(font, "§8Seçimin kaydedilir; sunucuya girince herkes görür.", 8, height - 21, 0xFFFFFFFF, false);
    }

    private void kartCiz(GuiGraphics g, int x, int y, Kozmetik k, boolean secili, boolean ustunde) {
        boolean sahip = k == null || Kozmetikler.sahip(k.kimlik());
        g.fill(x, y, x + KART_G, y + KART_Y, ustunde ? 0xE0203028 : 0xD0141A17);
        g.fill(x + 1, y + 1, x + KART_G - 1, y + 3, k == null ? 0xFF3A443E : k.nadirlikRengi());
        g.renderOutline(x, y, KART_G, KART_Y, secili ? 0xFF2BD35A : ustunde ? 0xFF5A6A60 : 0xFF303A34);
        if (secili) g.renderOutline(x + 1, y + 1, KART_G - 2, KART_Y - 2, 0xFF2BD35A);
        int ix = x + (KART_G - 40) / 2, iy = y + 6;
        if (k == null) {
            // Çarpı: boş yuva.
            for (int i = 0; i < 16; i++) {
                g.fill(ix + 12 + i, iy + 12 + i, ix + 14 + i, iy + 14 + i, 0xFF6B746F);
                g.fill(ix + 26 - i, iy + 12 + i, ix + 28 - i, iy + 14 + i, 0xFF6B746F);
            }
        } else {
            g.blit(RenderPipelines.GUI_TEXTURED, HapiClient.kimlik("textures/kozmetik/ikon/" + k.kimlik() + ".png"),
                    ix, iy, 0f, 0f, 40, 40, 64, 64, 64, 64);
        }
        // Ad: küçük yazıyla, en çok iki satır.
        String ad = k == null ? "Yok" : k.ad();
        float o = 0.75f;
        int enCok = (int) ((KART_G - 4) / o);
        List<String> satirlar = new ArrayList<>();
        StringBuilder satir = new StringBuilder();
        for (String kelime : ad.split(" ")) {
            if (satir.length() > 0 && font.width(satir + " " + kelime) > enCok) {
                satirlar.add(satir.toString());
                satir = new StringBuilder();
            }
            if (satir.length() > 0) satir.append(' ');
            satir.append(kelime);
        }
        satirlar.add(satir.toString());
        g.pose().pushMatrix();
        g.pose().translate(x + KART_G / 2f, y + (satirlar.size() > 1 ? 48 : 51));
        g.pose().scale(o, o);
        for (int i = 0; i < Math.min(2, satirlar.size()); i++)
            g.drawCenteredString(font, satirlar.get(i), 0, i * 10, sahip ? 0xFFFFFFFF : 0xFF8A938E);
        g.pose().popMatrix();
        if (secili) g.drawString(font, "§a✔", x + KART_G - 11, y + 5, 0xFFFFFFFF, true);
        if (!sahip) {
            g.fill(x + 1, y + 3, x + KART_G - 1, y + KART_Y - 1, 0x90000000);
            g.drawCenteredString(font, "§7Kilitli", x + KART_G / 2, y + 22, 0xFFFFFFFF);
        }
    }

    /** Auranın desenini mankenin çevresinde küçük ışık noktalarıyla gösterir (dünyadaki parçacıkların taklidi). */
    private void auraCiz(GuiGraphics g, Kozmetik k, int mx, int my, float olcek) {
        int renk = switch (k.parcacik()) {
            case "kalp" -> 0xFFFF4F6D;
            case "alev" -> 0xFFFF8A1E;
            case "buyu" -> 0xFFB28DFF;
            case "nota" -> 0xFF4FC3FF;
            case "kiraz" -> 0xFFFFB7D5;
            case "ruh" -> 0xFF40E0D0;
            case "kar" -> 0xFFE8FFFF;
            case "simsek" -> 0xFFFFF066;
            case "hapi" -> 0xFF2BD35A;
            default -> 0xFFFFFFFF;
        };
        double t = System.nanoTime() / 5e7;
        // Manken yaklaşık 2 blok boyunda; ayak ve baş hizası ekran pikseli olarak.
        float ayak = my + olcek * 1.05f, bas = my - olcek * 0.95f, yari = olcek * 0.75f;
        for (int i = 0; i < 12; i++) {
            double faz = i * 1.7 + t * 0.05;
            float x, y;
            switch (k.desen()) {
                case "yorunge" -> {
                    double a = t * 0.22 + i * Math.PI;
                    double h = ((t * 0.035 + (i / 2) * 0.33) % 2.0) / 2.0;
                    x = mx + (float) Math.cos(a) * yari;
                    y = ayak + (bas - ayak) * (float) h;
                }
                case "yagan" -> {
                    double h = (t * 0.03 + i * 0.37) % 1.0;
                    x = mx + (float) Math.sin(i * 2.4) * yari;
                    y = bas - olcek * 0.3f + (ayak - bas + olcek * 0.3f) * (float) h;
                }
                case "halka" -> {
                    double a = t * 0.3 + i * Math.PI / 6;
                    x = mx + (float) Math.cos(a) * yari;
                    y = ayak - 2 + (float) Math.sin(a) * yari * 0.25f;
                }
                case "sacilan" -> {
                    double h = (faz * 3) % 1.0;
                    x = mx + (float) Math.sin(i * 3.1 + Math.floor(faz * 3)) * yari;
                    y = my + (float) Math.cos(i * 1.9 + Math.floor(faz * 3)) * olcek * 0.8f - (float) h * 6;
                }
                default -> {
                    double h = (t * 0.02 + i * 0.29) % 1.0;
                    x = mx + (float) Math.sin(i * 2.4) * yari * 0.8f;
                    y = k.parcacik().equals("kalp") ? bas - (float) h * olcek * 0.5f : ayak + (bas - ayak) * (float) h;
                }
            }
            int b = i % 3 == 0 ? 3 : 2;
            g.fill((int) x, (int) y, (int) x + b, (int) y + b, renk);
        }
    }

    // ---- fare
    @Override
    public boolean mouseClicked(MouseButtonEvent olay, boolean cift) {
        if (super.mouseClicked(olay, cift)) return true;
        if (olay.button() != 0) return false;
        int kart = kartBul(olay.x(), olay.y());
        if (kart >= 0) {
            Kozmetik k = kart == 0 ? null : liste.get(kart - 1);
            if (k != null && !Kozmetikler.sahip(k.kimlik())) return true;
            String yuvaAdi = Kozmetikler.YUVALAR[yuva];
            boolean zatenTakili = k != null && k.kimlik().equals(Ayarlar.A.kozmetik.get(yuvaAdi));
            // Takılı karta yeniden tıklamak çıkarır.
            Kozmetikler.sec(yuvaAdi, k == null || zatenTakili ? "" : k.kimlik());
            AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
            rebuildWidgets();
            return true;
        }
        if (onizlemede(olay.x(), olay.y())) {
            surukleniyor = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent olay, double dx, double dy) {
        if (surukleniyor) {
            elleDonduruldu = true;
            yaw += (float) dx * 1.2f;
            pitch = Math.max(-15, Math.min(45, pitch + (float) dy * 0.6f));
            return true;
        }
        return super.mouseDragged(olay, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent olay) {
        surukleniyor = false;
        return super.mouseReleased(olay);
    }

    @Override
    public boolean mouseScrolled(double fx, double fy, double yatay, double dikey) {
        if (onizlemede(fx, fy)) {
            yakinlik = Math.max(0.6f, Math.min(1.9f, yakinlik + (float) dikey * 0.1f));
            return true;
        }
        kaydirma = Math.max(0, Math.min(azamiKaydirma(), kaydirma - (int) (dikey * 24)));
        return true;
    }
}
