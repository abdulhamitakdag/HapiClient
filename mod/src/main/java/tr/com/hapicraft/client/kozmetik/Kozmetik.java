package tr.com.hapicraft.client.kozmetik;

/** Tek bir kozmetik. bicim yuvaya göre anlam taşır (şapka türü, kanat türü, parçacık adı…). */
public record Kozmetik(String kimlik, String ad, String yuva, String bicim, int renk1, int renk2) {}
