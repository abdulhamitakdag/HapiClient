package tr.com.hapicraft.client.performans;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import tr.com.hapicraft.client.Ayarlar;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Sistem taraması. Launcher'ın yazdığı donanim.json varsa onunla birleştirilir. */
public record Donanim(String gpu, int cekirdek, long sistemRamMB, long javaRamMB) {

    public static Donanim tara() {
        String gpu = "Bilinmiyor";
        try {
            GpuDevice aygit = RenderSystem.tryGetDevice();
            if (aygit != null) gpu = aygit.getRenderer();
        } catch (Throwable ignored) { }
        long sistem = 0;
        try {
            if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os)
                sistem = os.getTotalMemorySize() / (1024 * 1024);
        } catch (Throwable ignored) { }
        try {
            Path dosya = Ayarlar.dizin().resolve("donanim.json");
            if (Files.exists(dosya)) {
                JsonObject j = JsonParser.parseString(Files.readString(dosya)).getAsJsonObject();
                if (sistem == 0 && j.has("ramMB")) sistem = j.get("ramMB").getAsLong();
                if (gpu.equals("Bilinmiyor") && j.has("gpu")) gpu = j.get("gpu").getAsString();
            }
        } catch (Throwable ignored) { }
        return new Donanim(gpu, Runtime.getRuntime().availableProcessors(), sistem,
                Runtime.getRuntime().maxMemory() / (1024 * 1024));
    }

    /** 0 = çok zayıf … 4 = çok güçlü. Saf fonksiyon. */
    public int puan() {
        int ekran = ekranSinifi();
        int bellek = sistemRamMB == 0 ? 2 : sistemRamMB < 5000 ? 0 : sistemRamMB < 9000 ? 1 : sistemRamMB < 17000 ? 3 : 4;
        int islemci = cekirdek <= 2 ? 0 : cekirdek <= 4 ? 1 : cekirdek <= 8 ? 3 : 4;
        int enZayif = Math.min(ekran, Math.min(bellek, islemci));
        int ortalama = Math.round((ekran * 2 + bellek + islemci) / 4f);
        int sifir = (ekran == 0 ? 1 : 0) + (bellek == 0 ? 1 : 0) + (islemci == 0 ? 1 : 0);
        if (sifir >= 2) return 0;
        return Math.max(0, Math.min(4, Math.min(ortalama, enZayif + 1)));
    }

    /** Ekran kartının kaba sınıfı: 0 = yazılımsal … 4 = üst seviye. Saf fonksiyon. */
    public int ekranSinifi() {
        String g = gpu.toLowerCase(Locale.ROOT);
        int ekran;
        if (g.contains("llvmpipe") || g.contains("software") || g.contains("swiftshader")) ekran = 0;
        else if (g.matches(".*(rtx|rx [5-9]\\d{3}|rx [5-9]\\d\\d\\b|arc a[5-9]|arc b|apple m[1-9]).*")) ekran = 4;
        else if (g.matches(".*(gtx 1[06]\\d\\d|gtx 9[5-9]\\d|gtx 10[5-9]\\d|rx [4-5]\\d\\d|arc a[1-4]).*")) ekran = 3;
        else if (g.matches(".*(gtx|geforce mx|radeon rx|iris xe|iris\\(r\\) xe|radeon\\(tm\\) graphics|vega 8|vega 1[01]|radeon 6[6-9]\\dm|radeon 7[4-9]\\dm).*")) ekran = 2;
        else if (g.matches(".*(intel|uhd|hd graphics|mesa|radeon r[2-7]|vega [3-6]|geforce [1-9]\\d\\d\\b|gt [1-9]\\d\\d).*")) ekran = 1;
        else ekran = 2;
        return ekran;
    }

    /**
     * 0–1 puan Ultra FPS Boost, 2 FPS Boost, 3 Normal, 4 Yüksek Kalite. Gölgelendiricili mod yalnız
     * her yönden güçlü sistemlere önerilir. Launcher'ın önerisi (donanim.json) varsa o geçerlidir.
     */
    public Profil onerilen() {
        try {
            Path dosya = Ayarlar.dizin().resolve("donanim.json");
            if (Files.exists(dosya)) {
                JsonObject j = JsonParser.parseString(Files.readString(dosya)).getAsJsonObject();
                Profil p = j.has("profil") ? Profil.bul(j.get("profil").getAsString()) : null;
                if (p != null) return p;
            }
        } catch (Throwable ignored) { }
        return hesaplananOneri();
    }

    /** Yalnız bu kayıttaki değerlerden hesaplanan öneri. Saf fonksiyon. */
    public Profil hesaplananOneri() {
        return switch (puan()) {
            case 0, 1 -> Profil.ULTRA_FPS;
            case 2 -> Profil.FPS;
            case 3 -> Profil.NORMAL;
            default -> ekranSinifi() >= 4 ? Profil.KALITE : Profil.NORMAL;
        };
    }
}
