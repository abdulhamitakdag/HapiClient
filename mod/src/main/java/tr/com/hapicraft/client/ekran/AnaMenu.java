package tr.com.hapicraft.client.ekran;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.HapiClient;
import tr.com.hapicraft.client.performans.Donanim;

/** HapiClient ana menüsü: tek hedef HapiCraft. */
public final class AnaMenu extends Screen {
    public AnaMenu() {
        super(Component.literal("HapiClient"));
    }

    public static void baglan(Screen kaynak) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        ConnectScreen.startConnecting(kaynak, mc, ServerAddress.parseString(HapiClient.SUNUCU_ADRESI),
                new ServerData("HapiCraft", HapiClient.SUNUCU_ADRESI, ServerData.Type.OTHER), false, null);
    }

    @Override
    protected void init() {
        if (Ayarlar.A.ilkAcilis) {
            Ayarlar.A.ilkAcilis = false;
            try { Donanim.tara().onerilen().uygula(minecraft); } catch (Throwable t) { HapiClient.LOG.warn("İlk profil uygulanamadı", t); }
            Ayarlar.kaydet();
        }
        int g = 220, x = (width - g) / 2, y = height / 2 - 10, yari = g / 2 - 2;
        dugme("§lOYNA", x, y, g, 28, () -> baglan(this)).vurgulu = true;
        dugme("Kozmetikler", x, y + 34, yari, 20, () -> minecraft.setScreen(new KozmetikEkrani(this)));
        dugme("HapiClient Ayarları", x + g / 2 + 2, y + 34, yari, 20, () -> minecraft.setScreen(new HapiMenu(this)));
        dugme("Oyun Seçenekleri", x, y + 58, yari, 20, () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
        dugme("Çıkış", x + g / 2 + 2, y + 58, yari, 20, minecraft::stop);
    }

    private HapiDugme dugme(String yazi, int x, int y, int w, int h, Runnable is) {
        return addRenderableWidget(new HapiDugme(x, y, w, h, Component.literal(yazi), b -> is.run()));
    }

    @Override
    public void renderBackground(GuiGraphics g, int fx, int fy, float kismi) {
        renderPanorama(g, kismi);
        g.fillGradient(0, 0, width, height, 0x70061A0D, 0xD0050806);
    }

    @Override
    public void render(GuiGraphics g, int fx, int fy, float kismi) {
        super.render(g, fx, fy, kismi);
        float olcek = 4f;
        String a = "Hapi", b = "Client";
        int toplam = font.width(a + b);
        g.pose().pushMatrix();
        g.pose().translate(width / 2f, height / 2f - 70);
        g.pose().scale(olcek, olcek);
        g.drawString(font, "§l" + a, -toplam / 2 - 3, 0, HapiClient.YESIL, true);
        g.drawString(font, "§l" + b, -toplam / 2 + font.width("§l" + a) - 3, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
        g.drawCenteredString(font, "HapiCraft'ın resmi istemcisi", width / 2, height / 2 - 30, 0xFFB6FFCB);
        g.drawString(font, "HapiClient " + HapiClient.SURUM + " · Minecraft 1.21.11", 4, height - 11, 0x90FFFFFF, false);
        String sag = HapiClient.SUNUCU_ADRESI;
        g.drawString(font, sag, width - font.width(sag) - 4, height - 11, 0x90FFFFFF, false);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
