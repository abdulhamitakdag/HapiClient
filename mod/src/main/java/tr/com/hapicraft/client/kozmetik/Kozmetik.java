package tr.com.hapicraft.client.kozmetik;

/**
 * Tek bir kozmetik. Alanlar katalog.json'dan (kozmetik/uret.py çıktısı) okunur; yuvaya göre
 * yalnız ilgili olanlar doludur.
 */
public final class Kozmetik {
    String kimlik, ad, yuva, nadirlik, aciklama;
    /** Kanat ve pelerin: doku adı. */
    String doku;
    /** Kanat: model pikseli cinsinden en/boy, kökün dokudaki dikey yeri (0..1) ve hareket türü. */
    float en, boy, kok;
    String hareket;
    boolean isik, saydam;
    /** Kanat üstünde dönen dişliler: {u, v, yarıçap, hız (derece/tik)}. */
    float[][] disliler;
    /** Şapka ve hale: voksel model adı. */
    String model;
    /** Aura: parçacık türü ve deseni. */
    String parcacik, desen;

    public String kimlik() { return kimlik; }
    public String ad() { return ad; }
    public String yuva() { return yuva; }
    public String nadirlik() { return nadirlik == null ? "yaygin" : nadirlik; }
    public String aciklama() { return aciklama == null ? "" : aciklama; }
    public String doku() { return doku; }
    public float en() { return en; }
    public float boy() { return boy; }
    public float kok() { return kok; }
    public String hareket() { return hareket == null ? "tuy" : hareket; }
    public boolean isik() { return isik; }
    public boolean saydam() { return saydam; }
    public float[][] disliler() { return disliler == null ? new float[0][] : disliler; }
    public String model() { return model; }
    public String parcacik() { return parcacik == null ? "" : parcacik; }
    public String desen() { return desen == null ? "yukselen" : desen; }

    public String nadirlikAdi() {
        return switch (nadirlik()) {
            case "nadir" -> "Nadir";
            case "destansi" -> "Destansı";
            case "efsanevi" -> "Efsanevi";
            default -> "Yaygın";
        };
    }

    public int nadirlikRengi() {
        return switch (nadirlik()) {
            case "nadir" -> 0xFF4FC3FF;
            case "destansi" -> 0xFFB98CFF;
            case "efsanevi" -> 0xFFFFC53D;
            default -> 0xFFB8C4BC;
        };
    }
}
