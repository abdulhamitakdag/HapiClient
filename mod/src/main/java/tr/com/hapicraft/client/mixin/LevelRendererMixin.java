package tr.com.hapicraft.client.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import tr.com.hapicraft.client.ozellik.Gorunum;
import tr.com.hapicraft.client.ozellik.Ozellikler;

/** Bakılan bloğun çerçevesi: renk ve kalınlık. İkinci renderHitOutline çağrısı asıl çerçevedir. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Unique
    private static final String CIZIM = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline("
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDD"
            + "Lnet/minecraft/client/renderer/state/BlockOutlineRenderState;IF)V";

    @ModifyArg(method = "renderBlockOutline", at = @At(value = "INVOKE", target = CIZIM, ordinal = 1), index = 6)
    private int hapi$cerceveRengi(int asil) {
        return Ozellikler.acik("blokcercevesi", false) ? Gorunum.renk("blok.renk") : asil;
    }

    @ModifyArg(method = "renderBlockOutline", at = @At(value = "INVOKE", target = CIZIM, ordinal = 1), index = 7)
    private float hapi$cerceveKalinligi(float asil) {
        return Ozellikler.acik("blokcercevesi", false) ? asil * Gorunum.kalinlik("blok.kalinlik") : asil;
    }
}
