package tr.com.hapicraft.client.proxy.cekirdek;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** config.json içeriği. Değiştirilemez; yenilemede yeni bir örnek oluşturulur. */
public final class Ayarlar {

    public static final String VARSAYILAN_JSON = """
            {
              "kapali": [],
              "sunucu_kapali": {
                "Pillars": ["tamparlaklik"]
              },
              "ucretsiz": ["*"],
              "ucretli": [],
              "indirim": 10,
              "izinli_modlar": []
            }
            """;

    private final List<String> kapali;
    /** Küçük harfli sunucu adı -> o sunucuda kapatılacak modüller (genel listenin yerine geçer). */
    private final Map<String, List<String>> sunucuKapali;
    private final Set<String> ucretsiz;
    private final List<String> ucretli;
    private final int indirim;
    /** Küçük harfli mod kimlikleri; boşsa denetim kapalı. */
    private final Set<String> izinliModlar;

    private Ayarlar(List<String> kapali, Map<String, List<String>> sunucuKapali, Set<String> ucretsiz,
                    List<String> ucretli, int indirim, Set<String> izinliModlar) {
        this.kapali = kapali;
        this.sunucuKapali = sunucuKapali;
        this.ucretsiz = ucretsiz;
        this.ucretli = ucretli;
        this.indirim = indirim;
        this.izinliModlar = izinliModlar;
    }

    public static Ayarlar varsayilan() {
        return coz(VARSAYILAN_JSON);
    }

    /**
     * JSON metnini çözer. Eksik alan varsayılanını alır; yanlış türdeki alan ya da bozuk JSON
     * IllegalArgumentException olur (çağıran eski ayarı korur).
     */
    public static Ayarlar coz(String json) {
        JsonObject kok;
        try {
            JsonElement e = JsonParser.parseString(json);
            if (!e.isJsonObject()) {
                throw new IllegalArgumentException("config kökü bir JSON nesnesi olmalı");
            }
            kok = e.getAsJsonObject();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException | StackOverflowError e) {
            throw new IllegalArgumentException("config bozuk JSON: " + e.getMessage());
        }

        List<String> kapali = metinListesi(kok, "kapali", List.of());
        Map<String, List<String>> sunucuKapali = new HashMap<>();
        JsonElement sk = kok.get("sunucu_kapali");
        if (sk != null) {
            if (!sk.isJsonObject()) {
                throw new IllegalArgumentException("sunucu_kapali bir nesne olmalı");
            }
            for (String sunucu : sk.getAsJsonObject().keySet()) {
                sunucuKapali.put(sunucu.toLowerCase(Locale.ROOT),
                        metinListesi(sk.getAsJsonObject(), sunucu, List.of()));
            }
        }
        Set<String> ucretsiz = new LinkedHashSet<>(metinListesi(kok, "ucretsiz", List.of("*")));
        List<String> ucretli = metinListesi(kok, "ucretli", List.of());
        int indirim = 10;
        JsonElement ind = kok.get("indirim");
        if (ind != null) {
            if (!ind.isJsonPrimitive() || !ind.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("indirim bir sayı olmalı");
            }
            indirim = Math.max(0, Math.min(100, (int) ind.getAsDouble()));
        }
        Set<String> izinliModlar = new LinkedHashSet<>();
        for (String mod : metinListesi(kok, "izinli_modlar", List.of())) {
            izinliModlar.add(mod.toLowerCase(Locale.ROOT));
        }
        return new Ayarlar(kapali, Map.copyOf(sunucuKapali), Set.copyOf(ucretsiz), ucretli, indirim,
                Set.copyOf(izinliModlar));
    }

    private static List<String> metinListesi(JsonObject o, String alan, List<String> varsayilan) {
        JsonElement e = o.get(alan);
        if (e == null) {
            return varsayilan;
        }
        if (!e.isJsonArray()) {
            throw new IllegalArgumentException(alan + " bir liste olmalı");
        }
        List<String> liste = new ArrayList<>();
        for (JsonElement oge : e.getAsJsonArray()) {
            if (!oge.isJsonPrimitive() || !oge.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException(alan + " yalnız metin içermeli");
            }
            liste.add(oge.getAsString());
        }
        return List.copyOf(liste);
    }

    /** Verilen sunucuda kapatılacak modüller: sunucuya özel liste varsa o, yoksa genel liste. */
    public List<String> kapali(String sunucu) {
        List<String> ozel = sunucu == null ? null : sunucuKapali.get(sunucu.toLowerCase(Locale.ROOT));
        return ozel != null ? ozel : kapali;
    }

    public Set<String> ucretsiz() {
        return ucretsiz;
    }

    public List<String> ucretli() {
        return ucretli;
    }

    public int indirim() {
        return indirim;
    }

    /**
     * Bildirilen mod listesinden izinli olmayanları döndürür ("sodium@0.8.15" -> kimlik "sodium").
     * {@code izinli_modlar} boşsa denetim kapalıdır ve sonuç boştur.
     */
    public List<String> izinsizModlar(List<String> modlar) {
        if (izinliModlar.isEmpty()) {
            return List.of();
        }
        List<String> izinsiz = new ArrayList<>();
        for (String mod : modlar) {
            int at = mod.indexOf('@');
            String kimlik = (at >= 0 ? mod.substring(0, at) : mod).toLowerCase(Locale.ROOT);
            if (!izinliModlar.contains(kimlik)) {
                izinsiz.add(mod);
            }
        }
        return izinsiz;
    }
}
