package tr.com.hapicraft.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** Fabric yükleyicisi altında bütün mixin hedeflerini yükler; enjeksiyon noktası bozuksa burada patlar. */
class MixinTest {
    private static final String[] HEDEFLER = {
            "net.minecraft.client.Minecraft",
            "net.minecraft.client.MouseHandler",
            "net.minecraft.client.renderer.GameRenderer",
            "net.minecraft.client.renderer.LightTexture",
            "net.minecraft.client.model.HumanoidModel",
            "net.minecraft.client.renderer.entity.player.AvatarRenderer",
            "net.minecraft.client.gui.screens.ConnectScreen",
    };

    @Test
    void butunMixinlerUygulanir() {
        for (String hedef : HEDEFLER) {
            assertDoesNotThrow(() -> Class.forName(hedef, false, MixinTest.class.getClassLoader()), hedef);
        }
    }
}
