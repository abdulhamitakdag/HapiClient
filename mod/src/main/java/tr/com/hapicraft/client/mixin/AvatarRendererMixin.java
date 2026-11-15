package tr.com.hapicraft.client.mixin;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tr.com.hapicraft.client.HapiClient;
import tr.com.hapicraft.client.kozmetik.Kozmetik;
import tr.com.hapicraft.client.kozmetik.Kozmetikler;

/** Seçili pelerini oyuncunun skin kaydına yerleştirir; çizimi vanilla pelerin katmanı yapar. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void hapi$pelerin(Avatar varlik, AvatarRenderState durum, float kismi, CallbackInfo ci) {
        Kozmetik k = Kozmetikler.takili(varlik.getUUID(), "pelerin");
        if (k == null || durum.skin == null) return;
        ClientAsset.Texture doku = new ClientAsset.ResourceTexture(
                HapiClient.kimlik("pelerin/" + k.bicim()),
                HapiClient.kimlik("textures/pelerin/" + k.bicim() + ".png"));
        PlayerSkin s = durum.skin;
        durum.skin = new PlayerSkin(s.body(), doku, s.elytra(), s.model(), s.secure());
        durum.showCape = true;
    }
}
