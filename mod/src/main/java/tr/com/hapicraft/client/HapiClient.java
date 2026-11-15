package tr.com.hapicraft.client;

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
import tr.com.hapicraft.client.hud.Hud;
import tr.com.hapicraft.client.kozmetik.Kozmetikler;
import tr.com.hapicraft.client.ozellik.Ozellikler;
import tr.com.hapicraft.client.sohbet.SessizSohbet;

public final class HapiClient implements ClientModInitializer {
    public static final String KIMLIK = "hapiclient";
    public static final String SURUM = "0.1.0";
    public static final String SUNUCU_ADRESI = "hapicraft.net";
    public static final Logger LOG = LoggerFactory.getLogger("HapiClient");

    public static final int YESIL = 0xFF2BD35A;
    public static final int KOYU = 0xC0101512;

    public static KeyMapping tusMenu, tusEmote, tusZoom, tusSessiz, tusParlaklik;

    public static Identifier kimlik(String yol) {
        return Identifier.fromNamespaceAndPath(KIMLIK, yol);
    }

    @Override
    public void onInitializeClient() {
        Ayarlar.yukle();
        KeyMapping.Category kategori = KeyMapping.Category.register(kimlik("genel"));
        tusMenu = tus("menu", GLFW.GLFW_KEY_RIGHT_SHIFT, kategori);
        tusEmote = tus("emote", GLFW.GLFW_KEY_B, kategori);
        tusZoom = tus("zoom", GLFW.GLFW_KEY_C, kategori);
        tusSessiz = tus("sessiz", GLFW.GLFW_KEY_N, kategori);
        tusParlaklik = tus("parlaklik", GLFW.GLFW_KEY_G, kategori);

        Ag.kur();
        Hud.kur();
        Kozmetikler.kur();
        SessizSohbet.kur();

        ClientTickEvents.END_CLIENT_TICK.register(HapiClient::tik);
    }

    private static KeyMapping tus(String ad, int kod, KeyMapping.Category kategori) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.hapiclient." + ad, InputConstants.Type.KEYSYM, kod, kategori));
    }

    private static void tik(Minecraft mc) {
        while (tusMenu.consumeClick()) mc.setScreen(new HapiMenu(null));
        while (tusEmote.consumeClick()) if (mc.player != null) mc.setScreen(new EmoteEkrani());
        while (tusSessiz.consumeClick()) SessizSohbet.degistir();
        while (tusParlaklik.consumeClick()) Ozellikler.parlaklikDegistir();
        EmoteYoneticisi.tik(mc);
        Kozmetikler.tik(mc);
        Hud.tik(mc);
    }
}
