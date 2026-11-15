package tr.com.hapicraft.client.ekran;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.hud.Hud;
import tr.com.hapicraft.client.hud.HudModul;

/** HUD modüllerini fareyle sürükleyerek yerleştirme ekranı. */
public final class HudDuzenleyici extends Screen {
    private final Screen onceki;
    private HudModul tutulan;
    private int farkX, farkY;

    public HudDuzenleyici(Screen onceki) {
        super(Component.literal("HUD Düzenle"));
        this.onceki = onceki;
    }

    @Override
    protected void init() {
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Konumları Sıfırla"), b -> {
            for (HudModul m : Hud.MODULLER) m.varsayilanKonum();
        }).bounds(width / 2 - 122, height / 2 - 10, 120, 20).build());
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Bitti"), b -> onClose())
                .bounds(width / 2 + 2, height / 2 - 10, 120, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int fx, int fy, float kismi) {
        g.fill(0, 0, width, height, 0x60000000);
    }

    @Override
    public void render(GuiGraphics g, int fx, int fy, float kismi) {
        super.render(g, fx, fy, kismi);
        g.drawCenteredString(font, "Göstergeleri sürükleyerek yerleştir", width / 2, height / 2 - 26, 0xFFFFFFFF);
        for (HudModul m : Hud.MODULLER) {
            if (!m.acik()) continue;
            int x = m.x(width), y = m.y(height);
            m.ciz(g, minecraft, x, y, true);
            boolean uzerinde = fx >= x && fx < x + m.genislik && fy >= y && fy < y + m.yukseklik;
            g.renderOutline(x - 1, y - 1, m.genislik + 2, m.yukseklik + 2, m == tutulan || uzerinde ? 0xFF2BD35A : 0x60FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent olay, boolean cift) {
        if (super.mouseClicked(olay, cift)) return true;
        for (HudModul m : Hud.MODULLER) {
            if (!m.acik()) continue;
            int x = m.x(width), y = m.y(height);
            if (olay.x() >= x && olay.x() < x + m.genislik && olay.y() >= y && olay.y() < y + m.yukseklik) {
                tutulan = m; farkX = (int) olay.x() - x; farkY = (int) olay.y() - y;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent olay, double dx, double dy) {
        if (tutulan != null) {
            tutulan.tasi((int) olay.x() - farkX, (int) olay.y() - farkY, width, height);
            return true;
        }
        return super.mouseDragged(olay, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent olay) {
        tutulan = null;
        return super.mouseReleased(olay);
    }

    @Override
    public void onClose() {
        Ayarlar.kaydet();
        minecraft.setScreen(onceki);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
