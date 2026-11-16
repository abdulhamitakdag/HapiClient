package tr.com.hapicraft.client.ozellik;

import tr.com.hapicraft.client.Ayarlar;

import java.util.List;

/** Renk, şekil ve kalınlık gibi çok değerli seçenekler. Menüde tıkladıkça sıradaki değere geçer. */
public final class Gorunum {
    public record Secenek(String anahtar, String ad, String[] degerler, int varsayilan) {}

    private static final String[] RENK_ADI = {"Beyaz", "Siyah", "Yeşil", "Kırmızı", "Mavi", "Sarı",
            "Turuncu", "Mor", "Pembe", "Turkuaz", "Gökkuşağı"};
    private static final int[] RENK = {0xFFFFFFFF, 0xFF000000, 0xFF2BD35A, 0xFFFF4040, 0xFF3F8CFF, 0xFFFFD23F,
            0xFFFF8A1F, 0xFFA855F7, 0xFFFF6EC7, 0xFF1FE0D0};
    private static final String[] KALINLIK_ADI = {"Normal", "Kalın", "Çok Kalın", "En Kalın"};
    private static final float[] KALINLIK = {1f, 2f, 3f, 4.5f};
    private static final String[] BOYUT_ADI = {"Küçük", "Orta", "Büyük"};

    public static final List<Secenek> SECENEKLER = List.of(
            new Secenek("nisangah.sekil", "Nişangah Şekli", Nisangah.SEKIL_ADI, 0),
            new Secenek("nisangah.renk", "Nişangah Rengi", RENK_ADI, 0),
            new Secenek("nisangah.boyut", "Nişangah Boyutu", BOYUT_ADI, 1),
            new Secenek("blok.renk", "Blok Çerçevesi Rengi", RENK_ADI, 2),
            new Secenek("blok.kalinlik", "Blok Çerçevesi Kalınlığı", KALINLIK_ADI, 1),
            new Secenek("misina.renk", "Olta Misinası Rengi", RENK_ADI, 0),
            new Secenek("misina.kalinlik", "Olta Misinası Kalınlığı", KALINLIK_ADI, 1),
            new Secenek("karakter.boyut", "Karakter Boyutu", BOYUT_ADI, 1));

    private Gorunum() {}

    public static Secenek bul(String anahtar) {
        for (Secenek s : SECENEKLER) if (s.anahtar().equals(anahtar)) return s;
        throw new IllegalArgumentException(anahtar);
    }

    /** Seçili değerin sırası; kayıt bozuksa ya da aralık dışındaysa varsayılan. */
    public static int deger(String anahtar) {
        Secenek s = bul(anahtar);
        Integer v = Ayarlar.A.secenek.get(anahtar);
        return v == null || v < 0 || v >= s.degerler().length ? s.varsayilan() : v;
    }

    public static String degerAdi(String anahtar) {
        return bul(anahtar).degerler()[deger(anahtar)];
    }

    public static void ilerlet(String anahtar) {
        Ayarlar.A.secenek.put(anahtar, (deger(anahtar) + 1) % bul(anahtar).degerler().length);
    }

    /** Renk seçeneğinin ARGB değeri (opak). */
    public static int renk(String anahtar) {
        int i = deger(anahtar);
        return i < RENK.length ? RENK[i] : gokkusagi(System.currentTimeMillis());
    }

    public static float kalinlik(String anahtar) {
        return KALINLIK[deger(anahtar)];
    }

    /** 4 saniyede bir tur atan tam doygun renk. */
    public static int gokkusagi(long ms) {
        float h = (ms % 4000L) / 4000f * 6f;
        float x = 1f - Math.abs(h % 2f - 1f);
        float r, g, b;
        switch ((int) h) {
            case 0 -> { r = 1; g = x; b = 0; }
            case 1 -> { r = x; g = 1; b = 0; }
            case 2 -> { r = 0; g = 1; b = x; }
            case 3 -> { r = 0; g = x; b = 1; }
            case 4 -> { r = x; g = 0; b = 1; }
            default -> { r = 1; g = 0; b = x; }
        }
        return 0xFF000000 | Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }
}
