#!/usr/bin/env python3
"""Ham kozmetik görsellerini Codex'in resim aracıyla üretir. Kullanım: uret_ham.py [ad ...]
Var olan dosyayı yeniden üretmez; yenilemek için dosyayı sil."""
import subprocess, sys, pathlib, concurrent.futures

KOK = pathlib.Path(__file__).resolve().parent

KANAT_ORTAK = """Konu: bir Minecraft kozmetiği için TEK bir {konu} (yalnızca bir kanat, çift değil). Ayrıntılı piksel sanatı (pixel art), küçük ve net pikseller, sert kenarlar, kenar yumuşatma (anti-aliasing) yok, bulanıklık yok.
Yerleşim: kanat tam yandan düz görünüm (perspektif yok). Kanadın omuza bağlanan kökü görselin SOL kenarının ortasında; kanat sağa doğru açılmış, ucu sağ üstte. Kanat görselin büyük kısmını doldursun, kenarlara taşmasın, tek parça olsun (kopuk parçacık, kıvılcım, yıldız saçılması olmasın).
Görünüm: {gorunum} Her parçanın koyu dış çizgisi ve gölgelendirmesi olsun.
Arka plan: tamamen düz, tek renk saf {zemin}. Gölge, zemin, yazı, çerçeve, karakter yok."""

PELERIN_ORTAK = """Konu: bir Minecraft pelerininin sırt deseni: {konu}. Ayrıntılı piksel sanatı (pixel art), küçük ve net pikseller, sert kenarlar, kenar yumuşatma yok, bulanıklık yok.
Yerleşim: dikey dikdörtgen kumaş deseni; desen tuvalin TAMAMINI kenardan kenara doldursun (boş arka plan, çerçeve dışı boşluk, pelerin silüeti, kıvrım, askı, karakter yok; düz dikdörtgen bir kumaş yüzeyi). Ana motif üst-orta bölgede, alt kısım desenin devamı.
Görünüm: {gorunum}
Yazı, harf, logo, filigran yok."""

MOR = "macenta (#FF00FF)"
YESIL = "yeşil (#00FF00)"

ISLER = {
 "kanat/seytan": ("1024x1024", KANAT_ORTAK.format(konu="şeytan kanadı (yarasa kanadı biçiminde)", zemin=YESIL,
   gorunum="Üstte kalın siyah-kömür rengi kemik kol, ondan aşağı yelpaze gibi açılan dört uzun parmak kemiği; aralarında koyu kızıl-bordo deri zar, zarın alt kenarı sivri kavisli ve yer yer yırtık. Parmak uçlarında ve eklemde fildişi pençeler. Zarın içinde kor gibi parlayan ince turuncu-kırmızı damarlar.")),
 "kanat/ejderha": ("1024x1024", KANAT_ORTAK.format(konu="ejderha kanadı", zemin=MOR,
   gorunum="Kol kemiği boyunca üst üste binmiş zümrüt yeşili pullar, eklemlerde fildişi dikenler ve uçta büyük bir pençe. Parmak kemikleri arasında açık sarı-yeşil yarı mat zar, zarda ince koyu yeşil damarlar ve kenara doğru koyulaşan ton. Alt kenar kavisli dişli.")),
 "kanat/kelebek": ("1024x1024", KANAT_ORTAK.format(konu="kral kelebeği kanadı (tek taraf: büyük ön kanat üstte, küçük arka kanat altta, ikisi kökte birleşik)", zemin=MOR,
   gorunum="Canlı turuncu hücreler, kalın siyah damarlar, siyah kalın kenar bandı ve bantta iki sıra küçük beyaz benek. Turuncu hücrelerde kökten uca sarıdan koyu turuncuya geçiş.")),
 "kanat/peri": ("1024x1024", KANAT_ORTAK.format(konu="peri kanadı (yusufçuk kanadı gibi ince uzun; üstte uzun ön kanat, altta kısa arka kanat, kökte birleşik)", zemin="siyah (#000000)",
   gorunum="Cam gibi yarı saydam görünen buz mavisi-turkuaz yüzey, pembe ve lila yanardöner yansımalar, ince beyaz damar ağı, uçlara doğru açılan parlak beyaz kenar. Koyu dış çizgi yerine açık camgöbeği dış çizgi kullan.")),
 "kanat/hapi": ("1024x1024", KANAT_ORTAK.format(konu="yeşil tüylü kuş kanadı", zemin=MOR,
   gorunum="Gerçek kuş kanadı gibi üç sıra katmanlı tüy: üstte kısa örtü tüyleri, ortada ikinci sıra, altta uzun uçuş telekleri. Tüyler canlı çimen yeşili (#2BD35A) ve koyu zümrüt gölgeli; her tüyün ucu bembeyaz. Omuz kökünde beyaz çerçeveli parlak yeşil bir zümrüt taş.")),
 "kanat/anka": ("1024x1024", KANAT_ORTAK.format(konu="anka kuşu (zümrüdüanka) kanadı", zemin=YESIL,
   gorunum="Katmanlı tüyler kökte koyu kızıl, ortada turuncu, uçlarda parlak sarı; uzun uçuş teleklerinin uçları alev diline dönüşüyor ama kanada bağlı kalıyor. Kol hattı boyunca altın sarısı parlak tüy sapları. Sıcak, ışıldayan renkler.")),
 "kanat/gece": ("1024x1024", KANAT_ORTAK.format(konu="gece göğü temalı tüylü kanat", zemin=YESIL,
   gorunum="Katmanlı tüyler koyu lacivert ve gece moru; tüylerin üzerinde küçük parlak beyaz ve açık mavi yıldız noktaları ile birkaç takımyıldız çizgisi. Uzun teleklerin uçları parlak mor-eflatun. Omuz kökünde küçük gümüş bir hilal süsü.")),
 "kanat/mekanik": ("1024x1024", KANAT_ORTAK.format(konu="mekanik steampunk kanat", zemin=MOR,
   gorunum="Pirinç ve bakır borulardan eklemli bir iskelet kol; eklemlerde perçinler ve pistonlar. İskeletten aşağı yelpaze gibi açılan, üst üste binen çelik mavisi-gri metal bıçak tüyler, her bıçakta perçin ve yansıma çizgisi. Omuz kökünde ve orta eklemde birer yuvarlak boş pirinç yuva (içine dişli takılacak, yuvaların içi koyu). Kol boyunca ince camgöbeği parlayan enerji hattı.")),
 "kanat/buz": ("1024x1024", KANAT_ORTAK.format(konu="buz kristali kanadı", zemin=MOR,
   gorunum="Tüy yerine keskin, köşeli buz kristali parçaları: kol hattı boyunca kısa kristaller, aşağı ve sağa doğru uzayan sivri buz sarkıtları. Renkler beyaz, buz mavisi ve derin gölgelerde koyu mavi; kristallerin içinde çapraz yansıma çizgileri ve beyaz parıltı noktaları. Dış çizgi koyu lacivert.")),

 "pelerin/hapi": ("1024x1536", PELERIN_ORTAK.format(konu="canlı yeşil üzerine beyaz yonca amblemi",
   gorunum="Zemin canlı çimen yeşili (#2BD35A), kenarlara doğru koyu zümrüt gölge. Üst-ortada büyük beyaz dört yapraklı yonca amblemi, çevresinde ince beyaz halka. Alt yarıda açık yeşil ince çapraz baklava dokusu ve en altta beyaz iki şerit bordür.")),
 "pelerin/gece": ("1024x1536", PELERIN_ORTAK.format(konu="yıldızlı gece göğü",
   gorunum="Üstten alta koyu laciverdden mora geçen gökyüzü, üst-ortada parlak hilal, irili ufaklı yıldızlar ve bir kayan yıldız, altta koyu çam ağacı ve dağ silüetleri.")),
 "pelerin/alev": ("1024x1536", PELERIN_ORTAK.format(konu="alttan yükselen alevler",
   gorunum="Üst kısım kömür siyahı ve koyu bordo; alttan yukarı kıvrılarak yükselen iri alev dilleri: dışta kızıl, ortada turuncu, içte parlak sarı. Alevlerin üstünde birkaç kor parçası.")),
 "pelerin/buz": ("1024x1536", PELERIN_ORTAK.format(konu="buz ve kar kristali",
   gorunum="Zemin açık buz mavisinden koyu maviye geçiş. Üst-ortada büyük ayrıntılı beyaz kar tanesi kristali, çevresinde küçük kar taneleri; alt kenarda sivri buz sarkıtı gibi beyaz-mavi kırağı dokusu.")),
 "pelerin/altin": ("1024x1536", PELERIN_ORTAK.format(konu="krallara yakışır altın işlemeli kumaş",
   gorunum="Zemin koyu bordo kadife. Üst-ortada altın sarısı işlemeli taç ve altında defne dalları; kenarlarda altın kıvrımlı sarmaşık bordür, alt kenarda altın püskül sırası. Altın kısımlarda parlak sarı vurgular.")),
 "pelerin/gokkusagi": ("1024x1536", PELERIN_ORTAK.format(konu="gökkuşağı",
   gorunum="Yukarıdan aşağı yedi renkli geniş yatay gökkuşağı bantları (kırmızı, turuncu, sarı, yeşil, mavi, lacivert, mor), bantlar arasında piksel piksel titrek (dither) geçişler; üst-ortada küçük beyaz bulut ve birkaç beyaz parıltı.")),
 "pelerin/kareli": ("1024x1536", PELERIN_ORTAK.format(konu="yeşil ekose (tartan) kumaş",
   gorunum="Koyu orman yeşili ve canlı yeşil kalın kareler, aralarında ince beyaz ve sarı çizgilerle klasik İskoç ekosesi; kumaş dokusu hissi veren piksel taraması. Simetrik ve düzenli.")),
 "pelerin/siyah": ("1024x1536", PELERIN_ORTAK.format(konu="gizemli kara pelerin",
   gorunum="Zemin simsiyah ve çok koyu mor; kumaşta belli belirsiz koyu mor damask (kıvrımlı yaprak) işlemesi. Üst-ortada gümüş renkli ince işlenmiş bir kuzgun kafatası amblemi; alt kenar gümüş ince zikzak bordürlü.")),
 "pelerin/kalp": ("1024x1536", PELERIN_ORTAK.format(konu="kalpli sevimli pelerin",
   gorunum="Zemin şeker pembesi, altta koyu pembeye geçiş. Üst-ortada büyük parlak kırmızı kalp (beyaz parlama noktalı), çevresinde irili ufaklı beyaz ve açık pembe kalpler; alt kenarda beyaz dantel bordür.")),
}

def uret(ad):
    hedef = KOK / (ad + ".png")
    if hedef.exists(): return ad, "var"
    boyut, istek = ISLER[ad]
    tam = (f"Resim üretme aracınla TEK bir görsel üret ve bu dizine {hedef.name} adıyla kaydet ({boyut} PNG). "
           f"Başka dosya değiştirme, başka dosya oluşturma.\n\n{istek}")
    with open(KOK / "log" / (ad.replace("/", "_") + ".log"), "w") as log:
        subprocess.run(["codex", "exec", "--skip-git-repo-check", "--sandbox", "workspace-write", "-C", str(hedef.parent), tam],
                       stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, timeout=1200)
    return ad, "tamam" if hedef.exists() else "YOK"

if __name__ == "__main__":
    adlar = sys.argv[1:] or list(ISLER)
    with concurrent.futures.ThreadPoolExecutor(2) as h:
        for ad, sonuc in h.map(uret, adlar): print(ad, sonuc, flush=True)
