#!/usr/bin/env python3
"""Pelerin dokularını, beyaz dokuyu ve simgeyi üretir. Çalıştır: python3 araclar/doku_uret.py"""
import math, os, random
from PIL import Image, ImageDraw

KOK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "mod", "src", "main", "resources", "assets", "hapiclient")
G, Y = 10, 16  # pelerin yüzü

def karistir(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)

def yuz(ad):
    """10x16 piksellik tasarımı döndürür."""
    im = Image.new("RGBA", (G, Y))
    p = im.load()
    rnd = random.Random(ad)
    for y in range(Y):
        for x in range(G):
            t = y / (Y - 1)
            if ad == "hapi":
                c = karistir((43, 211, 90), (20, 110, 45), t)
                h = [(3, r) for r in range(4, 12)] + [(6, r) for r in range(4, 12)] + [(4, 7), (5, 7), (4, 8), (5, 8)]
                if (x, y) in h: c = (255, 255, 255, 255)
            elif ad == "bayrak":
                c = (227, 10, 23, 255)
                d1 = math.hypot(x - 4.2, y - 7.5); d2 = math.hypot(x - 5.2, y - 7.5)
                if d1 <= 3.2 and d2 > 2.5: c = (255, 255, 255, 255)
                if (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]: c = (255, 255, 255, 255)
            elif ad == "gece":
                c = karistir((16, 16, 48), (60, 30, 110), t)
                if rnd.random() < 0.09: c = (255, 255, 230, 255)
            elif ad == "alev":
                dalga = 6 + 3 * math.sin(x * 1.3) + rnd.random() * 2
                c = karistir((255, 220, 70), (255, 80, 0), min(1, max(0, (Y - y) / dalga))) if Y - y < dalga + 3 else (60, 15, 10, 255)
            elif ad == "buz":
                c = karistir((235, 250, 255), (90, 190, 255), t)
                if (x + y * 2) % 7 == 0: c = (255, 255, 255, 255)
            elif ad == "altin":
                c = karistir((255, 236, 150), (200, 150, 20), (math.sin((x + y) * 0.7) + 1) / 2)
            elif ad == "gokkusagi":
                r = [(230, 40, 40), (245, 140, 30), (250, 220, 50), (60, 190, 80), (50, 120, 230), (130, 60, 200)]
                c = r[min(5, y * 6 // Y)] + (255,)
            elif ad == "kareli":
                c = (43, 211, 90, 255) if (x // 2 + y // 2) % 2 == 0 else (20, 90, 40, 255)
            elif ad == "siyah":
                c = karistir((40, 40, 46), (10, 10, 12), t)
                if x in (0, G - 1) or y == Y - 1: c = (120, 20, 20, 255)
            elif ad == "kalp":
                c = (255, 123, 172, 255)
                kalp = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]
                kx, ky = x - 1, y - 5
                if 0 <= ky < 6 and 0 <= kx < 7 and kalp[ky][kx] == "1": c = (255, 255, 255, 255)
            p[x, y] = c
    return im

def pelerin(ad):
    f = yuz(ad)
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    kenar = f.resize((1, 1)).getpixel((0, 0))
    ImageDraw.Draw(im).rectangle([0, 0, 21, 16], fill=kenar)
    im.paste(f, (1, 1))
    im.paste(f.transpose(Image.FLIP_LEFT_RIGHT), (12, 1))
    im.save(os.path.join(KOK, "textures", "pelerin", ad + ".png"))

for ad in ["hapi", "bayrak", "gece", "alev", "buz", "altin", "gokkusagi", "kareli", "siyah", "kalp"]:
    pelerin(ad)
Image.new("RGBA", (4, 4), (255, 255, 255, 255)).save(os.path.join(KOK, "textures", "beyaz.png"))

# Simge: yeşil zemin üzerinde beyaz H
s = Image.new("RGBA", (16, 16), (43, 211, 90, 255)); d = ImageDraw.Draw(s)
d.rectangle([0, 0, 15, 15], outline=(20, 110, 45, 255))
d.rectangle([4, 3, 5, 12], fill="white"); d.rectangle([10, 3, 11, 12], fill="white"); d.rectangle([6, 7, 9, 8], fill="white")
s.resize((128, 128), Image.NEAREST).save(os.path.join(KOK, "icon.png"))
print("tamam")
