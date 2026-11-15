package tr.com.hapicraft.client.proxy.cekirdek;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtokolTest {

    static byte[] ham(int no, String json) {
        byte[] g = json.getBytes(StandardCharsets.UTF_8);
        byte[] v = new byte[g.length + 1];
        v[0] = (byte) no;
        System.arraycopy(g, 0, v, 1, g.length);
        return v;
    }

    @Test
    void kodlananPaketGeriCozulur() throws Exception {
        JsonObject o = new JsonObject();
        o.addProperty("metin", "çğıöşü İĞ <b>");
        byte[] veri = Protokol.kodla(Protokol.BILDIRIM, o);
        assertEquals(20, veri[0]);
        // HTML kaçışı yok, Türkçe karakterler ham UTF-8
        assertTrue(new String(veri, 1, veri.length - 1, StandardCharsets.UTF_8).contains("çğıöşü İĞ <b>"));
        Protokol.Paket p = Protokol.coz(veri);
        assertEquals(Protokol.BILDIRIM, p.no());
        assertEquals("çğıöşü İĞ <b>", p.govde().get("metin").getAsString());
    }

    @Test
    void paketNumarasiIsaretsizOkunur() throws Exception {
        assertEquals(200, Protokol.coz(ham(200, "{}")).no());
    }

    @Test
    void bozukGirdilerProtokolHatasiVerir() {
        List<byte[]> bozuklar = List.of(
                new byte[0],
                new byte[]{1},
                ham(1, "{"),
                ham(1, "merhaba"),
                ham(1, "[1,2]"),
                ham(1, "\"metin\""),
                ham(1, "null"),
                ham(1, "{\"a\":}"),
                new byte[]{1, (byte) 0xFF, (byte) 0xFE, 0x00},
                ham(1, "[".repeat(8000)),
                ham(1, "{\"a\":".repeat(2000)));
        for (byte[] b : bozuklar) {
            assertThrows(Protokol.ProtokolHatasi.class, () -> Protokol.coz(b), Arrays.toString(Arrays.copyOf(b, Math.min(b.length, 12))));
        }
        assertThrows(Protokol.ProtokolHatasi.class, () -> Protokol.coz(null));
    }

    @Test
    void onAltiKbSiniri() throws Exception {
        String dolgu = "a".repeat(Protokol.MAX_BOYUT - 1 - "{\"a\":\"\"}".length());
        byte[] tamSinir = ham(1, "{\"a\":\"" + dolgu + "\"}");
        assertEquals(Protokol.MAX_BOYUT, tamSinir.length);
        Protokol.coz(tamSinir);

        byte[] birFazla = ham(1, "{\"a\":\"" + dolgu + "a\"}");
        assertThrows(Protokol.ProtokolHatasi.class, () -> Protokol.coz(birFazla));

        JsonObject buyuk = new JsonObject();
        buyuk.addProperty("a", "x".repeat(Protokol.MAX_BOYUT));
        assertThrows(IllegalArgumentException.class, () -> Protokol.kodla(1, buyuk));
    }

    @Test
    void kimlikDeseni() {
        assertTrue(Protokol.kimlikGecerli(""));
        assertTrue(Protokol.kimlikGecerli("alkis_2"));
        assertTrue(Protokol.kimlikGecerli("a".repeat(32)));
        assertFalse(Protokol.kimlikGecerli("a".repeat(33)));
        assertFalse(Protokol.kimlikGecerli("Alkis"));
        assertFalse(Protokol.kimlikGecerli("alkış"));
        assertFalse(Protokol.kimlikGecerli("a b"));
        assertFalse(Protokol.kimlikGecerli("a\n"));
        assertFalse(Protokol.kimlikGecerli(null));
    }

    @Test
    void merhabaNormalGovde() throws Exception {
        Protokol.Merhaba m = Protokol.merhabaCoz(Protokol.coz(
                ham(1, "{\"surum\":\"0.1.0\",\"proto\":1,\"modlar\":[\"sodium@0.8.15\",\"lithium@0.21\"]}")).govde());
        assertEquals("0.1.0", m.surum());
        assertEquals(1, m.proto());
        assertEquals(List.of("sodium@0.8.15", "lithium@0.21"), m.modlar());
    }

    @Test
    void merhabaModListesiKesilir() {
        JsonObject g = new JsonObject();
        JsonArray modlar = new JsonArray();
        for (int i = 0; i < 250; i++) {
            modlar.add("m" + i + "-" + "x".repeat(100));
        }
        g.add("modlar", modlar);
        Protokol.Merhaba m = Protokol.merhabaCoz(g);
        assertEquals(200, m.modlar().size());
        for (String mod : m.modlar()) {
            assertEquals(80, mod.length());
        }
        assertTrue(m.modlar().get(199).startsWith("m199-"));
    }

    @Test
    void merhabaYanlisTurlerVeEksikAlanlar() throws Exception {
        Protokol.Merhaba bos = Protokol.merhabaCoz(new JsonObject());
        assertEquals("?", bos.surum());
        assertEquals(0, bos.proto());
        assertTrue(bos.modlar().isEmpty());

        Protokol.Merhaba m = Protokol.merhabaCoz(Protokol.coz(ham(1,
                "{\"surum\":{\"a\":1},\"proto\":\"bir\",\"modlar\":[1,null,{\"a\":2},[\"x\"],\"\",\"gecerli\",true]}")).govde());
        assertEquals("?", m.surum());
        assertEquals(0, m.proto());
        assertEquals(List.of("gecerli"), m.modlar());

        Protokol.Merhaba m2 = Protokol.merhabaCoz(Protokol.coz(ham(1,
                "{\"surum\":\"1.0\\n\\u001b[31m" + "s".repeat(100) + "\",\"proto\":1e99,\"modlar\":\"sodium\"}")).govde());
        assertEquals(32, m2.surum().length());
        assertTrue(m2.surum().startsWith("1.0[31ms"));
        assertTrue(m2.modlar().isEmpty());
    }

    @Test
    void temizleVekilCiftiniBolmez() {
        String s = "ab😀";
        assertEquals("ab", Protokol.temizle(s, 3));
        assertEquals(s, Protokol.temizle(s, 4));
        assertEquals("", Protokol.temizle(null, 5));
    }

    @Test
    void hosgeldinGovdesi() throws Exception {
        Protokol.Paket p = Protokol.coz(Protokol.hosgeldin(List.of("tamparlaklik"), List.of("*"), 10, "Towny2"));
        assertEquals(Protokol.HOSGELDIN, p.no());
        assertEquals("{\"proto\":1,\"kapali\":[\"tamparlaklik\"],\"sahip\":[\"*\"],\"indirim\":10,\"sunucu\":\"Towny2\"}",
                p.govde().toString());
    }

    @Test
    void oyuncularGovdesiVeBosListe() throws Exception {
        UUID u = UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<byte[]> paketler = Protokol.oyuncular(
                List.of(new Protokol.Kayit(u, Kozmetikler.dogrula(Map.of("pelerin", "kirmizi"), k -> true))), false);
        assertEquals(1, paketler.size());
        assertEquals("{\"liste\":[{\"uuid\":\"00000000-0000-0000-0000-000000000001\",\"k\":{\"pelerin\":\"kirmizi\","
                        + "\"kanat\":\"\",\"sapka\":\"\",\"hale\":\"\",\"aura\":\"\"}}],\"sifirla\":false}",
                Protokol.coz(paketler.get(0)).govde().toString());

        List<byte[]> bos = Protokol.oyuncular(List.of(), true);
        assertEquals(1, bos.size());
        assertEquals("{\"liste\":[],\"sifirla\":true}", Protokol.coz(bos.get(0)).govde().toString());
    }

    @Test
    void oyuncularBuyukListeParcalanirVeSinirinAltindaKalir() throws Exception {
        String uzun = "a".repeat(32);
        Map<String, String> dolu = Kozmetikler.dogrula(
                Map.of("pelerin", uzun, "kanat", uzun, "sapka", uzun, "hale", uzun, "aura", uzun), k -> true);
        List<Protokol.Kayit> kayitlar = new ArrayList<>();
        for (int i = 0; i < 205; i++) {
            kayitlar.add(new Protokol.Kayit(new UUID(7, i), dolu));
        }
        List<byte[]> paketler = Protokol.oyuncular(kayitlar, true);
        assertEquals(6, paketler.size());
        int toplam = 0;
        for (int i = 0; i < paketler.size(); i++) {
            assertTrue(paketler.get(i).length <= Protokol.MAX_BOYUT);
            JsonObject g = Protokol.coz(paketler.get(i)).govde();
            assertEquals(i == 0, g.get("sifirla").getAsBoolean(), "sifirla yalnız ilk parçada");
            toplam += g.getAsJsonArray("liste").size();
        }
        assertEquals(205, toplam);
    }

    @Test
    void digerSunucuPaketleri() throws Exception {
        UUID u = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertEquals("{\"uuid\":\"" + u + "\"}", Protokol.coz(Protokol.ayrildi(u)).govde().toString());
        Protokol.Paket e = Protokol.coz(Protokol.emote(u, "alkis"));
        assertEquals(Protokol.EMOTE_YAYIN, e.no());
        assertEquals("{\"uuid\":\"" + u + "\",\"id\":\"alkis\"}", e.govde().toString());
        Protokol.Paket b = Protokol.coz(Protokol.bildirim("Başlık", "ş".repeat(20000)));
        assertEquals("Başlık", b.govde().get("baslik").getAsString());
        assertEquals(Protokol.MAX_METIN_UZUNLUK, b.govde().get("metin").getAsString().length());
    }
}
