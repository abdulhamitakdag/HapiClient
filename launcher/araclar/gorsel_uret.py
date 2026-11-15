#!/usr/bin/env python3
"""HapiClient görsellerini logodan üretir: uygulama simgeleri, yükleme halkası ve arka plan.

Kullanım: python3 araclar/gorsel_uret.py /yol/logo.png
Gerekli: Pillow.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

KOK = Path(__file__).resolve().parent.parent
GORSELLER = KOK / 'app' / 'assets' / 'images'
YESIL = (43, 211, 90)


def kare_logo(logo: Image.Image, boyut: int, dolgu: float = 0.06) -> Image.Image:
    """Logoyu saydam kare tuvale ortalar."""
    logo = logo.crop(logo.getbbox())
    ic = int(boyut * (1 - 2 * dolgu))
    oran = min(ic / logo.width, ic / logo.height)
    kucuk = logo.resize((round(logo.width * oran), round(logo.height * oran)), Image.LANCZOS)
    tuval = Image.new('RGBA', (boyut, boyut), (0, 0, 0, 0))
    tuval.paste(kucuk, ((boyut - kucuk.width) // 2, (boyut - kucuk.height) // 2), kucuk)
    return tuval


def yukleme_halkasi(boyut: int = 512) -> Image.Image:
    """Dönen yükleme halkası: kuyruğu sönen yeşil yay."""
    olcek = 4
    b = boyut * olcek
    tuval = Image.new('RGBA', (b, b), (0, 0, 0, 0))
    ciz = ImageDraw.Draw(tuval)
    kalinlik = int(b * 0.035)
    kutu = [kalinlik, kalinlik, b - kalinlik, b - kalinlik]
    adim = 3
    for aci in range(0, 300, adim):
        alfa = int(255 * (aci / 300) ** 1.5)
        ciz.arc(kutu, aci - 90, aci - 90 + adim + 1, fill=YESIL + (alfa,), width=kalinlik)
    return tuval.resize((boyut, boyut), Image.LANCZOS)


def arka_plan(gen: int = 1920, yuk: int = 1080) -> Image.Image:
    """Koyu zemin üzerinde yeşil ışımalı sade degrade."""
    kucuk_gen, kucuk_yuk = gen // 4, yuk // 4
    tuval = Image.new('RGB', (kucuk_gen, kucuk_yuk))
    piksel = tuval.load()
    isiklar = [(0.82, 0.18, 0.75, 0.55), (0.10, 0.95, 0.60, 0.30)]  # x, y, yarıçap, güç
    for y in range(kucuk_yuk):
        for x in range(kucuk_gen):
            u, v = x / kucuk_gen, y / kucuk_yuk
            taban = 1 - 0.45 * v
            r, g, b = 9 * taban, 18 * taban, 13 * taban
            for ix, iy, yaricap, guc in isiklar:
                uzaklik = math.hypot((u - ix) * gen / yuk, v - iy)
                etki = max(0.0, 1 - uzaklik / yaricap) ** 2 * guc
                r += YESIL[0] * etki * 0.55
                g += YESIL[1] * etki * 0.55
                b += YESIL[2] * etki * 0.55
            piksel[x, y] = (min(255, int(r)), min(255, int(g)), min(255, int(b)))
    return tuval.resize((gen, yuk), Image.BICUBIC).filter(ImageFilter.GaussianBlur(6))


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    logo = Image.open(sys.argv[1]).convert('RGBA')

    buyuk = kare_logo(logo, 1024)
    buyuk.resize((512, 512), Image.LANCZOS).save(GORSELLER / 'logo.png', optimize=True)
    buyuk.save(GORSELLER / 'logo.ico', sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    buyuk.save(GORSELLER / 'logo.icns')
    buyuk.save(KOK / 'build' / 'icon.png', optimize=True)

    yukleme_halkasi().save(GORSELLER / 'yukleniyor.png', optimize=True)

    (GORSELLER / 'backgrounds').mkdir(exist_ok=True)
    arka_plan().save(GORSELLER / 'backgrounds' / '0.jpg', quality=88)


if __name__ == '__main__':
    main()
