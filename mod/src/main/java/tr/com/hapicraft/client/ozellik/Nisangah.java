package tr.com.hapicraft.client.ozellik;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Özel nişangah: şekil dikdörtgenlerden kurulur, tek renkle çizilir. */
public final class Nisangah {
    public static final String[] SEKIL_ADI = {"Artı", "Nokta", "Çember", "Kare", "Çarpı", "T", "Artı + Nokta"};

    private Nisangah() {}

    /**
     * Merkez pikseli (0,0) kabul eden dikdörtgenler: {x1, y1, x2, y2}, x2 ve y2 dışarıda.
     * boyut: 0 küçük, 1 orta, 2 büyük.
     */
    public static List<int[]> sekil(int sekil, int boyut) {
        int kol = 3 + boyut * 2, bosluk = boyut == 0 ? 1 : 2, yaricap = 3 + boyut * 2;
        List<int[]> d = new ArrayList<>();
        switch (sekil) {
            case 1 -> d.add(new int[]{-boyut, -boyut, boyut + 1, boyut + 1});
            case 2 -> {
                boolean[][] dolu = new boolean[yaricap * 2 + 1][yaricap * 2 + 1];
                for (int a = 0; a < 360; a += 3) {
                    int x = (int) Math.round(Math.cos(Math.toRadians(a)) * yaricap);
                    int y = (int) Math.round(Math.sin(Math.toRadians(a)) * yaricap);
                    if (dolu[x + yaricap][y + yaricap]) continue;
                    dolu[x + yaricap][y + yaricap] = true;
                    d.add(new int[]{x, y, x + 1, y + 1});
                }
            }
            case 3 -> {
                d.add(new int[]{-yaricap, -yaricap, yaricap + 1, -yaricap + 1});
                d.add(new int[]{-yaricap, yaricap, yaricap + 1, yaricap + 1});
                d.add(new int[]{-yaricap, -yaricap + 1, -yaricap + 1, yaricap});
                d.add(new int[]{yaricap, -yaricap + 1, yaricap + 1, yaricap});
            }
            case 4 -> {
                for (int i = bosluk; i < bosluk + kol; i++) {
                    d.add(new int[]{i, i, i + 1, i + 1});
                    d.add(new int[]{-i, i, -i + 1, i + 1});
                    d.add(new int[]{i, -i, i + 1, -i + 1});
                    d.add(new int[]{-i, -i, -i + 1, -i + 1});
                }
            }
            default -> {
                if (sekil != 5) d.add(new int[]{0, -bosluk - kol, 1, -bosluk});
                d.add(new int[]{0, bosluk + 1, 1, bosluk + kol + 1});
                d.add(new int[]{-bosluk - kol, 0, -bosluk, 1});
                d.add(new int[]{bosluk + 1, 0, bosluk + kol + 1, 1});
                if (sekil == 6) d.add(new int[]{0, 0, 1, 1});
            }
        }
        return d;
    }

    /** Seçili şekli (mx, my) merkezine çizer. */
    public static void cizSekil(GuiGraphics g, int mx, int my) {
        int renk = Gorunum.renk("nisangah.renk");
        for (int[] d : sekil(Gorunum.deger("nisangah.sekil"), Gorunum.deger("nisangah.boyut")))
            g.fill(mx + d[0], my + d[1], mx + d[2], my + d[3], renk);
    }

    /** Vanilla nişangah yerine çağrılır; modül kapalıysa false döner ve vanilla çizilir. */
    public static boolean ciz(GuiGraphics g) {
        if (!Ozellikler.acik("nisangah", false)) return false;
        cizSekil(g, g.guiWidth() / 2, g.guiHeight() / 2);
        return true;
    }
}
