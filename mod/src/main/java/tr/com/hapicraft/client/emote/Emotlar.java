package tr.com.hapicraft.client.emote;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bütün danslar. Yeni dans eklemek = buraya bir satır. */
public final class Emotlar {
    private static final Map<String, Emote> HEPSI = new LinkedHashMap<>();
    private static final float PI = (float) Math.PI;

    private static float s(float x) { return (float) Math.sin(x); }
    private static float c(float x) { return (float) Math.cos(x); }
    private static float m(float x) { return Math.abs(s(x)); }
    /** 0..1 arası yumuşak git-gel. */
    private static float dalga(float x) { return 0.5f - 0.5f * c(x); }
    private static float kis(float x) { return Math.max(0f, Math.min(1f, x)); }

    private static void ekle(String kimlik, String ad, String tur, float sure, Emote.Cizim cizim) {
        HEPSI.put(kimlik, new Emote(kimlik, ad, tur, sure, cizim));
    }

    public static Emote bul(String kimlik) { return HEPSI.get(kimlik); }
    public static List<Emote> liste() { return List.copyOf(HEPSI.values()); }

    static {
        // ---- Selamlaşma ve tepkiler ----
        ekle("el_salla", "El Salla", "Tepki", 3f, (t, p) -> {
            p.sagKolX = 0; p.sagKolZ = 2.6f + 0.35f * s(t * 12f); p.basZ = 0.08f * s(t * 6f);
        });
        ekle("selam", "Asker Selamı", "Tepki", 2.5f, (t, p) -> {
            p.dur(); p.sagKolX = -2.2f; p.sagKolY = -0.6f; p.sagKolZ = 0.5f; p.basX = 0;
        });
        ekle("alkis", "Alkış", "Tepki", 4f, (t, p) -> {
            float a = 0.35f + 0.25f * m(t * 9f);
            p.sagKolX = -1.25f; p.solKolX = -1.25f; p.sagKolY = -a; p.solKolY = a; p.sagKolZ = 0; p.solKolZ = 0;
        });
        ekle("egil", "Reverans", "Tepki", 2.6f, (t, p) -> {
            float e = dalga(Math.min(t, 2.6f) / 2.6f * 2 * PI);
            p.govdeX = 0.9f * e; p.basX = 0.6f * e;
            p.sagKolX = -0.9f * e; p.sagKolY = -0.9f * e; p.solKolX = 0.5f * e; p.solKolZ = -0.4f * e;
            p.kokY = 1.5f * e;
        });
        ekle("evet", "Evet", "Tepki", 2f, (t, p) -> p.basX = 0.35f * s(t * 10f));
        ekle("hayir", "Hayır", "Tepki", 2f, (t, p) -> p.basY = 0.6f * s(t * 9f));
        ekle("dusun", "Düşünüyorum", "Tepki", 4f, (t, p) -> {
            p.sagKolX = -2.3f; p.sagKolY = -0.5f; p.sagKolZ = 0.25f; p.basX = 0.25f; p.basZ = 0.15f;
            p.solKolX = -0.7f; p.solKolY = 1.0f;
        });
        ekle("yuzunu_kapat", "Olamaz!", "Tepki", 3f, (t, p) -> {
            p.sagKolX = -2.6f; p.sagKolY = -0.55f; p.basX = 0.45f; p.basY = 0.25f * s(t * 7f);
        });
        ekle("kahkaha", "Kahkaha", "Tepki", 3.5f, (t, p) -> {
            float z = m(t * 14f);
            p.basX = -0.5f + 0.15f * z; p.govdeX = -0.15f; p.kollar(-0.3f, 0, 0.5f + 0.1f * z); p.kokY = -0.6f * z;
        });
        ekle("agla", "Ağla", "Tepki", 4f, (t, p) -> {
            p.kollar(-2.5f, -0.45f, 0); p.basX = 0.5f + 0.08f * s(t * 16f); p.govdeX = 0.2f;
        });
        ekle("zafer", "Zafer", "Tepki", 3f, (t, p) -> {
            float z = m(t * 8f);
            p.kollar(0, 0, 2.7f + 0.2f * z); p.kokY = -3f * z; p.basX = -0.3f;
        });
        ekle("kas", "Kas Göster", "Tepki", 3.5f, (t, p) -> {
            float g = 0.15f * s(t * 6f);
            p.kollar(0, 0, 1.6f); p.sagKolX = -1.3f + g; p.solKolX = -1.3f + g; p.sagKolY = 1.2f; p.solKolY = -1.2f;
        });
        ekle("opucuk", "Öpücük", "Tepki", 2.4f, (t, p) -> {
            float a = kis(t / 0.8f), b = kis((t - 1f) / 0.6f);
            p.sagKolX = -2.4f * a + 0.9f * b; p.sagKolY = -0.5f * a + 0.5f * b; p.basX = -0.1f * b;
        });
        ekle("omuz_silk", "Bilmem", "Tepki", 2.2f, (t, p) -> {
            float e = dalga(t / 2.2f * 2 * PI);
            p.kollar(-0.5f * e, 0, 0.9f * e); p.sagKolY = 1.2f * e; p.solKolY = -1.2f * e; p.basZ = 0.2f * e;
        });
        ekle("dua", "Dua", "Tepki", 0f, (t, p) -> {
            p.kollar(-1.3f, 0.35f, 0); p.basX = 0.3f + 0.03f * s(t * 2f);
        });

        // ---- Danslar ----
        ekle("disko", "Disko", "Dans", 0f, (t, p) -> {
            float f = t * 5f; boolean yukari = s(f) > 0;
            p.sagKolX = 0; p.sagKolZ = yukari ? 2.5f : 0.4f; p.sagKolY = 0;
            p.solKolX = -0.4f; p.solKolY = 0.8f; p.solKolZ = 0;
            p.govdeY = 0.25f * s(f); p.kokY = -1.2f * m(f); p.bacaklar(0, 0, 0.12f);
            p.basY = 0.3f * s(f);
        });
        ekle("twist", "Twist", "Dans", 0f, (t, p) -> {
            float f = t * 8f;
            p.govdeY = 0.5f * s(f); p.kollar(-0.9f, 0, 0.25f); p.sagKolY = 0.5f * s(f); p.solKolY = 0.5f * s(f);
            p.sagBacakY = -0.4f * s(f); p.solBacakY = -0.4f * s(f); p.kokY = 2f + 1f * c(f * 0.5f); p.basY = -0.3f * s(f);
            p.bacaklar(-0.35f, p.sagBacakY, 0.05f); p.solBacakY = p.sagBacakY;
        });
        ekle("robot", "Robot", "Dans", 0f, (t, p) -> {
            float k = (float) Math.floor(t * 3f); float a = (k % 4);
            p.dur();
            p.sagKolX = (a == 0 || a == 1) ? -1.57f : 0; p.sagKolY = a == 1 ? -0.8f : 0;
            p.solKolX = (a == 2 || a == 3) ? -1.57f : 0; p.solKolY = a == 3 ? 0.8f : 0;
            p.basY = (a % 2 == 0 ? 0.6f : -0.6f); p.basX = 0; p.govdeY = a < 2 ? 0.2f : -0.2f;
        });
        ekle("halay", "Halay", "Dans", 0f, (t, p) -> {
            float f = t * 6f;
            p.kollar(0, 0, 1.45f); p.sagKolX = -0.2f; p.solKolX = -0.2f;
            p.sagBacakX = -0.9f * Math.max(0, s(f)); p.solBacakX = -0.9f * Math.max(0, -s(f));
            p.kokY = -1.5f * m(f); p.govdeZ = 0.08f * s(f * 0.5f); p.basZ = 0.1f * s(f * 0.5f);
            p.sagKolZ += 0.15f * s(f * 2); p.solKolZ -= 0.15f * s(f * 2);
        });
        ekle("zeybek", "Zeybek", "Dans", 0f, (t, p) -> {
            float f = t * 2.2f;
            p.kollar(0, 0, 1.75f + 0.2f * s(f)); p.sagKolX = -0.3f * s(f); p.solKolX = 0.3f * s(f);
            float diz = Math.max(0, s(f));
            p.sagBacakX = -1.3f * diz; p.kokY = 3.5f * Math.max(0, -s(f)); p.solBacakX = 0.9f * Math.max(0, -s(f));
            p.kokDonus = 0.5f * s(f * 0.5f); p.basX = -0.15f;
        });
        ekle("horon", "Horon", "Dans", 0f, (t, p) -> {
            float f = t * 14f;
            p.kollar(-0.15f, 0, 0.9f); p.govdeX = 0.25f; p.basX = -0.1f;
            p.sagBacakX = -0.6f * Math.max(0, s(f)); p.solBacakX = -0.6f * Math.max(0, -s(f));
            p.kokY = 1.2f - 0.8f * m(f); p.govdeY = 0.12f * s(f); p.sagKolZ += 0.1f * s(f); p.solKolZ -= 0.1f * s(f);
        });
        ekle("kolbasti", "Kolbastı", "Dans", 0f, (t, p) -> {
            float f = t * 9f;
            p.sagKolX = -1.2f + 0.7f * s(f); p.solKolX = -1.2f - 0.7f * s(f); p.sagKolZ = 0.3f; p.solKolZ = -0.3f;
            p.sagBacakX = 0.7f * s(f); p.solBacakX = -0.7f * s(f); p.govdeX = 0.3f; p.kokY = 1f - 1.5f * m(f);
            p.kokDonus = 0.35f * s(f * 0.25f);
        });
        ekle("ankara", "Ankara Havası", "Dans", 0f, (t, p) -> {
            float f = t * 7f;
            p.kollar(0, 0, 2.3f); p.sagKolX = 0.35f * s(f); p.solKolX = -0.35f * s(f);
            p.govdeZ = 0.14f * s(f); p.basZ = -0.14f * s(f); p.kokY = -1f * m(f);
            p.sagBacakZ = 0.1f; p.solBacakZ = -0.1f; p.sagBacakX = 0.25f * s(f); p.solBacakX = -0.25f * s(f);
            p.kokDonus = t * 1.2f;
        });
        ekle("dab", "Dab", "Dans", 2.5f, (t, p) -> {
            float e = kis(t / 0.25f);
            p.sagKolX = -1.6f * e; p.sagKolY = -1.0f * e; p.sagKolZ = 0.3f * e;
            p.solKolX = 0; p.solKolZ = -2.1f * e; p.basX = 0.6f * e; p.basY = 0.5f * e; p.govdeZ = -0.12f * e;
        });
        ekle("tavuk", "Tavuk Dansı", "Dans", 0f, (t, p) -> {
            float f = t * 10f;
            p.sagKolX = -0.3f; p.solKolX = -0.3f; p.sagKolY = 1.3f; p.solKolY = -1.3f;
            p.sagKolZ = 1.1f + 0.5f * s(f); p.solKolZ = -1.1f - 0.5f * s(f);
            p.kokY = 2.5f + 1.5f * s(f * 0.5f); p.bacaklar(-0.5f, 0, 0.1f); p.basX = 0.25f * s(f);
        });
        ekle("ziplama", "Jumping Jack", "Dans", 0f, (t, p) -> {
            float a = dalga(t * 7f);
            p.kollar(0, 0, 0.2f + 2.6f * a); p.bacaklar(0, 0, 0.45f * a); p.kokY = -3f * s(Math.min(PI, (t * 7f) % (2 * PI)));
        });
        ekle("donus", "Topaç", "Dans", 0f, (t, p) -> {
            p.kokDonus = t * 7f; p.kollar(0, 0, 1.5f); p.sagBacakX = -0.4f; p.basX = -0.2f;
        });
        ekle("makarna", "Makarena", "Dans", 0f, (t, p) -> {
            int a = (int) (t * 2.2f) % 8; float f = t * 4.4f * PI;
            p.sagKolX = a >= 1 ? -1.57f : 0; p.solKolX = a >= 2 ? -1.57f : 0;
            if (a >= 3) { p.sagKolX = -2.5f; p.sagKolY = -0.6f; }
            if (a >= 4) { p.solKolX = -2.5f; p.solKolY = 0.6f; }
            if (a >= 5) { p.sagKolX = -0.5f; p.sagKolY = -1.0f; }
            if (a >= 6) { p.solKolX = -0.5f; p.solKolY = 1.0f; }
            if (a == 7) p.kokDonus = PI / 2 * (t * 2.2f % 1f);
            p.govdeZ = 0.1f * s(f); p.kokY = -0.6f * m(f);
        });
        ekle("dalga", "Dalga", "Dans", 0f, (t, p) -> {
            float f = t * 4f;
            p.sagKolX = 0; p.solKolX = 0; p.sagKolZ = 1.57f + 0.5f * s(f); p.solKolZ = -1.57f + 0.5f * s(f);
            p.govdeZ = 0.2f * s(f - 0.6f); p.basZ = 0.25f * s(f - 1.2f); p.kokY = 0.8f * s(f * 2);
        });
        ekle("omuz", "Omuz Oyunu", "Dans", 0f, (t, p) -> {
            float f = t * 11f;
            p.kollar(-0.2f, 0, 0.5f); p.sagKolZ = 0.5f + 0.25f * s(f); p.solKolZ = -0.5f + 0.25f * s(f);
            p.govdeZ = 0.1f * s(f); p.basZ = -0.1f * s(f); p.kokY = 1f + 0.6f * s(f * 0.5f); p.bacaklar(-0.2f, 0, 0.08f);
        });
        ekle("penguen", "Penguen", "Dans", 0f, (t, p) -> {
            float f = t * 6f;
            p.kollar(0, 0, 0.6f); p.kokYatis = 0.22f * s(f); p.sagBacakZ = 0.1f; p.solBacakZ = -0.1f;
            p.kokDonus = 0.3f * s(f * 0.5f); p.basZ = -0.15f * s(f);
        });
        ekle("zombi", "Zombi", "Dans", 0f, (t, p) -> {
            float f = t * 3f;
            p.kollar(-1.45f + 0.1f * s(f), 0, 0); p.basZ = 0.3f * s(f); p.basX = 0.1f;
            p.kokYatis = 0.1f * s(f); p.sagBacakX = 0.3f * s(f); p.solBacakX = -0.3f * s(f);
        });
        ekle("ucak", "Uçak", "Dans", 0f, (t, p) -> {
            float f = t * 2.5f;
            p.kollar(0, 0, 1.57f); p.kokYatis = 0.4f * s(f); p.govdeX = 0.25f; p.basX = -0.2f; p.kokDonus = t * 1.5f;
        });
        ekle("helikopter", "Helikopter", "Dans", 0f, (t, p) -> {
            p.sagKolX = 0; p.sagKolZ = 2.9f; p.sagKolY = t * 18f; p.kokDonus = -t * 2f; p.kokY = -1.5f - 1f * s(t * 3f);
            p.bacaklar(0.2f, 0, 0.05f);
        });
        ekle("sinav", "Mekik", "Spor", 0f, (t, p) -> {
            float a = dalga(t * 5f);
            p.kollar(-2.6f, 0, 0.3f); p.govdeX = 0.9f * a; p.basX = 0.5f * a; p.bacaklar(-1.4f, 0, 0.1f); p.kokY = 10f;
        });
        ekle("squat", "Squat", "Spor", 0f, (t, p) -> {
            float a = dalga(t * 4f);
            p.kollar(-1.5f, 0, 0); p.bacaklar(-1.1f * a, 0, 0.15f); p.kokY = 5f * a; p.govdeX = 0.35f * a;
        });
        ekle("boks", "Boks", "Spor", 0f, (t, p) -> {
            float f = t * 10f;
            p.sagKolX = -1.5f - 0.3f * Math.max(0, s(f)); p.solKolX = -1.5f - 0.3f * Math.max(0, -s(f));
            p.sagKolY = -0.5f + 0.4f * Math.max(0, s(f)); p.solKolY = 0.5f - 0.4f * Math.max(0, -s(f));
            p.govdeY = 0.25f * s(f); p.kokY = -0.5f * m(f * 2); p.sagBacakX = 0.3f; p.solBacakX = -0.3f;
        });
        ekle("takla", "Takla", "Spor", 1.2f, (t, p) -> {
            float a = kis(t / 1.0f);
            p.kokEgim = -2 * PI * a; p.kokY = -9f * s(PI * a); p.kollar(-2.8f, 0, 0); p.bacaklar(-0.9f * s(PI * a), 0, 0);
        });

        // ---- Duruşlar ----
        ekle("otur", "Otur", "Duruş", 0f, (t, p) -> {
            p.bacaklar(-1.5f, 0.35f, 0.1f); p.kokY = 10f; p.kollar(-0.55f, 0, 0.1f); p.basX += 0.03f * s(t * 2f);
        });
        ekle("uzan", "Uzan", "Duruş", 0f, (t, p) -> {
            p.kokEgim = -PI / 2; p.kokY = 21f; p.kollar(-2.9f, 0, 0.5f); p.bacaklar(0, 0, 0.15f);
            p.sagBacakX = -0.25f; p.basX = -0.2f;
        });
        ekle("bagdas", "Bağdaş Kur", "Duruş", 0f, (t, p) -> {
            p.sagBacakX = -1.5f; p.solBacakX = -1.5f; p.sagBacakY = 0.9f; p.solBacakY = -0.9f; p.kokY = 10f;
            p.kollar(-0.7f, 0, 0.35f); p.basX += 0.03f * s(t * 2f);
        });
        ekle("kollarini_kavustur", "Kollarını Kavuştur", "Duruş", 0f, (t, p) -> {
            p.sagKolX = -0.9f; p.solKolX = -0.9f; p.sagKolY = -0.9f; p.solKolY = 0.9f; p.govdeX = -0.05f; p.basX = -0.05f;
        });
        ekle("super", "Süper Kahraman", "Duruş", 0f, (t, p) -> {
            p.kollar(0, 0, 0.75f); p.sagKolX = -0.2f; p.solKolX = -0.2f; p.sagKolY = 0.9f; p.solKolY = -0.9f;
            p.bacaklar(0, 0, 0.2f); p.basX = -0.25f;
        });
        ekle("goster", "İşaret Et", "Duruş", 3f, (t, p) -> {
            p.sagKolX = -1.57f + p.basX; p.sagKolY = p.basY; p.sagKolZ = 0;
        });
    }

    private Emotlar() {}
}
