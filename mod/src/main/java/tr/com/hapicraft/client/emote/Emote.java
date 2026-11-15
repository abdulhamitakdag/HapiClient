package tr.com.hapicraft.client.emote;

/**
 * Tek bir dans/hareket. sure saniye; 0 ise oyuncu hareket edene kadar döner.
 * cizim: t saniyesindeki duruşu p üzerine yazar (p vanilla duruşuyla dolu gelir).
 */
public record Emote(String kimlik, String ad, String tur, float sure, Cizim cizim) {
    @FunctionalInterface
    public interface Cizim {
        void uygula(float t, Poz p);
    }
}
