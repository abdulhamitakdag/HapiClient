package tr.com.hapicraft.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tr.com.hapicraft.client.HapiClient;
import tr.com.hapicraft.client.ekran.AnaMenu;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** Vanilla ana menü ve sunucu listesi yerine HapiClient ana menüsü. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen hapi$ekranDegistir(Screen ekran) {
        if (ekran instanceof TitleScreen || ekran instanceof JoinMultiplayerScreen
                || (ekran != null && ekran.getClass().getName().startsWith("com.mojang.realmsclient."))) {
            return new AnaMenu();
        }
        return ekran;
    }

    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
    private void hapi$baslik(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("HapiClient " + HapiClient.SURUM + " — HapiCraft");
    }
}
