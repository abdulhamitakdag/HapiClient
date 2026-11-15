package tr.com.hapicraft.client.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tr.com.hapicraft.client.hud.Hud;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void hapi$cps(long pencere, MouseButtonInfo bilgi, int eylem, CallbackInfo ci) {
        if (eylem == 1) Hud.tiklama(bilgi.button());
    }
}
