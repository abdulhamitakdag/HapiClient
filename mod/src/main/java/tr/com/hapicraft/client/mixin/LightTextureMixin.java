package tr.com.hapicraft.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tr.com.hapicraft.client.ozellik.Ozellikler;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    /** İkinci Double.floatValue çağrısı gamma ayarıdır (ilki karanlık efekti ölçeği). */
    @ModifyExpressionValue(method = "updateLightTexture",
            at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1))
    private float hapi$tamParlaklik(float gamma) {
        return Ozellikler.tamParlaklik() ? 12f : gamma;
    }
}
