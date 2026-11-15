package tr.com.hapicraft.client.proxy.cekirdek;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Oyuncu başına kayan pencereli hız sınırı: herhangi bir {@code pencereMs} aralığında en fazla
 * {@code azami} istek. Zaman dışarıdan verilir (test edilebilirlik). Thread-güvenlidir.
 */
public final class HizSiniri {

    private final int azami;
    private final long pencereMs;
    /** Oyuncu başına son kabul edilen {@code azami} isteğin zamanı (dairesel). */
    private final ConcurrentMap<UUID, Pencere> pencereler = new ConcurrentHashMap<>();

    private static final class Pencere {
        final long[] zamanlar;
        int adet;
        int sira;

        Pencere(int azami) {
            zamanlar = new long[azami];
        }
    }

    public HizSiniri(int azami, long pencereMs) {
        if (azami < 1 || pencereMs < 1) {
            throw new IllegalArgumentException("azami ve pencere pozitif olmalı");
        }
        this.azami = azami;
        this.pencereMs = pencereMs;
    }

    /** İstek kabul edilirse true döner ve sayılır; reddedilen istek sayılmaz. */
    public boolean izinVer(UUID oyuncu, long simdiMs) {
        Pencere p = pencereler.computeIfAbsent(oyuncu, k -> new Pencere(azami));
        synchronized (p) {
            if (p.adet == azami) {
                // sira, dolu pencerede en eski kabulü gösterir.
                long enEski = p.zamanlar[p.sira];
                if (simdiMs - enEski < pencereMs) {
                    return false;
                }
            } else {
                p.adet++;
            }
            p.zamanlar[p.sira] = simdiMs;
            p.sira = (p.sira + 1) % azami;
            return true;
        }
    }

    public void sil(UUID oyuncu) {
        pencereler.remove(oyuncu);
    }
}
