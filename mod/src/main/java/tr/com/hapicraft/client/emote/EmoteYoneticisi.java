package tr.com.hapicraft.client.emote;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import tr.com.hapicraft.client.ag.Ag;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Kimin hangi dansı oynadığını tutar ve modele uygular. */
public final class EmoteYoneticisi {
    private record Aktif(Emote emote, long baslangic) {
        float gecen() { return (System.nanoTime() - baslangic) / 1_000_000_000f; }
    }

    private static final Map<UUID, Aktif> AKTIF = new ConcurrentHashMap<>();
    private static final float GECIS = 0.25f;
    private static final float AZAMI_DONGU = 120f;
    private static final Poz VANILLA = new Poz(), DANS = new Poz();
    private static CameraType oncekiKamera;

    private EmoteYoneticisi() {}

    public static void baslat(UUID uuid, String kimlik) {
        Emote e = kimlik == null || kimlik.isEmpty() ? null : Emotlar.bul(kimlik);
        if (e == null) AKTIF.remove(uuid);
        else AKTIF.put(uuid, new Aktif(e, System.nanoTime()));
    }

    public static void durdur(UUID uuid) { AKTIF.remove(uuid); }

    public static void hepsiniDurdur() {
        AKTIF.clear();
        kamerayiGeriAl();
    }

    public static boolean oynuyor(UUID uuid) { return AKTIF.containsKey(uuid); }

    /** Yerel oyuncu bir dans seçti. */
    public static void yerelOynat(String kimlik) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        baslat(mc.player.getUUID(), kimlik);
        Ag.emoteGonder(kimlik);
        if (oncekiKamera == null && mc.options.getCameraType().isFirstPerson()) {
            oncekiKamera = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
    }

    public static void yerelDurdur() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || AKTIF.remove(mc.player.getUUID()) == null) return;
        Ag.emoteGonder("");
        kamerayiGeriAl();
    }

    private static void kamerayiGeriAl() {
        if (oncekiKamera != null) {
            Minecraft.getInstance().options.setCameraType(oncekiKamera);
            oncekiKamera = null;
        }
    }

    public static void tik(Minecraft mc) {
        AKTIF.entrySet().removeIf(g -> {
            float sure = g.getValue().emote().sure();
            return g.getValue().gecen() > (sure > 0 ? sure : AZAMI_DONGU);
        });
        LocalPlayer ben = mc.player;
        if (ben == null) { if (oncekiKamera != null) kamerayiGeriAl(); return; }
        boolean oynuyorum = AKTIF.containsKey(ben.getUUID());
        if (oynuyorum) {
            boolean hareket = ben.input.getMoveVector().lengthSquared() > 0.001f || ben.input.keyPresses.jump()
                    || ben.input.keyPresses.shift() || ben.hurtTime > 0 || ben.swinging || ben.isUsingItem()
                    || !ben.onGround() && !ben.isPassenger();
            if (hareket) yerelDurdur();
        } else if (oncekiKamera != null) {
            kamerayiGeriAl();
        }
    }

    /** HumanoidModel.setupAnim sonunda çağrılır (çizim iş parçacığı). */
    public static void uygula(HumanoidModel<?> model, AvatarRenderState durum) {
        if (AKTIF.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity varlik = mc.level.getEntity(durum.id);
        if (varlik == null) return;
        Aktif a = AKTIF.get(varlik.getUUID());
        if (a == null) return;
        float t = a.gecen();
        float sure = a.emote().sure();
        float w = Math.min(1f, t / GECIS);
        if (sure > 0) w = Math.min(w, Math.max(0f, (sure - t) / GECIS));
        w = w * w * (3 - 2 * w);
        VANILLA.oku(model);
        DANS.oku(model);
        a.emote().cizim().uygula(t, DANS);
        DANS.yaz(model, VANILLA, w);
    }
}
