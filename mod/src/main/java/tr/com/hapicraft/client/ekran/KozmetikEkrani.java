package tr.com.hapicraft.client.ekran;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.kozmetik.Kozmetik;
import tr.com.hapicraft.client.kozmetik.Kozmetikler;

import java.util.List;

/** Kozmetik seçimi; oyundayken sağda canlı önizleme. */
public final class KozmetikEkrani extends HapiEkran {
    private static int yuva = 0;

    public KozmetikEkrani(Screen onceki) {
        super("Kozmetikler", onceki);
    }

    @Override
    protected void init() {
        int sekmeG = Math.min(78, (width - 20) / Kozmetikler.YUVALAR.length - 3);
        for (int i = 0; i < Kozmetikler.YUVALAR.length; i++) {
            final int no = i;
            dugme((i == yuva ? "§a§l" : "") + Kozmetikler.YUVA_ADLARI[i], 10 + i * (sekmeG + 3), 34, sekmeG,
                    () -> { yuva = no; rebuildWidgets(); });
        }
        String yuvaAdi = Kozmetikler.YUVALAR[yuva];
        String secili = Ayarlar.A.kozmetik.getOrDefault(yuvaAdi, "");
        List<Kozmetik> liste = Kozmetikler.yuvadakiler(yuvaAdi);
        boolean onizleme = minecraft.player != null;
        int alan = onizleme ? width - 150 : width - 20;
        int dg = 118, sutun = Math.max(1, Math.min(4, alan / (dg + 4)));
        int y0 = 62;
        dugme((secili.isEmpty() ? "§a✔ " : "") + "Yok", 10, y0, dg, () -> { Kozmetikler.sec(yuvaAdi, ""); rebuildWidgets(); });
        for (int i = 0; i < liste.size(); i++) {
            Kozmetik k = liste.get(i);
            int sira = i + 1;
            boolean sahip = Kozmetikler.sahip(k.kimlik());
            String etiket = (k.kimlik().equals(secili) ? "§a✔ " : "") + (sahip ? "" : "§8") + k.ad() + (sahip ? "" : " §7(kilitli)");
            Button b = dugme(etiket, 10 + (sira % sutun) * (dg + 4), y0 + (sira / sutun) * 23, dg,
                    () -> { Kozmetikler.sec(yuvaAdi, k.kimlik()); rebuildWidgets(); });
            b.active = sahip;
        }
        dugme("Bitti", width / 2 - 50, height - 28, 100, this::onClose);
    }

    @Override
    public void render(GuiGraphics g, int fx, int fy, float kismi) {
        super.render(g, fx, fy, kismi);
        if (minecraft.player != null) {
            int x0 = width - 135, y0 = 40, x1 = width - 10, y1 = height - 36;
            g.fill(x0, y0, x1, y1, 0x70000000);
            g.renderOutline(x0, y0, x1 - x0, y1 - y0, 0xFF2BD35A);
            int olcek = Math.max(30, Math.min(80, (y1 - y0) / 3));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x0, y0, x1, y1, olcek, 0.0625f, fx, fy, minecraft.player);
        } else {
            g.drawCenteredString(font, "Önizleme oyuna girince görünür", width / 2, height - 44, 0xFFAAAAAA);
        }
    }
}
