package tr.com.hapicraft.client.proxy.cekirdek;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CekirdekTest {

    @TempDir
    Path dizin;

    private record Giden(UUID hedef, int no, JsonObject govde) {
    }

    private final List<Giden> gidenler = new CopyOnWriteArrayList<>();
    private final Set<String> izinler = ConcurrentHashMap.newKeySet();
    private final List<String> uyarilar = new CopyOnWriteArrayList<>();
    private final List<String> bilgiler = new CopyOnWriteArrayList<>();
    private volatile Ayarlar ayarlar = Ayarlar.varsayilan();
    private volatile long saat = 1_000_000;
    private KozmetikDeposu depo;
    private Cekirdek c;

    private final UUID ali = new UUID(0, 1);
    private final UUID veli = new UUID(0, 2);
    private final UUID ayse = new UUID(0, 3);

    @BeforeEach
    void kur() {
        depo = new KozmetikDeposu(dizin.resolve("kozmetikler.json"), 3_600_000, uyarilar::add);
        c = new Cekirdek(
                (hedef, veri) -> {
                    try {
                        Protokol.Paket p = Protokol.coz(veri);
                        gidenler.add(new Giden(hedef, p.no(), p.govde()));
                    } catch (Protokol.ProtokolHatasi e) {
                        throw new AssertionError(e);
                    }
                },
                (oyuncu, izin) -> izinler.contains(oyuncu + ":" + izin),
                new Cekirdek.Gunluk() {
                    @Override
                    public void bilgi(String mesaj) {
                        bilgiler.add(mesaj);
                    }

                    @Override
                    public void uyari(String mesaj) {
                        uyarilar.add(mesaj);
                    }
                },
                () -> ayarlar, depo, () -> saat);
    }

    @AfterEach
    void kapat() {
        depo.close();
    }

    private void merhaba(UUID u, String ad, String sunucu) {
        c.paketGeldi(u, ad, sunucu, ProtokolTest.ham(1, "{\"surum\":\"0.1.0\",\"proto\":1,\"modlar\":[\"sodium@0.8.15\"]}"));
    }

    private void kozmetik(UUID u, String json) {
        c.paketGeldi(u, "x", "onemsiz", ProtokolTest.ham(2, json));
    }

    private void emote(UUID u, String id) {
        c.paketGeldi(u, "x", "onemsiz", ProtokolTest.ham(3, "{\"id\":\"" + id + "\"}"));
    }

    private List<Giden> al(UUID hedef) {
        List<Giden> liste = new ArrayList<>();
        for (Giden g : gidenler) {
            if (g.hedef().equals(hedef)) {
                liste.add(g);
            }
        }
        return liste;
    }

    private static Set<String> uuidler(JsonObject oyuncular) {
        Set<String> s = new HashSet<>();
        oyuncular.getAsJsonArray("liste").forEach(e -> s.add(e.getAsJsonObject().get("uuid").getAsString()));
        return s;
    }

    @Test
    void merhabaHosgeldinVeBosListeIleYanitlanir() {
        merhaba(ali, "Ali", "Towny2");

        assertTrue(c.kullaniyorMu(ali));
        assertFalse(c.kullaniyorMu(veli));
        assertEquals("0.1.0", c.oturum(ali).orElseThrow().surum());
        assertEquals(List.of("sodium@0.8.15"), c.oturum("ALI").orElseThrow().modlar());

        List<Giden> g = al(ali);
        assertEquals(2, g.size());
        assertEquals(Protokol.HOSGELDIN, g.get(0).no());
        assertEquals("{\"proto\":1,\"kapali\":[],\"sahip\":[\"*\"],\"indirim\":10,\"sunucu\":\"Towny2\"}",
                g.get(0).govde().toString());
        assertEquals(Protokol.OYUNCULAR, g.get(1).no());
        assertEquals("{\"liste\":[],\"sifirla\":true}", g.get(1).govde().toString());
        assertEquals(1, bilgiler.size());
    }

    @Test
    void hosgeldinSunucuyaOzelKapaliVeIzinlereGoreSahip() {
        ayarlar = Ayarlar.coz("{\"sunucu_kapali\":{\"Pillars\":[\"tamparlaklik\"]},\"ucretsiz\":[\"tac\"],"
                + "\"ucretli\":[\"vip_pelerin\",\"vip_kanat\"],\"indirim\":15}");
        izinler.add(ali + ":hapiclient.kozmetik.vip_kanat");
        izinler.add(veli + ":hapiclient.kozmetik.*");

        merhaba(ali, "Ali", "Pillars");
        merhaba(veli, "Veli", "Towny2");

        assertEquals("{\"proto\":1,\"kapali\":[\"tamparlaklik\"],\"sahip\":[\"tac\",\"vip_kanat\"],\"indirim\":15,"
                + "\"sunucu\":\"Pillars\"}", al(ali).get(0).govde().toString());
        assertEquals("{\"proto\":1,\"kapali\":[],\"sahip\":[\"*\"],\"indirim\":15,\"sunucu\":\"Towny2\"}",
                al(veli).get(0).govde().toString());
    }

    @Test
    void ayniSunucudakilerBirbiriniGorurBaskaSunucudakiGormez() {
        merhaba(ali, "Ali", "Towny2");
        kozmetik(ali, "{\"pelerin\":\"kirmizi\"}");
        merhaba(ayse, "Ayşe", "Skyblock");
        gidenler.clear();

        merhaba(veli, "Veli", "Towny2");

        // Veli: HOSGELDIN + Ali'nin kozmetikleriyle tam liste
        List<Giden> v = al(veli);
        assertEquals(2, v.size());
        assertTrue(v.get(1).govde().get("sifirla").getAsBoolean());
        JsonArray liste = v.get(1).govde().getAsJsonArray("liste");
        assertEquals(1, liste.size());
        assertEquals(ali.toString(), liste.get(0).getAsJsonObject().get("uuid").getAsString());
        assertEquals("kirmizi", liste.get(0).getAsJsonObject().getAsJsonObject("k").get("pelerin").getAsString());

        // Ali: Veli'nin tek kayıtlı duyurusu
        List<Giden> a = al(ali);
        assertEquals(1, a.size());
        assertEquals(Protokol.OYUNCULAR, a.get(0).no());
        assertFalse(a.get(0).govde().get("sifirla").getAsBoolean());
        assertEquals(Set.of(veli.toString()), uuidler(a.get(0).govde()));

        // Skyblock'taki Ayşe hiçbir şey almaz
        assertTrue(al(ayse).isEmpty());
    }

    @Test
    void merhabaTekrariZararsiz() {
        merhaba(ali, "Ali", "Towny2");
        merhaba(veli, "Veli", "Towny2");
        gidenler.clear();
        bilgiler.clear();

        merhaba(veli, "Veli", "Towny2");

        assertEquals(2, c.oturumlar().size());
        // Veli yeniden HOSGELDIN + tam liste alır; kimseye AYRILDI gitmez
        assertEquals(List.of(Protokol.HOSGELDIN, Protokol.OYUNCULAR), al(veli).stream().map(Giden::no).toList());
        assertEquals(Set.of(ali.toString()), uuidler(al(veli).get(1).govde()));
        assertEquals(List.of(Protokol.OYUNCULAR), al(ali).stream().map(Giden::no).toList());
        assertTrue(bilgiler.isEmpty(), "bağlandı günlüğü yalnız ilk MERHABA'da");
    }

    @Test
    void backendGecisiSenaryosu() {
        merhaba(ali, "Ali", "Cakmalobi");
        merhaba(veli, "Veli", "Cakmalobi");
        merhaba(ayse, "Ayşe", "Towny2");
        kozmetik(veli, "{\"sapka\":\"tac\"}");
        gidenler.clear();

        // 1) Veli Towny2'ye geçer: önce proxy olayı gelir.
        c.sunucuDegisti(veli, "Towny2");
        assertEquals(1, gidenler.size());
        assertEquals(new Giden(ali, Protokol.AYRILDI, ayrildiGovde(veli)), gidenler.get(0));
        assertTrue(c.kullaniyorMu(veli), "geçiş sırasında HapiClient işareti korunur");
        assertEquals(null, c.oturum(veli).orElseThrow().sunucu());

        // Geçiş sürerken Veli hiçbir backend'in listesinde değildir ve dansı kimseye gitmez.
        gidenler.clear();
        emote(veli, "alkis");
        merhaba(ali, "Ali", "Cakmalobi");
        assertTrue(al(ali).get(1).govde().getAsJsonArray("liste").isEmpty());
        assertTrue(al(ayse).isEmpty());

        // 2) İstemci yeni backend'de MERHABA gönderir.
        gidenler.clear();
        merhaba(veli, "Veli", "Towny2");
        assertEquals("Towny2", al(veli).get(0).govde().get("sunucu").getAsString());
        assertEquals(Set.of(ayse.toString()), uuidler(al(veli).get(1).govde()));
        List<Giden> a = al(ayse);
        assertEquals(1, a.size());
        assertEquals("tac", a.get(0).govde().getAsJsonArray("liste").get(0).getAsJsonObject()
                .getAsJsonObject("k").get("sapka").getAsString(), "saklanan kozmetik yeni backend'e taşınır");
        assertTrue(al(ali).isEmpty());

        // 3) Olay tekrarı (ya da MERHABA'dan sonra gelen olay) hiçbir şey yapmaz.
        gidenler.clear();
        c.sunucuDegisti(veli, "Towny2");
        assertTrue(gidenler.isEmpty());
        assertEquals("Towny2", c.oturum(veli).orElseThrow().sunucu());
    }

    @Test
    void merhabaOlaydanOnceGelirseEskiBackendeAyrildiGider() {
        merhaba(ali, "Ali", "Cakmalobi");
        merhaba(veli, "Veli", "Cakmalobi");
        gidenler.clear();

        merhaba(veli, "Veli", "Towny2");

        assertEquals(List.of(new Giden(ali, Protokol.AYRILDI, ayrildiGovde(veli))), al(ali));
        assertEquals("Towny2", c.oturum(veli).orElseThrow().sunucu());
    }

    @Test
    void cikistaAyniSunucudakilereAyrildiGider() {
        merhaba(ali, "Ali", "Towny2");
        merhaba(veli, "Veli", "Towny2");
        merhaba(ayse, "Ayşe", "Skyblock");
        gidenler.clear();

        c.ayrildi(veli);
        c.ayrildi(veli);
        c.ayrildi(new UUID(9, 9));

        assertFalse(c.kullaniyorMu(veli));
        assertEquals(List.of(new Giden(ali, Protokol.AYRILDI, ayrildiGovde(veli))), List.copyOf(gidenler));
    }

    @Test
    void kozmetikSahiplikDenetimiVeDuyuru() {
        ayarlar = Ayarlar.coz("{\"ucretsiz\":[\"tac\"]}");
        izinler.add(ali + ":hapiclient.kozmetik.vip_kanat");
        merhaba(ali, "Ali", "Towny2");
        merhaba(veli, "Veli", "Towny2");
        merhaba(ayse, "Ayşe", "Skyblock");
        gidenler.clear();

        kozmetik(ali, "{\"pelerin\":\"vip_pelerin\",\"kanat\":\"vip_kanat\",\"sapka\":\"tac\",\"hale\":\"BÜYÜK\",\"aura\":7}");

        Map<String, String> beklenen = Map.of("pelerin", "", "kanat", "vip_kanat", "sapka", "tac", "hale", "", "aura", "");
        assertEquals(beklenen, depo.al(ali));
        // Gönderen dahil aynı backend'deki herkes tek kayıtlı OYUNCULAR alır; Skyblock almaz.
        assertEquals(2, gidenler.size());
        for (UUID hedef : List.of(ali, veli)) {
            Giden g = al(hedef).get(0);
            assertEquals(Protokol.OYUNCULAR, g.no());
            assertEquals("{\"liste\":[{\"uuid\":\"" + ali + "\",\"k\":{\"pelerin\":\"\",\"kanat\":\"vip_kanat\","
                    + "\"sapka\":\"tac\",\"hale\":\"\",\"aura\":\"\"}}],\"sifirla\":false}", g.govde().toString());
        }

        // Joker izin her şeyi açar.
        izinler.add(veli + ":hapiclient.kozmetik.*");
        kozmetik(veli, "{\"pelerin\":\"vip_pelerin\"}");
        assertEquals("vip_pelerin", depo.al(veli).get("pelerin"));
    }

    @Test
    void izniBitenKozmetikSonrakiMerhabadaDuser() {
        ayarlar = Ayarlar.coz("{\"ucretsiz\":[\"tac\"]}");
        izinler.add(ali + ":hapiclient.kozmetik.vip_kanat");
        merhaba(ali, "Ali", "Towny2");
        kozmetik(ali, "{\"kanat\":\"vip_kanat\",\"sapka\":\"tac\"}");
        merhaba(veli, "Veli", "Towny2");

        izinler.clear();
        gidenler.clear();
        merhaba(ali, "Ali", "Towny2");

        assertEquals("", depo.al(ali).get("kanat"));
        assertEquals("tac", depo.al(ali).get("sapka"));
        JsonObject k = al(veli).get(0).govde().getAsJsonArray("liste").get(0).getAsJsonObject().getAsJsonObject("k");
        assertEquals("", k.get("kanat").getAsString());
        assertEquals("tac", k.get("sapka").getAsString());
    }

    @Test
    void merhabaGondermeyenOyuncununPaketleriYokSayilir() {
        merhaba(ali, "Ali", "Towny2");
        gidenler.clear();

        kozmetik(veli, "{\"pelerin\":\"kirmizi\"}");
        emote(veli, "alkis");

        assertTrue(gidenler.isEmpty());
        assertEquals(Kozmetikler.bos(), depo.al(veli));
        assertFalse(c.kullaniyorMu(veli));
    }

    @Test
    void emoteGonderenHaricAyniSunucuyaIletilirVeHizSinirlidir() {
        merhaba(ali, "Ali", "Towny2");
        merhaba(veli, "Veli", "Towny2");
        merhaba(ayse, "Ayşe", "Skyblock");
        gidenler.clear();

        emote(ali, "alkis");
        emote(ali, "");
        emote(ali, "dans");
        saat += 999;
        emote(ali, "dans");

        assertEquals(2, gidenler.size());
        assertEquals(new Giden(veli, Protokol.EMOTE_YAYIN, emoteGovde(ali, "alkis")), gidenler.get(0));
        assertEquals(new Giden(veli, Protokol.EMOTE_YAYIN, emoteGovde(ali, "")), gidenler.get(1));

        saat += 1;
        emote(ali, "dans");
        assertEquals(3, gidenler.size());
        assertEquals(emoteGovde(ali, "dans"), gidenler.get(2).govde());

        // Sınır oyuncu başınadır.
        emote(veli, "alkis");
        assertEquals(new Giden(ali, Protokol.EMOTE_YAYIN, emoteGovde(veli, "alkis")), gidenler.get(3));
    }

    @Test
    void gecersizEmoteHizSinirindanDusmezVeIletilmez() {
        merhaba(ali, "Ali", "Towny2");
        merhaba(veli, "Veli", "Towny2");
        gidenler.clear();

        emote(ali, "ALKIŞ");
        emote(ali, "a".repeat(33));
        c.paketGeldi(ali, "Ali", "Towny2", ProtokolTest.ham(3, "{\"id\":5}"));
        c.paketGeldi(ali, "Ali", "Towny2", ProtokolTest.ham(3, "{}"));
        assertTrue(gidenler.isEmpty());

        emote(ali, "alkis");
        emote(ali, "alkis");
        assertEquals(2, gidenler.size());
    }

    @Test
    void bozukVeBilinmeyenPaketlerDurumuBozmaz() {
        merhaba(ali, "Ali", "Towny2");
        gidenler.clear();

        c.paketGeldi(ali, "Ali", "Towny2", null);
        c.paketGeldi(ali, "Ali", "Towny2", new byte[0]);
        c.paketGeldi(ali, "Ali", "Towny2", new byte[]{2});
        c.paketGeldi(ali, "Ali", "Towny2", ProtokolTest.ham(1, "{\"surum\":"));
        c.paketGeldi(ali, "Ali", "Towny2", ProtokolTest.ham(2, "[]"));
        c.paketGeldi(ali, "Ali", "Towny2", ProtokolTest.ham(99, "{}"));
        c.paketGeldi(ali, "Ali", "Towny2", ProtokolTest.ham(16, "{\"sahip\":[\"*\"]}"));
        c.paketGeldi(ali, "Ali", "Towny2", ProtokolTest.ham(1, "{\"a\":\"" + "x".repeat(Protokol.MAX_BOYUT) + "\"}"));
        // Backend'i bilinmeyen MERHABA
        c.paketGeldi(veli, "Veli", null, ProtokolTest.ham(1, "{}"));

        assertTrue(gidenler.isEmpty());
        assertTrue(uyarilar.isEmpty(), uyarilar.toString());
        assertEquals("0.1.0", c.oturum(ali).orElseThrow().surum());
        assertFalse(c.kullaniyorMu(veli));
    }

    @Test
    void izinliOlmayanModYalnizLoglanir() {
        ayarlar = Ayarlar.coz("{\"izinli_modlar\":[\"sodium\"]}");
        c.paketGeldi(ali, "Ali", "Towny2", ProtokolTest.ham(1, "{\"surum\":\"0.1.0\",\"modlar\":[\"sodium@1\",\"wurst@7\"]}"));

        assertTrue(c.kullaniyorMu(ali));
        assertEquals(Protokol.HOSGELDIN, al(ali).get(0).no());
        assertEquals(1, uyarilar.size());
        assertTrue(uyarilar.get(0).contains("wurst@7") && !uyarilar.get(0).contains("sodium"), uyarilar.get(0));
    }

    @Test
    void bildirimYalnizHapiClientKullanicilarinaGider() {
        merhaba(ali, "Ali", "Towny2");
        merhaba(ayse, "Ayşe", "Skyblock");
        gidenler.clear();

        assertEquals(2, c.bildir(null, "Duyuru", "Etkinlik başlıyor"));
        assertEquals(1, c.bildir(ali, "Özel", "Merhaba"));
        assertEquals(0, c.bildir(veli, "Özel", "Merhaba"));

        assertEquals(3, gidenler.size());
        assertEquals("{\"baslik\":\"Özel\",\"metin\":\"Merhaba\"}", al(ali).get(1).govde().toString());
        assertEquals(1, al(ayse).size());
    }

    @Test
    void kalabalikSunucudaListeParcalanir() {
        for (int i = 0; i < 95; i++) {
            merhaba(new UUID(5, i), "Oyuncu" + i, "Towny2");
        }
        gidenler.clear();

        merhaba(ali, "Ali", "Towny2");

        List<Giden> g = al(ali);
        assertEquals(List.of(Protokol.HOSGELDIN, Protokol.OYUNCULAR, Protokol.OYUNCULAR, Protokol.OYUNCULAR),
                g.stream().map(Giden::no).toList());
        Set<String> hepsi = new HashSet<>();
        for (int i = 1; i < g.size(); i++) {
            assertEquals(i == 1, g.get(i).govde().get("sifirla").getAsBoolean());
            hepsi.addAll(uuidler(g.get(i).govde()));
        }
        assertEquals(95, hepsi.size());
        assertFalse(hepsi.contains(ali.toString()));
    }

    @Test
    void esZamanliGirisCikistaTabloTutarliKalir() throws Exception {
        int isSayisi = 8;
        int oyuncuBasina = 50;
        CountDownLatch basla = new CountDownLatch(1);
        Thread[] isler = new Thread[isSayisi];
        for (int i = 0; i < isSayisi; i++) {
            int no = i;
            isler[i] = new Thread(() -> {
                try {
                    basla.await();
                } catch (InterruptedException e) {
                    return;
                }
                for (int j = 0; j < oyuncuBasina; j++) {
                    UUID u = new UUID(100 + no, j);
                    merhaba(u, "o" + no + "_" + j, j % 2 == 0 ? "Towny2" : "Skyblock");
                    kozmetik(u, "{\"pelerin\":\"p" + j + "\"}");
                    c.sunucuDegisti(u, "Pillars");
                    merhaba(u, "o" + no + "_" + j, "Pillars");
                    if (j % 2 == 1) {
                        c.ayrildi(u);
                    }
                }
            });
            isler[i].start();
        }
        basla.countDown();
        for (Thread t : isler) {
            t.join();
        }

        assertTrue(uyarilar.isEmpty(), uyarilar.toString());
        List<Cekirdek.Oturum> kalan = c.oturumlar();
        assertEquals(isSayisi * oyuncuBasina / 2, kalan.size());
        for (Cekirdek.Oturum o : kalan) {
            assertEquals("Pillars", o.sunucu());
        }
        // Yeni gelen biri, kalan herkesi tek tutarlı listede görür.
        gidenler.clear();
        merhaba(ali, "Ali", "Pillars");
        Set<String> gorulen = new HashSet<>();
        for (Giden g : al(ali)) {
            if (g.no() == Protokol.OYUNCULAR) {
                gorulen.addAll(uuidler(g.govde()));
            }
        }
        assertEquals(kalan.size(), gorulen.size());
    }

    private static JsonObject ayrildiGovde(UUID u) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", u.toString());
        return o;
    }

    private static JsonObject emoteGovde(UUID u, String id) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", u.toString());
        o.addProperty("id", id);
        return o;
    }
}
