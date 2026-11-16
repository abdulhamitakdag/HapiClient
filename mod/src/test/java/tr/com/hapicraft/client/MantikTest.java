package tr.com.hapicraft.client;

import org.junit.jupiter.api.Test;
import tr.com.hapicraft.client.emote.Emote;
import tr.com.hapicraft.client.emote.Emotlar;
import tr.com.hapicraft.client.emote.Poz;
import tr.com.hapicraft.client.hud.Hud;
import tr.com.hapicraft.client.ozellik.Gorunum;
import tr.com.hapicraft.client.ozellik.Nisangah;
import tr.com.hapicraft.client.ozellik.Ozellikler;
import tr.com.hapicraft.client.performans.Donanim;
import tr.com.hapicraft.client.performans.Profil;
import tr.com.hapicraft.client.sohbet.SessizSohbet;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MantikTest {
    @Test
    void genelSohbetGizlenir() {
        assertTrue(SessizSohbet.gizlenmeli("[[SwindLer] Çırak] EclipseTT: Ok ark atıyom", "Hapitolar"));
        assertTrue(SessizSohbet.gizlenmeli("§a[VIP] §fWasTakenVoid999: Bende hata veriyo", "Hapitolar"));
        assertTrue(SessizSohbet.gizlenmeli("[Oyuncu] .Ragnarv999: selam", "Hapitolar"));
    }

    @Test
    void adimGecenVeKendiMesajimGorunur() {
        assertFalse(SessizSohbet.gizlenmeli("[VIP] litwer: hapitolar gelsene", "Hapitolar"));
        assertFalse(SessizSohbet.gizlenmeli("[Admin] Hapitolar: merhaba", "Hapitolar"));
    }

    @Test
    void sistemVeOzelMesajGorunur() {
        assertFalse(SessizSohbet.gizlenmeli("HapiCraft » Kasaban saldırı altında!", "Hapitolar"));
        assertFalse(SessizSohbet.gizlenmeli("[litwer -> Ben] naber", "Hapitolar"));
        assertFalse(SessizSohbet.gizlenmeli("Sunucu 5 dakika sonra yeniden başlayacak.", "Hapitolar"));
    }

    @Test
    void donanimPuaniSiralidir() {
        assertEquals(Profil.PATATES, new Donanim("Intel(R) HD Graphics 4000", 2, 4000, 1024).onerilen());
        assertEquals(Profil.ULTRA, new Donanim("NVIDIA GeForce RTX 4070/PCIe/SSE2", 16, 32000, 4096).onerilen());
        Profil orta = new Donanim("NVIDIA GeForce GTX 1050 Ti/PCIe/SSE2", 4, 8000, 3072).onerilen();
        assertTrue(orta == Profil.DUSUK || orta == Profil.DENGELI, orta.name());
        assertTrue(new Donanim("llvmpipe (LLVM 15.0.7, 256 bits)", 16, 64000, 4096).puan() <= 1);
        assertEquals(Profil.DENGELI, new Donanim("Bilinmiyor", 6, 0, 2048).onerilen());
    }

    @Test
    void nisangahSekilleriDoluVeSimetrik() {
        for (int sekil = 0; sekil < Nisangah.SEKIL_ADI.length; sekil++) {
            for (int boyut = 0; boyut < 3; boyut++) {
                Set<Long> pikseller = new HashSet<>();
                for (int[] d : Nisangah.sekil(sekil, boyut)) {
                    assertTrue(d[0] < d[2] && d[1] < d[3], "boş dikdörtgen " + sekil + "/" + boyut);
                    for (int x = d[0]; x < d[2]; x++) for (int y = d[1]; y < d[3]; y++) {
                        assertTrue(Math.abs(x) <= 12 && Math.abs(y) <= 12, "taşma " + sekil + "/" + boyut);
                        pikseller.add((long) x << 32 | (y & 0xFFFFFFFFL));
                    }
                }
                assertFalse(pikseller.isEmpty(), "boş şekil " + sekil);
                for (long p : pikseller) {
                    long ayna = -(p >> 32) << 32 | (p & 0xFFFFFFFFL);
                    assertTrue(pikseller.contains(ayna), "simetri " + sekil + "/" + boyut);
                }
            }
        }
        // T şeklinde üst kol yoktur, artıda vardır.
        assertTrue(Nisangah.sekil(0, 1).stream().anyMatch(d -> d[3] <= 0));
        assertTrue(Nisangah.sekil(5, 1).stream().noneMatch(d -> d[3] <= 0));
    }

    @Test
    void gorunumSecenekleriGecerli() {
        Set<String> anahtarlar = new HashSet<>();
        for (Gorunum.Secenek s : Gorunum.SECENEKLER) {
            assertTrue(anahtarlar.add(s.anahtar()), s.anahtar());
            assertTrue(s.varsayilan() >= 0 && s.varsayilan() < s.degerler().length, s.anahtar());
        }
        Ayarlar.A = new Ayarlar();
        assertEquals("Yeşil", Gorunum.degerAdi("blok.renk"));
        assertEquals(0xFF2BD35A, Gorunum.renk("blok.renk"));
        // Bozuk kayıt varsayılana düşer, ilerletme sona gelince başa sarar.
        Ayarlar.A.secenek.put("blok.renk", 99);
        assertEquals("Yeşil", Gorunum.degerAdi("blok.renk"));
        Ayarlar.A.secenek.put("nisangah.boyut", 2);
        Gorunum.ilerlet("nisangah.boyut");
        assertEquals(0, Gorunum.deger("nisangah.boyut"));
        for (long ms = 0; ms < 4000; ms += 37) assertEquals(0xFF, Gorunum.gokkusagi(ms) >>> 24);
        assertEquals(0xFFFF0000, Gorunum.gokkusagi(0));
        Ayarlar.A = new Ayarlar();
    }

    @Test
    void pusulaIsaretiDogruTarafta() {
        // Güneye (0°) bakarken batı (90°) sağda, doğu (270°) soldadır.
        assertEquals(90f, Hud.pusulaFarki(90f, 0f), 0.001f);
        assertEquals(-90f, Hud.pusulaFarki(270f, 0f), 0.001f);
        // Oyunun açısı sarılmadan büyür; 170° ile -170° arası 20°dir.
        assertEquals(20f, Hud.pusulaFarki(190f, 170f), 0.001f);
        assertEquals(20f, Hud.pusulaFarki(180f, -200f), 0.001f);
        assertEquals(0f, Hud.pusulaFarki(0f, 720f), 0.001f);
    }

    @Test
    void zoomTekerlegiSinirdaKalir() {
        float s = 4f;
        assertTrue(Ozellikler.zoomSonraki(s, 1) > s);
        assertTrue(Ozellikler.zoomSonraki(s, -1) < s);
        for (int i = 0; i < 50; i++) s = Ozellikler.zoomSonraki(s, 1);
        assertEquals(16f, s, 0.001f);
        for (int i = 0; i < 50; i++) s = Ozellikler.zoomSonraki(s, -1);
        assertEquals(1.5f, s, 0.001f);
    }

    @Test
    void butunDanslarSonluDegerUretir() throws Exception {
        Set<String> kimlikler = new HashSet<>();
        assertTrue(Emotlar.liste().size() >= 30);
        for (Emote e : Emotlar.liste()) {
            assertTrue(kimlikler.add(e.kimlik()), e.kimlik());
            assertTrue(e.kimlik().matches("[a-z0-9_]{1,32}"), e.kimlik());
            for (float t = 0; t < 12f; t += 0.07f) {
                Poz p = new Poz();
                e.cizim().uygula(t, p);
                for (Field f : Poz.class.getFields()) {
                    float v = f.getFloat(p);
                    assertTrue(Float.isFinite(v), e.kimlik() + "." + f.getName());
                    if (!f.getName().startsWith("kok") && !f.getName().equals("sagKolY"))
                        assertTrue(Math.abs(v) < 7f, e.kimlik() + "." + f.getName() + "=" + v);
                }
            }
        }
    }
}
