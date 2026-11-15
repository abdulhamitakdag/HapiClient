package tr.com.hapicraft.client.ag;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.HapiClient;
import tr.com.hapicraft.client.emote.EmoteYoneticisi;
import tr.com.hapicraft.client.kozmetik.Kozmetikler;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Sunucuyla (Velocity'deki HapiClientProxy) konuşan katman. Protokol: PLAN.md bölüm 5. */
public final class Ag {
    public static final int PROTO = 1;
    public static final int MERHABA = 1, KOZMETIK = 2, EMOTE = 3;
    public static final int HOSGELDIN = 16, OYUNCULAR = 17, AYRILDI = 18, EMOTE_YAYIN = 19, BILDIRIM = 20;
    private static final int AZAMI = 16 * 1024;

    /** Sunucunun uzaktan kapattığı modüller. */
    public static volatile Set<String> kapali = Set.of();
    /** Sahip olunan kozmetikler; "*" = hepsi. Sunucu yanıt vermediyse hepsi açık sayılır. */
    public static volatile Set<String> sahip = Set.of("*");
    public static volatile String sunucu = "";
    public static volatile int indirim = 0;
    public static volatile boolean tanindi = false;

    private Ag() {}

    public static void kur() {
        PayloadTypeRegistry.playC2S().register(HamPaket.TIP, HamPaket.KODEK);
        PayloadTypeRegistry.playS2C().register(HamPaket.TIP, HamPaket.KODEK);
        ClientPlayNetworking.registerGlobalReceiver(HamPaket.TIP,
                (paket, baglam) -> baglam.client().execute(() -> al(paket.veri())));
        ClientPlayConnectionEvents.JOIN.register((dinleyici, gonderici, mc) -> {
            merhaba();
            kozmetikGonder();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((dinleyici, mc) -> sifirla());
    }

    private static void sifirla() {
        kapali = Set.of();
        sahip = Set.of("*");
        sunucu = "";
        indirim = 0;
        tanindi = false;
        Kozmetikler.digerleriniTemizle();
        EmoteYoneticisi.hepsiniDurdur();
    }

    private static void gonder(int no, JsonObject govde) {
        try {
            byte[] json = govde.toString().getBytes(StandardCharsets.UTF_8);
            if (json.length + 1 > AZAMI) return;
            byte[] veri = new byte[json.length + 1];
            veri[0] = (byte) no;
            System.arraycopy(json, 0, veri, 1, json.length);
            ClientPlayNetworking.send(new HamPaket(veri));
        } catch (Exception e) {
            HapiClient.LOG.debug("Paket gönderilemedi: {}", e.toString());
        }
    }

    private static void merhaba() {
        JsonObject g = new JsonObject();
        g.addProperty("surum", HapiClient.SURUM);
        g.addProperty("proto", PROTO);
        JsonArray modlar = new JsonArray();
        FabricLoader.getInstance().getAllMods().stream()
                .map(m -> m.getMetadata().getId() + "@" + m.getMetadata().getVersion().getFriendlyString())
                .filter(s -> !s.startsWith("fabric-") || s.startsWith("fabric-api@"))
                .sorted().limit(200)
                .forEach(s -> modlar.add(s.length() > 80 ? s.substring(0, 80) : s));
        g.add("modlar", modlar);
        gonder(MERHABA, g);
    }

    public static void kozmetikGonder() {
        JsonObject g = new JsonObject();
        for (String yuva : Kozmetikler.YUVALAR) g.addProperty(yuva, Ayarlar.A.kozmetik.getOrDefault(yuva, ""));
        gonder(KOZMETIK, g);
    }

    public static void emoteGonder(String kimlik) {
        JsonObject g = new JsonObject();
        g.addProperty("id", kimlik);
        gonder(EMOTE, g);
    }

    private static void al(byte[] veri) {
        if (veri.length < 1 || veri.length > AZAMI) return;
        try {
            JsonObject g = JsonParser.parseString(new String(veri, 1, veri.length - 1, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            switch (veri[0]) {
                case HOSGELDIN -> {
                    kapali = dizi(g, "kapali");
                    Set<String> s = dizi(g, "sahip");
                    sahip = s.isEmpty() ? Set.of() : s;
                    sunucu = metin(g, "sunucu");
                    indirim = g.has("indirim") ? g.get("indirim").getAsInt() : 0;
                    tanindi = true;
                }
                case OYUNCULAR -> {
                    if (g.has("sifirla") && g.get("sifirla").getAsBoolean()) Kozmetikler.digerleriniTemizle();
                    for (JsonElement e : g.getAsJsonArray("liste")) {
                        JsonObject o = e.getAsJsonObject();
                        UUID uuid = UUID.fromString(metin(o, "uuid"));
                        JsonObject k = o.getAsJsonObject("k");
                        Map<String, String> secim = new java.util.HashMap<>();
                        for (String yuva : Kozmetikler.YUVALAR) secim.put(yuva, metin(k, yuva));
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null && mc.player.getUUID().equals(uuid)) {
                            // Sunucu sahip olmadığımız kozmetiği boşalttıysa yerelde de kaldır.
                            boolean degisti = false;
                            for (String yuva : Kozmetikler.YUVALAR)
                                if (secim.get(yuva).isEmpty() && Ayarlar.A.kozmetik.remove(yuva) != null) degisti = true;
                            if (degisti) Ayarlar.kaydet();
                        } else Kozmetikler.digeriniAyarla(uuid, secim);
                    }
                }
                case AYRILDI -> {
                    UUID uuid = UUID.fromString(metin(g, "uuid"));
                    Kozmetikler.digeriniSil(uuid);
                    EmoteYoneticisi.durdur(uuid);
                }
                case EMOTE_YAYIN -> EmoteYoneticisi.baslat(UUID.fromString(metin(g, "uuid")), metin(g, "id"));
                case BILDIRIM -> SystemToast.add(Minecraft.getInstance().getToastManager(),
                        SystemToast.SystemToastId.NARRATOR_TOGGLE,
                        Component.literal(kirp(metin(g, "baslik"), 60)), Component.literal(kirp(metin(g, "metin"), 200)));
                default -> { }
            }
        } catch (Exception e) {
            HapiClient.LOG.debug("Bozuk sunucu paketi: {}", e.toString());
        }
    }

    private static String kirp(String s, int n) {
        return s.length() > n ? s.substring(0, n) : s;
    }

    private static String metin(JsonObject g, String alan) {
        return g != null && g.has(alan) && g.get(alan).isJsonPrimitive() ? g.get(alan).getAsString() : "";
    }

    private static Set<String> dizi(JsonObject g, String alan) {
        Set<String> sonuc = new HashSet<>();
        if (g.has(alan) && g.get(alan).isJsonArray())
            for (JsonElement e : g.getAsJsonArray(alan)) sonuc.add(e.getAsString());
        return Set.copyOf(sonuc);
    }
}
