package tr.com.hapicraft.client.ekran;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.ag.Ag;
import tr.com.hapicraft.client.hud.Hud;
import tr.com.hapicraft.client.hud.HudModul;
import tr.com.hapicraft.client.ozellik.Gorunum;
import tr.com.hapicraft.client.ozellik.Nisangah;
import tr.com.hapicraft.client.ozellik.Ozellikler;

import java.util.ArrayList;
import java.util.List;

/** Sağ Shift ile açılan ana ayar ekranı: solda bölümler ve kısayollar, sağda anahtarlı ayar satırları. */
public final class HapiMenu extends HapiEkran {
    /** kimlik boş değilse sunucu bu satırı kapatabilir. ipucu: tuş ya da kısa not (boş olabilir). */
    private record Satir(String kimlik, String ad, String ipucu, boolean varsayilan, boolean secenek) {}

    private static final String[] SEKMELER = {"Özellikler", "Göstergeler", "Görünüm"};
    private static final int GORUNUM = 2, SOL_G = 104, SATIR_Y = 22;
    private static int sekme = 0;
    private int sayfa = 0, sayfaSayisi = 1, sayfaX, sayfaY;

    public HapiMenu(Screen onceki) {
        super("Ayarlar", onceki);
    }

    private static List<Satir> satirlar() {
        List<Satir> s = new ArrayList<>();
        switch (sekme) {
            case 0 -> {
                s.add(new Satir("zoom", "Yakınlaştırma", "C + tekerlek", true, false));
                s.add(new Satir("serbestbakis", "360° Bakış", "Sol Alt", true, false));
                s.add(new Satir("tamparlaklik", "Tam Parlaklık", "G", false, false));
                s.add(new Satir("sessizsohbet", "Sessiz Sohbet", "N", false, false));
                s.add(new Satir("nisangah", "Özel Nişangah", "", false, false));
                s.add(new Satir("blokcercevesi", "Blok Çerçevesi", "", false, false));
                s.add(new Satir("misina", "Olta Misinası", "", false, false));
                s.add(new Satir("alcakates", "Alçak Ateş", "", false, false));
                s.add(new Satir("yagmurgizle", "Yağmuru Gizle", "", false, false));
                s.add(new Satir("skorgizle", "Skor Tablosunu Gizle", "", false, false));
            }
            case 1 -> {
                for (HudModul m : Hud.MODULLER) s.add(new Satir(m.kimlik, m.ad, "", m.varsayilanAcik, false));
            }
            default -> {
                for (Gorunum.Secenek g : Gorunum.SECENEKLER) s.add(new Satir(g.anahtar(), g.ad(), "", false, true));
            }
        }
        return s;
    }

    @Override
    protected void init() {
        int x = 8, y = 36;
        for (int i = 0; i < SEKMELER.length; i++, y += SATIR_Y) {
            int no = i;
            dugme(SEKMELER[i], x, y, SOL_G, () -> { sekme = no; sayfa = 0; rebuildWidgets(); }).secili = i == sekme;
        }
        // Kısayollar alta yaslanır; alçak ekranda sekmelerin hemen altından başlar.
        y = Math.max(y + 6, height - 8 - 4 * SATIR_Y + 2);
        dugme("HUD'u Düzenle", x, y, SOL_G, () -> minecraft.setScreen(new HudDuzenleyici(this)));
        dugme("Kozmetikler", x, y + SATIR_Y, SOL_G, () -> minecraft.setScreen(new KozmetikEkrani(this)));
        dugme("Danslar", x, y + SATIR_Y * 2, SOL_G, () -> { if (minecraft.player != null) minecraft.setScreen(new EmoteEkrani()); })
                .active = minecraft.player != null;
        dugme("Performans", x, y + SATIR_Y * 3, SOL_G, () -> minecraft.setScreen(new PerformansEkrani(this)));

        List<Satir> satirlar = satirlar();
        int x0 = x + SOL_G + 10, alanG = width - x0 - 8, y0 = 36, alt = height - 30;
        int sutun = alanG >= 340 ? 2 : 1, sutunG = (alanG - (sutun - 1) * 6) / sutun;
        int sayfaBoyu = Math.max(1, (alt - y0) / SATIR_Y) * sutun;
        sayfaSayisi = Math.max(1, (satirlar.size() + sayfaBoyu - 1) / sayfaBoyu);
        sayfa = Math.min(sayfa, sayfaSayisi - 1);
        for (int i = sayfa * sayfaBoyu, yer = 0; i < satirlar.size() && yer < sayfaBoyu; i++, yer++) {
            Satir s = satirlar.get(i);
            AyarSatiri b = new AyarSatiri(x0 + (yer % sutun) * (sutunG + 6), y0 + (yer / sutun) * SATIR_Y, sutunG, s);
            b.active = s.secenek || Ozellikler.izinli(s.kimlik);
            addRenderableWidget(b);
        }
        if (sayfaSayisi > 1) {
            sayfaX = x0 + alanG - 46;
            sayfaY = height - 28;
            dugme("‹", sayfaX - 46, sayfaY, 20, () -> { sayfa--; rebuildWidgets(); }).active = sayfa > 0;
            dugme("›", x0 + alanG - 20, sayfaY, 20, () -> { sayfa++; rebuildWidgets(); }).active = sayfa < sayfaSayisi - 1;
        }
    }

    /** Bir ayar satırı: solda ad ve tuş ipucu, sağda aç/kapa anahtarı ya da seçili değer. */
    private final class AyarSatiri extends HapiDugme {
        private final Satir s;

        AyarSatiri(int x, int y, int w, Satir s) {
            super(x, y, w, 20, Component.literal(s.ad), b -> degistir(s));
            this.s = s;
        }

        @Override
        protected void renderContents(GuiGraphics g, int fx, int fy, float kismi) {
            int x = getX(), y = getY(), w = getWidth(), sag = x + w - 7;
            boolean ustunde = active && isHoveredOrFocused();
            g.fill(x, y, x + w, y + 20, ustunde ? 0xE0203028 : 0xD0141A17);
            g.renderOutline(x, y, w, 20, ustunde ? 0xFF2BD35A : 0xFF303A34);
            g.drawString(font, s.ad + (s.ipucu.isEmpty() ? "" : " §8" + s.ipucu), x + 7, y + 6, active ? 0xFFFFFFFF : 0xFF6B746F, false);
            if (s.secenek) {
                String deger = "‹ " + Gorunum.degerAdi(s.kimlik) + " ›";
                g.drawString(font, deger, sag - font.width(deger), y + 6, 0xFF2BD35A, false);
            } else if (!active) {
                String kapali = "sunucuda kapalı";
                g.drawString(font, kapali, sag - font.width(kapali), y + 6, 0xFF6B746F, false);
            } else {
                // Anahtar: 20x10 ray, içinde 8x8 topuz.
                boolean acik = durum(s);
                int ax = sag - 20, ay = y + 5;
                g.fill(ax, ay, ax + 20, ay + 10, acik ? 0xFF1E7A3A : 0xFF2A302D);
                g.renderOutline(ax, ay, 20, 10, acik ? 0xFF2BD35A : 0xFF4A534E);
                int tx = acik ? ax + 11 : ax + 1;
                g.fill(tx, ay + 1, tx + 8, ay + 9, acik ? 0xFF2BD35A : 0xFF8A948E);
            }
        }
    }

    private boolean durum(Satir s) {
        return switch (s.kimlik) {
            case "tamparlaklik" -> Ayarlar.A.tamParlaklik;
            case "sessizsohbet" -> Ayarlar.A.sessiz;
            default -> Ayarlar.A.acik(s.kimlik, s.varsayilan);
        };
    }

    private void degistir(Satir s) {
        if (s.secenek) {
            Gorunum.ilerlet(s.kimlik);
        } else {
            boolean yeni = !durum(s);
            switch (s.kimlik) {
                case "tamparlaklik" -> Ayarlar.A.tamParlaklik = yeni;
                case "sessizsohbet" -> Ayarlar.A.sessiz = yeni;
                default -> Ayarlar.A.modul.put(s.kimlik, yeni);
            }
        }
        Ayarlar.kaydet();
    }

    @Override
    public void render(GuiGraphics g, int fx, int fy, float kismi) {
        super.render(g, fx, fy, kismi);
        if (Ag.tanindi) {
            String bilgi = "§aBağlı" + (Ag.sunucu.isEmpty() ? "" : " §7· §f" + Ag.sunucu)
                    + (Ag.indirim > 0 ? " §7· §eMağazada %" + Ag.indirim + " indirim" : "");
            g.drawString(font, bilgi, width - font.width(bilgi) - 10, 9, 0xFFFFFFFF, true);
        }
        // Ekran açıkken oyundaki nişangah görünmez; seçilen şekil başlık şeridinde önizlenir.
        if (sekme == GORUNUM) Nisangah.cizSekil(g, width / 2, 13);
        if (sayfaSayisi > 1) g.drawCenteredString(font, (sayfa + 1) + " / " + sayfaSayisi, sayfaX, sayfaY + 6, 0xFFB8C4BC);
    }
}
