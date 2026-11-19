package tr.com.hapicraft.client;

import org.junit.jupiter.api.Test;
import tr.com.hapicraft.client.kozmetik.Kozmetik;
import tr.com.hapicraft.client.kozmetik.KozmetikKatmani;
import tr.com.hapicraft.client.kozmetik.Kozmetikler;
import tr.com.hapicraft.client.kozmetik.VokselModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Üretilen katalog, doku ve modellerin modun beklediği biçimde olduğunu sınar. */
class KozmetikTest {
    private static boolean varMi(String yol) {
        return KozmetikTest.class.getResource("/assets/hapiclient/" + yol) != null;
    }

    @Test
    void katalogHerYuvadaDoluVeKimliklerTekil() {
        Set<String> kimlikler = new HashSet<>();
        for (String yuva : Kozmetikler.YUVALAR) {
            List<Kozmetik> liste = Kozmetikler.yuvadakiler(yuva);
            assertTrue(liste.size() >= 5, yuva + " yuvasında " + liste.size() + " kozmetik var");
            for (Kozmetik k : liste) {
                assertTrue(kimlikler.add(k.kimlik()), "yinelenen kimlik " + k.kimlik());
                assertFalse(k.ad().isBlank(), k.kimlik());
                assertFalse(k.aciklama().isBlank(), k.kimlik());
                assertTrue(varMi("textures/kozmetik/ikon/" + k.kimlik() + ".png"), "ikon yok: " + k.kimlik());
            }
        }
        assertEquals(kimlikler.size(), Kozmetikler.hepsi().size(), "tanınmayan yuvada kozmetik var");
    }

    @Test
    void kanatVePelerinDokulariYerinde() {
        for (Kozmetik k : Kozmetikler.yuvadakiler("kanat")) {
            assertTrue(varMi("textures/kozmetik/kanat/" + k.doku() + ".png"), k.kimlik());
            assertEquals(k.isik(), varMi("textures/kozmetik/kanat/" + k.doku() + "_isik.png"), k.kimlik());
            assertTrue(k.en() > 8 && k.en() < 32 && k.boy() > 8 && k.boy() < 32, k.kimlik() + " boyutu");
            assertTrue(k.kok() > 0.1f && k.kok() < 0.9f, k.kimlik() + " kökü");
            for (float[] d : k.disliler()) assertEquals(4, d.length, k.kimlik());
        }
        for (Kozmetik k : Kozmetikler.yuvadakiler("pelerin"))
            assertTrue(varMi("textures/kozmetik/pelerin/" + k.doku() + ".png"), k.kimlik());
        assertTrue(varMi("textures/kozmetik/disli.png"));
    }

    @Test
    void sapkaVeHaleModelleriCizilebilir() {
        for (String yuva : new String[]{"sapka", "hale"}) for (Kozmetik k : Kozmetikler.yuvadakiler(yuva)) {
            VokselModel m = VokselModel.al(k.model());
            assertFalse(m.parcalar().isEmpty(), k.kimlik() + " modeli boş");
            int dortgen = 0;
            for (VokselModel.Parca p : m.parcalar()) {
                assertTrue(p.dortgenSayisi() > 0, k.kimlik() + " boş parça");
                dortgen += p.dortgenSayisi();
            }
            // Her karede yeniden yazıldığı için model başına dörtgen sayısı sınırlı kalmalı.
            assertTrue(dortgen < 9000, k.kimlik() + ": " + dortgen + " dörtgen");
        }
    }

    @Test
    void kanatAcilariMakulAraliktaVeHareketli() {
        for (String hareket : new String[]{"tuy", "zar", "kelebek", "bocek", "mekanik", "kristal"}) {
            float enAz = 9, enCok = -9;
            for (float t = 0; t < 400; t += 0.5f) {
                for (boolean egik : new boolean[]{false, true}) {
                    float[] a = KozmetikKatmani.kanatAcilari(hareket, t, egik, t % 2);
                    assertEquals(KozmetikKatmani.EKLEM, a.length);
                    for (float v : a) assertTrue(v > -0.35f && v < 2.3f, hareket + " açısı " + v);
                }
                float kok = KozmetikKatmani.kanatAcilari(hareket, t, false, 0)[0];
                enAz = Math.min(enAz, kok);
                enCok = Math.max(enCok, kok);
            }
            assertTrue(enCok - enAz > 0.15f, hareket + " kanadı çırpmıyor");
        }
    }

    /** Bilinen değerler: hareket hesabı fark edilmeden değişmesin. */
    @Test
    void kanatAcilariSabit() {
        float[] tuy = {0.741f, 0.774f, 0.789f, 0.789f, 0.775f, 0.754f, 0.729f, 0.706f};
        float[] mekanik = {0.946f, 0.964f, 0.97f, 0.964f, 0.946f, 0.916f, 0.886f, 0.856f};
        assertArrayEquals(tuy, KozmetikKatmani.kanatAcilari("tuy", 37.5f, false, 0.4f), 0.002f);
        assertArrayEquals(mekanik, KozmetikKatmani.kanatAcilari("mekanik", 120f, true, 0f), 0.002f);
    }
}
