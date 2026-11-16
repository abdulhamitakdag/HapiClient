package tr.com.hapicraft.client.mixin;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tr.com.hapicraft.client.giris.OtoGiris;

/** Sunucunun giriş penceresi, launcher'da yazılan şifreyle gösterilmeden yanıtlanır. */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {
    @Shadow @Final protected Connection connection;

    @Inject(method = "handleShowDialog", at = @At("HEAD"), cancellable = true)
    private void hapi$otoGiris(ClientboundShowDialogPacket paket, CallbackInfo ci) {
        if (OtoGiris.pencere(paket.dialog().value(), connection, connection::send)) ci.cancel();
    }
}
