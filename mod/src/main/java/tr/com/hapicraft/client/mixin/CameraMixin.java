package tr.com.hapicraft.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tr.com.hapicraft.client.ozellik.SerbestBakis;

/** 360° bakış: kamera oyuncunun değil, serbest bakışın açısını kullanır. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyExpressionValue(method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float hapi$serbestYaw(float asil) {
        return SerbestBakis.etkin ? SerbestBakis.yaw : asil;
    }

    @ModifyExpressionValue(method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float hapi$serbestPitch(float asil) {
        return SerbestBakis.etkin ? SerbestBakis.pitch : asil;
    }
}
