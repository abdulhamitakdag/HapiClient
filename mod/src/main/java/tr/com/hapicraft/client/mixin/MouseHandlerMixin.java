package tr.com.hapicraft.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tr.com.hapicraft.client.hud.Hud;
import tr.com.hapicraft.client.ozellik.Ozellikler;
import tr.com.hapicraft.client.ozellik.SerbestBakis;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void hapi$cps(long pencere, MouseButtonInfo bilgi, int eylem, CallbackInfo ci) {
        if (eylem == 1) Hud.tiklama(bilgi.button());
    }

    /** Yakınlaştırma tuşu basılıyken tekerlek yakınlaştırma seviyesini değiştirir. */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void hapi$zoomTekerlek(long pencere, double yatay, double dikey, CallbackInfo ci) {
        if (Ozellikler.zoomKaydir(dikey)) ci.cancel();
    }

    /** 360° bakış açıkken fare oyuncuyu değil kamerayı çevirir. */
    @WrapWithCondition(method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private boolean hapi$serbestBakis(LocalPlayer oyuncu, double dx, double dy) {
        return !SerbestBakis.cevir(dx, dy);
    }
}
