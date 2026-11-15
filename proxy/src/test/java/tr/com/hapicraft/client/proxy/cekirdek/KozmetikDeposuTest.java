package tr.com.hapicraft.client.proxy.cekirdek;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KozmetikDeposuTest {

    @TempDir
    Path dizin;

    private final List<String> hatalar = new ArrayList<>();
    private final UUID a = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final UUID b = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static Map<String, String> secim(String pelerin) {
        return Kozmetikler.dogrula(Map.of("pelerin", pelerin), k -> true);
    }

    /** Gecikmesi çok uzun depo: yalnız açık kaydet()/close() yazar. */
    private KozmetikDeposu depo(Path dosya) {
        return new KozmetikDeposu(dosya, 3_600_000, hatalar::add);
    }

    @Test
    void kaydedilenSecimYenidenYuklenir() throws Exception {
        Path dosya = dizin.resolve("alt").resolve("kozmetikler.json");
        try (KozmetikDeposu d = depo(dosya)) {
            d.yukle();
            assertEquals(Kozmetikler.bos(), d.al(a));
            d.ayarla(a, secim("kirmizi"));
            d.ayarla(b, secim("mavi"));
            assertFalse(Files.exists(dosya), "gecikme dolmadan yazılmamalı");
            d.kaydet();
            assertTrue(Files.exists(dosya));
            assertFalse(Files.exists(dosya.resolveSibling("kozmetikler.json.tmp")));
        }
        try (KozmetikDeposu d = depo(dosya)) {
            d.yukle();
            assertEquals(2, d.kayitSayisi());
            assertEquals(secim("kirmizi"), d.al(a));
            assertEquals(secim("mavi"), d.al(b));
        }
        assertTrue(hatalar.isEmpty(), hatalar.toString());
    }

    @Test
    void kapatmaBekleyenDegisikligiYazar() throws Exception {
        Path dosya = dizin.resolve("k.json");
        try (KozmetikDeposu d = depo(dosya)) {
            d.ayarla(a, secim("kirmizi"));
        }
        assertTrue(Files.readString(dosya).contains("kirmizi"));
    }

    @Test
    void bosSecimKaydiSiler() throws Exception {
        Path dosya = dizin.resolve("k.json");
        try (KozmetikDeposu d = depo(dosya)) {
            d.ayarla(a, secim("kirmizi"));
            d.ayarla(a, Kozmetikler.bos());
            assertEquals(0, d.kayitSayisi());
            assertEquals(Kozmetikler.bos(), d.al(a));
        }
        assertEquals("{}", Files.readString(dosya).trim());
    }

    @Test
    void degismeyenSecimDiskeYazdirmaz() throws Exception {
        Path dosya = dizin.resolve("k.json");
        try (KozmetikDeposu d = depo(dosya)) {
            d.ayarla(a, Kozmetikler.bos());
            d.kaydet();
            assertFalse(Files.exists(dosya));
            d.ayarla(a, secim("kirmizi"));
            d.kaydet();
            Files.delete(dosya);
            d.ayarla(a, secim("kirmizi"));
            d.kaydet();
            assertFalse(Files.exists(dosya));
        }
    }

    @Test
    void gecikmeliKayitDegisiklikleriTekYazmadaToplar() throws Exception {
        Path dosya = dizin.resolve("k.json");
        try (KozmetikDeposu d = new KozmetikDeposu(dosya, 150, hatalar::add)) {
            d.ayarla(a, secim("bir"));
            d.ayarla(a, secim("iki"));
            d.ayarla(b, secim("uc"));
            assertFalse(Files.exists(dosya));
            long son = System.currentTimeMillis() + 5000;
            while (!Files.exists(dosya) && System.currentTimeMillis() < son) {
                Thread.sleep(20);
            }
            String icerik = Files.readString(dosya);
            assertTrue(icerik.contains("iki") && icerik.contains("uc") && !icerik.contains("bir"), icerik);

            // İlk yazmadan sonraki değişiklik yeni bir gecikmeli kayıt planlar.
            Files.delete(dosya);
            d.ayarla(a, secim("dort"));
            son = System.currentTimeMillis() + 5000;
            while (!Files.exists(dosya) && System.currentTimeMillis() < son) {
                Thread.sleep(20);
            }
            assertTrue(Files.readString(dosya).contains("dort"));
        }
    }

    @Test
    void bozukDosyaKenaraAlinirVeBosBaslanir() throws Exception {
        Path dosya = dizin.resolve("k.json");
        Files.writeString(dosya, "{\"00000000-0000-0000-0000-00000000000a\": {\"pelerin\":");
        try (KozmetikDeposu d = depo(dosya)) {
            d.yukle();
            assertEquals(0, d.kayitSayisi());
            assertEquals(1, hatalar.size());
            assertTrue(Files.exists(dizin.resolve("k.json.bozuk")));
            assertFalse(Files.exists(dosya));
        }
    }

    @Test
    void gecersizKayitlarAtlanir() throws Exception {
        Path dosya = dizin.resolve("k.json");
        Files.writeString(dosya, """
                {
                  "uuid-degil": {"pelerin": "x"},
                  "00000000-0000-0000-0000-00000000000a": {"pelerin": "kirmizi", "kanat": "GEÇERSİZ", "kuyruk": "x"},
                  "00000000-0000-0000-0000-00000000000b": "nesne degil",
                  "00000000-0000-0000-0000-00000000000c": {"pelerin": 5}
                }
                """);
        try (KozmetikDeposu d = depo(dosya)) {
            d.yukle();
            assertEquals(1, d.kayitSayisi());
            assertEquals(secim("kirmizi"), d.al(a));
        }
        assertTrue(hatalar.isEmpty());
    }

    @Test
    void esZamanliYazmalarKayipsizSaklanir() throws Exception {
        Path dosya = dizin.resolve("k.json");
        try (KozmetikDeposu d = new KozmetikDeposu(dosya, 1, hatalar::add)) {
            Thread[] isler = new Thread[8];
            for (int i = 0; i < isler.length; i++) {
                int no = i;
                isler[i] = new Thread(() -> {
                    for (int j = 0; j < 200; j++) {
                        d.ayarla(new UUID(no, j), secim("p" + j));
                    }
                });
                isler[i].start();
            }
            for (Thread t : isler) {
                t.join();
            }
        }
        try (KozmetikDeposu d = depo(dosya)) {
            d.yukle();
            assertEquals(1600, d.kayitSayisi());
            assertEquals(secim("p199"), d.al(new UUID(7, 199)));
        }
        assertTrue(hatalar.isEmpty(), hatalar.toString());
    }
}
