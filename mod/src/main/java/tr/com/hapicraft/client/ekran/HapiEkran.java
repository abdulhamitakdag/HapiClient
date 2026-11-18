package tr.com.hapicraft.client.ekran;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Ortak görünüm: koyu zemin, yeşil başlık şeridi. */
public abstract class HapiEkran extends Screen {
    protected final Screen onceki;

    protected HapiEkran(String baslik, Screen onceki) {
        super(Component.literal(baslik));
        this.onceki = onceki;
    }

    @Override
    public void renderBackground(GuiGraphics g, int fx, int fy, float kismi) {
        if (minecraft.level == null) renderPanorama(g, kismi);
        g.fill(0, 0, width, height, 0xB0080C0A);
        g.fill(0, 0, width, 26, 0xD0101512);
        g.fill(0, 26, width, 27, 0xFF2BD35A);
    }

    @Override
    public void render(GuiGraphics g, int fx, int fy, float kismi) {
        super.render(g, fx, fy, kismi);
        g.drawString(font, "§a§lHapi§f§lClient §8| §f" + title.getString(), 10, 9, 0xFFFFFFFF, true);
    }

    protected HapiDugme dugme(String yazi, int x, int y, int w, Runnable is) {
        return addRenderableWidget(new HapiDugme(x, y, w, 20, Component.literal(yazi), b -> is.run()));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(onceki);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
