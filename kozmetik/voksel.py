"""Küçük küplerden (voksel) kozmetik modeli: yazım yardımcıları, dışa aktarım ve ikon çizimi.

Yazım uzayı ("şapka uzayı"), birim = model pikseli (1/16 blok):
  X sağa, Y yukarı (0 = başın tepesi), Z arkaya (yüz -Z tarafında). Baş X,Z: -4..4.
Oyundaki kafa kemiği uzayına çeviri: model_y = -8 - Y.
"""
import base64
import zlib

import numpy as np
from PIL import Image, ImageDraw

OLCEK = 3  # bir model pikselindeki küp sayısı


def renk(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float32)


def karistir(a, b, t):
    """a ve b renkleri arasında t (dizi olabilir) kadar geçiş."""
    t = np.clip(np.asarray(t, dtype=np.float32), 0, 1)[..., None]
    return a * (1 - t) + b * t


class Parca:
    """Tek bir voksel ızgarası. sinir = (x0, x1, y0, y1, z0, z1) model pikseli."""

    def __init__(self, ad, sinir, olcek=OLCEK, isik=False, anim=None, eksen=None):
        self.ad, self.olcek, self.isik, self.anim = ad, olcek, isik, anim or {}
        x0, x1, y0, y1, z0, z1 = sinir
        self.koken = (x0, y0, z0)
        self.n = (round((x1 - x0) * olcek), round((y1 - y0) * olcek), round((z1 - z0) * olcek))
        nx, ny, nz = self.n
        i, j, k = np.meshgrid(np.arange(nx), np.arange(ny), np.arange(nz), indexing='ij')
        self.X = x0 + (i + 0.5) / olcek
        self.Y = y0 + (j + 0.5) / olcek
        self.Z = z0 + (k + 0.5) / olcek
        self.dolu = np.zeros(self.n, dtype=bool)
        self.rgb = np.zeros(self.n + (3,), dtype=np.float32)
        self.eksen = eksen if eksen is not None else ((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2)
        r = np.random.RandomState(zlib.crc32(ad.encode()))
        self.gurultu = r.rand(*self.n).astype(np.float32)

    def boya(self, maske, c, oynama=0.0):
        """maske içindeki küpleri c rengine boyar. c tek renk ya da ızgara boyutunda renk dizisi olabilir.
        oynama: küp başına rastgele parlaklık oynaması (0.06 = ±%6), üç basamağa yuvarlanır."""
        maske = np.asarray(maske, dtype=bool)
        c = np.broadcast_to(np.asarray(c, dtype=np.float32), self.rgb.shape)
        if oynama:
            basamak = (np.floor(self.gurultu * 3) - 1) * oynama
            c = c * (1 + basamak[..., None])
        self.rgb[maske] = np.clip(c[maske], 0, 255)
        self.dolu |= maske
        return self

    def sil(self, maske):
        self.dolu &= ~np.asarray(maske, dtype=bool)
        return self

    def yuzey(self):
        """Dışarıdan görünen (en az bir yüzü açık) küpler."""
        d = np.pad(self.dolu, 1)
        ic = d[2:, 1:-1, 1:-1] & d[:-2, 1:-1, 1:-1] & d[1:-1, 2:, 1:-1] & d[1:-1, :-2, 1:-1] \
            & d[1:-1, 1:-1, 2:] & d[1:-1, 1:-1, :-2]
        return self.dolu & ~ic

    def disa_aktar(self):
        nx, ny, nz = self.n
        yuzey = self.yuzey()
        # Renkleri 5 bit'e yuvarla ki palet 255'i aşmasın.
        q = (np.round(self.rgb / 8) * 8).clip(0, 255).astype(np.uint8)
        # 0 = boş, 255 = dolu ama görünmeyen iç küp, 1..254 = palet sırası + 1
        palet, veri = [], np.where(self.dolu, 255, 0).astype(np.uint8)
        sozluk = {}
        for i, j, k in zip(*np.nonzero(yuzey)):
            anahtar = tuple(int(v) for v in q[i, j, k])
            if anahtar not in sozluk:
                if len(palet) >= 254:
                    anahtar = min(sozluk, key=lambda p: sum((a - b) ** 2 for a, b in zip(p, anahtar)))
                else:
                    sozluk[anahtar] = len(palet) + 1
                    palet.append('%02x%02x%02x' % anahtar)
            veri[i, j, k] = sozluk[anahtar]
        ham = veri.transpose(2, 1, 0).tobytes()  # sıra: i + nx*(j + ny*k)
        x0, y0, z0 = self.koken
        ex, ey, ez = self.eksen
        return {
            'ad': self.ad, 'olcek': self.olcek, 'boyut': [nx, ny, nz],
            # Kafa kemiği uzayı: y aşağı doğru artar. j arttıkça model y azalır.
            'koken': [x0, -8 - y0, z0], 'eksen': [ex, -8 - ey, ez],
            'palet': palet, 'veri': base64.b64encode(zlib.compress(ham, 9)).decode(),
            'isik': self.isik, 'anim': self.anim,
        }


def ikon_ciz(parcalar, boyut=64, yaw=-35, pitch=22, buyut=4, bosluk=0.08):
    """Vokselleri eş ölçekli (izometrik benzeri) çizer; ikon ve gözle denetim için."""
    # Şapka uzayı sol elli (X sağ, Y yukarı, Z arka; yüz -Z'de). Kamera öne, yaw kadar yana, pitch kadar yukarıda.
    ya, pi_ = np.radians(yaw), np.radians(pitch)
    kam = np.array([np.sin(ya) * np.cos(pi_), np.sin(pi_), -np.cos(ya) * np.cos(pi_)])
    ileri = -kam
    sag = np.cross([0, 1, 0], ileri)
    sag /= np.linalg.norm(sag)
    ust = np.cross(ileri, sag)

    def izdusum(P):
        return np.stack([P @ sag, -(P @ ust), P @ kam], -1)  # ekran x, ekran y, yakınlık

    yonler = [((1, 0, 0), 0.72), ((-1, 0, 0), 0.78), ((0, 1, 0), 1.0), ((0, -1, 0), 0.5),
              ((0, 0, 1), 0.6), ((0, 0, -1), 0.88)]
    bakis = kam
    yuzler = []
    for p in parcalar:
        d = np.pad(p.dolu, 1)
        s = 1.0 / p.olcek
        x0, y0, z0 = p.koken
        for (n, golge) in yonler:
            if np.dot(n, bakis) <= 0.01:
                continue
            komsu = d[1 + n[0]:d.shape[0] - 1 + n[0], 1 + n[1]:d.shape[1] - 1 + n[1], 1 + n[2]:d.shape[2] - 1 + n[2]]
            acik = p.dolu & ~komsu
            idx = np.argwhere(acik)
            if not len(idx):
                continue
            eks = [a for a in range(3) if n[a] == 0]
            taban = idx.astype(np.float32)
            taban[:, [a for a in range(3) if n[a] != 0][0]] += 1 if sum(n) > 0 else 0
            koseler = []
            for da, db in ((0, 0), (1, 0), (1, 1), (0, 1)):
                k = taban.copy()
                k[:, eks[0]] += da
                k[:, eks[1]] += db
                koseler.append(k * s + np.array([x0, y0, z0]))
            K3 = np.stack(koseler, 1)
            if 'yorunge' in p.anim:  # durağan çizimde yörüngedeki parçayı başlangıç açısına yerleştir
                r, _, faz = p.anim['yorunge']
                a = np.radians(faz)
                e = np.array(p.eksen)
                q = K3 - e + np.array([r, 0, 0])
                K3 = np.stack([q[..., 0] * np.cos(a) + q[..., 2] * np.sin(a), q[..., 1],
                               -q[..., 0] * np.sin(a) + q[..., 2] * np.cos(a)], -1) + e
            K = izdusum(K3)
            c = p.rgb[acik] * (1.0 if p.isik else golge)
            for q, cc in zip(K, c):
                yuzler.append((q[:, 2].mean(), q[:, :2], cc))
    if not yuzler:
        return Image.new('RGBA', (boyut, boyut))
    tum = np.concatenate([y[1] for y in yuzler])
    mn, mx = tum.min(0), tum.max(0)
    genis = max(mx - mn)
    S = boyut * buyut
    o = S * (1 - 2 * bosluk) / genis
    kay = (S - (mx - mn) * o) / 2 - mn * o
    im = Image.new('RGBA', (S, S))
    ciz = ImageDraw.Draw(im)
    for _, q, c in sorted(yuzler, key=lambda y: y[0]):
        ciz.polygon([tuple(v * o + kay) for v in q], fill=tuple(int(v) for v in np.clip(c, 0, 255)) + (255,))
    return im.resize((boyut, boyut), Image.LANCZOS)
