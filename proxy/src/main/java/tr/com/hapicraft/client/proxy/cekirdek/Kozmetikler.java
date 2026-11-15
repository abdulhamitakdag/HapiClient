package tr.com.hapicraft.client.proxy.cekirdek;

import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Kozmetik yuvaları, doğrulama ve sahiplik kuralları. Velocity'ye bağımlı değildir. */
public final class Kozmetikler {

    public static final List<String> YUVALAR = List.of("pelerin", "kanat", "sapka", "hale", "aura");
    public static final String IZIN_ONEK = "hapiclient.kozmetik.";
    public static final String IZIN_HEPSI = IZIN_ONEK + "*";

    private Kozmetikler() {
    }

    /** Bütün yuvaları boş olan seçim (değiştirilemez). */
    public static Map<String, String> bos() {
        Map<String, String> m = new LinkedHashMap<>();
        for (String yuva : YUVALAR) {
            m.put(yuva, "");
        }
        return Collections.unmodifiableMap(m);
    }

    public static boolean bosMu(Map<String, String> secim) {
        for (String v : secim.values()) {
            if (!v.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Oyuncu bu kozmetiği kullanabilir mi?
     *
     * @param izin oyuncunun izin denetimi (izin düğümü -> var mı)
     */
    public static boolean sahipMi(String kimlik, Set<String> ucretsiz, Predicate<String> izin) {
        return ucretsiz.contains("*") || ucretsiz.contains(kimlik)
                || izin.test(IZIN_HEPSI) || izin.test(IZIN_ONEK + kimlik);
    }

    /**
     * HOSGELDIN'deki {@code sahip} alanı. Her şey açıksa {@code ["*"]}; değilse ücretsizler ile
     * {@code ucretli} kataloğundan izni olunanlar. (Velocity izinleri sıralanamadığı için ücretli
     * kimlikler config'teki katalogdan tek tek sorulur.)
     */
    public static List<String> sahipListesi(Set<String> ucretsiz, List<String> ucretli, Predicate<String> izin) {
        if (ucretsiz.contains("*") || izin.test(IZIN_HEPSI)) {
            return List.of("*");
        }
        Set<String> sahip = new LinkedHashSet<>(ucretsiz);
        for (String kimlik : ucretli) {
            if (izin.test(IZIN_ONEK + kimlik)) {
                sahip.add(kimlik);
            }
        }
        return List.copyOf(sahip);
    }

    /**
     * KOZMETIK gövdesini doğrular. Sonuçta her zaman bilinen beş yuva bulunur; eksik, metin olmayan,
     * biçimi bozuk ya da sahip olunmayan değer boşaltılır. Bilinmeyen alanlar yok sayılır.
     */
    public static Map<String, String> dogrula(JsonObject govde, Predicate<String> sahipMi) {
        Map<String, String> sonuc = new LinkedHashMap<>();
        for (String yuva : YUVALAR) {
            sonuc.put(yuva, gecerliDeger(Protokol.metinAlani(govde, yuva), sahipMi));
        }
        return Collections.unmodifiableMap(sonuc);
    }

    /** Saklanan seçimi yeniden doğrular (izin süresi dolmuş olabilir). */
    public static Map<String, String> dogrula(Map<String, String> secim, Predicate<String> sahipMi) {
        Map<String, String> sonuc = new LinkedHashMap<>();
        for (String yuva : YUVALAR) {
            sonuc.put(yuva, gecerliDeger(secim.get(yuva), sahipMi));
        }
        return Collections.unmodifiableMap(sonuc);
    }

    private static String gecerliDeger(String deger, Predicate<String> sahipMi) {
        if (deger == null || deger.isEmpty() || !Protokol.kimlikGecerli(deger) || !sahipMi.test(deger)) {
            return "";
        }
        return deger;
    }
}
