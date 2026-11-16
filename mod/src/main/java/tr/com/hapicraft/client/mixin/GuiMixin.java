package tr.com.hapicraft.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tr.com.hapicraft.client.ozellik.Nisangah;
import tr.com.hapicraft.client.ozellik.Ozellikler;

@Mixin(Gui.class)
public abstract class GuiMixin {
    /** İlk blitSprite vanilla nişangahtır; sonrakiler saldırı göstergesidir ve yerinde kalır. */
    @WrapOperation(method = "renderCrosshair", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void hapi$nisangah(GuiGraphics g, RenderPipeline hat, Identifier doku, int x, int y, int w, int h,
                               Operation<Void> asil) {
        if (!Nisangah.ciz(g)) asil.call(g, hat, doku, x, y, w, h);
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void hapi$skorGizle(GuiGraphics g, Objective hedef, CallbackInfo ci) {
        if (Ozellikler.acik("skorgizle", false)) ci.cancel();
    }
}
