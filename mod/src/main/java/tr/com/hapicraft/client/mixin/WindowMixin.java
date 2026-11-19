package tr.com.hapicraft.client.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tr.com.hapicraft.client.Ayarlar;

@Mixin(Window.class)
public abstract class WindowMixin {
    /** Arayüz ölçeği "Otomatik" (0) iken bir kademe küçük seçilir; elle seçilen ölçeğe dokunulmaz. */
    @Inject(method = "calculateScale", at = @At("RETURN"), cancellable = true)
    private void hapi$otomatikOlcek(int istenen, boolean unicode, CallbackInfoReturnable<Integer> cir) {
        if (istenen == 0) cir.setReturnValue(Ayarlar.arayuzOlcegi(cir.getReturnValue()));
    }
}
