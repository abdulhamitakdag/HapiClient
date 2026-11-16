package tr.com.hapicraft.client.giris;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.Input;
import tr.com.hapicraft.client.HapiClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Otomatik giriş: şifre launcher'da yazılır ve oyuna {@code HAPICLIENT_SIFRE} ortam değişkeniyle
 * gelir. Sunucunun giriş/kayıt penceresi (ya da pencere kapalıysa sohbetteki giriş uyarısı)
 * bu şifreyle kendiliğinden yanıtlanır.
 *
 * Her bağlantıda yalnız bir kez denenir: şifre yanlışsa sunucunun ikinci penceresi oyuncuya kalır.
 */
public final class OtoGiris {
    /** Sunucunun giriş penceresinde yanıt beklediği tıklama kimliği. */
    private static final Identifier TIKLAMA = Identifier.fromNamespaceAndPath("hapicraft", "kayit");
    private static final String SIFRE = System.getenv("HAPICLIENT_SIFRE");

    private static Connection denenen;

    private OtoGiris() {}

    public static void kur() {
        ClientReceiveMessageEvents.GAME.register((mesaj, ustYazi) -> {
            if (ustYazi) return;
            ClientPacketListener dinleyici = Minecraft.getInstance().getConnection();
            if (dinleyici == null) return;
            String komut = komut(mesaj.getString(), SIFRE);
            if (komut != null && ilkDeneme(dinleyici.getConnection())) {
                dinleyici.sendCommand(komut);
                HapiClient.LOG.info("Otomatik giriş: sohbet uyarısı yanıtlandı.");
            }
        });
    }

    /**
     * Sunucu bir pencere gösterdiğinde çağrılır (ağ iş parçacığından da gelebilir).
     *
     * @return pencere otomatik yanıtlandıysa true; o zaman oyuncuya gösterilmez.
     */
    public static boolean pencere(Dialog pencere, Connection baglanti, java.util.function.Consumer<Packet<?>> gonder) {
        List<String> alanlar = pencere.common().inputs().stream().map(Input::key).toList();
        Map<String, String> yanit = yanit(alanlar, SIFRE);
        if (yanit == null || !ilkDeneme(baglanti)) return false;
        CompoundTag govde = new CompoundTag();
        yanit.forEach(govde::putString);
        gonder.accept(new ServerboundCustomClickActionPacket(TIKLAMA, Optional.of(govde)));
        HapiClient.LOG.info("Otomatik giriş: giriş penceresi yanıtlandı.");
        return true;
    }

    private static synchronized boolean ilkDeneme(Connection baglanti) {
        if (baglanti == denenen) return false;
        denenen = baglanti;
        return true;
    }

    /** Saf karar: pencere bizim giriş/kayıt penceremizse alan → değer eşlemesi, değilse null. */
    public static Map<String, String> yanit(List<String> alanlar, String sifre) {
        if (sifre == null || sifre.isEmpty() || alanlar.isEmpty() || !alanlar.contains("sifre")) return null;
        Map<String, String> yanit = new LinkedHashMap<>();
        for (String alan : alanlar) {
            if (!alan.equals("sifre") && !alan.equals("sifre2")) return null;
            yanit.put(alan, sifre);
        }
        return yanit;
    }

    /** Saf karar: sohbetteki giriş uyarısına karşılık gönderilecek komut (bölü işaretsiz), yoksa null. */
    public static String komut(String satir, String sifre) {
        if (sifre == null || sifre.isEmpty()) return null;
        String duz = satir.replaceAll("§.", "");
        if (duz.contains("/giriş <şifre>")) return "giriş " + sifre;
        if (duz.contains("/kayıt <şifre> <tekrar>")) return "kayıt " + sifre + " " + sifre;
        return null;
    }
}
