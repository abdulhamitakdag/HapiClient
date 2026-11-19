package tr.com.hapicraft.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tr.com.hapicraft.client.ekran.HapiDugme;
import tr.com.hapicraft.client.ekran.KozmetikEkrani;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.hapicraft.client.ag.Ag;
import tr.com.hapicraft.client.ekran.EmoteEkrani;
import tr.com.hapicraft.client.ekran.HapiMenu;
import tr.com.hapicraft.client.emote.EmoteYoneticisi;
import tr.com.hapicraft.client.giris.OtoGiris;
import tr.com.hapicraft.client.hud.Hud;
import tr.com.hapicraft.client.kozmetik.Aura;
import tr.com.hapicraft.client.kozmetik.Kozmetikler;
import tr.com.hapicraft.client.ozellik.Ozellikler;
import tr.com.hapicraft.client.ozellik.SerbestBakis;
import tr.com.hapicraft.client.sohbet.SessizSohbet;

public final class HapiClient implements ClientModInitializer {
    public static final String KIMLIK = "hapiclient";
    public static final String SURUM = "0.4.1";
    public static final String SUNUCU_ADRESI = "hapicraft.net";
    public static final Logger LOG = LoggerFactory.getLogger("HapiClient");

    public static final int YESIL = 0xFF2BD35A;
    public static final int KOYU = 0xC0101512;

    public static KeyMapping tusMenu, tusKozmetik, tusEmote, tusZoom, tusSessiz, tusParlaklik, tusSerbest;

    public static Identifier kimlik(String yol) {
        return Identifier.fromNamespaceAndPath(KIMLIK, yol);
    }

    @Override
    public void onInitializeClient() {
        Ayarlar.yukle();
        KeyMapping.Category kategori = KeyMapping.Category.register(kimlik("genel"));
        tusMenu = tus("menu", GLFW.GLFW_KEY_RIGHT_SHIFT, kategori);
        tusKozmetik = tus("kozmetik", GLFW.GLFW_KEY_K, kategori);
        tusEmote = tus("emote", GLFW.GLFW_KEY_B, kategori);
        tusZoom = tus("zoom", GLFW.GLFW_KEY_C, kategori);
        tusSessiz = tus("sessiz", GLFW.GLFW_KEY_N, kategori);
        tusParlaklik = tus("parlaklik", GLFW.GLFW_KEY_G, kategori);
        tusSerbest = tus("serbest", GLFW.GLFW_KEY_LEFT_ALT, kategori);

        Ag.kur();
        Hud.kur();
        Kozmetikler.kur();
        SessizSohbet.kur();
        OtoGiris.kur();

        ClientTickEvents.END_CLIENT_TICK.register(HapiClient::tik);
        ScreenEvents.AFTER_INIT.register(HapiClient::duraklatmaDugmeleri);
    }

    private static KeyMapping tus(String ad, int kod, KeyMapping.Category kategori) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.hapiclient." + ad, InputConstants.Type.KEYSYM, kod, kategori));
    }

    /** ESC menüsünün sağ üst köşesine kozmetik ve ayar kısayolları. */
    private static void duraklatmaDugmeleri(Minecraft mc, Screen ekran, int w, int h) {
        // F3+Esc ile açılan düğmesiz duraklatmada bir şey eklenmez.
        if (!(ekran instanceof PauseScreen) || Screens.getButtons(ekran).isEmpty()) return;
        HapiDugme kozmetik = new HapiDugme(w - 118, 8, 110, 20, Component.literal("Kozmetikler"),
                b -> mc.setScreen(new KozmetikEkrani(ekran)));
        kozmetik.vurgulu = true;
        Screens.getButtons(ekran).add(kozmetik);
        Screens.getButtons(ekran).add(new HapiDugme(w - 118, 31, 110, 20, Component.literal("HapiClient Ayarları"),
                b -> mc.setScreen(new HapiMenu(ekran))));
    }

    private static void tik(Minecraft mc) {
        while (tusMenu.consumeClick()) mc.setScreen(new HapiMenu(null));
        while (tusKozmetik.consumeClick()) mc.setScreen(new KozmetikEkrani(null));
        while (tusEmote.consumeClick()) if (mc.player != null) mc.setScreen(new EmoteEkrani());
        while (tusSessiz.consumeClick()) SessizSohbet.degistir();
        while (tusParlaklik.consumeClick()) Ozellikler.parlaklikDegistir();
        SerbestBakis.tik(mc);
        EmoteYoneticisi.tik(mc);
        Aura.tik(mc);
        Hud.tik(mc);
    }
}
