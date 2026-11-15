package tr.com.hapicraft.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import tr.com.hapicraft.client.HapiClient;
import tr.com.hapicraft.client.ag.Ag;
import tr.com.hapicraft.client.ekran.HudDuzenleyici;
import tr.com.hapicraft.client.sohbet.SessizSohbet;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class Hud {
    public static final List<HudModul> MODULLER = new ArrayList<>();
    private static final Deque<Long> SOL = new ArrayDeque<>(), SAG = new ArrayDeque<>();
    private static final DateTimeFormatter SAAT = DateTimeFormatter.ofPattern("HH:mm");
    private static long girisAni = 0;

    private Hud() {}

    /** Fare mixin'inden çağrılır. */
    public static void tiklama(int dugme) {
        long simdi = System.currentTimeMillis();
        if (dugme == 0) SOL.addLast(simdi);
        else if (dugme == 1) SAG.addLast(simdi);
    }

    private static int cps(Deque<Long> kuyruk) {
        long sinir = System.currentTimeMillis() - 1000;
        while (!kuyruk.isEmpty() && kuyruk.peekFirst() < sinir) kuyruk.pollFirst();
        return kuyruk.size();
    }

    public static void tik(Minecraft mc) {
        if (mc.level == null) girisAni = 0;
        else if (girisAni == 0) girisAni = System.currentTimeMillis();
    }

    public static void kur() {
        MODULLER.add(new HudModul("fps", "FPS", true, 0.005f, 0.01f) {
            public void ciz(GuiGraphics g, Minecraft mc, int x, int y, boolean d) {
                kutuluYazi(g, mc, x, y, mc.getFps() + " FPS");
            }
        });
        MODULLER.add(new HudModul("ping", "Ping", true, 0.005f, 0.045f) {
            public void ciz(GuiGraphics g, Minecraft mc, int x, int y, boolean d) {
                int ms = 0;
                if (mc.player != null && mc.getConnection() != null) {
                    PlayerInfo bilgi = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                    if (bilgi != null) ms = bilgi.getLatency();
                }
                kutuluYazi(g, mc, x, y, ms + " ms");
            }
        });
        MODULLER.add(new HudModul("koordinat", "Koordinat", true, 0.005f, 0.08f) {
            public void ciz(GuiGraphics g, Minecraft mc, int x, int y, boolean d) {
                if (mc.player == null) { kutuluYazi(g, mc, x, y, "X 0  Y 64  Z 0  Kuzey"); return; }
                kutuluYazi(g, mc, x, y, String.format("X %d  Y %d  Z %d  %s", mc.player.getBlockX(),
                        mc.player.getBlockY(), mc.player.getBlockZ(), yon(mc.player.getDirection())));
            }
        });
        MODULLER.add(new HudModul("saat", "Saat", false, 0.005f, 0.115f) {
            public void ciz(GuiGraphics g, Minecraft mc, int x, int y, boolean d) {
                kutuluYazi(g, mc, x, y, LocalTime.now().format(SAAT));
            }
        });
        MODULLER.add(new HudModul("oturum", "Oturum Süresi", false, 0.005f, 0.15f) {
            public void ciz(GuiGraphics g, Minecraft mc, int x, int y, boolean d) {
                long sn = girisAni == 0 ? 0 : (System.currentTimeMillis() - girisAni) / 1000;
                kutuluYazi(g, mc, x, y, String.format("%d:%02d:%02d", sn / 3600, sn / 60 % 60, sn % 60));
            }
        });
        MODULLER.add(new HudModul("sunucu", "Sunucu", false, 0.005f, 0.185f) {
            public void ciz(GuiGraphics g, Minecraft mc, int x, int y, boolean d) {
                kutuluYazi(g, mc, x, y, Ag.sunucu.isEmpty() ? "HapiCraft" : Ag.sunucu);
            }
        });
        MODULLER.add(new HudModul("tuslar", "Tuş Vuruşları", false, 0.005f, 0.6f) {
            public void ciz(GuiGraphics g, Minecraft mc, int x, int y, boolean d) {
                genislik = 62; yukseklik = 62;
                tus(g, mc, x + 21, y, 20, 20, "W", mc.options.keyUp.isDown());
                tus(g, mc, x, y + 21, 20, 20, "A", mc.options.keyLeft.isDown());
                tus(g, mc, x + 21, y + 21, 20, 20, "S", mc.options.keyDown.isDown());
                tus(g, mc, x + 42, y + 21, 20, 20, "D", mc.options.keyRight.isDown());
                tus(g, mc, x, y + 42, 30, 20, cps(SOL) + " CPS", mc.options.keyAttack.isDown());
                tus(g, mc, x + 32, y + 42, 30, 20, cps(SAG) + " CPS", mc.options.keyUse.isDown());
            }
        });
        MODULLER.add(new HudModul("zirh", "Zırh Durumu", true, 0.005f, 0.4f) {
            private static final EquipmentSlot[] YUVALAR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
            public void ciz(GuiGraphics g, Minecraft mc, int x, int y, boolean d) {
                genislik = 52; int satir = 0;
                if (mc.player != null) for (EquipmentSlot yuva : YUVALAR) {
                    ItemStack es = mc.player.getItemBySlot(yuva);
                    if (es.isEmpty()) continue;
                    int sy = y + satir * 17;
                    g.renderItem(es, x, sy);
                    String yazi; int renk = 0xFFFFFFFF;
                    if (es.isDamageableItem()) {
                        int kalan = es.getMaxDamage() - es.getDamageValue();
                        float oran = (float) kalan / es.getMaxDamage();
                        renk = oran > 0.5f ? 0xFF2BD35A : oran > 0.2f ? 0xFFFFD23F : 0xFFFF5555;
                        yazi = String.valueOf(kalan);
                    } else yazi = es.getCount() > 1 ? "x" + es.getCount() : "";
                    g.drawString(mc.font, yazi, x + 19, sy + 4, renk, true);
                    satir++;
                }
                yukseklik = Math.max(17, satir * 17);
                if (satir == 0 && d) g.drawString(mc.font, "Zırh", x + 2, y + 4, 0xFFAAAAAA, true);
            }
        });
        MODULLER.add(new HudModul("iksir", "İksir Etkileri", false, 0.86f, 0.3f) {
            public void ciz(GuiGraphics g, Minecraft mc, int x, int y, boolean d) {
                genislik = 90; int satir = 0;
                if (mc.player != null) for (MobEffectInstance etki : mc.player.getActiveEffects()) {
                    int sn = etki.getDuration() / 20;
                    String sure = etki.isInfiniteDuration() ? "∞" : String.format("%d:%02d", sn / 60, sn % 60);
                    String adi = etki.getEffect().value().getDisplayName().getString()
                            + (etki.getAmplifier() > 0 ? " " + (etki.getAmplifier() + 1) : "");
                    g.drawString(mc.font, adi, x, y + satir * 10, 0xFFFFFFFF, true);
                    g.drawString(mc.font, sure, x + genislik - mc.font.width(sure), y + satir * 10, 0xFF2BD35A, true);
                    satir++;
                }
                if (satir == 0 && d) { g.drawString(mc.font, "İksir etkileri", x, y, 0xFFAAAAAA, true); satir = 1; }
                yukseklik = Math.max(10, satir * 10);
            }
        });
        MODULLER.add(new HudModul("sessizgosterge", "Sessiz Sohbet Göstergesi", true, 0.005f, 0.9f) {
            public void ciz(GuiGraphics g, Minecraft mc, int x, int y, boolean d) {
                if (!SessizSohbet.etkin() && !d) { genislik = 0; yukseklik = 0; return; }
                kutuluYazi(g, mc, x, y, "Sessiz sohbet" + (SessizSohbet.gizlenen > 0 ? " · " + SessizSohbet.gizlenen : ""));
            }
        });

        HudElementRegistry.addLast(HapiClient.kimlik("hud"), (g, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui || mc.screen instanceof HudDuzenleyici || mc.getDebugOverlay().showDebugScreen()) return;
            int w = g.guiWidth(), h = g.guiHeight();
            for (HudModul m : MODULLER) if (m.acik()) m.ciz(g, mc, m.x(w), m.y(h), false);
        });
    }

    private static void tus(GuiGraphics g, Minecraft mc, int x, int y, int w, int h, String yazi, boolean basili) {
        g.fill(x, y, x + w, y + h, basili ? 0xC02BD35A : 0x90000000);
        g.drawString(mc.font, yazi, x + (w - mc.font.width(yazi)) / 2, y + (h - 8) / 2, basili ? 0xFF000000 : 0xFFFFFFFF, false);
    }

    private static String yon(Direction d) {
        return switch (d) {
            case NORTH -> "Kuzey"; case SOUTH -> "Güney"; case EAST -> "Doğu"; case WEST -> "Batı";
            default -> "";
        };
    }
}
