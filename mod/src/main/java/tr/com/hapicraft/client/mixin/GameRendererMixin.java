package tr.com.hapicraft.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tr.com.hapicraft.client.ozellik.Ozellikler;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void hapi$zoom(CallbackInfoReturnable<Float> cir) {
        float carpan = Ozellikler.zoomCarpani();
        if (carpan != 1f) cir.setReturnValue(cir.getReturnValue() * carpan);
    }
}
