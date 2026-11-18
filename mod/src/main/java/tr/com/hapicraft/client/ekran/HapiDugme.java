package tr.com.hapicraft.client.ekran;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** HapiClient ekranlarının düz, koyu zeminli düğmesi. vurgulu = yeşil dolgulu ana eylem. */
public class HapiDugme extends Button {
    public boolean vurgulu;
    /** Seçili sekme gibi kalıcı işaret: sol kenarda yeşil şerit. */
    public boolean secili;

    public HapiDugme(int x, int y, int w, int h, Component yazi, OnPress is) {
        super(x, y, w, h, yazi, is, DEFAULT_NARRATION);
    }

    @Override
    protected void renderContents(GuiGraphics g, int fx, int fy, float kismi) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean ustunde = active && isHoveredOrFocused();
        int zemin = !active ? 0xA0101412 : vurgulu ? (ustunde ? 0xFF3BE56C : 0xFF2BD35A)
                : ustunde || secili ? 0xE0203028 : 0xD0141A17;
        int cerceve = !active ? 0xFF262C29 : ustunde || vurgulu ? 0xFF2BD35A : secili ? 0xFF1E7A3A : 0xFF303A34;
        g.fill(x, y, x + w, y + h, zemin);
        g.renderOutline(x, y, w, h, cerceve);
        if (secili) g.fill(x, y, x + 2, y + h, 0xFF2BD35A);
        int renk = !active ? 0xFF6B746F : vurgulu ? 0xFF06200E : 0xFFFFFFFF;
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + w / 2, y + (h - 8) / 2, renk);
    }
}
