package tr.com.hapicraft.client.proxy.cekirdek;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HizSiniriTest {

    private final UUID a = new UUID(0, 1);
    private final UUID b = new UUID(0, 2);

    @Test
    void saniyedeEnFazlaIki() {
        HizSiniri s = new HizSiniri(2, 1000);
        assertTrue(s.izinVer(a, 0));
        assertTrue(s.izinVer(a, 10));
        assertFalse(s.izinVer(a, 20));
        assertFalse(s.izinVer(a, 999));
        // t=0'daki kabul pencereden çıktı
        assertTrue(s.izinVer(a, 1000));
        // pencerede t=10 ve t=1000 var
        assertFalse(s.izinVer(a, 1009));
        assertTrue(s.izinVer(a, 1010));
    }

    @Test
    void reddedilenIstekPencereyiUzatmaz() {
        HizSiniri s = new HizSiniri(2, 1000);
        assertTrue(s.izinVer(a, 0));
        assertTrue(s.izinVer(a, 0));
        for (int t = 1; t < 1000; t += 50) {
            assertFalse(s.izinVer(a, t));
        }
        assertTrue(s.izinVer(a, 1000));
        assertTrue(s.izinVer(a, 1000));
        assertFalse(s.izinVer(a, 1000));
    }

    @Test
    void oyuncularBagimsizVeSilSifirlar() {
        HizSiniri s = new HizSiniri(1, 1000);
        assertTrue(s.izinVer(a, 0));
        assertFalse(s.izinVer(a, 1));
        assertTrue(s.izinVer(b, 1));
        s.sil(a);
        assertTrue(s.izinVer(a, 2));
    }

    @Test
    void gecersizAyarReddedilir() {
        assertThrows(IllegalArgumentException.class, () -> new HizSiniri(0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new HizSiniri(1, 0));
    }

    @Test
    void esZamanliIsteklerdeSinirAsilmaz() throws Exception {
        HizSiniri s = new HizSiniri(2, 1000);
        AtomicInteger kabul = new AtomicInteger();
        CountDownLatch basla = new CountDownLatch(1);
        Thread[] isler = new Thread[8];
        for (int i = 0; i < isler.length; i++) {
            isler[i] = new Thread(() -> {
                try {
                    basla.await();
                } catch (InterruptedException e) {
                    return;
                }
                for (int j = 0; j < 1000; j++) {
                    if (s.izinVer(a, 500)) {
                        kabul.incrementAndGet();
                    }
                }
            });
            isler[i].start();
        }
        basla.countDown();
        for (Thread t : isler) {
            t.join();
        }
        assertEquals(2, kabul.get());
    }
}
