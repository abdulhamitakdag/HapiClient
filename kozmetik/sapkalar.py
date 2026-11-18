"""Şapka ve hale modelleri. Her işlev Parca listesi döndürür. Birimler: voksel.py başlığına bak."""
import colorsys

import numpy as np

from voksel import Parca, renk, karistir

PI = np.pi


def kutup(p, cx=0.0, cz=0.0):
    return np.hypot(p.X - cx, p.Z - cz), np.arctan2(p.Z - cz, p.X - cx)


def parlama(th, yon, guc=8):
    """th açısında, yon yönüne bakan yüzeyde saten/metal parlaması (0..1)."""
    return np.maximum(0, np.cos(th - yon)) ** guc


def kure(p, cx, cy, cz, r):
    return (p.X - cx) ** 2 + (p.Y - cy) ** 2 + (p.Z - cz) ** 2 <= r * r


def elipsoid(p, cx, cy, cz, rx, ry, rz):
    return ((p.X - cx) / rx) ** 2 + ((p.Y - cy) / ry) ** 2 + ((p.Z - cz) / rz) ** 2 <= 1


def egri_mesafe(p, noktalar):
    """Her küpün, verilen eğri noktalarına en kısa uzaklığı ve o noktanın sırası (0..1)."""
    en = np.full(p.X.shape, 1e9, dtype=np.float32)
    sira = np.zeros(p.X.shape, dtype=np.float32)
    n = len(noktalar)
    for i, (x, y, z) in enumerate(noktalar):
        d = np.sqrt((p.X - x) ** 2 + (p.Y - y) ** 2 + (p.Z - z) ** 2)
        yakin = d < en
        en[yakin] = d[yakin]
        sira[yakin] = i / max(1, n - 1)
    return en, sira


def bant(p, y0=0.3, y1=0.9, kalin=0.55):
    """Kulaklı şapkalar için başın üstünden iki yana inen saç bandı maskesi."""
    ust = (np.abs(p.Z) < kalin) & (p.Y >= y0) & (p.Y < y1) & (np.abs(p.X) < 5.2)
    yan = (np.abs(p.Z) < kalin) & (np.abs(p.X) >= 4.6) & (np.abs(p.X) < 5.2) & (p.Y >= -3.5) & (p.Y < y1)
    return ust | yan


# ---------------------------------------------------------------- şapkalar

def silindir():
    p = Parca('silindir', (-7.5, 7.5, 0, 10, -7.5, 7.5))
    R, th = kutup(p)
    siyah, acik = renk('1b1b22'), renk('4a4a5c')
    kenar = (R <= 7.2) & (p.Y >= 0.35 + 0.004 * R ** 2.6) & (p.Y < 1.1 + 0.004 * R ** 2.6)
    govde = (R <= 4.35 + 0.06 * p.Y) & (p.Y >= 0.9) & (p.Y < 9.4)
    isilti = parlama(th, -2.2) * 0.9 + parlama(th, 0.9, 14) * 0.35
    p.boya(kenar, karistir(siyah, acik, 0.25 * (R / 7.2) ** 3), 0.05)
    p.boya(govde, karistir(siyah, acik, isilti), 0.04)
    p.boya(govde & (p.Y > 9.05), karistir(siyah, acik, 0.18 + 0.2 * (R < 3.6)))
    serit = (R <= 4.75 + 0.06 * p.Y) & (R > 4.0) & (p.Y >= 1.2) & (p.Y < 3.3)
    yesil, yesil2 = renk('1fae49'), renk('7df0a0')
    p.boya(serit, karistir(yesil, yesil2, isilti * 0.9 + 0.25 * ((p.Y > 2.0) & (p.Y < 2.4))))
    altin, altin2 = renk('d9a520'), renk('fff0a0')
    toka = (np.abs(p.X) < 1.5) & (p.Y >= 1.05) & (p.Y < 3.45) & (p.Z < -4.2) & (p.Z > -5.35)
    delik = (np.abs(p.X) < 0.8) & (p.Y >= 1.7) & (p.Y < 2.8)
    p.boya(toka & ~delik, karistir(altin, altin2, (p.X + p.Y * 0.6) % 1.4 < 0.5))
    return [p]


def tac():
    p = Parca('tac', (-6, 6, 0, 8.4, -6, 6))
    R, th = kutup(p)
    altin, koyu, acik = renk('e8b422'), renk('9a6a10'), renk('fff4b0')
    dilim = (th / (2 * PI) * 8) % 1.0
    sivri = (1 - 2 * np.abs(dilim - 0.5)) ** 1.25
    ust = 3.0 + 3.6 * sivri
    cember = (R <= 5.0) & (R > 4.25) & (p.Y >= 0.9) & (p.Y < ust)
    p.boya(cember, karistir(karistir(koyu, altin, 0.55 + 0.45 * np.sin(th * 8 + 1.2)), acik, parlama(th, -2.0, 6) * 0.8), 0.04)
    p.boya(cember & (p.Y > 2.7) & (p.Y < 3.1), koyu)
    kadife = elipsoid(p, 0, 1.0, 0, 4.3, 4.2, 4.3) & (p.Y >= 1.0)
    p.boya(kadife & ~cember, karistir(renk('7a0f2b'), renk('c2284f'), np.clip(p.Y / 5, 0, 1) * 0.7 + 0.3 * np.sin(th * 6) ** 2), 0.05)
    kurk = (R <= 5.45) & (R > 4.1) & (p.Y >= 0.3) & (p.Y < 1.25)
    benek = (np.sin(th * 11) > 0.93) & (p.Y > 0.5) & (p.Y < 1.05) & (R > 5.0)
    p.boya(kurk, renk('f6f3ea'), 0.03)
    p.boya(kurk & benek, renk('1a1a1a'))
    taslar = [renk('e0115f'), renk('1fc46a'), renk('2f7bff'), renk('b44cff')]
    for i in range(8):
        a = (i + 0.5) * PI / 4
        cx, cz = np.cos(a) * 4.95, np.sin(a) * 4.95
        tas = kure(p, cx, 2.0, cz, 0.72)
        c = taslar[i % 4]
        p.boya(tas, karistir(c, renk('ffffff'), ((p.Y > 2.15) & (np.hypot(p.X - cx * 1.06, p.Z - cz * 1.06) < 0.4)) * 0.75))
        p.boya(kure(p, np.cos(a) * 4.6, 6.75, np.sin(a) * 4.6, 0.62), karistir(renk('fffaf0'), renk('c9d6e8'), p.Y < 6.7))
    return [p]


def fes():
    p = Parca('fes', (-5, 5, 0, 8.6, -5, 5))
    R, th = kutup(p)
    kirmizi, koyu, acik = renk('b3001b'), renk('74000f'), renk('e23a4e')
    govde = (R <= 4.45 - 0.13 * p.Y) & (p.Y >= 0.3) & (p.Y < 7.6)
    p.boya(govde, karistir(karistir(kirmizi, koyu, 0.35 * (1 - p.Y / 7.6)), acik, parlama(th, -2.3, 5) * 0.6), 0.05)
    p.boya(govde & (p.Y < 0.75), koyu)
    p.boya(govde & (p.Y > 7.25), karistir(kirmizi, koyu, R / 3.6 * 0.5))
    p.boya((R < 0.7) & (p.Y >= 7.6) & (p.Y < 8.2), renk('111114'))

    s = Parca('fes_puskul', (-1, 6.4, 1.2, 9.4, -1.2, 1.2), anim={'sallan': [2, 7, 0.11, 0]}, eksen=(0, 8.0, 0))
    ip = (np.abs(s.Z) < 0.34) & (s.Y >= 7.9) & (s.Y < 8.5) & (s.X >= 0) & (s.X < 5.0)
    sark = (np.hypot(s.X - 4.9, s.Z) < 0.38) & (s.Y >= 4.6) & (s.Y < 8.5)
    Rp = np.hypot(s.X - 4.9, s.Z)
    puskul = (Rp < 0.55 + 0.16 * (4.9 - s.Y)) & (s.Y >= 1.5) & (s.Y < 4.7)
    siyah = renk('15151a')
    s.boya(ip | sark, siyah)
    s.boya(puskul, karistir(siyah, renk('3d3d4a'), (np.floor(np.arctan2(s.Z, s.X - 4.9) * 4) % 2)), 0.05)
    s.boya((Rp < 0.75) & (s.Y >= 4.3) & (s.Y < 4.9), renk('d9a520'))
    return [p, s]


def parti():
    p = Parca('parti', (-4.6, 4.6, 0, 11.6, -4.6, 4.6))
    R, th = kutup(p)
    koni = (R <= 3.7 * (1 - p.Y / 10.2)) & (p.Y >= 0.3) & (p.Y < 9.8)
    serit = np.floor((th / (2 * PI) * 4 + p.Y / 2.6) % 4)
    renkler = [renk('ff4fa3'), renk('ffd84a'), renk('4fc3ff'), renk('8a5cff')]
    c = np.zeros(p.rgb.shape, dtype=np.float32)
    for i, r in enumerate(renkler):
        c[serit == i] = r
    p.boya(koni, karistir(c, renk('ffffff'), parlama(th, -2.2, 6) * 0.45), 0.03)
    nokta = (np.sin(th * 9 + p.Y * 3.1) > 0.9) & (np.sin(p.Y * 4.3) > 0.5)
    p.boya(koni & nokta, renk('ffffff'))
    firfir = (np.hypot(R - 3.75, p.Y - 0.6) < 0.62 + 0.18 * np.sin(th * 12))
    p.boya(firfir, karistir(renk('ffffff'), renk('ffd0e6'), np.sin(th * 12) < 0))
    ponpon = kure(p, 0, 10.3, 0, 1.25)
    p.boya(ponpon, karistir(renk('ffffff'), renk('ffe36b'), p.gurultu > 0.55), 0.05)
    return [p]


def hasir():
    p = Parca('hasir', (-8.5, 8.5, -0.6, 5.4, -8.5, 8.5))
    R, th = kutup(p)
    acik, orta, koyu = renk('ecd088'), renk('cfa850'), renk('9c7630')
    egim = -0.012 * np.maximum(R - 4, 0) ** 2
    kenar = (R <= 8.1) & (p.Y >= 0.35 + egim) & (p.Y < 1.05 + egim)
    orgu = (np.floor(R * 1.6) + np.floor(th * 18 / PI)) % 2
    p.boya(kenar, karistir(acik, orta, orgu), 0.05)
    p.boya(kenar & (R > 7.6), koyu)
    tepe = (R <= 4.75) & (p.Y >= 0.8) & (p.Y < 4.7) & (((R / 4.75) ** 6 + (np.maximum(p.Y - 2.2, 0) / 2.5) ** 6) <= 1)
    orgu2 = (np.floor(p.Y * 1.7) + np.floor(th * 16 / PI)) % 2
    p.boya(tepe, karistir(acik, orta, orgu2), 0.05)
    serit = (R <= 5.05) & (R > 4.2) & (p.Y >= 1.05) & (p.Y < 2.35)
    p.boya(serit, karistir(renk('c0392b'), renk('ee6b5b'), parlama(th, -2.2, 5) * 0.7 + 0.2 * ((p.Y > 1.55) & (p.Y < 1.85))))
    # Arkada fiyonk.
    for s in (-1, 1):
        f = elipsoid(p, s * 1.3, 1.7, 5.2, 1.35, 0.95, 0.6)
        p.boya(f, karistir(renk('a52a1e'), renk('ee6b5b'), np.abs(p.X) / 2.4))
    p.boya(elipsoid(p, 0, 1.7, 5.3, 0.5, 0.6, 0.6), renk('8c2116'))
    return [p]


def boynuz():
    parcalar = []
    for s, ad in ((-1, 'boynuz_sag'), (1, 'boynuz_sol')):
        p = Parca(ad, (s * 5.2 - 3.4, s * 5.2 + 3.4, -0.4, 9.2, -2.6, 2.6))
        t = np.linspace(0, 1, 60)
        egri = [(s * (2.9 + 3.3 * np.sin(u * 1.75) - 1.5 * u ** 3), 0.1 + 8.4 * u ** 0.95, 0.4 - 1.5 * u ** 2) for u in t]
        d, sira = egri_mesafe(p, egri)
        govde = d <= 1.45 * (1 - sira) ** 0.75 + 0.22
        koyu, kizil, uc = renk('3a0606'), renk('b5121b'), renk('120305')
        halka = (np.sin(sira * 46) > 0.45)
        c = karistir(karistir(kizil, uc, sira ** 1.3), koyu, halka * 0.55)
        c = karistir(c, renk('e8503f'), ((p.Z < -0.35) & (np.abs(sira - 0.3) < 0.2) & ~halka) * 0.55)
        p.boya(govde, c, 0.04)
        taban = elipsoid(p, s * 2.9, 0.35, 0.4, 1.9, 0.5, 1.9)
        p.boya(taban, renk('1c0a0a'))
        parcalar.append(p)
    return parcalar


def _kulak_bandi(ad, c1, c2):
    p = Parca(ad, (-5.4, 5.4, -3.6, 1.0, -0.8, 0.8))
    p.boya(bant(p), karistir(c1, c2, (p.Y > 0.6) | (np.abs(p.X) > 4.95)), 0.03)
    return p


def kedi():
    kurk, kurk2, pembe = renk('2b2b30'), renk('55555f'), renk('ff9ec4')
    parcalar = [_kulak_bandi('kedi_bant', renk('1c1c20'), renk('3a3a44'))]
    for s, ad, faz in ((-1, 'kedi_sag', 0.0), (1, 'kedi_sol', 1.7)):
        cx = s * 2.9
        p = Parca(ad, (cx - 2.6, cx + 2.6, 0.2, 5.2, -1.2, 0.9),
                  anim={'sallan': [2, 5, 0.07, faz]}, eksen=(cx, 0.6, 0))
        merkez = cx + s * 0.22 * (p.Y - 0.6)
        yari = 2.05 * (1 - (p.Y - 0.5) / 4.3)
        kalin = 0.75 - 0.1 * (p.Y - 0.5)
        kulak = (np.abs(p.X - merkez) <= yari) & (p.Y >= 0.5) & (p.Y < 4.8) & (p.Z > -kalin) & (p.Z < kalin * 0.8)
        p.boya(kulak, karistir(kurk, kurk2, (p.Y > 3.9) | (np.abs(p.X - merkez) > yari - 0.4)), 0.06)
        ic = (np.abs(p.X - merkez) <= yari * 0.62 - 0.1) & (p.Y >= 0.9) & (p.Y < 3.7) & (p.Z < -kalin + 0.4)
        p.boya(kulak & ic, karistir(pembe, renk('ffd0e2'), (p.Y - 0.9) / 3))
        parcalar.append(p)
    return parcalar


def tavsan():
    beyaz, gri, pembe = renk('fbfbfd'), renk('d4d8e4'), renk('ffa6c9')
    parcalar = [_kulak_bandi('tavsan_bant', renk('e9ebf2'), renk('ffffff'))]
    for s, ad, faz, kivrik in ((-1, 'tavsan_sag', 0.0, False), (1, 'tavsan_sol', 2.1, True)):
        cx = s * 2.3
        p = Parca(ad, (cx - 2.2, cx + 2.2, 0.2, 11.2, -4.4, 1.0),
                  anim={'sallan': [2, 4, 0.06, faz]}, eksen=(cx, 0.6, 0))
        # Sol kulağın üst yarısı öne kıvrık.
        yy = p.Y
        zoff = np.zeros_like(yy)
        if kivrik:
            zoff = -np.maximum(yy - 6.2, 0) * 0.95
            yy = np.where(yy > 6.2, 6.2 + (yy - 6.2) * 1.25, yy)
        merkez = cx + s * 0.07 * (yy - 0.6)
        yari = 1.45 * np.sqrt(np.clip(1 - ((yy - 5.4) / 5.0) ** 2, 0, 1))
        z = p.Z - zoff
        kulak = (np.abs(p.X - merkez) <= yari) & (p.Y >= 0.5) & (z > -0.55) & (z < 0.5) & (yy < 10.4)
        p.boya(kulak, karistir(beyaz, gri, (np.abs(p.X - merkez) > yari - 0.35)), 0.03)
        ic = (np.abs(p.X - merkez) <= yari * 0.55) & (yy >= 1.3) & (yy < 9.3) & (z < -0.2)
        p.boya(kulak & ic, karistir(pembe, renk('ffd3e4'), np.abs(p.X - merkez) / 0.9))
        parcalar.append(p)
    return parcalar


def asci():
    p = Parca('asci', (-7, 7, 0, 10.4, -7, 7))
    R, th = kutup(p)
    beyaz, golge = renk('ffffff'), renk('c9d0de')
    alt = (R <= 4.7) & (p.Y >= 0.3) & (p.Y < 4.6)
    pile = 0.5 + 0.5 * np.cos(th * 16)
    p.boya(alt, karistir(golge, beyaz, pile * 0.8 + 0.2), 0.02)
    p.boya(alt & (p.Y < 1.0), karistir(golge, beyaz, 0.6))
    ust = elipsoid(p, 0, 6.6, 0, 5.4, 3.0, 5.4)
    yakin = np.full(p.X.shape, 9.0, dtype=np.float32)
    for i in range(7):
        a = i * 2 * PI / 7 + 0.3
        cx, cz, cy, r = np.cos(a) * 4.0, np.sin(a) * 4.0, 6.5 + 0.5 * np.sin(i * 2.4), 2.55
        ust |= kure(p, cx, cy, cz, r)
        yakin = np.minimum(yakin, np.sqrt((p.X - cx) ** 2 + (p.Y - cy) ** 2 + (p.Z - cz) ** 2) / r)
    ust |= kure(p, 0, 7.7, 0, 3.3)
    yakin = np.minimum(yakin, np.sqrt(p.X ** 2 + (p.Y - 7.7) ** 2 + p.Z ** 2) / 3.3)
    p.boya(ust & (p.Y >= 4.2), karistir(golge, beyaz, np.clip((yakin - 0.55) * 2.6, 0, 1) * 0.75 + 0.25 * (p.Y > 6.5)), 0.02)
    return [p]


def buyucu():
    p = Parca('buyucu', (-8, 8, -0.4, 14.5, -8, 9))
    R, th = kutup(p)
    mor, koyu, acik = renk('5b2a86'), renk('35154f'), renk('8a55bd')
    dalga = 0.55 * np.sin(th * 3 + 0.5) * (R / 7.6) ** 2
    kenar = (R <= 7.6) & (p.Y >= 0.35 + dalga) & (p.Y < 1.15 + dalga)
    p.boya(kenar, karistir(mor, koyu, (R > 7.0) * 0.8 + 0.25 * (np.sin(th * 3 + 0.5) < 0)), 0.05)
    # Ucu arkaya kıvrılan koni.
    cz = 0.028 * np.maximum(p.Y, 0) ** 2.05
    cx = 0.6 * np.sin(p.Y * 0.35) * (p.Y / 13)
    Rk = np.hypot(p.X - cx, p.Z - cz)
    thk = np.arctan2(p.Z - cz, p.X - cx)
    yari = 4.55 * np.clip(1 - p.Y / 13.6, 0, 1) ** 0.8 + 0.22 * np.sin(p.Y * 2.3 + thk)
    koni = (Rk <= yari) & (p.Y >= 0.9) & (p.Y < 13.4)
    kat = 0.5 + 0.5 * np.sin(p.Y * 2.3 + thk)
    p.boya(koni, karistir(karistir(koyu, mor, kat), acik, parlama(thk, -2.2, 5) * 0.55), 0.05)
    yildiz = (np.sin(thk * 5.0 + p.Y * 1.7) > 0.965) & (np.sin(p.Y * 3.9 + thk * 2) > 0.3) & (Rk > yari - 0.5)
    p.boya(koni & yildiz & (p.Y > 3.2), renk('ffe066'))
    serit = (Rk <= yari + 0.35) & (Rk > yari - 0.6) & (p.Y >= 1.15) & (p.Y < 2.75)
    p.boya(serit, karistir(renk('c8961a'), renk('ffe680'), parlama(thk, -2.2, 5)))
    toka = (np.abs(p.X) < 1.45) & (p.Y >= 0.95) & (p.Y < 3.0) & (p.Z < -3.9) & (p.Z > -5.2)
    delik = (np.abs(p.X) < 0.75) & (p.Y >= 1.5) & (p.Y < 2.45)
    p.boya(toka & ~delik, renk('fff0a0'))

    y = Parca('buyucu_yildiz', (-1.9, 1.9, 7.4, 11.2, -0.5, 0.5), isik=True,
              anim={'yorunge': [6.4, 3.2, 0], 'sek': [0.7, 0.09, 0], 'don': [0, 6, 0]}, eksen=(0, 9.3, 0))
    y.boya(_yildiz_maske(y, 0, 9.3, 1.75, 0.72), karistir(renk('ffe066'), renk('ffffff'), np.hypot(y.X, y.Y - 9.3) < 0.7))
    return [p, y]


def _yildiz_maske(p, cx, cy, dis, ic):
    """XY düzleminde beş köşeli yıldız."""
    a = np.arctan2(p.X - cx, p.Y - cy)
    r = np.hypot(p.X - cx, p.Y - cy)
    dilim = PI / 5
    yerel = np.abs(((a + dilim) % (2 * dilim)) - dilim)
    # Köşe (dis, 0) ile girinti (ic*cos, ic*sin) arasındaki doğrunun kutupsal sınırı.
    x1, y1, x2, y2 = dis, 0.0, ic * np.cos(dilim), ic * np.sin(dilim)
    payda = (y2 - y1) * np.cos(yerel) - (x2 - x1) * np.sin(yerel)
    sinir = (x1 * (y2 - y1) - y1 * (x2 - x1)) / payda
    return r <= sinir


def kep():
    p = Parca('kep', (-5.4, 5.4, -0.6, 5.2, -9.2, 5.4))
    R, th = kutup(p)
    yesil, koyu, acik, beyaz = renk('2bd35a'), renk('168a38'), renk('7df0a0'), renk('f7fbf8')
    kubbe = elipsoid(p, 0, 0.3, 0, 4.95, 4.3, 4.95) & (p.Y >= 0.3)
    dikis = np.abs(np.sin(th * 3)) < 0.1
    p.boya(kubbe, karistir(karistir(yesil, koyu, dikis * 0.8), acik, parlama(th, -2.3, 5) * 0.4 * (p.Y > 1.5)), 0.04)
    on = kubbe & (p.Z < -2.6) & (np.abs(p.X) < 3.1) & (p.Y > 0.7) & (p.Y < 3.9)
    p.boya(on, beyaz, 0.02)
    # Ön panelde yeşil H.
    hx, hy = np.abs(p.X), p.Y
    harf = (((hx > 0.75) & (hx < 1.45)) | ((hx <= 0.75) & (hy > 2.05) & (hy < 2.65))) & (hy > 1.2) & (hy < 3.5)
    p.boya(on & harf & (p.Z < -3.2), koyu)
    p.boya(kure(p, 0, 4.55, 0, 0.6), koyu)
    p.boya(kubbe & (p.Y < 0.75), koyu)
    egim = -0.035 * p.X ** 2
    yari = 4.75 * np.sqrt(np.clip(1 - (np.clip(-p.Z - 3.9, 0, None) / 5.1) ** 2.4, 0, 1))
    siper = (p.Z < -3.9) & (p.Z > -8.9) & (np.abs(p.X) <= yari) & (p.Y >= 0.3 + egim) & (p.Y < 0.95 + egim)
    kenar_cizgi = siper & (np.abs(p.X) > yari - 0.4) & (p.Z < -6.2)
    p.boya(siper & ~kubbe, karistir(yesil, koyu, (p.Y < 0.6 + egim) * 0.7), 0.04)
    p.boya(kenar_cizgi & ~kubbe, beyaz)
    return [p]


def cicek():
    p = Parca('cicek', (-6.6, 6.6, -0.2, 3.0, -6.6, 6.6))
    R, th = kutup(p)
    dal = np.hypot(R - 4.95, p.Y - 0.85) < 0.5 + 0.12 * np.sin(th * 9)
    p.boya(dal, karistir(renk('2e7d32'), renk('6d4c2a'), np.sin(th * 9 + p.Y * 4) > 0.2), 0.05)
    for i in range(14):
        a = i * 2 * PI / 14 + 0.2
        cx, cz = np.cos(a) * 5.35, np.sin(a) * 5.35
        # Dala teğet uzanan yaprak.
        u = (p.X - cx) * -np.sin(a) + (p.Z - cz) * np.cos(a)
        v = (p.X - cx) * np.cos(a) + (p.Z - cz) * np.sin(a)
        yaprak = (u / 1.25) ** 2 + (v / 0.55) ** 2 + ((p.Y - 0.75 - 0.25 * (i % 2)) / 0.38) ** 2 <= 1
        p.boya(yaprak, karistir(renk('3fae49'), renk('9be36b'), np.abs(u) < 0.2), 0.04)
    tac_renk = [renk('ff7bac'), renk('ffffff'), renk('b08cff'), renk('ff5a5a'), renk('ffd84a'), renk('7fd8ff')]
    for i in range(9):
        a = i * 2 * PI / 9
        mx, mz, my = np.cos(a) * 5.15, np.sin(a) * 5.15, 1.45
        c = tac_renk[i % len(tac_renk)]
        buyukluk = 1.0 if i % 3 else 1.25
        for j in range(5):
            b = j * 2 * PI / 5 + i
            # Taç yaprakları, dışa ve yukarı bakan bir düzlemde.
            tx, tz = -np.sin(a), np.cos(a)
            ox = mx + (np.cos(b) * tx) * 0.78 * buyukluk + np.cos(a) * 0.25
            oz = mz + (np.cos(b) * tz) * 0.78 * buyukluk + np.sin(a) * 0.25
            oy = my + np.sin(b) * 0.78 * buyukluk
            yap = kure(p, ox, oy, oz, 0.6 * buyukluk)
            p.boya(yap, karistir(c, renk('ffffff'), np.sqrt((p.X - mx) ** 2 + (p.Y - my) ** 2 + (p.Z - mz) ** 2) > 1.05 * buyukluk), 0.03)
        p.boya(kure(p, mx + np.cos(a) * 0.5, my, mz + np.sin(a) * 0.5, 0.5 * buyukluk), renk('ffc400') if i % len(tac_renk) != 4 else renk('ff8a00'))
    return [p]


def pervane():
    p = Parca('pervane', (-5.4, 5.4, 0, 6.2, -6.6, 5.4))
    R, th = kutup(p)
    kubbe = elipsoid(p, 0, 0.3, 0, 4.95, 3.7, 4.95) & (p.Y >= 0.3)
    dilim = np.floor(((th + PI) / (2 * PI) * 6)) % 6
    renkler = [renk('e53935'), renk('fdd835'), renk('1e88e5'), renk('43a047'), renk('fb8c00'), renk('8e24aa')]
    c = np.zeros(p.rgb.shape, dtype=np.float32)
    for i, r in enumerate(renkler):
        c[dilim == i] = r
    p.boya(kubbe, karistir(c, renk('ffffff'), parlama(th, -2.3, 6) * 0.35 * (p.Y > 1.2)), 0.04)
    p.boya(kubbe & (np.abs(np.sin(th * 3)) < 0.07), renk('2a2a2a'))
    p.boya(kubbe & (p.Y < 0.8), renk('2a2a2a'))
    siper = (p.Z < -4.0) & (p.Z > -6.3) & (np.abs(p.X) <= 3.6 * np.sqrt(np.clip(1 - ((-p.Z - 4.0) / 2.4) ** 2, 0, 1))) \
        & (p.Y >= 0.3) & (p.Y < 0.85)
    p.boya(siper & ~kubbe, renk('e53935'), 0.04)
    p.boya((R < 0.45) & (p.Y >= 3.8) & (p.Y < 5.6), renk('b0b6c2'))

    k = Parca('pervane_kanat', (-4.6, 4.6, 5.0, 6.6, -4.6, 4.6), anim={'don': [0, 22, 0]}, eksen=(0, 5.8, 0))
    Rk, thk = kutup(k)
    # İki pal: X ekseni boyunca uzanır, hafif burgulu.
    burgu = 0.16 * k.X * np.sign(k.Z + 1e-6) * 0
    pal = ((np.abs(k.X) / 4.3) ** 2 + (k.Z / 1.15) ** 2 <= 1) & (np.abs(k.Y - 5.75 - 0.09 * k.Z * np.sign(k.X)) < 0.3) & (Rk > 0.5)
    k.boya(pal, karistir(renk('fdd835'), renk('e53935'), np.abs(k.X) > 3.2), 0.03)
    k.boya(pal & (np.abs(k.X) > 2.2) & (np.abs(k.X) < 2.6), renk('ffffff'))
    k.boya((Rk < 0.85) & (k.Y >= 5.3) & (k.Y < 6.4), renk('1e88e5'))
    return [p, k]


# ---------------------------------------------------------------- haleler
HALE_Y = 3.4


def _halka(ad, yaricap, tup_r, tup_y, renk_fn, anim, y=HALE_Y):
    g = yaricap + tup_r + 0.4
    p = Parca(ad, (-g, g, y - tup_y - 0.4, y + tup_y + 0.4, -g, g), isik=True, anim=anim, eksen=(0, y, 0))
    R, th = kutup(p)
    maske = ((R - yaricap) / tup_r) ** 2 + ((p.Y - y) / tup_y) ** 2 <= 1
    p.boya(maske, renk_fn(p, R, th))
    return p


def _pirilti(ad, y, renkli, adet=3, yaricap=6.2, hiz=-2.4, faz=0.0):
    """Halkanın çevresinde dönen küçük artı biçimli parıltılar."""
    g = yaricap + 1.4
    p = Parca(ad, (-g, g, y - 2.0, y + 2.4, -g, g), isik=True, anim={'don': [0, hiz, 0], 'sek': [0.5, 0.08, faz]},
              eksen=(0, y, 0))
    for i in range(adet):
        a = i * 2 * PI / adet + faz
        cx, cz, cy = np.cos(a) * yaricap, np.sin(a) * yaricap, y + (i % 2) * 1.1 - 0.4
        dx, dy, dz = np.abs(p.X - cx), np.abs(p.Y - cy), np.abs(p.Z - cz)
        boy = 1.0 - 0.3 * (i % 2)
        arti = ((dx < boy) & (dy < 0.2) & (dz < 0.2)) | ((dy < boy) & (dx < 0.2) & (dz < 0.2)) | ((dz < boy) & (dx < 0.2) & (dy < 0.2))
        p.boya(arti, karistir(renk('ffffff'), renkli, (dx + dy + dz) / boy))
    return p


def hale_altin():
    altin, acik, koyu = renk('ffcf33'), renk('fff8d0'), renk('d99a00')
    halka = _halka('hale_altin', 4.7, 0.85, 0.55,
                   lambda p, R, th: karistir(karistir(koyu, altin, (p.Y > HALE_Y - 0.2) * 0.7 + 0.3), acik,
                                             np.clip(parlama(th, 0.6, 3) + parlama(th, 3.6, 3), 0, 1) * (p.Y > HALE_Y)),
                   {'don': [0, 1.6, 0], 'sek': [0.35, 0.07, 0]})
    return [halka, _pirilti('hale_altin_pirilti', HALE_Y, renk('ffe680'))]


def hale_hapi():
    yesil, acik, koyu = renk('2bd35a'), renk('c9ffd8'), renk('12913a')
    halka = _halka('hale_hapi', 4.7, 0.6, 0.5,
                   lambda p, R, th: karistir(karistir(yesil, renk('ffffff'), (np.floor(th * 6 / PI) % 2) * 0.9), acik,
                                             (p.Y > HALE_Y + 0.15) * 0.5),
                   {'don': [0, 1.4, 0], 'sek': [0.35, 0.07, 0]})
    # Halkadan dışa uzanan defne yaprakları.
    for i in range(12):
        a = i * 2 * PI / 12 + PI / 12
        cx, cz = np.cos(a) * 5.75, np.sin(a) * 5.75
        e = a + 0.75
        u = (halka.X - cx) * np.cos(e) + (halka.Z - cz) * np.sin(e)
        v = -(halka.X - cx) * np.sin(e) + (halka.Z - cz) * np.cos(e)
        # Parça sınırı yapraklara yetmediği için genişletilmiş ayrı parçada çiziyoruz.
    yaprak = Parca('hale_hapi_yaprak', (-7.4, 7.4, HALE_Y - 0.9, HALE_Y + 0.9, -7.4, 7.4), isik=True,
                   anim={'don': [0, 1.4, 0], 'sek': [0.35, 0.07, 0]}, eksen=(0, HALE_Y, 0))
    for i in range(12):
        a = i * 2 * PI / 12 + PI / 12
        cx, cz = np.cos(a) * 5.8, np.sin(a) * 5.8
        e = a + 0.85
        u = (yaprak.X - cx) * np.cos(e) + (yaprak.Z - cz) * np.sin(e)
        v = -(yaprak.X - cx) * np.sin(e) + (yaprak.Z - cz) * np.cos(e)
        m = (u / 1.35) ** 2 + (v / 0.6) ** 2 + ((yaprak.Y - HALE_Y) / 0.36) ** 2 <= 1
        yaprak.boya(m, karistir(koyu, renk('7df0a0'), (np.abs(v) < 0.18) * 0.9 + 0.25 * (u > 0)))
    return [halka, yaprak]


def hale_ates():
    kizil, turuncu, sari = renk('d81e05'), renk('ff7a00'), renk('ffe14d')
    halka = _halka('hale_ates', 4.7, 0.75, 0.5,
                   lambda p, R, th: karistir(karistir(kizil, turuncu, 0.5 + 0.5 * np.sin(th * 7)), sari, (p.Y > HALE_Y + 0.1) * 0.6),
                   {'don': [0, 2.2, 0], 'sek': [0.3, 0.07, 0]})
    parcalar = [halka]
    for takim in range(2):
        p = Parca('hale_ates_alev%d' % takim, (-6.4, 6.4, HALE_Y - 0.3, HALE_Y + 5.2, -6.4, 6.4), isik=True,
                  anim={'don': [0, 2.2 if takim == 0 else -3.1, 0], 'sek': [0.45, 0.31 + 0.12 * takim, takim * 2.0]},
                  eksen=(0, HALE_Y, 0))
        for i in range(6):
            a = i * 2 * PI / 6 + takim * PI / 6
            boy = 3.2 + 1.4 * ((i * 7 + takim * 3) % 4) / 3
            cx, cz = np.cos(a) * 4.7, np.sin(a) * 4.7
            h = (p.Y - HALE_Y) / boy
            # Damla biçimi; yukarı çıktıkça dönüş yönüne yatar.
            kay = 0.5 * h ** 2
            d = np.hypot(p.X - cx + np.sin(a) * kay, p.Z - cz - np.cos(a) * kay)
            alev = (h >= 0) & (h <= 1) & (d <= 0.78 * np.sqrt(np.clip(1 - h, 0, 1)) * (0.55 + 0.9 * np.clip(h * 3, 0, 1) * (1 - h) + 0.35))
            p.boya(alev, karistir(karistir(sari, turuncu, np.clip(h * 1.6, 0, 1)), kizil, np.clip((h - 0.55) * 2.2, 0, 1)))
        parcalar.append(p)
    return parcalar


def hale_gokkusagi():
    def boyama(p, R, th):
        ton = ((th + PI) / (2 * PI)) % 1.0
        ton = np.floor(ton * 14) / 14
        c = np.zeros(p.rgb.shape, dtype=np.float32)
        for t in np.unique(ton):
            c[ton == t] = np.array(colorsys.hsv_to_rgb(float(t), 0.78, 1.0)) * 255
        return karistir(c, renk('ffffff'), (p.Y > HALE_Y + 0.25) * 0.45)
    halka = _halka('hale_gokkusagi', 4.7, 0.9, 0.6, boyama, {'don': [0, 4.5, 0], 'sek': [0.35, 0.07, 0]})
    return [halka, _pirilti('hale_gokkusagi_pirilti', HALE_Y, renk('ffffff'), adet=4, hiz=-1.6)]


def hale_yildiz():
    parcalar = []
    renkler = [renk('ffffff'), renk('9bf5ff'), renk('ffe680'), renk('c9b6ff'), renk('9bffc4')]
    for i in range(5):
        y = HALE_Y + 0.6 + (i % 2) * 1.3
        p = Parca('hale_yildiz%d' % i, (-1.9, 1.9, y - 1.9, y + 1.9, -0.45, 0.45), isik=True,
                  anim={'yorunge': [6.3, 3.0, i * 72], 'sek': [0.6, 0.1, i * 1.3], 'don': [0, 7, 0]}, eksen=(0, y, 0))
        m = _yildiz_maske(p, 0, y, 1.75, 0.74)
        p.boya(m, karistir(renkler[i], renk('ffffff'), np.hypot(p.X, p.Y - y) < 0.75))
        parcalar.append(p)
    return parcalar


SAPKALAR = {
    'silindir': silindir, 'tac': tac, 'fes': fes, 'parti': parti, 'hasir': hasir, 'boynuz': boynuz, 'kedi': kedi,
    'tavsan': tavsan, 'asci': asci, 'buyucu': buyucu, 'kep': kep, 'cicek': cicek, 'pervane': pervane,
}
HALELER = {
    'hale_altin': hale_altin, 'hale_hapi': hale_hapi, 'hale_ates': hale_ates, 'hale_gokkusagi': hale_gokkusagi,
    'hale_yildiz': hale_yildiz,
}
