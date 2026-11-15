package tr.com.hapicraft.client.proxy.cekirdek;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KozmetiklerTest {

    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void gecerliSecimAynenKalirVeYuvaSirasiSabit() {
        Map<String, String> s = Kozmetikler.dogrula(
                json("{\"aura\":\"alev\",\"pelerin\":\"kirmizi\",\"kanat\":\"\",\"sapka\":\"tac\",\"hale\":\"altin\"}"),
                k -> true);
        assertEquals(List.of("pelerin", "kanat", "sapka", "hale", "aura"), List.copyOf(s.keySet()));
        assertEquals(Map.of("pelerin", "kirmizi", "kanat", "", "sapka", "tac", "hale", "altin", "aura", "alev"), s);
    }

    @Test
    void bozukAlanlarBosaltilir() {
        Map<String, String> s = Kozmetikler.dogrula(json(
                "{\"pelerin\":\"Kırmızı\",\"kanat\":5,\"sapka\":null,\"hale\":[\"x\"],\"aura\":\"" + "a".repeat(33)
                        + "\",\"bilinmeyen\":\"x\",\"kuyruk\":\"tilki\"}"), k -> true);
        assertEquals(Kozmetikler.bos(), s);
        assertEquals(5, s.size());
        assertEquals(Kozmetikler.bos(), Kozmetikler.dogrula(new JsonObject(), k -> true));
    }

    @Test
    void sahipOlunmayanYuvaBosaltilirDigerleriKalir() {
        Map<String, String> s = Kozmetikler.dogrula(
                json("{\"pelerin\":\"vip_pelerin\",\"sapka\":\"tac\"}"), k -> k.equals("tac"));
        assertEquals("", s.get("pelerin"));
        assertEquals("tac", s.get("sapka"));
    }

    @Test
    void sonucDegistirilemez() {
        Map<String, String> s = Kozmetikler.dogrula(new JsonObject(), k -> true);
        assertThrows(UnsupportedOperationException.class, () -> s.put("pelerin", "x"));
        assertThrows(UnsupportedOperationException.class, () -> Kozmetikler.bos().put("pelerin", "x"));
    }

    @Test
    void sahiplikKurallari() {
        // ücretsiz listesinde
        assertTrue(Kozmetikler.sahipMi("tac", Set.of("tac"), i -> false));
        assertFalse(Kozmetikler.sahipMi("kanat1", Set.of("tac"), i -> false));
        // ücretsiz = *
        assertTrue(Kozmetikler.sahipMi("kanat1", Set.of("*"), i -> false));
        // tekil izin
        assertTrue(Kozmetikler.sahipMi("kanat1", Set.of(), i -> i.equals("hapiclient.kozmetik.kanat1")));
        assertFalse(Kozmetikler.sahipMi("kanat2", Set.of(), i -> i.equals("hapiclient.kozmetik.kanat1")));
        // joker izin
        assertTrue(Kozmetikler.sahipMi("kanat2", Set.of(), i -> i.equals("hapiclient.kozmetik.*")));
    }

    @Test
    void sahipListesi() {
        assertEquals(List.of("*"), Kozmetikler.sahipListesi(Set.of("*"), List.of("a"), i -> false));
        assertEquals(List.of("*"), Kozmetikler.sahipListesi(Set.of("tac"), List.of("a"),
                i -> i.equals("hapiclient.kozmetik.*")));
        assertEquals(List.of("tac"), Kozmetikler.sahipListesi(Set.of("tac"), List.of("vip1", "vip2"), i -> false));
        assertEquals(List.of("tac", "vip2"), Kozmetikler.sahipListesi(Set.of("tac"), List.of("vip1", "vip2", "tac"),
                i -> i.equals("hapiclient.kozmetik.vip2")));
        assertEquals(List.of(), Kozmetikler.sahipListesi(Set.of(), List.of(), i -> false));
    }

    @Test
    void saklananSecimYenidenDogrulanir() {
        Map<String, String> s = Kozmetikler.dogrula(Map.of("pelerin", "vip", "sapka", "tac", "yabanci", "x"),
                k -> k.equals("tac"));
        assertEquals(Map.of("pelerin", "", "kanat", "", "sapka", "tac", "hale", "", "aura", ""), s);
        assertFalse(Kozmetikler.bosMu(s));
        assertTrue(Kozmetikler.bosMu(Kozmetikler.bos()));
    }
}
