package tr.com.hapicraft.client.proxy.cekirdek;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * hapiclient:ana kanalının paket biçimi: 1 bayt paket numarası + UTF-8 JSON gövde, en fazla 16 KB.
 * Velocity'ye bağımlı değildir.
 */
public final class Protokol {

    public static final String KANAL = "hapiclient:ana";
    public static final int PROTO = 1;
    public static final int MAX_BOYUT = 16 * 1024;

    // İstemci -> sunucu
    public static final int MERHABA = 1;
    public static final int KOZMETIK = 2;
    public static final int EMOTE = 3;
    // Sunucu -> istemci
    public static final int HOSGELDIN = 16;
    public static final int OYUNCULAR = 17;
    public static final int AYRILDI = 18;
    public static final int EMOTE_YAYIN = 19;
    public static final int BILDIRIM = 20;

    public static final int MAX_MOD = 200;
    public static final int MAX_MOD_UZUNLUK = 80;
    public static final int MAX_SURUM_UZUNLUK = 32;
    public static final int MAX_BASLIK_UZUNLUK = 64;
    public static final int MAX_METIN_UZUNLUK = 512;
    /** Tek OYUNCULAR paketindeki en fazla kayıt; en kötü durumda kayıt ~300 bayt, 16 KB'ın altında kalır. */
    public static final int OYUNCULAR_PARCA = 40;

    private static final Pattern KIMLIK = Pattern.compile("[a-z0-9_]{0,32}");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private Protokol() {
    }

    public record Paket(int no, JsonObject govde) {
    }

    public record Merhaba(String surum, int proto, List<String> modlar) {
    }

    /** Bir oyuncunun kozmetik kaydı (OYUNCULAR listesinin öğesi). */
    public record Kayit(UUID uuid, Map<String, String> kozmetik) {
    }

    public static final class ProtokolHatasi extends Exception {
        public ProtokolHatasi(String mesaj) {
            super(mesaj);
        }
    }

    public static boolean kimlikGecerli(String kimlik) {
        return kimlik != null && KIMLIK.matcher(kimlik).matches();
    }

    /** Ham paketi çözer. Bozuk her girdi ProtokolHatasi olur; başka istisna sızmaz. */
    public static Paket coz(byte[] veri) throws ProtokolHatasi {
        if (veri == null || veri.length < 2) {
            throw new ProtokolHatasi("paket boş ya da gövdesiz");
        }
        if (veri.length > MAX_BOYUT) {
            throw new ProtokolHatasi("paket 16 KB sınırını aşıyor: " + veri.length);
        }
        int no = veri[0] & 0xFF;
        String metin = new String(veri, 1, veri.length - 1, StandardCharsets.UTF_8);
        JsonElement oge;
        try {
            oge = JsonParser.parseString(metin);
        } catch (RuntimeException | StackOverflowError e) {
            throw new ProtokolHatasi("bozuk JSON");
        }
        if (oge == null || !oge.isJsonObject()) {
            throw new ProtokolHatasi("gövde bir JSON nesnesi değil");
        }
        return new Paket(no, oge.getAsJsonObject());
    }

    public static byte[] kodla(int no, JsonObject govde) {
        byte[] json = GSON.toJson(govde).getBytes(StandardCharsets.UTF_8);
        if (json.length + 1 > MAX_BOYUT) {
            throw new IllegalArgumentException("paket 16 KB sınırını aşıyor: " + (json.length + 1));
        }
        byte[] veri = new byte[json.length + 1];
        veri[0] = (byte) no;
        System.arraycopy(json, 0, veri, 1, json.length);
        return veri;
    }

    /** Alan metin değilse ya da yoksa null döner. */
    public static String metinAlani(JsonObject govde, String alan) {
        JsonElement e = govde.get(alan);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
            return null;
        }
        return e.getAsString();
    }

    /** Denetim karakterlerini atar ve en fazla {@code max} karaktere keser. */
    public static String temizle(String s, int max) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(Math.min(s.length(), max));
        for (int i = 0; i < s.length() && sb.length() < max; i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c != 0x7F) {
                sb.append(c);
            }
        }
        // Kesme bir vekil çiftini ortadan böldüyse yarım kalan karakteri at.
        if (sb.length() > 0 && Character.isHighSurrogate(sb.charAt(sb.length() - 1))) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public static Merhaba merhabaCoz(JsonObject govde) {
        String surum = temizle(metinAlani(govde, "surum"), MAX_SURUM_UZUNLUK);
        if (surum.isEmpty()) {
            surum = "?";
        }
        int proto = 0;
        JsonElement p = govde.get("proto");
        if (p != null && p.isJsonPrimitive() && p.getAsJsonPrimitive().isNumber()) {
            try {
                proto = p.getAsInt();
            } catch (NumberFormatException ignored) {
                // sayı int'e sığmıyor: 0 kalır
            }
        }
        List<String> modlar = new ArrayList<>();
        JsonElement m = govde.get("modlar");
        if (m != null && m.isJsonArray()) {
            for (JsonElement e : m.getAsJsonArray()) {
                if (modlar.size() >= MAX_MOD) {
                    break;
                }
                if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                    String mod = temizle(e.getAsString(), MAX_MOD_UZUNLUK);
                    if (!mod.isEmpty()) {
                        modlar.add(mod);
                    }
                }
            }
        }
        return new Merhaba(surum, proto, List.copyOf(modlar));
    }

    public static byte[] hosgeldin(Collection<String> kapali, Collection<String> sahip, int indirim, String sunucu) {
        JsonObject o = new JsonObject();
        o.addProperty("proto", PROTO);
        o.add("kapali", dizi(kapali));
        o.add("sahip", dizi(sahip));
        o.addProperty("indirim", indirim);
        o.addProperty("sunucu", sunucu);
        return kodla(HOSGELDIN, o);
    }

    /**
     * OYUNCULAR paket(ler)i. Liste 16 KB'a sığmayabileceği için parçalanır; {@code sifirla} yalnız
     * ilk parçada true olur. Boş liste de tek paket üretir (istemci tablosunu sıfırlayabilsin).
     */
    public static List<byte[]> oyuncular(List<Kayit> kayitlar, boolean sifirla) {
        List<byte[]> paketler = new ArrayList<>();
        int i = 0;
        do {
            JsonArray liste = new JsonArray();
            int son = Math.min(kayitlar.size(), i + OYUNCULAR_PARCA);
            for (; i < son; i++) {
                Kayit k = kayitlar.get(i);
                JsonObject kayit = new JsonObject();
                kayit.addProperty("uuid", k.uuid().toString());
                JsonObject koz = new JsonObject();
                k.kozmetik().forEach(koz::addProperty);
                kayit.add("k", koz);
                liste.add(kayit);
            }
            JsonObject o = new JsonObject();
            o.add("liste", liste);
            o.addProperty("sifirla", sifirla && paketler.isEmpty());
            paketler.add(kodla(OYUNCULAR, o));
        } while (i < kayitlar.size());
        return paketler;
    }

    public static byte[] ayrildi(UUID uuid) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", uuid.toString());
        return kodla(AYRILDI, o);
    }

    public static byte[] emote(UUID uuid, String id) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", uuid.toString());
        o.addProperty("id", id);
        return kodla(EMOTE_YAYIN, o);
    }

    public static byte[] bildirim(String baslik, String metin) {
        JsonObject o = new JsonObject();
        o.addProperty("baslik", temizle(baslik, MAX_BASLIK_UZUNLUK));
        o.addProperty("metin", temizle(metin, MAX_METIN_UZUNLUK));
        return kodla(BILDIRIM, o);
    }

    private static JsonArray dizi(Collection<String> ogeler) {
        JsonArray a = new JsonArray();
        ogeler.forEach(a::add);
        return a;
    }
}
