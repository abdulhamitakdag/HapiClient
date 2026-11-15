package tr.com.hapicraft.client.proxy;

import tr.com.hapicraft.client.proxy.cekirdek.Cekirdek;

import java.util.Optional;
import java.util.UUID;

/**
 * Diğer Velocity eklentileri için genel API. Eklenti yüklenmeden önce ve kapandıktan sonra
 * "kullanmıyor" yanıtı verir. Her thread'den çağrılabilir.
 */
public final class HapiClientApi {

    private static volatile Cekirdek cekirdek;

    private HapiClientApi() {
    }

    static void bagla(Cekirdek c) {
        cekirdek = c;
    }

    /** Oyuncu bu proxy oturumunda HapiClient el sıkışmasını yaptı mı? */
    public static boolean kullaniyorMu(UUID oyuncu) {
        Cekirdek c = cekirdek;
        return c != null && c.kullaniyorMu(oyuncu);
    }

    /** Oyuncunun bildirdiği HapiClient sürümü; HapiClient kullanmıyorsa boş. */
    public static Optional<String> surum(UUID oyuncu) {
        Cekirdek c = cekirdek;
        return c == null ? Optional.empty() : c.oturum(oyuncu).map(Cekirdek.Oturum::surum);
    }
}
