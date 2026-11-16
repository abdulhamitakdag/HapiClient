package tr.com.hapicraft.client.mixin;

import net.minecraft.client.renderer.entity.FishingHookRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import tr.com.hapicraft.client.ozellik.Gorunum;
import tr.com.hapicraft.client.ozellik.Ozellikler;

/** Olta misinası: renk ve kalınlık. */
@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {
    @ModifyArg(method = "stringVertex", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static int hapi$misinaRengi(int asil) {
        return Ozellikler.acik("misina", false) ? Gorunum.renk("misina.renk") : asil;
    }

    @ModifyArg(method = "stringVertex", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setLineWidth(F)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static float hapi$misinaKalinligi(float asil) {
        return Ozellikler.acik("misina", false) ? asil * Gorunum.kalinlik("misina.kalinlik") : asil;
    }
}
