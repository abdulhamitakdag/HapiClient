package tr.com.hapicraft.client.ekran;

import net.minecraft.client.gui.GuiGraphics;
import tr.com.hapicraft.client.emote.Emote;
import tr.com.hapicraft.client.emote.EmoteYoneticisi;
import tr.com.hapicraft.client.emote.Emotlar;

import java.util.List;

/** B ile açılan dans seçici. Tıkla = oyna. */
public final class EmoteEkrani extends HapiEkran {
    private static int sayfa = 0;
    private int sayfaSayisi = 1;

    public EmoteEkrani() {
        super("Danslar", null);
    }

    @Override
    protected void init() {
        List<Emote> liste = Emotlar.liste();
        int dg = 104, sutun = Math.max(2, Math.min(6, (width - 20) / (dg + 4)));
        int satir = Math.max(2, (height - 80) / 23);
        int sayfaBoyu = sutun * satir;
        sayfaSayisi = Math.max(1, (liste.size() + sayfaBoyu - 1) / sayfaBoyu);
        sayfa = Math.min(sayfa, sayfaSayisi - 1);
        int x0 = (width - (sutun * (dg + 4) - 4)) / 2, y0 = 38;
        for (int i = 0; i < sayfaBoyu; i++) {
            int sira = sayfa * sayfaBoyu + i;
            if (sira >= liste.size()) break;
            Emote e = liste.get(sira);
            dugme(e.ad(), x0 + (i % sutun) * (dg + 4), y0 + (i / sutun) * 23, dg, () -> {
                EmoteYoneticisi.yerelOynat(e.kimlik());
                minecraft.setScreen(null);
            });
        }
        int alt = height - 28;
        dugme("‹", width / 2 - 100, alt, 30, () -> { sayfa = Math.max(0, sayfa - 1); rebuildWidgets(); }).active = sayfa > 0;
        dugme("Dansı Durdur", width / 2 - 60, alt, 120, () -> { EmoteYoneticisi.yerelDurdur(); minecraft.setScreen(null); });
        dugme("›", width / 2 + 70, alt, 30, () -> { sayfa = Math.min(sayfaSayisi - 1, sayfa + 1); rebuildWidgets(); })
                .active = sayfa < sayfaSayisi - 1;
    }

    @Override
    public void render(GuiGraphics g, int fx, int fy, float kismi) {
        super.render(g, fx, fy, kismi);
        g.drawCenteredString(font, "Sayfa " + (sayfa + 1) + " / " + sayfaSayisi + "  ·  hareket edince dans biter",
                width / 2, height - 42, 0xFFAAAAAA);
    }
}
