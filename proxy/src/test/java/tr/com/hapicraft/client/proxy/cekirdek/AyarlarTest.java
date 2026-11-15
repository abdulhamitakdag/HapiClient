package tr.com.hapicraft.client.proxy.cekirdek;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AyarlarTest {

    @Test
    void varsayilanlar() {
        Ayarlar a = Ayarlar.varsayilan();
        assertEquals(List.of(), a.kapali("Towny2"));
        assertEquals(List.of("tamparlaklik"), a.kapali("Pillars"));
        assertEquals(Set.of("*"), a.ucretsiz());
        assertEquals(List.of(), a.ucretli());
        assertEquals(10, a.indirim());
        assertTrue(a.izinsizModlar(List.of("herhangi@1")).isEmpty());
    }

    @Test
    void eksikAlanlarVarsayilanAlir() {
        Ayarlar a = Ayarlar.coz("{}");
        assertEquals(Set.of("*"), a.ucretsiz());
        assertEquals(10, a.indirim());
        assertEquals(List.of(), a.kapali("Pillars"));
        assertEquals(List.of(), a.kapali(null));
    }

    @Test
    void sunucuyaOzelListeGenelinYerineGecerVeBuyukKucukHarfDuyarsiz() {
        Ayarlar a = Ayarlar.coz("{\"kapali\":[\"yakinlastirma\"],\"sunucu_kapali\":{\"Pillars\":[\"tamparlaklik\"],\"lobi\":[]}}");
        assertEquals(List.of("yakinlastirma"), a.kapali("Towny2"));
        assertEquals(List.of("tamparlaklik"), a.kapali("pillars"));
        assertEquals(List.of("tamparlaklik"), a.kapali("PILLARS"));
        assertEquals(List.of(), a.kapali("Lobi"));
    }

    @Test
    void indirimSinirlanir() {
        assertEquals(100, Ayarlar.coz("{\"indirim\":250}").indirim());
        assertEquals(0, Ayarlar.coz("{\"indirim\":-5}").indirim());
        assertEquals(25, Ayarlar.coz("{\"indirim\":25}").indirim());
    }

    @Test
    void bozukConfigReddedilir() {
        for (String bozuk : List.of("", "{", "[]", "{\"kapali\":\"x\"}", "{\"ucretsiz\":[1]}",
                "{\"indirim\":\"on\"}", "{\"sunucu_kapali\":[]}", "{\"sunucu_kapali\":{\"a\":\"b\"}}")) {
            assertThrows(IllegalArgumentException.class, () -> Ayarlar.coz(bozuk), bozuk);
        }
    }

    @Test
    void izinliModDenetimi() {
        Ayarlar a = Ayarlar.coz("{\"izinli_modlar\":[\"sodium\",\"Lithium\",\"hapiclient\"]}");
        assertEquals(List.of("wurst@7.0", "meteor"), a.izinsizModlar(
                List.of("sodium@0.8.15", "lithium@0.21", "wurst@7.0", "HapiClient@0.1.0", "meteor")));
        assertTrue(a.izinsizModlar(List.of()).isEmpty());
    }
}
