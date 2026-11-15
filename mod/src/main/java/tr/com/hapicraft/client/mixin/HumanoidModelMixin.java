package tr.com.hapicraft.client.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tr.com.hapicraft.client.emote.EmoteYoneticisi;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void hapi$dans(HumanoidRenderState durum, CallbackInfo ci) {
        if (durum instanceof AvatarRenderState oyuncu) {
            EmoteYoneticisi.uygula((HumanoidModel<?>) (Object) this, oyuncu);
        }
    }
}
