package tr.com.hapicraft.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tr.com.hapicraft.client.ozellik.Ozellikler;

/**
 * Yağmuru gizle: yalnız çizim ve damla sesi/parçacığı kapanır. Dünyanın yağmur durumuna
 * dokunulmaz; oynanışı etkileyen denetimler (ör. yağmurda mızrak) aynı kalır.
 */
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
    @ModifyExpressionValue(method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getRainLevel(F)F"))
    private float hapi$yagmurCizimi(float asil) {
        return Ozellikler.acik("yagmurgizle", false) ? 0f : asil;
    }

    @ModifyExpressionValue(method = "tickRainParticles",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private float hapi$yagmurParcacigi(float asil) {
        return Ozellikler.acik("yagmurgizle", false) ? 0f : asil;
    }
}
