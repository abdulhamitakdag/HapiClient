package tr.com.hapicraft.client.proxy;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import tr.com.hapicraft.client.proxy.cekirdek.Cekirdek;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** /hapiclient liste | bilgi | yenile | bildir */
final class HapiClientKomutu implements SimpleCommand {

    private static final String IZIN = "hapiclient.yonet";
    private static final Component ONEK = MiniMessage.miniMessage().deserialize(
            "<green><bold>Hapi</bold></green><white><bold>Craft</bold></white> <gray>» <gray>");
    private static final List<String> ALT_KOMUTLAR = List.of("liste", "bilgi", "yenile", "bildir");

    private final HapiClientProxyPlugin eklenti;
    private final ProxyServer proxy;
    private final Cekirdek cekirdek;

    HapiClientKomutu(HapiClientProxyPlugin eklenti, ProxyServer proxy, Cekirdek cekirdek) {
        this.eklenti = eklenti;
        this.proxy = proxy;
        this.cekirdek = cekirdek;
    }

    @Override
    public boolean hasPermission(Invocation cagri) {
        return cagri.source().hasPermission(IZIN);
    }

    @Override
    public void execute(Invocation cagri) {
        CommandSource kaynak = cagri.source();
        String[] arg = cagri.arguments();
        String alt = arg.length == 0 ? "" : arg[0].toLowerCase(Locale.ROOT);
        switch (alt) {
            case "liste" -> liste(kaynak);
            case "bilgi" -> bilgi(kaynak, arg);
            case "yenile" -> yenile(kaynak);
            case "bildir" -> bildir(kaynak, arg);
            default -> {
                yaz(kaynak, "Kullanım:");
                yaz(kaynak, "/hapiclient liste");
                yaz(kaynak, "/hapiclient bilgi <oyuncu>");
                yaz(kaynak, "/hapiclient yenile");
                yaz(kaynak, "/hapiclient bildir <oyuncu|*> <başlık> | <metin>");
            }
        }
    }

    private void liste(CommandSource kaynak) {
        List<Cekirdek.Oturum> oturumlar = cekirdek.oturumlar();
        if (oturumlar.isEmpty()) {
            yaz(kaynak, "Şu an HapiClient kullanan oyuncu yok.");
            return;
        }
        yaz(kaynak, "HapiClient kullanan " + oturumlar.size() + " oyuncu (toplam çevrimiçi: "
                + proxy.getPlayerCount() + "):");
        for (Cekirdek.Oturum o : oturumlar) {
            yaz(kaynak, o.ad() + " — sürüm " + o.surum() + ", sunucu: " + sunucuAdi(o));
        }
    }

    private void bilgi(CommandSource kaynak, String[] arg) {
        if (arg.length < 2) {
            yaz(kaynak, "Kullanım: /hapiclient bilgi <oyuncu>");
            return;
        }
        Optional<Cekirdek.Oturum> bulunan = cekirdek.oturum(arg[1]);
        if (bulunan.isEmpty()) {
            yaz(kaynak, arg[1] + " çevrimiçi değil ya da HapiClient kullanmıyor.");
            return;
        }
        Cekirdek.Oturum o = bulunan.get();
        yaz(kaynak, o.ad() + " — sürüm " + o.surum() + ", protokol " + o.proto() + ", sunucu: " + sunucuAdi(o));
        yaz(kaynak, "Modlar (" + o.modlar().size() + "): "
                + (o.modlar().isEmpty() ? "bildirilmedi" : String.join(", ", o.modlar())));
    }

    private void yenile(CommandSource kaynak) {
        String hata = eklenti.ayarlariYukle();
        if (hata == null) {
            yaz(kaynak, "Ayarlar yeniden okundu. Değişiklik oyunculara bir sonraki sunucu geçişinde yansır.");
        } else {
            yaz(kaynak, "config.json okunamadı, eski ayarlar yürürlükte kaldı: " + hata);
        }
    }

    private void bildir(CommandSource kaynak, String[] arg) {
        String govde = arg.length < 3 ? "" : String.join(" ", Arrays.copyOfRange(arg, 2, arg.length));
        int ayrac = govde.indexOf('|');
        if (ayrac < 0) {
            yaz(kaynak, "Kullanım: /hapiclient bildir <oyuncu|*> <başlık> | <metin>");
            return;
        }
        String baslik = govde.substring(0, ayrac).trim();
        String metin = govde.substring(ayrac + 1).trim();
        if (baslik.isEmpty() || metin.isEmpty()) {
            yaz(kaynak, "Başlık ve metin boş olamaz.");
            return;
        }
        if (arg[1].equals("*")) {
            int adet = cekirdek.bildir(null, baslik, metin);
            yaz(kaynak, "Bildirim " + adet + " HapiClient kullanıcısına gönderildi.");
            return;
        }
        Optional<Cekirdek.Oturum> hedef = cekirdek.oturum(arg[1]);
        if (hedef.isEmpty() || cekirdek.bildir(hedef.get().uuid(), baslik, metin) == 0) {
            yaz(kaynak, arg[1] + " çevrimiçi değil ya da HapiClient kullanmıyor.");
            return;
        }
        yaz(kaynak, "Bildirim " + hedef.get().ad() + " oyuncusuna gönderildi.");
    }

    @Override
    public List<String> suggest(Invocation cagri) {
        String[] arg = cagri.arguments();
        if (arg.length <= 1) {
            return onEkle(ALT_KOMUTLAR, arg.length == 0 ? "" : arg[0]);
        }
        if (arg.length == 2 && (arg[0].equalsIgnoreCase("bilgi") || arg[0].equalsIgnoreCase("bildir"))) {
            List<String> adlar = new ArrayList<>();
            if (arg[0].equalsIgnoreCase("bildir")) {
                adlar.add("*");
            }
            cekirdek.oturumlar().forEach(o -> adlar.add(o.ad()));
            return onEkle(adlar, arg[1]);
        }
        return List.of();
    }

    private static List<String> onEkle(List<String> secenekler, String onEk) {
        String kucuk = onEk.toLowerCase(Locale.ROOT);
        return secenekler.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(kucuk)).toList();
    }

    private static String sunucuAdi(Cekirdek.Oturum o) {
        return o.sunucu() != null ? o.sunucu() : "geçiş yapıyor";
    }

    /** Oyuncudan gelen metin (sürüm, mod adı) düz metin olarak eklenir; MiniMessage olarak yorumlanmaz. */
    private static void yaz(CommandSource kaynak, String mesaj) {
        kaynak.sendMessage(Component.text().append(ONEK).append(Component.text(mesaj, NamedTextColor.GRAY)).build());
    }
}
