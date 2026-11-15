package tr.com.hapicraft.client.proxy.cekirdek;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Eklentinin Velocity'den bağımsız iş mantığı: oturum tablosu, el sıkışma, kozmetik ve dans dağıtımı.
 *
 * Velocity olayları farklı thread'lerde geldiği için durumu değiştiren bütün işlemler tek kilit
 * altında yürür (trafik düşük: oyuncu başına saniyede birkaç paket). Okuma yapan sorgular
 * (kullaniyorMu, oturum) kilitsizdir; oturum kayıtları değiştirilemez.
 */
public final class Cekirdek {

    /** Bir oyuncunun istemcisine ham paket yollar. Oyuncu çevrimdışıysa sessizce yok sayar. */
    public interface Gonderici {
        void gonder(UUID hedef, byte[] veri);
    }

    public interface Izinler {
        boolean var(UUID oyuncu, String izin);
    }

    public interface Gunluk {
        void bilgi(String mesaj);

        void uyari(String mesaj);
    }

    /** {@code sunucu} null ise oyuncu backend değiştirmiştir ve yeni MERHABA beklenmektedir. */
    public record Oturum(UUID uuid, String ad, String surum, int proto, List<String> modlar, String sunucu) {
    }

    private final Gonderici gonderici;
    private final Izinler izinler;
    private final Gunluk gunluk;
    private final Supplier<Ayarlar> ayarlar;
    private final KozmetikDeposu depo;
    private final LongSupplier saatMs;

    private final ConcurrentMap<UUID, Oturum> oturumlar = new ConcurrentHashMap<>();
    private final Object kilit = new Object();

    private final HizSiniri emoteSiniri = new HizSiniri(2, 1000);
    // Protokolde yalnız EMOTE sınırı var; bunlar yayın/disk yükünü büyüten paket seline karşı.
    private final HizSiniri merhabaSiniri = new HizSiniri(5, 5000);
    private final HizSiniri kozmetikSiniri = new HizSiniri(5, 1000);

    public Cekirdek(Gonderici gonderici, Izinler izinler, Gunluk gunluk, Supplier<Ayarlar> ayarlar,
                    KozmetikDeposu depo, LongSupplier saatMs) {
        this.gonderici = gonderici;
        this.izinler = izinler;
        this.gunluk = gunluk;
        this.ayarlar = ayarlar;
        this.depo = depo;
        this.saatMs = saatMs;
    }

    /**
     * İstemciden gelen ham paket. Hiçbir girdi istisna fırlatmaz.
     *
     * @param sunucu oyuncunun şu an bağlı olduğu backend'in adı (bilinmiyorsa null)
     */
    public void paketGeldi(UUID uuid, String ad, String sunucu, byte[] veri) {
        try {
            Protokol.Paket paket = Protokol.coz(veri);
            switch (paket.no()) {
                case Protokol.MERHABA -> merhaba(uuid, ad, sunucu, paket);
                case Protokol.KOZMETIK -> kozmetik(uuid, paket);
                case Protokol.EMOTE -> emote(uuid, paket);
                default -> {
                    // bilinmeyen paket numarası: yok say
                }
            }
        } catch (Protokol.ProtokolHatasi e) {
            // Bozuk paket: sessizce at (log seli olmasın).
        } catch (RuntimeException e) {
            gunluk.uyari(ad + " oyuncusunun paketi işlenemedi: " + e);
        }
    }

    private void merhaba(UUID uuid, String ad, String sunucu, Protokol.Paket paket) {
        if (sunucu == null || !merhabaSiniri.izinVer(uuid, saatMs.getAsLong())) {
            return;
        }
        Protokol.Merhaba m = Protokol.merhabaCoz(paket.govde());
        Ayarlar a = ayarlar.get();
        Predicate<String> izin = dugum -> izinler.var(uuid, dugum);
        boolean ilk;
        synchronized (kilit) {
            Oturum eski = oturumlar.get(uuid);
            ilk = eski == null;
            if (eski != null && eski.sunucu() != null && !eski.sunucu().equals(sunucu)) {
                ayrildiDuyur(uuid, eski.sunucu());
            }
            oturumlar.put(uuid, new Oturum(uuid, ad, m.surum(), m.proto(), m.modlar(), sunucu));

            gonderici.gonder(uuid, Protokol.hosgeldin(a.kapali(sunucu),
                    Kozmetikler.sahipListesi(a.ucretsiz(), a.ucretli(), izin), a.indirim(), sunucu));

            // Saklanan seçim yeniden doğrulanır: izin süresi dolmuş ya da kozmetik ücretli olmuş olabilir.
            Map<String, String> secim = Kozmetikler.dogrula(depo.al(uuid),
                    k -> Kozmetikler.sahipMi(k, a.ucretsiz(), izin));
            depo.ayarla(uuid, secim);

            List<UUID> digerleri = ayniSunucudakiler(sunucu, uuid);
            List<Protokol.Kayit> kayitlar = new ArrayList<>(digerleri.size());
            for (UUID diger : digerleri) {
                kayitlar.add(new Protokol.Kayit(diger, depo.al(diger)));
            }
            for (byte[] p : Protokol.oyuncular(kayitlar, true)) {
                gonderici.gonder(uuid, p);
            }
            byte[] kendi = Protokol.oyuncular(List.of(new Protokol.Kayit(uuid, secim)), false).get(0);
            for (UUID diger : digerleri) {
                gonderici.gonder(diger, kendi);
            }
        }
        if (ilk) {
            gunluk.bilgi(ad + " HapiClient " + m.surum() + " ile bağlandı (" + m.modlar().size() + " mod).");
        }
        List<String> izinsiz = a.izinsizModlar(m.modlar());
        if (ilk && !izinsiz.isEmpty()) {
            gunluk.uyari(ad + " izinli listede olmayan mod bildirdi: " + String.join(", ", izinsiz));
        }
    }

    private void kozmetik(UUID uuid, Protokol.Paket paket) {
        if (!oturumlar.containsKey(uuid) || !kozmetikSiniri.izinVer(uuid, saatMs.getAsLong())) {
            return;
        }
        Ayarlar a = ayarlar.get();
        Map<String, String> secim = Kozmetikler.dogrula(paket.govde(),
                k -> Kozmetikler.sahipMi(k, a.ucretsiz(), dugum -> izinler.var(uuid, dugum)));
        synchronized (kilit) {
            Oturum o = oturumlar.get(uuid);
            if (o == null) {
                return;
            }
            depo.ayarla(uuid, secim);
            if (o.sunucu() == null) {
                return;
            }
            // Gönderen de alır: geçersiz yuvanın boşaltıldığını böylece öğrenir.
            byte[] p = Protokol.oyuncular(List.of(new Protokol.Kayit(uuid, secim)), false).get(0);
            gonderici.gonder(uuid, p);
            for (UUID diger : ayniSunucudakiler(o.sunucu(), uuid)) {
                gonderici.gonder(diger, p);
            }
        }
    }

    private void emote(UUID uuid, Protokol.Paket paket) {
        String id = Protokol.metinAlani(paket.govde(), "id");
        if (!Protokol.kimlikGecerli(id)) {
            return;
        }
        synchronized (kilit) {
            Oturum o = oturumlar.get(uuid);
            if (o == null || o.sunucu() == null || !emoteSiniri.izinVer(uuid, saatMs.getAsLong())) {
                return;
            }
            byte[] p = Protokol.emote(uuid, id);
            for (UUID diger : ayniSunucudakiler(o.sunucu(), uuid)) {
                gonderici.gonder(diger, p);
            }
        }
    }

    /**
     * Oyuncu başka bir backend'e geçti. Eski backend'dekilere AYRILDI gider; oyuncu yeni MERHABA
     * gönderene kadar hiçbir backend'in listesinde görünmez. Yeni backend için MERHABA daha önce
     * işlendiyse (olay sırası yarışı) hiçbir şey yapmaz.
     */
    public void sunucuDegisti(UUID uuid, String yeniSunucu) {
        synchronized (kilit) {
            Oturum o = oturumlar.get(uuid);
            if (o == null || o.sunucu() == null || o.sunucu().equals(yeniSunucu)) {
                return;
            }
            ayrildiDuyur(uuid, o.sunucu());
            oturumlar.put(uuid, new Oturum(uuid, o.ad(), o.surum(), o.proto(), o.modlar(), null));
        }
    }

    /** Oyuncu proxy'den çıktı. */
    public void ayrildi(UUID uuid) {
        synchronized (kilit) {
            Oturum o = oturumlar.remove(uuid);
            if (o != null && o.sunucu() != null) {
                ayrildiDuyur(uuid, o.sunucu());
            }
        }
        emoteSiniri.sil(uuid);
        merhabaSiniri.sil(uuid);
        kozmetikSiniri.sil(uuid);
    }

    /**
     * BILDIRIM yollar.
     *
     * @param hedef null ise bütün HapiClient kullanıcıları
     * @return bildirimin gönderildiği oyuncu sayısı
     */
    public int bildir(UUID hedef, String baslik, String metin) {
        byte[] p = Protokol.bildirim(baslik, metin);
        if (hedef != null) {
            if (!oturumlar.containsKey(hedef)) {
                return 0;
            }
            gonderici.gonder(hedef, p);
            return 1;
        }
        int adet = 0;
        for (UUID uuid : oturumlar.keySet()) {
            gonderici.gonder(uuid, p);
            adet++;
        }
        return adet;
    }

    public boolean kullaniyorMu(UUID uuid) {
        return oturumlar.containsKey(uuid);
    }

    public Optional<Oturum> oturum(UUID uuid) {
        return Optional.ofNullable(oturumlar.get(uuid));
    }

    public Optional<Oturum> oturum(String ad) {
        for (Oturum o : oturumlar.values()) {
            if (o.ad().equalsIgnoreCase(ad)) {
                return Optional.of(o);
            }
        }
        return Optional.empty();
    }

    /** Ada göre sıralı anlık görüntü. */
    public List<Oturum> oturumlar() {
        List<Oturum> liste = new ArrayList<>(oturumlar.values());
        liste.sort(Comparator.comparing(Oturum::ad, String.CASE_INSENSITIVE_ORDER));
        return liste;
    }

    private void ayrildiDuyur(UUID uuid, String sunucu) {
        byte[] p = Protokol.ayrildi(uuid);
        for (UUID diger : ayniSunucudakiler(sunucu, uuid)) {
            gonderici.gonder(diger, p);
        }
    }

    private List<UUID> ayniSunucudakiler(String sunucu, UUID haric) {
        List<UUID> liste = new ArrayList<>();
        for (Oturum o : oturumlar.values()) {
            if (sunucu.equals(o.sunucu()) && !o.uuid().equals(haric)) {
                liste.add(o.uuid());
            }
        }
        return liste;
    }
}
