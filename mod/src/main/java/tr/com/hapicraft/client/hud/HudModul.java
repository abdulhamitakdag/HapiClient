package tr.com.hapicraft.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import tr.com.hapicraft.client.Ayarlar;
import tr.com.hapicraft.client.ozellik.Ozellikler;

/** Ekranda sürüklenebilen tek bir gösterge. Konum ekranın oranı olarak saklanır. */
public abstract class HudModul {
    public final String kimlik, ad;
    public final boolean varsayilanAcik;
    private final float vx, vy;
    public int genislik = 40, yukseklik = 12;

    protected HudModul(String kimlik, String ad, boolean varsayilanAcik, float vx, float vy) {
        this.kimlik = kimlik; this.ad = ad; this.varsayilanAcik = varsayilanAcik; this.vx = vx; this.vy = vy;
    }

    public boolean acik() { return Ozellikler.acik(kimlik, varsayilanAcik); }

    public void ac(boolean deger) { Ayarlar.A.modul.put(kimlik, deger); }

    public int x(int ekranG) {
        float[] k = Ayarlar.A.konum.get(kimlik);
        float fx = k != null && k.length == 2 ? k[0] : vx;
        return Math.max(0, Math.min(ekranG - genislik, Math.round(fx * ekranG)));
    }

    public int y(int ekranY) {
        float[] k = Ayarlar.A.konum.get(kimlik);
        float fy = k != null && k.length == 2 ? k[1] : vy;
        return Math.max(0, Math.min(ekranY - yukseklik, Math.round(fy * ekranY)));
    }

    public void tasi(int px, int py, int ekranG, int ekranY) {
        Ayarlar.A.konum.put(kimlik, new float[]{(float) px / ekranG, (float) py / ekranY});
    }

    public void varsayilanKonum() { Ayarlar.A.konum.remove(kimlik); }

    /** x,y sol üst köşe. Çizerken genislik/yukseklik güncellenmeli. duzenleme: örnek veriyle çiz. */
    public abstract void ciz(GuiGraphics g, Minecraft mc, int x, int y, boolean duzenleme);

    protected void kutuluYazi(GuiGraphics g, Minecraft mc, int x, int y, String yazi) {
        genislik = mc.font.width(yazi) + 8;
        yukseklik = 13;
        g.fill(x, y, x + genislik, y + yukseklik, 0x90000000);
        g.fill(x, y, x + 1, y + yukseklik, 0xFF2BD35A);
        g.drawString(mc.font, yazi, x + 5, y + 3, 0xFFFFFFFF, false);
    }
}
