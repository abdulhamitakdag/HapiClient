#!/usr/bin/env python3
"""HapiClient dağıtım dosyasını (distribution.json) üretir.

Yaptıkları:
  * Modrinth'ten her modun seçili Minecraft sürümü + Fabric için en güncel sürümünü bulur,
    "required" bağımlılıklarını da ekler.
  * Her dosyayı indirir, Modrinth'in verdiği sha512 ile doğrular, Helios'un istediği MD5 ve boyutu
    hesaplar. (Helios dosyaları yalnız MD5 ile doğrular.)
  * Fabric loader profilini ve kütüphanelerini meta.fabricmc.net üzerinden alır.
  * Kendi modumuzu (hapiclient) mod/build/libs altından alır.
  * Sonucu `dagitim/` klasörüne yazar: distribution.json, versions/…, mods/…, ikon.png.
    Bu klasör olduğu gibi web sunucusuna (TABAN_URL altına) kopyalanır.

Kullanım:
  python3 araclar/dagitim_uret.py                      # üret
  python3 araclar/dagitim_uret.py --taban-url http://127.0.0.1:8080   # yerel deneme için
  python3 araclar/dagitim_uret.py --dogrula            # var olan distribution.json'daki her URL'yi
                                                       # yeniden indirip MD5/boyut karşılaştır

Sürüm güncellemek için aşağıdaki sabitleri değiştirip betiği yeniden çalıştırmak yeterlidir.
Yalnız Python standart kitaplığını kullanır.
"""
import argparse
import hashlib
import json
import shutil
import sys
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

# ---------------------------------------------------------------------------------------------
# Ayarlar
# ---------------------------------------------------------------------------------------------

MINECRAFT = '1.21.11'
FABRIC_LOADER = '0.20.1'
DAGITIM_SURUMU = '1.0.0'

SUNUCU_KIMLIK = 'HapiCraft'
SUNUCU_AD = 'HapiCraft'
SUNUCU_ACIKLAMA = 'HapiCraft sunucu ağı'
SUNUCU_ADRES = 'hapicraft.net'

TABAN_URL = 'https://client.hapicraft.com.tr/dagitim'

# Modrinth proje kısa adları. zorunlu=False olanlar ayarlardan açılıp kapatılabilir;
# varsayilan, ilk kurulumdaki durumdur.
MODLAR = [
    {'slug': 'fabric-api'},
    {'slug': 'sodium'},
    {'slug': 'lithium'},
    {'slug': 'ferrite-core'},
    {'slug': 'immediatelyfast'},
    {'slug': 'entityculling'},
    {'slug': 'moreculling'},
    {'slug': 'dynamic-fps'},
    {'slug': 'iris', 'zorunlu': False, 'varsayilan': False},
    {'slug': 'sodium-extra'},
    {'slug': 'lambdynamiclights', 'zorunlu': False, 'varsayilan': True},
    {'slug': 'badoptimizations'},
    {'slug': 'krypton'},
]

# Gölgelendirici paketleri (Modrinth). Oyun dizinindeki shaderpacks/ klasörüne iner; yalnız
# "Yüksek Kalite" oyun modunda etkinleştirilir (launcher: app/assets/js/hapi/profil.js).
# Complementary lisansı gereği dosya değiştirilmeden, doğrudan Modrinth adresinden indirilir;
# kendi sunucumuza kopyalanmaz.
GOLGELENDIRICILER = ['complementary-reimagined']

# Java'ya ayrılacak bellek sınırları (MB). Asıl değer launcher'daki donanım taramasıyla seçilir.
RAM_EN_AZ = 1024
RAM_ONERILEN = 3072

KULLANICI_ARACI = 'launcher-dagitim-araci/1.0'

KOK = Path(__file__).resolve().parent.parent
CIKTI = KOK / 'dagitim'
ONBELLEK = Path(__file__).resolve().parent / '.onbellek'
HAPICLIENT_JAR_DIZINI = KOK.parent / 'mod' / 'build' / 'libs'
LOGO = KOK / 'app' / 'assets' / 'images' / 'logo.png'

MODRINTH = 'https://api.modrinth.com/v2'
FABRIC_META = 'https://meta.fabricmc.net/v2'


# ---------------------------------------------------------------------------------------------
# Yardımcılar
# ---------------------------------------------------------------------------------------------

class Hata(Exception):
    pass


def indir(url: str) -> bytes:
    istek = urllib.request.Request(url, headers={'User-Agent': KULLANICI_ARACI})
    try:
        with urllib.request.urlopen(istek, timeout=60) as yanit:
            return yanit.read()
    except urllib.error.URLError as e:
        raise Hata(f'İndirilemedi: {url} ({e})') from e


def json_al(url: str):
    return json.loads(indir(url))


def ozet(veri: bytes, algoritma: str) -> str:
    return hashlib.new(algoritma, veri).hexdigest()


def dogrulanmis_indir(url: str, algoritma: str, beklenen: str) -> bytes:
    """Dosyayı indirir (ya da önbellekten okur) ve verilen özetle doğrular."""
    ONBELLEK.mkdir(exist_ok=True)
    yol = ONBELLEK / f'{algoritma}-{beklenen}'
    if yol.exists():
        veri = yol.read_bytes()
        if ozet(veri, algoritma) == beklenen:
            return veri
    veri = indir(url)
    gercek = ozet(veri, algoritma)
    if gercek != beklenen:
        raise Hata(f'Özet tutmuyor: {url}\n  beklenen {algoritma}={beklenen}\n  gelen    {algoritma}={gercek}')
    yol.write_bytes(veri)
    return veri


def artifact(veri: bytes, url: str) -> dict:
    return {'size': len(veri), 'MD5': ozet(veri, 'md5'), 'url': url}


def maven_yolu(kimlik: str) -> str:
    """'grup:ad:surum' → 'grup/yolu/ad/surum/ad-surum.jar'"""
    grup, ad, surum = kimlik.split(':')[:3]
    return f"{grup.replace('.', '/')}/{ad}/{surum}/{ad}-{surum}.jar"


def maven_guvenli(metin: str) -> str:
    """Maven kimliğinde ayraç olan ':' ve '@' karakterlerini temizler."""
    return metin.replace(':', '_').replace('@', '_').replace(' ', '_')


# ---------------------------------------------------------------------------------------------
# Modrinth
# ---------------------------------------------------------------------------------------------

def modrinth_surum_sec(proje: str) -> dict:
    """Projenin MINECRAFT + fabric için en güncel sürümü (önce kararlı sürümler)."""
    sorgu = urllib.parse.urlencode({
        'game_versions': json.dumps([MINECRAFT]),
        'loaders': json.dumps(['fabric']),
    })
    surumler = json_al(f'{MODRINTH}/project/{proje}/version?{sorgu}')
    if not surumler:
        raise Hata(f'Modrinth: "{proje}" için {MINECRAFT} + fabric sürümü yok.')
    surumler.sort(key=lambda s: s['date_published'], reverse=True)
    for tur in ('release', 'beta', 'alpha'):
        for surum in surumler:
            if surum['version_type'] == tur:
                if tur != 'release':
                    print(f'  UYARI: {proje} için kararlı sürüm yok, {tur} kullanılıyor: {surum["version_number"]}')
                return surum
    raise Hata(f'Modrinth: "{proje}" için uygun sürüm bulunamadı.')


def modrinth_modulleri() -> list:
    """MODLAR ve zorunlu bağımlılıkları için FabricMod modülleri."""
    secilen = {}   # proje kimliği → {'surum', 'proje', 'ayar', 'neden'}
    kuyruk = [(m['slug'], None, m, None) for m in MODLAR]

    while kuyruk:
        proje_ref, surum_kimligi, ayar, neden = kuyruk.pop(0)
        if surum_kimligi is not None:
            surum = json_al(f'{MODRINTH}/version/{surum_kimligi}')
            if MINECRAFT not in surum['game_versions'] or 'fabric' not in surum['loaders']:
                # Bağımlılık eski bir sürüme sabitlenmiş; bu Minecraft sürümüne uygun olanı seç.
                surum = modrinth_surum_sec(surum['project_id'])
        else:
            surum = modrinth_surum_sec(proje_ref)

        proje_kimligi = surum['project_id']
        if proje_kimligi in secilen:
            # Listede açıkça isteğe bağlı sayılan bir mod, bağımlılık olarak gelse de öyle kalır;
            # yalnız bağımlılık olarak gelenler zorunludur.
            continue

        proje = json_al(f'{MODRINTH}/project/{proje_kimligi}')
        secilen[proje_kimligi] = {'surum': surum, 'proje': proje, 'ayar': ayar, 'neden': neden}

        for bagimlilik in surum['dependencies']:
            if bagimlilik['dependency_type'] != 'required':
                continue
            hedef = bagimlilik.get('project_id')
            if hedef is None and bagimlilik.get('version_id') is None:
                continue
            if hedef in secilen:
                continue
            kuyruk.append((hedef, bagimlilik.get('version_id'), {}, proje['slug']))

    # Açıkça listelenen bir mod, başka bir modun bağımlılığı olarak önce işlenmiş olabilir.
    acik = {m['slug']: m for m in MODLAR}
    moduller = []
    for kayit in secilen.values():
        surum, proje = kayit['surum'], kayit['proje']
        ayar = acik.get(proje['slug'], kayit['ayar'])
        dosya = next((d for d in surum['files'] if d['primary']), surum['files'][0])
        veri = dogrulanmis_indir(dosya['url'], 'sha512', dosya['hashes']['sha512'])
        if ozet(veri, 'sha1') != dosya['hashes']['sha1'] or len(veri) != dosya['size']:
            raise Hata(f'{proje["slug"]}: sha1/boyut Modrinth kaydıyla uyuşmuyor.')

        modul = {
            'id': f"modrinth:{proje['slug']}:{maven_guvenli(surum['version_number'])}@jar",
            'name': proje['title'],
            'type': 'FabricMod',
            'artifact': artifact(veri, dosya['url']),
        }
        if not ayar.get('zorunlu', True):
            modul['required'] = {'value': False, 'def': ayar.get('varsayilan', True)}
        moduller.append(modul)

        ek = f' (bağımlılık: {kayit["neden"]})' if kayit['neden'] else ''
        durum = '' if ayar.get('zorunlu', True) else f' [isteğe bağlı, varsayılan {"açık" if ayar.get("varsayilan", True) else "kapalı"}]'
        print(f'  {proje["slug"]:<22} {surum["version_number"]:<28} {len(veri):>9} bayt{durum}{ek}')
    return moduller


def golgelendirici_modulleri() -> list:
    """GOLGELENDIRICILER için File modülleri (oyun dizinine göre shaderpacks/<dosya>)."""
    moduller = []
    for slug in GOLGELENDIRICILER:
        proje = json_al(f'{MODRINTH}/project/{slug}')
        surumler = [s for s in json_al(f'{MODRINTH}/project/{slug}/version')
                    if MINECRAFT in s['game_versions'] and s['version_type'] == 'release']
        if not surumler:
            raise Hata(f'Modrinth: "{slug}" için {MINECRAFT} sürümü yok.')
        surum = max(surumler, key=lambda s: s['date_published'])
        dosya = next((d for d in surum['files'] if d['primary']), surum['files'][0])
        veri = dogrulanmis_indir(dosya['url'], 'sha512', dosya['hashes']['sha512'])
        modul_artifact = artifact(veri, dosya['url'])
        modul_artifact['path'] = f"shaderpacks/{dosya['filename']}"
        moduller.append({
            'id': f"modrinth:{proje['slug']}:{maven_guvenli(surum['version_number'])}@zip",
            'name': proje['title'],
            'type': 'File',
            'artifact': modul_artifact,
        })
        print(f'  {proje["slug"]:<22} {surum["version_number"]:<28} {len(veri):>9} bayt')
    return moduller


# ---------------------------------------------------------------------------------------------
# Fabric loader
# ---------------------------------------------------------------------------------------------

def fabric_modulu(taban_url: str) -> dict:
    """Fabric modülü: loader jar'ı + VersionManifest + kütüphaneler (Nebula'nın ürettiği biçim)."""
    ham = indir(f'{FABRIC_META}/versions/loader/{MINECRAFT}/{FABRIC_LOADER}/profile/json')
    profil = json.loads(ham)

    manifest_kimlik = f'{MINECRAFT}-fabric-{FABRIC_LOADER}'
    manifest_goreli = f'versions/{manifest_kimlik}/{manifest_kimlik}.json'
    manifest_yolu = CIKTI / manifest_goreli
    manifest_yolu.parent.mkdir(parents=True, exist_ok=True)
    manifest_yolu.write_bytes(ham)

    alt_moduller = [{
        'id': manifest_kimlik,
        'name': 'Fabric (version.json)',
        'type': 'VersionManifest',
        'artifact': artifact(ham, f'{taban_url}/{manifest_goreli}'),
    }]
    fabric = None
    for kutuphane in profil['libraries']:
        ad = kutuphane['name']
        url = kutuphane['url'].rstrip('/') + '/' + maven_yolu(ad)
        if 'sha1' in kutuphane:
            veri = dogrulanmis_indir(url, 'sha1', kutuphane['sha1'])
        else:
            # Profil özet vermiyorsa Maven deposundaki .sha1 dosyasıyla doğrula.
            veri = dogrulanmis_indir(url, 'sha1', indir(url + '.sha1').decode().split()[0])
        modul = {
            'id': ad,
            'name': f"Fabric ({ad.split(':')[1]})",
            'type': 'Library',
            'artifact': artifact(veri, url),
        }
        print(f'  {ad:<52} {len(veri):>9} bayt')
        if ad.startswith('net.fabricmc:fabric-loader:'):
            modul['type'] = 'Fabric'
            modul['name'] = 'Fabric (fabric-loader)'
            fabric = modul
        else:
            alt_moduller.append(modul)

    if fabric is None:
        raise Hata('Fabric profilinde fabric-loader kütüphanesi yok.')
    fabric['subModules'] = alt_moduller
    return fabric


# ---------------------------------------------------------------------------------------------
# Kendi modumuz
# ---------------------------------------------------------------------------------------------

def hapiclient_modulu(taban_url: str) -> dict:
    adaylar = [j for j in HAPICLIENT_JAR_DIZINI.glob('hapiclient-*.jar')
               if not j.name.endswith(('-sources.jar', '-dev.jar'))]
    if not adaylar:
        raise Hata(f'hapiclient jar\'ı bulunamadı: {HAPICLIENT_JAR_DIZINI} (önce modu derle)')
    jar = max(adaylar, key=lambda j: j.stat().st_mtime)
    veri = jar.read_bytes()
    with zipfile.ZipFile(jar) as z:
        bilgi = json.loads(z.read('fabric.mod.json'))

    hedef = CIKTI / 'mods' / jar.name
    hedef.parent.mkdir(parents=True, exist_ok=True)
    for eski in hedef.parent.glob('hapiclient-*.jar'):
        eski.unlink()
    hedef.write_bytes(veri)

    print(f'  {"hapiclient":<22} {bilgi["version"]:<28} {len(veri):>9} bayt  ({jar.name})')
    return {
        'id': f"tr.com.hapicraft:{bilgi['id']}:{maven_guvenli(bilgi['version'])}@jar",
        'name': bilgi.get('name', 'HapiClient'),
        'type': 'FabricMod',
        'artifact': artifact(veri, f'{taban_url}/mods/{urllib.parse.quote(jar.name)}'),
    }


# ---------------------------------------------------------------------------------------------
# Üretim ve doğrulama
# ---------------------------------------------------------------------------------------------

def uret(taban_url: str) -> None:
    taban_url = taban_url.rstrip('/')
    CIKTI.mkdir(exist_ok=True)

    print(f'Fabric loader {FABRIC_LOADER} (Minecraft {MINECRAFT}):')
    moduller = [fabric_modulu(taban_url)]
    print('Modrinth modları:')
    moduller += modrinth_modulleri()
    print('Gölgelendiriciler:')
    moduller += golgelendirici_modulleri()
    print('Kendi modumuz:')
    moduller.append(hapiclient_modulu(taban_url))

    kimlikler = [m['id'].rsplit(':', 1)[0] for m in moduller]
    if len(kimlikler) != len(set(kimlikler)):
        raise Hata('Aynı mod iki kez eklendi.')

    shutil.copyfile(LOGO, CIKTI / 'ikon.png')

    dagitim = {
        'version': DAGITIM_SURUMU,
        'servers': [{
            'id': SUNUCU_KIMLIK,
            'name': SUNUCU_AD,
            'description': SUNUCU_ACIKLAMA,
            'icon': f'{taban_url}/ikon.png',
            'version': DAGITIM_SURUMU,
            'address': SUNUCU_ADRES,
            'minecraftVersion': MINECRAFT,
            'mainServer': True,
            'autoconnect': True,
            'javaOptions': {'ram': {'minimum': RAM_EN_AZ, 'recommended': RAM_ONERILEN}},
            'modules': moduller,
        }],
    }
    hedef = CIKTI / 'distribution.json'
    hedef.write_text(json.dumps(dagitim, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

    toplam = sum(1 + len(m.get('subModules', [])) for m in moduller)
    print(f'\nYazıldı: {hedef} ({toplam} dosya kaydı)')


def tum_moduller(moduller: list):
    for modul in moduller:
        yield modul
        yield from tum_moduller(modul.get('subModules', []))


def dogrula(taban_url: str) -> None:
    """distribution.json'daki her URL'yi yeniden indirip MD5 ve boyutu karşılaştırır.

    TABAN_URL altındaki (henüz yayınlanmamış olabilecek) dosyalar yerel dagitim/ klasöründen okunur.
    """
    taban_url = taban_url.rstrip('/')
    dagitim = json.loads((CIKTI / 'distribution.json').read_text(encoding='utf-8'))
    hatali = 0
    sayi = 0
    for sunucu in dagitim['servers']:
        for modul in tum_moduller(sunucu['modules']):
            a = modul['artifact']
            sayi += 1
            if a['url'].startswith(taban_url + '/'):
                yerel = CIKTI / urllib.parse.unquote(a['url'][len(taban_url) + 1:])
                kaynak = 'yerel'
                veri = yerel.read_bytes() if yerel.exists() else None
            else:
                kaynak = 'uzak'
                try:
                    veri = indir(a['url'])
                except Hata:
                    veri = None
            if veri is None:
                durum = 'BULUNAMADI'
            elif len(veri) != a['size'] or ozet(veri, 'md5') != a['MD5']:
                durum = 'UYUŞMUYOR'
            else:
                durum = 'tamam'
            if durum != 'tamam':
                hatali += 1
            print(f'  [{durum:^10}] ({kaynak}) {modul["id"]}')
    print(f'\n{sayi} dosya denetlendi, {hatali} sorunlu.')
    if hatali:
        raise Hata('Doğrulama başarısız.')


def main() -> None:
    ayristirici = argparse.ArgumentParser(description='HapiClient dağıtım dosyasını üretir.')
    ayristirici.add_argument('--taban-url', default=TABAN_URL,
                             help=f'dagitim/ klasörünün yayınlanacağı adres (varsayılan: {TABAN_URL})')
    ayristirici.add_argument('--dogrula', action='store_true',
                             help='üretmek yerine var olan distribution.json\'ı doğrula')
    secenekler = ayristirici.parse_args()
    try:
        if secenekler.dogrula:
            dogrula(secenekler.taban_url)
        else:
            uret(secenekler.taban_url)
    except Hata as e:
        sys.exit(f'HATA: {e}')


if __name__ == '__main__':
    main()
