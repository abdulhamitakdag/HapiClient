package tr.com.hapicraft.client.ekran;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

/** Sağ Shift ile açılan ana ayar ekranı: Özellikler, Göstergeler ve Görünüm sekmeleri. */
public final class HapiMenu extends HapiEkran {
    /** kimlik boş değilse sunucu bu satırı kapatabilir. */
    private record Satir(String kimlik, String ad, boolean varsayilan, boolean secenek) {}

    private static final String[] SEKMELER = {"Özellikler", "Göstergeler", "Görünüm"};
    private static final int GORUNUM = 2;
    private static int sekme = 0;
    private int sayfa = 0;

    public HapiMenu(Screen onceki) {
        super("Ayarlar", onceki);
    }

    private static List<Satir> satirlar() {
        List<Satir> s = new ArrayList<>();
        switch (sekme) {
            case 0 -> {
                s.add(new Satir("zoom", "Yakınlaştırma (C + tekerlek)", true, false));
                s.add(new Satir("serbestbakis", "360° Bakış (Sol Alt)", true, false));
                s.add(new Satir("tamparlaklik", "Tam Parlaklık (G)", false, false));
                s.add(new Satir("sessizsohbet", "Sessiz Sohbet (N)", false, false));
                s.add(new Satir("nisangah", "Özel Nişangah", false, false));
                s.add(new Satir("blokcercevesi", "Blok Çerçevesi", false, false));
                s.add(new Satir("misina", "Olta Misinası", false, false));
                s.add(new Satir("alcakates", "Alçak Ateş", false, false));
                s.add(new Satir("yagmurgizle", "Yağmuru Gizle", false, false));
                s.add(new Satir("skorgizle", "Skor Tablosunu Gizle", false, false));
            }
            case 1 -> {
                for (HudModul m : Hud.MODULLER) s.add(new Satir(m.kimlik, m.ad, m.varsayilanAcik, false));
            }
            default -> {
                for (Gorunum.Secenek g : Gorunum.SECENEKLER) s.add(new Satir(g.anahtar(), g.ad(), false, true));
            }
        }
        return s;
    }

    @Override
    protected void init() {
        // Sol üstteki sayfa düğmelerine (55 px) yer kalsın diye dar ekranda sekmeler küçülür.
        int sekmeG = Math.min(100, (width - 116) / 3), sx = width / 2 - (sekmeG * 3 + 6) / 2;
        for (int i = 0; i < SEKMELER.length; i++) {
            int no = i;
            dugme(SEKMELER[i], sx + i * (sekmeG + 3), 32, sekmeG, () -> { sekme = no; sayfa = 0; rebuildWidgets(); })
                    .active = i != sekme;
        }

        List<Satir> satirlar = satirlar();
        int sutunG = Math.min(190, (width - 30) / 2), x0 = width / 2 - sutunG - 3, y0 = 58, alt = height - 28;
        int sayfaBoyu = Math.max(1, (alt - 6 - y0) / 22) * 2;
        int sayfaSayisi = (satirlar.size() + sayfaBoyu - 1) / sayfaBoyu;
        sayfa = Math.min(sayfa, sayfaSayisi - 1);
        for (int i = sayfa * sayfaBoyu, yer = 0; i < satirlar.size() && yer < sayfaBoyu; i++, yer++) {
            Satir s = satirlar.get(i);
            Button b = Button.builder(etiket(s), d -> { degistir(s); d.setMessage(etiket(s)); })
                    .bounds(x0 + (yer % 2) * (sutunG + 6), y0 + (yer / 2) * 22, sutunG, 20).build();
            b.active = s.secenek || Ozellikler.izinli(s.kimlik);
            addRenderableWidget(b);
        }
        if (sayfaSayisi > 1) {
            dugme("<", 6, 32, 20, () -> { sayfa--; rebuildWidgets(); }).active = sayfa > 0;
            dugme(">", 29, 32, 20, () -> { sayfa++; rebuildWidgets(); }).active = sayfa < sayfaSayisi - 1;
        }

        int g = Math.min(110, (width - 30) / 4);
        int bx = width / 2 - (g * 4 + 9) / 2;
        dugme("HUD'u Düzenle", bx, alt, g, () -> minecraft.setScreen(new HudDuzenleyici(this)));
        dugme("Kozmetikler", bx + g + 3, alt, g, () -> minecraft.setScreen(new KozmetikEkrani(this)));
        dugme("Danslar", bx + (g + 3) * 2, alt, g, () -> { if (minecraft.player != null) minecraft.setScreen(new EmoteEkrani()); })
                .active = minecraft.player != null;
        dugme("Performans", bx + (g + 3) * 3, alt, g, () -> minecraft.setScreen(new PerformansEkrani(this)));
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

    private Component etiket(Satir s) {
        if (s.secenek) return Component.literal(s.ad + ": §a" + Gorunum.degerAdi(s.kimlik));
        if (!Ozellikler.izinli(s.kimlik)) return Component.literal("§7" + s.ad + ": §8sunucuda kapalı");
        return Component.literal(s.ad + ": " + (durum(s) ? "§aAçık" : "§cKapalı"));
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
    }
}
