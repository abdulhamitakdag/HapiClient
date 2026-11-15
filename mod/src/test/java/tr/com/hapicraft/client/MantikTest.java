package tr.com.hapicraft.client;

import org.junit.jupiter.api.Test;
import tr.com.hapicraft.client.emote.Emote;
import tr.com.hapicraft.client.emote.Emotlar;
import tr.com.hapicraft.client.emote.Poz;
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
