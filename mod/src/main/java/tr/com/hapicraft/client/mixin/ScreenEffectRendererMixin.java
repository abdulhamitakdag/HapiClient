package tr.com.hapicraft.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tr.com.hapicraft.client.ozellik.Ozellikler;

/** Alçak ateş: yanarken ekranı kaplayan alev aşağı indirilir. */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    @Unique
    private static boolean hapi$indirildi;

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void hapi$atesBasi(PoseStack poz, MultiBufferSource kaynak, TextureAtlasSprite doku, CallbackInfo ci) {
        hapi$indirildi = Ozellikler.acik("alcakates", false);
        if (hapi$indirildi) {
            poz.pushPose();
            poz.translate(0f, -0.3f, 0f);
        }
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void hapi$atesSonu(PoseStack poz, MultiBufferSource kaynak, TextureAtlasSprite doku, CallbackInfo ci) {
        if (hapi$indirildi) poz.popPose();
    }
}
