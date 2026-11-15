package tr.com.hapicraft.client.sohbet;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.ozellik.Ozellikler;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sessiz sohbet: genel sohbet satırlarını gizler. Özel mesajlar, oyuncunun adının geçtiği
 * satırlar ve sistem/eklenti mesajları görünmeye devam eder.
 */
public final class SessizSohbet {
    /** "[Kasaba] Rütbe] Ad: mesaj" biçimindeki oyuncu satırı. */
    private static final Pattern OYUNCU_SATIRI = Pattern.compile("^(?:\\[.{0,48}] ?)*\\.?[A-Za-z0-9_]{3,16}: .*", Pattern.DOTALL);
    private static final Pattern OZEL_MESAJ = Pattern.compile("(?i)(->|→|»).{0,24}\\b(ben|sen|me|you)\\b|\\b(ben|sen|me|you)\\b.{0,24}(->|→)|fısıl|özel mesaj");
    public static int gizlenen = 0;

    private SessizSohbet() {}

    public static void kur() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((mesaj, ustYazi) -> ustYazi || gecsin(mesaj.getString()));
        ClientReceiveMessageEvents.ALLOW_CHAT.register((mesaj, imzali, gonderen, tur, zaman) -> gecsin(mesaj.getString()));
    }

    public static boolean etkin() {
        return Ayarlar.A.sessiz && Ozellikler.izinli("sessizsohbet");
    }

    public static boolean gecsin(String satir) {
        if (!etkin()) return true;
        Minecraft mc = Minecraft.getInstance();
        String ad = mc.getUser() == null ? "" : mc.getUser().getName();
        if (gizlenmeli(satir, ad)) {
            gizlenen++;
            return false;
        }
        return true;
    }

    /** Saf karar fonksiyonu (test edilebilir). */
    public static boolean gizlenmeli(String satir, String kendiAdi) {
        String duz = satir.replaceAll("§.", "").strip();
        if (!OYUNCU_SATIRI.matcher(duz).matches()) return false;
        if (OZEL_MESAJ.matcher(duz).find()) return false;
        int ikiNokta = duz.indexOf(": ");
        String govde = duz.substring(ikiNokta + 2).toLowerCase(Locale.ROOT);
        String gonderen = duz.substring(0, ikiNokta);
        if (!kendiAdi.isEmpty()) {
            if (gonderen.endsWith(kendiAdi)) return false;
            if (govde.contains(kendiAdi.toLowerCase(Locale.ROOT))) return false;
        }
        return true;
    }

    public static void degistir() {
        Ayarlar.A.sessiz = !Ayarlar.A.sessiz;
        Ayarlar.kaydet();
        if (Ayarlar.A.sessiz) {
            gizlenen = 0;
            Ozellikler.bilgi("Sessiz sohbet açık — genel sohbet gizleniyor");
        } else {
            Ozellikler.bilgi("Sessiz sohbet kapalı" + (gizlenen > 0 ? " (" + gizlenen + " mesaj gizlendi)" : ""));
        }
    }
}
