package tr.com.hapicraft.client.kozmetik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.Random;

/** Aura parçacıkları: her auranın kendi deseni vardır (yükselen, yörünge, yağan, saçılan, halka). */
public final class Aura {
    private static final Random R = new Random();

    private Aura() {}

    static SimpleParticleType tur(String parcacik) {
        return switch (parcacik) {
            case "kalp" -> ParticleTypes.HEART;
            case "alev" -> ParticleTypes.FLAME;
            case "buyu" -> ParticleTypes.ENCHANT;
            case "nota" -> ParticleTypes.NOTE;
            case "kiraz" -> ParticleTypes.CHERRY_LEAVES;
            case "ruh" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "yildiz" -> ParticleTypes.END_ROD;
            case "kar" -> ParticleTypes.SNOWFLAKE;
            case "simsek" -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.HAPPY_VILLAGER;
        };
    }

    public static void tik(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        int mesafe = tr.com.hapicraft.client.performans.Profil.aktif().auraMesafesi;
        for (AbstractClientPlayer o : mc.level.players()) {
            if (o.isInvisible() || o.distanceToSqr(mc.player) > mesafe * mesafe) continue;
            Kozmetik k = Kozmetikler.takili(o.getUUID(), "aura");
            if (k == null) continue;
            if (o == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            uret(mc, o, k);
        }
    }

    private static void uret(Minecraft mc, AbstractClientPlayer o, Kozmetik k) {
        SimpleParticleType tur = tur(k.parcacik());
        int t = o.tickCount;
        double x = o.getX(), y = o.getY(), z = o.getZ();
        switch (k.desen()) {
            case "yorunge" -> {
                // Çift sarmal: iki nokta karşılıklı döner, ayaktan başa çıkıp yeniden başlar.
                if (t % 2 != 0) return;
                double a = t * 0.22, h = (t * 0.035) % 2.0;
                for (int i = 0; i < 2; i++) {
                    double b = a + i * Math.PI;
                    mc.level.addParticle(tur, x + Math.cos(b) * 0.75, y + 0.1 + h, z + Math.sin(b) * 0.75, 0, 0, 0);
                }
            }
            case "yagan" -> {
                if (t % 3 != 0) return;
                double a = R.nextDouble() * Math.PI * 2, r = R.nextDouble() * 0.9;
                mc.level.addParticle(tur, x + Math.cos(a) * r, y + 2.5 + R.nextDouble() * 0.3, z + Math.sin(a) * r, 0, -0.03, 0);
            }
            case "sacilan" -> {
                if (t % 4 != 0) return;
                double a = R.nextDouble() * Math.PI * 2, r = 0.45 + R.nextDouble() * 0.4;
                // Nota parçacığında ilk hız değeri rengi belirler.
                double vx = k.parcacik().equals("nota") ? R.nextInt(25) / 24.0 : (R.nextDouble() - 0.5) * 0.08;
                mc.level.addParticle(tur, x + Math.cos(a) * r, y + 0.6 + R.nextDouble() * 1.6, z + Math.sin(a) * r,
                        vx, 0.02, (R.nextDouble() - 0.5) * 0.08);
            }
            case "halka" -> {
                double a = t * 0.3;
                for (int i = 0; i < 2; i++) {
                    double b = a + i * Math.PI;
                    mc.level.addParticle(tur, x + Math.cos(b) * 0.8, y + 0.08, z + Math.sin(b) * 0.8, 0, 0.012, 0);
                }
            }
            default -> {
                if (t % 5 != 0) return;
                double a = R.nextDouble() * Math.PI * 2, r = 0.3 + R.nextDouble() * 0.45;
                boolean kalp = k.parcacik().equals("kalp");
                mc.level.addParticle(tur, x + Math.cos(a) * r, y + (kalp ? 2.0 : 0.2 + R.nextDouble() * 1.6),
                        z + Math.sin(a) * r, 0, 0.03, 0);
            }
        }
    }
}
