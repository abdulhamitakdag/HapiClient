#!/usr/bin/env python3
"""HapiClient kozmetik üreticisi.

Tek kaynak: bu dizin. Çıktı modun kaynaklarına yazılır.
  ham/kanat/*.png, ham/pelerin/*.png  ->  işlenmiş piksel dokuları (ham görseller ham/uret_ham.py ile üretilir)
  sapkalar.py                          ->  voksel modeller (şapka, hale)
Kullanım: python3 uret.py            (hepsi)
          python3 uret.py --onizle D  (ayrıca D dizinine gözle denetim sayfaları)
"""
import colorsys
import json
import pathlib
import shutil
import sys

import numpy as np
from PIL import Image, ImageDraw

import sapkalar
import voksel

KOK = pathlib.Path(__file__).resolve().parent
MOD = KOK.parent / 'mod/src/main/resources/assets/hapiclient'
CIKTI = KOK / 'cikti'

KANAT_BOY = 128   # kanat dokusunun uzun kenarı (piksel)
PELERIN_OLCEK = 8  # vanilla 64x32 pelerin düzeninin katı


# ------------------------------------------------------------------ görsel işleme

def kucult(rgb, maske, tw, th, renk_sayisi=56):
    """Piksel sanatını bozmadan küçültür: her hedef hücreye, kaynakta en çok geçen palet rengi gelir."""
    h, w = maske.shape
    pal_im = Image.fromarray(rgb).quantize(renk_sayisi, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    idx = np.asarray(pal_im).astype(np.int64)
    palet = np.array(pal_im.getpalette()[:renk_sayisi * 3], dtype=np.uint8).reshape(-1, 3)
    yy, xx = np.mgrid[0:h, 0:w]
    hucre = (yy * th // h) * tw + (xx * tw // w)
    sayim = np.zeros((tw * th, renk_sayisi + 1), dtype=np.int32)
    np.add.at(sayim, (hucre.ravel(), np.where(maske, idx, renk_sayisi).ravel()), 1)
    dolu = sayim[:, :renk_sayisi].sum(1)
    toplam = sayim.sum(1)
    secim = sayim[:, :renk_sayisi].argmax(1)
    cikti = np.zeros((th * tw, 4), dtype=np.uint8)
    cikti[:, :3] = palet[secim]
    cikti[:, 3] = np.where(dolu * 2 >= toplam, 255, 0)
    return cikti.reshape(th, tw, 4)


def buyuk_parcalar(maske, oran=0.004):
    """Küçük kopuk lekeleri atar (4 komşuluk)."""
    h, w = maske.shape
    etiket = np.zeros((h, w), dtype=np.int32)
    kalan = np.zeros((h, w), dtype=bool)
    n = 0
    for y0, x0 in zip(*np.nonzero(maske)):
        if etiket[y0, x0]:
            continue
        n += 1
        yigin, uye = [(y0, x0)], []
        etiket[y0, x0] = n
        while yigin:
            y, x = yigin.pop()
            uye.append((y, x))
            for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                yy, xx = y + dy, x + dx
                if 0 <= yy < h and 0 <= xx < w and maske[yy, xx] and not etiket[yy, xx]:
                    etiket[yy, xx] = n
                    yigin.append((yy, xx))
        if len(uye) >= oran * h * w:
            for y, x in uye:
                kalan[y, x] = True
    return kalan


def hsv(rgba):
    r, g, b = [rgba[..., i].astype(np.float32) / 255 for i in range(3)]
    mx, mn = np.maximum(np.maximum(r, g), b), np.minimum(np.minimum(r, g), b)
    d = mx - mn
    h = np.zeros_like(mx)
    m = d > 1e-6
    rr = m & (mx == r)
    gg = m & (mx == g) & ~rr
    bb = m & ~rr & ~gg
    h[rr] = ((g - b)[rr] / d[rr]) % 6
    h[gg] = (b - r)[gg] / d[gg] + 2
    h[bb] = (r - g)[bb] / d[bb] + 4
    return h / 6, np.where(mx > 0, d / np.maximum(mx, 1e-6), 0), mx


def kanat_isle(ad, ayar):
    im = np.asarray(Image.open(KOK / 'ham/kanat' / (ad + '.png')).convert('RGB'))
    kose = np.concatenate([im[:24, :24].reshape(-1, 3), im[:24, -24:].reshape(-1, 3),
                           im[-24:, :24].reshape(-1, 3), im[-24:, -24:].reshape(-1, 3)])
    zemin = np.median(kose, 0)
    fark = np.sqrt(((im.astype(np.float32) - zemin) ** 2).sum(-1))
    maske = fark > ayar.get('esik', 120)
    ys, xs = np.nonzero(maske)
    y0, y1, x0, x1 = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
    im, maske = im[y0:y1, x0:x1], maske[y0:y1, x0:x1]
    h, w = maske.shape
    o = KANAT_BOY / max(h, w)
    tw, th = max(1, round(w * o)), max(1, round(h * o))
    k = kucult(im, maske, tw, th)
    dolu = buyuk_parcalar(k[..., 3] > 0)
    # Zemin rengine yakın kalan saçak pikselleri at.
    yakin = np.sqrt(((k[..., :3].astype(np.float32) - zemin) ** 2).sum(-1)) < ayar.get('sacak', 90)
    dolu &= ~yakin
    dolu = buyuk_parcalar(dolu)
    k[~dolu] = 0
    # Zemin renginin bulaştığı pikselleri temizle: macenta zeminde koyu dış çizgiye çevir, yeşil zeminde yeşili kıs.
    r, g, b = [k[..., i].astype(np.int32) for i in range(3)]
    if zemin[0] > 180 and zemin[2] > 180 and zemin[1] < 90:
        bulasik = (r > g * 1.5 + 20) & (b > g * 1.5 + 20) & (np.abs(r - b) < 80)
        k[bulasik, :3] = (30, 24, 34)
    elif zemin[1] > 180 and zemin[0] < 90 and zemin[2] < 90:
        k[..., 1] = np.minimum(g, np.maximum(r, b) + 12).astype(np.uint8)
    ys, xs = np.nonzero(dolu)
    k = k[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
    H, S, V = hsv(k)
    if ayar.get('saydam'):
        k[..., 3] = np.where(k[..., 3] > 0, np.clip(95 + 190 * V ** 1.5, 0, 255), 0).astype(np.uint8)
    isik = None
    if 'isik' in ayar:
        m = ayar['isik'](H, S, V) & (k[..., 3] > 0)
        if m.any():
            isik = np.zeros_like(k)
            isik[m] = k[m]
            isik[m, 3] = 255
            k[m] = 0
    sol = np.nonzero((k[..., 3] > 0)[:, :max(2, k.shape[1] // 40)].any(1) | (isik is not None and (isik[..., 3] > 0)[:, :2].any(1)))[0]
    kok = float(sol.mean() / k.shape[0]) if len(sol) else 0.5
    return Image.fromarray(k), (Image.fromarray(isik) if isik is not None else None), kok


def pelerin_sanati(ad):
    """Pelerinin sırt deseni: 10x16 oranında, PELERIN_OLCEK katı."""
    gw, gh = 10 * PELERIN_OLCEK, 16 * PELERIN_OLCEK
    if ad == 'bayrak':
        return bayrak(gw, gh)
    im = Image.open(KOK / 'ham/pelerin' / (ad + '.png')).convert('RGB')
    w, h = im.size
    hedef_w = h * 10 // 16
    if hedef_w <= w:
        im = im.crop(((w - hedef_w) // 2, 0, (w - hedef_w) // 2 + hedef_w, h))
    else:
        hh = w * 16 // 10
        im = im.crop((0, (h - hh) // 2, w, (h - hh) // 2 + hh))
    a = np.asarray(im)
    k = kucult(a, np.ones(a.shape[:2], dtype=bool), gw, gh, 64)
    return Image.fromarray(k[..., :3])


def bayrak(gw, gh):
    """Ay yıldız: Türk Bayrağı Kanunu'ndaki oranlarla (G = bayrak eni), dikey kumaşa dik yerleştirilir."""
    ss = 6
    W, H = gw * ss, gh * ss
    G = W * 1.18
    cy = H * 0.36
    # Amblemin yatay ortası (0.25G..0.9458G) kumaşın ortasına gelsin.
    x0 = W / 2 - 0.5979 * G
    im = Image.new('RGB', (W, H), (227, 10, 23))
    d = ImageDraw.Draw(im)

    def daire(cx, r, c):
        d.ellipse((x0 + cx * G - r * G, cy - r * G, x0 + cx * G + r * G, cy + r * G), fill=c)
    daire(0.5, 0.25, (255, 255, 255))
    daire(0.5625, 0.2, (227, 10, 23))
    yc, yr = 0.8208, 0.125
    nokta = []
    for i in range(10):
        a = np.pi + i * np.pi / 5  # bir köşe hilale bakar
        r = yr if i % 2 == 0 else yr * 0.381966
        nokta.append((x0 + (yc + r * np.cos(a)) * G, cy + r * np.sin(a) * G))
    d.polygon(nokta, fill=(255, 255, 255))
    im = im.resize((gw, gh), Image.LANCZOS)
    a = np.asarray(im).astype(np.float32)
    # İki renge indir, sonra kumaş gölgesi ekle.
    beyaz = a[..., 1] > 130
    yy, xx = np.mgrid[0:gh, 0:gw]
    kivrim = 0.5 + 0.5 * np.sin(xx / gw * np.pi * 5 + 0.6)
    ton = 1.0 - 0.07 * (kivrim ** 2) * (yy / gh) - 0.10 * (yy / gh) ** 2
    ton = np.round(ton * 14) / 14
    c = np.where(beyaz[..., None], np.array([255, 255, 255.0]), np.array([227, 10, 23.0])) * ton[..., None]
    golge = np.zeros((gh, gw), dtype=bool)
    golge[1:, 1:] = beyaz[:-1, :-1] & ~beyaz[1:, 1:]
    c[golge] *= 0.78
    kenar = (xx < 2) | (xx >= gw - 2) | (yy >= gh - 2)
    c[kenar & ~beyaz] *= 0.72
    return Image.fromarray(np.clip(c, 0, 255).astype(np.uint8))


def pelerin_dokusu(sanat):
    """Sırt desenini vanilla pelerin + elytra açılımına yerleştirir."""
    s = PELERIN_OLCEK
    doku = Image.new('RGBA', (64 * s, 32 * s), (0, 0, 0, 0))
    a = np.asarray(sanat).astype(np.float32)
    doku.paste(sanat, (1 * s, 1 * s))
    ic = Image.fromarray(np.clip(a[:, ::-1] * 0.45 + a.mean((0, 1)) * 0.25, 0, 255).astype(np.uint8))
    doku.paste(ic, (12 * s, 1 * s))
    doku.paste(Image.fromarray(np.repeat(a[:1], s, 0).astype(np.uint8)), (1 * s, 0))
    doku.paste(Image.fromarray(np.repeat(a[-1:], s, 0).astype(np.uint8)), (11 * s, 0))
    doku.paste(Image.fromarray(np.repeat(a[:, :1], s, 1).astype(np.uint8)), (0, 1 * s))
    doku.paste(Image.fromarray(np.repeat(a[:, -1:], s, 1).astype(np.uint8)), (11 * s, 1 * s))
    # Elytra: kanat kutusu (22,0) 10x20x2 açılımı. Pelerin takılıyken elytra bu dokudan çizilir.
    ort = tuple(int(v) for v in a.mean((0, 1)))
    doku.paste(Image.new('RGB', (24 * s, 22 * s), ort), (22 * s, 0))
    gw, gh = sanat.size
    kanat = sanat.resize((gw * 20 * s // gh if gw * 20 * s // gh >= 10 * s else 10 * s, 20 * s), Image.NEAREST)
    kx = (kanat.size[0] - 10 * s) // 2
    kanat = kanat.crop((kx, 0, kx + 10 * s, 20 * s))
    doku.paste(kanat, (24 * s, 2 * s))
    doku.paste(kanat.transpose(Image.FLIP_LEFT_RIGHT), (36 * s, 2 * s))
    return doku


def disli_dokusu(n=40, dis=10):
    ss = 8
    N = n * ss
    yy, xx = np.mgrid[0:N, 0:N]
    x, y = (xx + 0.5) / ss - n / 2, (yy + 0.5) / ss - n / 2
    r, th = np.hypot(x, y), np.arctan2(y, x)
    dis_r = np.where(np.cos(th * dis) > 0.15, n * 0.48, n * 0.38)
    govde = (r <= dis_r) & (r > n * 0.1)
    kol = (np.abs(np.sin(th * 2.5)) > 0.38) & (r > n * 0.17) & (r < n * 0.29)
    govde &= ~kol
    c = np.zeros((N, N, 4), dtype=np.float32)
    pirinc, koyu, acik = np.array([201, 150, 54.0]), np.array([112, 72, 22.0]), np.array([255, 226, 140.0])
    t = np.clip((x + y) / n + 0.5, 0, 1)[..., None]
    c[..., :3] = acik * (1 - t) + pirinc * t
    kenar = (r > dis_r - n * 0.035) | (np.abs(r - n * 0.31) < n * 0.02) | (r < n * 0.135)
    c[kenar, :3] = koyu
    c[..., 3] = govde * 255
    im = Image.fromarray(c.astype(np.uint8)).resize((n, n), Image.BOX)
    a = np.asarray(im).copy()
    a[..., 3] = np.where(a[..., 3] > 110, 255, 0)
    rgb = Image.fromarray(a[..., :3]).quantize(10, dither=Image.Dither.NONE).convert('RGB')
    a[..., :3] = np.asarray(rgb)
    return Image.fromarray(a)


AURA_IKON = {
    'kalp': ('ff4f6d', ['..##..##..', '.########.', '##########', '##########', '.########.', '..######..', '...####...', '....##....']),
    'alev': ('ff7a00', ['....#.....', '...##.....', '...###....', '..####.#..', '.######.#.', '.#######..', '.#######..', '..#####...']),
    'buyu': ('b28dff', ['.###..#...', '.#.#..#.#.', '.###..###.', '......#.#.', '.#.#......', '.###.####.', '...#.#....', '...#.####.']),
    'nota': ('4fc3ff', ['...######.', '...######.', '...#....#.', '...#....#.', '...#....#.', '.###..###.', '####.####.', '.##...##..']),
    'kiraz': ('ffb7d5', ['...##.....', '..####....', '.######...', '.#######..', '..######..', '...#####..', '....###...', '.....#....']),
    'ruh': ('40e0d0', ['....#.....', '...###....', '..#####...', '.###.###..', '.##...##..', '.###.###..', '..#####...', '...###....']),
    'yildiz': ('ffffff', ['....##....', '....##....', '.########.', '..######..', '...####...', '..######..', '..##..##..', '.#......#.']),
    'kar': ('e8ffff', ['.#..##..#.', '..#.##.#..', '...####...', '##########', '##########', '...####...', '..#.##.#..', '.#..##..#.']),
    'hapi': ('2bd35a', ['..##..##..', '.####.###.', '.########.', '..######..', '.########.', '.###.####.', '..##.###..', '....#.....']),
    'simsek': ('fff066', ['.....###..', '....###...', '...###....', '..######..', '....###...', '...###....', '..###.....', '..#.......']),
}


def aura_ikonu(bicim):
    c, satirlar = AURA_IKON[bicim]
    r, g, b = [int(c[i:i + 2], 16) for i in (0, 2, 4)]
    im = Image.new('RGBA', (64, 64))
    d = ImageDraw.Draw(im)
    rnd = np.random.RandomState(len(bicim) * 7 + ord(bicim[0]))
    # Üç boyda aynı simge: büyük ortada, iki küçük çevrede (parçacık hissi).
    for (ox, oy, s, alfa) in ((12, 16, 4, 255), (2, 4, 2, 190), (42, 2, 2, 150)):
        for j, satir in enumerate(satirlar):
            for i, ch in enumerate(satir):
                if ch == '#':
                    koyu = 0.72 if (j >= len(satirlar) - 3 or i >= 7) else 1.0
                    d.rectangle((ox + i * s, oy + j * s, ox + i * s + s - 1, oy + j * s + s - 1),
                                fill=(int(r * koyu), int(g * koyu), int(b * koyu), alfa))
    for _ in range(5):
        x, y = rnd.randint(4, 60), rnd.randint(50, 62)
        d.rectangle((x, y, x + 1, y + 1), fill=(r, g, b, 140))
    return im


def sigdir(im, boyut=64, bosluk=3):
    im = im.convert('RGBA')
    kutu = im.getbbox()
    if kutu:
        im = im.crop(kutu)
    o = (boyut - 2 * bosluk) / max(im.size)
    yeni = im.resize((max(1, round(im.size[0] * o)), max(1, round(im.size[1] * o))), Image.NEAREST if o >= 1 else Image.BOX)
    tuval = Image.new('RGBA', (boyut, boyut))
    tuval.alpha_composite(yeni, ((boyut - yeni.size[0]) // 2, (boyut - yeni.size[1]) // 2))
    return tuval


# ------------------------------------------------------------------ katalog

def parlak(alt_ton, ust_ton, s=0.45, v=0.75):
    return lambda H, S, V: (H >= alt_ton) & (H <= ust_ton) & (S >= s) & (V >= v)


# hareket: tuy (kuş çırpışı) | zar (yarasa/ejderha) | kelebek | bocek (hızlı titreşim) | mekanik (kademeli) | kristal
KANATLAR = [
    dict(kimlik='k_melek', ad='Melek Kanadı', doku='melek', en=19, hareket='tuy', nadirlik='nadir',
         aciklama='Kat kat bembeyaz tüyler, uçlarında altın ışıltı.'),
    dict(kimlik='k_seytan', ad='Şeytan Kanadı', doku='seytan', en=20, hareket='zar', nadirlik='destansi',
         aciklama='Kor damarlı deri zar ve keskin pençeler.', isik=parlak(0.0, 0.09, 0.6, 0.72)),
    dict(kimlik='k_ejderha', ad='Ejderha Kanadı', doku='ejderha', en=21, hareket='zar', nadirlik='destansi',
         aciklama='Zümrüt pullar, fildişi dikenler ve geniş zar.'),
    dict(kimlik='k_kelebek', ad='Kelebek Kanadı', doku='kelebek', en=16, hareket='kelebek', nadirlik='nadir',
         aciklama='Kral kelebeğinin turuncu-siyah desenli kanatları.'),
    dict(kimlik='k_peri', ad='Peri Kanadı', doku='peri', en=16, hareket='bocek', nadirlik='nadir', saydam=True,
         esik=45, sacak=40, aciklama='Cam gibi yanardöner, hızla titreşen ince kanatlar.'),
    dict(kimlik='k_hapi', ad='Hapi Kanadı', doku='hapi', en=19, hareket='tuy', nadirlik='nadir',
         aciklama='HapiCraft yeşili tüyler, beyaz uçlar ve zümrüt taş.'),
    dict(kimlik='k_anka', ad='Anka Kanadı', doku='anka', en=20, hareket='tuy', nadirlik='efsanevi',
         aciklama='Kızıldan sarıya yanan, alev uçlu tüyler. Karanlıkta parlar.', isik=parlak(0.07, 0.19, 0.5, 0.9)),
    dict(kimlik='k_gece', ad='Gece Kanadı', doku='gece', en=19, hareket='tuy', nadirlik='destansi',
         aciklama='Lacivert tüylere işlenmiş yıldızlar ve gümüş hilal.',
         isik=lambda H, S, V: (V >= 0.86) & ((S <= 0.4) | ((H > 0.68) & (H < 0.88)))),
    dict(kimlik='k_mekanik', ad='Mekanik Kanat', doku='mekanik', en=20, hareket='mekanik', nadirlik='efsanevi',
         aciklama='Pirinç iskelet, çelik bıçak tüyler ve dönen dişliler.', isik=parlak(0.45, 0.56, 0.45, 0.8)),
    dict(kimlik='k_buz', ad='Buz Kanadı', doku='buz', en=19, hareket='kristal', nadirlik='destansi',
         aciklama='Keskin buz kristallerinden yontulmuş kanatlar.'),
]

PELERINLER = [
    ('p_hapi', 'HapiCraft', 'hapi', 'yaygin', 'Yeşil zemin üstünde beyaz yonca.'),
    ('p_bayrak', 'Ay Yıldız', 'bayrak', 'yaygin', 'Al zemin üstünde ay yıldız.'),
    ('p_gece', 'Gece Göğü', 'gece', 'nadir', 'Hilal, yıldızlar ve dağ silüeti.'),
    ('p_alev', 'Alev', 'alev', 'nadir', 'Etekten yükselen alev dilleri.'),
    ('p_buz', 'Buz', 'buz', 'nadir', 'Dev kar tanesi ve kırağı.'),
    ('p_altin', 'Altın İşleme', 'altin', 'destansi', 'Bordo kadife üstüne altın taç ve defne.'),
    ('p_gokkusagi', 'Gökkuşağı', 'gokkusagi', 'nadir', 'Yedi renk, bir bulut.'),
    ('p_kareli', 'Ekose', 'kareli', 'yaygin', 'Yeşil İskoç ekosesi.'),
    ('p_siyah', 'Kara Pelerin', 'siyah', 'destansi', 'Gümüş kuzgun amblemli kara kumaş.'),
    ('p_kalp', 'Kalpli', 'kalp', 'yaygin', 'Şeker pembesi, kalp dolu.'),
]

SAPKALAR = [
    ('s_silindir', 'Silindir Şapka', 'silindir', 'yaygin', 'Saten şeritli, altın tokalı.'),
    ('s_tac', 'Kral Tacı', 'tac', 'efsanevi', 'Sekiz burçlu altın taç; yakut, zümrüt, safir.'),
    ('s_fes', 'Fes', 'fes', 'yaygin', 'Püskülü yürüdükçe sallanır.'),
    ('s_parti', 'Parti Şapkası', 'parti', 'yaygin', 'Sarmal şeritli, ponponlu.'),
    ('s_hasir', 'Hasır Şapka', 'hasir', 'yaygin', 'Örgü hasır, kırmızı kurdele.'),
    ('s_boynuz', 'Boynuz', 'boynuz', 'nadir', 'Halkalı, kıvrık bir çift boynuz.'),
    ('s_kedi', 'Kedi Kulağı', 'kedi', 'nadir', 'Arada bir seğiren kulaklar.'),
    ('s_tavsan', 'Tavşan Kulağı', 'tavsan', 'nadir', 'Biri dik, biri kıvrık.'),
    ('s_asci', 'Aşçı Şapkası', 'asci', 'yaygin', 'Pileli, kabarık şef kepi.'),
    ('s_buyucu', 'Büyücü Şapkası', 'buyucu', 'destansi', 'Ucu kıvrık; çevresinde bir yıldız döner.'),
    ('s_kep', 'Hapi Kep', 'kep', 'yaygin', 'Önünde H harfi olan yeşil kep.'),
    ('s_cicek', 'Çiçek Tacı', 'cicek', 'nadir', 'Dokuz çiçekli sarmaşık taç.'),
    ('s_pervane', 'Pervaneli Bere', 'pervane', 'destansi', 'Tepesindeki pervane durmadan döner.'),
]

HALELER = [
    ('h_altin', 'Altın Hale', 'hale_altin', 'nadir', 'Parıltılar eşliğinde dönen altın halka.'),
    ('h_hapi', 'Defne Halesi', 'hale_hapi', 'nadir', 'Yeşil-beyaz halka ve defne yaprakları.'),
    ('h_ates', 'Ateş Halesi', 'hale_ates', 'destansi', 'Dilleri titreşen alev çemberi.'),
    ('h_gokkusagi', 'Gökkuşağı Halesi', 'hale_gokkusagi', 'destansi', 'Renkleri dönen halka.'),
    ('h_yildiz', 'Yıldız Çemberi', 'hale_yildiz', 'efsanevi', 'Başının çevresinde dönen beş yıldız.'),
]

# desen: yukselen | yorunge | yagan | sacilan | halka
AURALAR = [
    ('a_kalp', 'Kalpler', 'kalp', 'yukselen', 'yaygin', 'Başının üstünde süzülen kalpler.'),
    ('a_alev', 'Alev Çemberi', 'alev', 'halka', 'nadir', 'Ayaklarının dibinde dönen ateş.'),
    ('a_buyu', 'Büyü Harfleri', 'buyu', 'yorunge', 'nadir', 'Çevrende dolanan büyü harfleri.'),
    ('a_nota', 'Notalar', 'nota', 'sacilan', 'yaygin', 'Renk renk notalar.'),
    ('a_kiraz', 'Kiraz Yaprakları', 'kiraz', 'yagan', 'nadir', 'Üstüne yağan pembe yapraklar.'),
    ('a_ruh', 'Ruh Ateşi', 'ruh', 'yorunge', 'destansi', 'Çift sarmal mavi alev.'),
    ('a_yildiz', 'Yıldız Tozu', 'yildiz', 'yorunge', 'destansi', 'Ağır ağır dönen ışık tozu.'),
    ('a_kar', 'Kar Taneleri', 'kar', 'yagan', 'yaygin', 'Hep kar yağar.'),
    ('a_hapi', 'Hapi Işıltısı', 'hapi', 'yukselen', 'nadir', 'Yeşil ışıltılar.'),
    ('a_simsek', 'Kıvılcım', 'simsek', 'sacilan', 'nadir', 'Çıtırdayan elektrik.'),
]

# Mekanik kanattaki dönen dişliler: [u, v (doku üzerinde 0..1), yarıçap (model pikseli), hız (derece/tik)]
DISLILER = {'mekanik': [[0.07, 0.452, 1.4, 6.0], [0.539, 0.30, 1.55, -9.0]]}


def yaz(im, *yollar):
    for y in yollar:
        y.parent.mkdir(parents=True, exist_ok=True)
        im.save(y, optimize=True)


def main():
    onizle = pathlib.Path(sys.argv[sys.argv.index('--onizle') + 1]) if '--onizle' in sys.argv else None
    if CIKTI.exists():
        shutil.rmtree(CIKTI)
    doku, ikon, model = CIKTI / 'doku', CIKTI / 'doku/ikon', CIKTI / 'model'
    katalog, eksik = [], []

    for k in KANATLAR:
        if not (KOK / 'ham/kanat' / (k['doku'] + '.png')).exists():
            eksik.append('kanat/' + k['doku'])
            continue
        im, isik, kok = kanat_isle(k['doku'], k)
        yaz(im, doku / 'kanat' / (k['doku'] + '.png'))
        if isik:
            yaz(isik, doku / 'kanat' / (k['doku'] + '_isik.png'))
        birlesik = im.copy()
        if isik:
            birlesik.alpha_composite(isik)
        yaz(sigdir(birlesik), ikon / (k['kimlik'] + '.png'))
        w, h = im.size
        katalog.append(dict(kimlik=k['kimlik'], ad=k['ad'], yuva='kanat', nadirlik=k['nadirlik'], aciklama=k['aciklama'],
                            doku=k['doku'], en=k['en'], boy=round(k['en'] * h / w, 2), kok=round(kok, 3),
                            hareket=k['hareket'], isik=bool(isik), saydam=bool(k.get('saydam')),
                            disliler=DISLILER.get(k['doku'], [])))
    yaz(disli_dokusu(), doku / 'disli.png')

    for kimlik, ad, d, nadirlik, aciklama in PELERINLER:
        if d != 'bayrak' and not (KOK / 'ham/pelerin' / (d + '.png')).exists():
            eksik.append('pelerin/' + d)
            continue
        sanat = pelerin_sanati(d)
        yaz(pelerin_dokusu(sanat), doku / 'pelerin' / (d + '.png'))
        yaz(sigdir(sanat, bosluk=2), ikon / (kimlik + '.png'))
        katalog.append(dict(kimlik=kimlik, ad=ad, yuva='pelerin', nadirlik=nadirlik, aciklama=aciklama, doku=d))

    toplam_kup = {}
    for liste, yuva, kaynak in ((SAPKALAR, 'sapka', sapkalar.SAPKALAR), (HALELER, 'hale', sapkalar.HALELER)):
        for kimlik, ad, m, nadirlik, aciklama in liste:
            parcalar = kaynak[m]()
            model.mkdir(parents=True, exist_ok=True)
            (model / (m + '.json')).write_text(json.dumps({'parcalar': [p.disa_aktar() for p in parcalar]},
                                                          separators=(',', ':')))
            toplam_kup[m] = sum(int(p.yuzey().sum()) for p in parcalar)
            yaz(voksel.ikon_ciz(parcalar, 64, yaw=-32, pitch=24 if yuva == 'sapka' else 34), ikon / (kimlik + '.png'))
            katalog.append(dict(kimlik=kimlik, ad=ad, yuva=yuva, nadirlik=nadirlik, aciklama=aciklama, model=m))

    for kimlik, ad, bicim, desen, nadirlik, aciklama in AURALAR:
        yaz(aura_ikonu(bicim), ikon / (kimlik + '.png'))
        katalog.append(dict(kimlik=kimlik, ad=ad, yuva='aura', nadirlik=nadirlik, aciklama=aciklama,
                            parcacik=bicim, desen=desen))

    (CIKTI / 'katalog.json').write_text(json.dumps({'surum': 2, 'kozmetikler': katalog}, ensure_ascii=False, indent=1))

    # Dağıtım: mod kaynakları.
    hedefler = [(MOD / 'textures/kozmetik', MOD / 'kozmetik')]
    for doku_hedef, veri_hedef in hedefler:
        for d in (doku_hedef, veri_hedef / 'model'):
            if d.exists():
                shutil.rmtree(d)
        shutil.copytree(doku, doku_hedef)
        shutil.copytree(model, veri_hedef / 'model')
        shutil.copy(CIKTI / 'katalog.json', veri_hedef / 'katalog.json')

    print('%d kozmetik yazıldı. En büyük model: %s' % (len(katalog), max(toplam_kup.items(), key=lambda t: t[1])))
    if eksik:
        print('EKSİK ham görsel (kataloğa girmedi):', ', '.join(eksik))

    if onizle:
        onizle.mkdir(parents=True, exist_ok=True)
        for yuva in ('kanat', 'pelerin', 'sapka', 'hale', 'aura'):
            liste = [k for k in katalog if k['yuva'] == yuva]
            B = 256 if yuva in ('kanat', 'pelerin') else 128
            sayfa = Image.new('RGBA', (B * min(5, len(liste)), B * ((len(liste) + 4) // 5)), (38, 42, 50, 255))
            for n, k in enumerate(liste):
                if yuva == 'kanat':
                    im = Image.open(doku / 'kanat' / (k['doku'] + '.png')).convert('RGBA')
                    if k['isik']:
                        im.alpha_composite(Image.open(doku / 'kanat' / (k['doku'] + '_isik.png')).convert('RGBA'))
                    im = sigdir(im, B, 4)
                elif yuva == 'pelerin':
                    s = PELERIN_OLCEK
                    im = sigdir(Image.open(doku / 'pelerin' / (k['doku'] + '.png')).crop((s, s, 11 * s, 17 * s)), B, 4)
                else:
                    im = Image.open(ikon / (k['kimlik'] + '.png')).resize((B, B), Image.NEAREST)
                sayfa.alpha_composite(im, ((n % 5) * B, (n // 5) * B))
            sayfa.convert('RGB').save(onizle / (yuva + '.png'))


if __name__ == '__main__':
    main()
