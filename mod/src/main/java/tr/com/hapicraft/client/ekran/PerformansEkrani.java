package tr.com.hapicraft.client.ekran;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.performans.Donanim;
import tr.com.hapicraft.client.performans.Profil;

/** Donanım taraması sonucu ve dört oyun modu. */
public final class PerformansEkrani extends HapiEkran {
    private Donanim donanim;

    public PerformansEkrani(Screen onceki) {
        super("Performans", onceki);
    }

    @Override
    protected void init() {
        donanim = Donanim.tara();
        Profil onerilen = donanim.onerilen();
        int g = Math.min(340, width - 40), x = (width - g) / 2, y = 112;
        Profil aktif = Profil.bul(Ayarlar.A.profil);
        for (Profil p : Profil.values()) {
            String etiket = (p == aktif ? "§a✔ " : "") + p.ad + " §7— " + p.aciklama
                    + (p == onerilen ? " §e★ önerilen" : "");
            dugme(etiket, x, y, g, () -> { p.uygula(minecraft); rebuildWidgets(); });
            y += 23;
        }
        dugme("Bitti", width / 2 - 50, height - 28, 100, this::onClose);
    }

    @Override
    public void render(GuiGraphics g, int fx, int fy, float kismi) {
        super.render(g, fx, fy, kismi);
        int x = (width - Math.min(340, width - 40)) / 2, y = 40;
        g.drawString(font, "§7Ekran kartı: §f" + donanim.gpu(), x, y, 0xFFFFFFFF, true);
        g.drawString(font, "§7İşlemci çekirdeği: §f" + donanim.cekirdek(), x, y + 12, 0xFFFFFFFF, true);
        g.drawString(font, "§7Sistem belleği: §f" + (donanim.sistemRamMB() > 0 ? donanim.sistemRamMB() / 1024 + " GB" : "bilinmiyor")
                + " §7· Oyuna ayrılan: §f" + donanim.javaRamMB() + " MB", x, y + 24, 0xFFFFFFFF, true);
        g.drawString(font, "§7Anlık: §a" + minecraft.getFps() + " FPS", x, y + 36, 0xFFFFFFFF, true);
        g.drawString(font, "§7Bilgisayarına göre önerilen mod: §e" + donanim.onerilen().ad, x, y + 54, 0xFFFFFFFF, true);
        g.drawString(font, "§8Görüntü ayarları hemen değişir; gölgelendirici ve mod listesi oyun yeniden açılınca.",
                x, 112 + 23 * Profil.values().length + 6, 0xFFFFFFFF, true);
    }
}
