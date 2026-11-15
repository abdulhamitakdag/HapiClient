package tr.com.hapicraft.client.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;
import tr.com.hapicraft.client.proxy.cekirdek.Ayarlar;
import tr.com.hapicraft.client.proxy.cekirdek.Cekirdek;
import tr.com.hapicraft.client.proxy.cekirdek.KozmetikDeposu;
import tr.com.hapicraft.client.proxy.cekirdek.Protokol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * HapiClient'ın sunucu tarafı: hapiclient:ana kanalında el sıkışma, kozmetik ve dans eşitlemesi.
 * İş mantığı {@link Cekirdek} içindedir; bu sınıf yalnız Velocity'ye bağlar.
 */
@Plugin(id = "hapiclientproxy", name = "HapiClientProxy", version = "0.1.0",
        description = "HapiClient el sıkışması, kozmetik ve dans eşitlemesi", authors = {"HapiCraft"})
public class HapiClientProxyPlugin {

    static final MinecraftChannelIdentifier KANAL = MinecraftChannelIdentifier.from(Protokol.KANAL);
    private static final long KAYIT_GECIKMESI_MS = 5000;

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path veriDizini;

    private volatile Ayarlar ayarlar = Ayarlar.varsayilan();
    private KozmetikDeposu depo;
    private Cekirdek cekirdek;

    @Inject
    public HapiClientProxyPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path veriDizini) {
        this.proxy = proxy;
        this.logger = logger;
        this.veriDizini = veriDizini;
    }

    @Subscribe
    public void acilis(ProxyInitializeEvent olay) {
        String hata = ayarlariYukle();
        if (hata != null) {
            logger.error("config.json okunamadı, varsayılan ayarlar kullanılıyor: {}", hata);
        }
        depo = new KozmetikDeposu(veriDizini.resolve("kozmetikler.json"), KAYIT_GECIKMESI_MS, logger::error);
        depo.yukle();

        cekirdek = new Cekirdek(
                (hedef, veri) -> proxy.getPlayer(hedef).ifPresent(o -> o.sendPluginMessage(KANAL, veri)),
                (oyuncu, izin) -> proxy.getPlayer(oyuncu).map(o -> o.hasPermission(izin)).orElse(false),
                new Cekirdek.Gunluk() {
                    @Override
                    public void bilgi(String mesaj) {
                        logger.info(mesaj);
                    }

                    @Override
                    public void uyari(String mesaj) {
                        logger.warn(mesaj);
                    }
                },
                () -> ayarlar, depo, System::currentTimeMillis);
        HapiClientApi.bagla(cekirdek);

        proxy.getChannelRegistrar().register(KANAL);
        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("hapiclient").plugin(this).build(),
                new HapiClientKomutu(this, proxy, cekirdek));
        logger.info("HapiClientProxy hazır ({} kayıtlı kozmetik seçimi).", depo.kayitSayisi());
    }

    @Subscribe
    public void kapanis(ProxyShutdownEvent olay) {
        HapiClientApi.bagla(null);
        if (depo != null) {
            depo.close();
        }
    }

    /**
     * Kanaldaki her paket burada biter: istemciden gelen işlenir ve backend'e iletilmez; backend'den
     * gelen (istemciye sahte paket enjeksiyonu) yutulur.
     */
    @Subscribe
    public void paket(PluginMessageEvent olay) {
        if (!KANAL.equals(olay.getIdentifier())) {
            return;
        }
        olay.setResult(PluginMessageEvent.ForwardResult.handled());
        if (cekirdek == null || !(olay.getSource() instanceof Player oyuncu)) {
            return;
        }
        String sunucu = oyuncu.getCurrentServer().map(b -> b.getServerInfo().getName()).orElse(null);
        cekirdek.paketGeldi(oyuncu.getUniqueId(), oyuncu.getUsername(), sunucu, olay.getData());
    }

    @Subscribe
    public void sunucuDegisti(ServerPostConnectEvent olay) {
        if (cekirdek == null) {
            return;
        }
        Player oyuncu = olay.getPlayer();
        oyuncu.getCurrentServer().ifPresent(b ->
                cekirdek.sunucuDegisti(oyuncu.getUniqueId(), b.getServerInfo().getName()));
    }

    @Subscribe
    public void cikis(DisconnectEvent olay) {
        if (cekirdek != null) {
            cekirdek.ayrildi(olay.getPlayer().getUniqueId());
        }
    }

    /**
     * config.json'u okur; yoksa varsayılanı yazar.
     *
     * @return hata iletisi; başarılıysa null (hata durumunda yürürlükteki ayar korunur)
     */
    String ayarlariYukle() {
        Path dosya = veriDizini.resolve("config.json");
        try {
            if (!Files.exists(dosya)) {
                Files.createDirectories(veriDizini);
                Files.writeString(dosya, Ayarlar.VARSAYILAN_JSON, StandardCharsets.UTF_8);
            }
            ayarlar = Ayarlar.coz(Files.readString(dosya, StandardCharsets.UTF_8));
            return null;
        } catch (IOException | IllegalArgumentException e) {
            return String.valueOf(e.getMessage());
        }
    }
}
