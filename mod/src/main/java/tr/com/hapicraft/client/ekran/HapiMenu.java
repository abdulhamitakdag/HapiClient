package tr.com.hapicraft.client.ekran;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.ag.Ag;
import tr.com.hapicraft.client.hud.Hud;
import tr.com.hapicraft.client.hud.HudModul;
import tr.com.hapicraft.client.ozellik.Ozellikler;

import java.util.ArrayList;
import java.util.List;

/** Sağ Shift ile açılan ana ayar ekranı. */
public final class HapiMenu extends HapiEkran {
    private record Satir(String kimlik, String ad, boolean varsayilan) {}

    public HapiMenu(Screen onceki) {
        super("Ayarlar", onceki);
    }

    @Override
    protected void init() {
        List<Satir> satirlar = new ArrayList<>();
        satirlar.add(new Satir("zoom", "Yakınlaştırma (C)", true));
        satirlar.add(new Satir("tamparlaklik", "Tam Parlaklık (G)", false));
        satirlar.add(new Satir("sessizsohbet", "Sessiz Sohbet (N)", false));
        for (HudModul m : Hud.MODULLER) satirlar.add(new Satir(m.kimlik, m.ad, m.acik()));

        int sutunG = Math.min(180, (width - 30) / 2), x0 = width / 2 - sutunG - 3, y0 = 38;
        for (int i = 0; i < satirlar.size(); i++) {
            Satir s = satirlar.get(i);
            int x = x0 + (i % 2) * (sutunG + 6), y = y0 + (i / 2) * 23;
            Button b = Button.builder(etiket(s), d -> { degistir(s); d.setMessage(etiket(s)); }).bounds(x, y, sutunG, 20).build();
            b.active = Ozellikler.izinli(s.kimlik);
            addRenderableWidget(b);
        }
        int alt = height - 28, g = Math.min(110, (width - 30) / 4);
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
        boolean yeni = !durum(s);
        switch (s.kimlik) {
            case "tamparlaklik" -> Ayarlar.A.tamParlaklik = yeni;
            case "sessizsohbet" -> Ayarlar.A.sessiz = yeni;
            default -> Ayarlar.A.modul.put(s.kimlik, yeni);
        }
        Ayarlar.kaydet();
    }

    private Component etiket(Satir s) {
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
    }
}
