package tr.com.hapicraft.client.kozmetik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.HapiClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Kozmetik ekranındaki 3B manken. Oyundayken oyuncunun kendisi, ana menüde varsayılan görünümlü bir
 * oyuncu çizilir; ikisi de dünyaya ihtiyaç duymadan çizim durumundan üretilir.
 */
public final class Onizleme {
    /** Mankenin çizim durumundaki varlık numarası; gerçek bir varlıkla çakışmaz. */
    public static final int NO = Integer.MIN_VALUE + 4177;
    /** Mankenin üstündekiler: takılı olanlar + üzerine gelinen kozmetik. */
    static final Map<String, String> SECIM = new HashMap<>();

    private Onizleme() {}

    /** Takılı kozmetiklerin üstüne, verilen yuvada denenecek kozmetiği koyar (kimlik null ise yalnız takılılar). */
    public static void ayarla(String yuva, String kimlik) {
        SECIM.clear();
        SECIM.putAll(Ayarlar.A.kozmetik);
        if (yuva != null && kimlik != null) {
            if (kimlik.isEmpty()) SECIM.remove(yuva);
            else SECIM.put(yuva, kimlik);
        }
    }

    public static PelerinDokusu pelerinDokusu(Kozmetik k) {
        return new PelerinDokusu(HapiClient.kimlik("kozmetik/pelerin/" + k.doku()),
                HapiClient.kimlik("textures/kozmetik/pelerin/" + k.doku() + ".png"));
    }

    /** Pelerin dokusunun kimliği ve dosya yolu. */
    public record PelerinDokusu(net.minecraft.resources.Identifier kimlik, net.minecraft.resources.Identifier yol) {
        public ClientAsset.Texture doku() { return new ClientAsset.ResourceTexture(kimlik, yol); }
    }

    /**
     * @param yaw   mankenin kendi ekseninde dönüşü (derece; 0 = yüzü bize dönük)
     * @param pitch kameranın yukarıdan bakış açısı (derece)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void ciz(GuiGraphics g, int x0, int y0, int x1, int y1, float olcek, float yaw, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        float zaman = (System.nanoTime() / 1_000_000L % 10_000_000L) / 50f;
        AvatarRenderState d = null;
        ClientAsset.Texture kendiPelerini = null;
        if (mc.player != null) {
            EntityRenderer cizer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
            EntityRenderState ham = cizer.createRenderState(mc.player, 1f);
            if (ham instanceof AvatarRenderState a) {
                d = a;
                // Skin kaydındaki pelerin bizim taktığımızsa asıl (Mojang) pelerin bilinmez; yok sayılır.
                if (Kozmetikler.takili(mc.player.getUUID(), "pelerin") == null && a.showCape) kendiPelerini = a.skin.cape();
            }
        }
        if (d == null) {
            d = new AvatarRenderState();
            d.entityType = EntityType.PLAYER;
            d.boundingBoxWidth = 0.6f;
            d.boundingBoxHeight = 1.8f;
            d.eyeHeight = 1.62f;
            d.skin = DefaultPlayerSkin.get(mc.getGameProfile());
            d.showHat = d.showJacket = d.showLeftPants = d.showRightPants = d.showLeftSleeve = d.showRightSleeve = true;
        }
        d.id = NO;
        d.ageInTicks = zaman;
        d.lightCoords = LightTexture.FULL_BRIGHT;
        d.shadowPieces.clear();
        d.outlineColor = 0;
        d.nameTag = null;
        d.scoreText = null;
        d.isInvisible = false;
        d.displayFireAnimation = false;
        d.bodyRot = 180f + yaw;
        d.yRot = 0;
        d.xRot = 0;
        d.capeFlap = 0;
        d.capeLean = 6f + (float) Math.sin(zaman * 0.07f) * 4f;
        d.capeLean2 = (float) Math.sin(zaman * 0.05f) * 3f;
        Kozmetik pelerin = Kozmetikler.takili(NO, "pelerin");
        ClientAsset.Texture p = pelerin != null ? pelerinDokusu(pelerin).doku() : kendiPelerini;
        PlayerSkin s = d.skin;
        d.skin = new PlayerSkin(s.body(), p, s.elytra(), s.model(), s.secure());
        d.showCape = p != null;

        Quaternionf egim = new Quaternionf().rotateX((float) Math.toRadians(pitch));
        Quaternionf donus = new Quaternionf().rotateZ((float) Math.PI).mul(egim);
        g.submitEntityRenderState(d, olcek, new Vector3f(0, d.boundingBoxHeight / 2f + 0.0625f, 0), donus, egim, x0, y0, x1, y1);
    }
}
