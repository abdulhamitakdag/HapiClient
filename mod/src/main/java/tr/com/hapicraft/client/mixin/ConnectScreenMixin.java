package tr.com.hapicraft.client.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tr.com.hapicraft.client.HapiClient;
import tr.com.hapicraft.client.ekran.AnaMenu;

import java.util.Locale;

/** Sunucu kilidi: yalnız HapiCraft adreslerine bağlanılır. */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {
    @Inject(method = "startConnecting", at = @At("HEAD"), cancellable = true)
    private static void hapi$kilit(Screen onceki, Minecraft mc, ServerAddress adres, ServerData veri, boolean hizli,
                                   TransferState aktarim, CallbackInfo ci) {
        String ana = adres.getHost().toLowerCase(Locale.ROOT);
        boolean izinli = ana.equals("hapicraft.net") || ana.endsWith(".hapicraft.net")
                || ana.equals("hapicraft.com.tr") || ana.endsWith(".hapicraft.com.tr")
                || (FabricLoader.getInstance().isDevelopmentEnvironment() && (ana.equals("localhost") || ana.equals("127.0.0.1")));
        if (!izinli) {
            HapiClient.LOG.warn("Engellenen bağlantı: {}", ana);
            mc.setScreen(new AnaMenu());
            ci.cancel();
        }
    }
}
