/**
 * Kozmetiklerin launcher tarafı: katalog, oyun dizinindeki seçim dosyası ve modla ortak hesaplar.
 *
 * Katalog, doku ve modeller `app/assets/kozmetik/` altındadır; `kozmetik/uret.py` aynı dosyaları moda da
 * yazar. Seçim, modun okuduğu `<oyun dizini>/hapiclient/ayarlar.json` dosyasının `kozmetik` alanıdır.
 */
const fs   = require('fs-extra')
const path = require('path')
const zlib = require('zlib')

const VARLIK_DIZINI = path.join(__dirname, '..', '..', 'kozmetik')

exports.VARLIK_DIZINI = VARLIK_DIZINI
exports.YUVALAR = ['pelerin', 'kanat', 'sapka', 'hale', 'aura']
exports.YUVA_ADLARI = { pelerin: 'Pelerin', kanat: 'Kanat', sapka: 'Şapka', hale: 'Hale', aura: 'Aura' }
exports.NADIRLIK = {
    yaygin: { ad: 'Yaygın', renk: '#b8c4bc' },
    nadir: { ad: 'Nadir', renk: '#4fc3ff' },
    destansi: { ad: 'Destansı', renk: '#b98cff' },
    efsanevi: { ad: 'Efsanevi', renk: '#ffc53d' }
}
/** Kanadın kökten uca bölündüğü eklem sayısı (moddaki KozmetikKatmani.EKLEM). */
exports.EKLEM = 8

let katalogOnbellek = null

/** @returns {Array<object>} Katalogdaki bütün kozmetikler. */
exports.katalog = function(){
    if(katalogOnbellek == null){
        katalogOnbellek = JSON.parse(fs.readFileSync(path.join(VARLIK_DIZINI, 'katalog.json'), 'utf8')).kozmetikler
    }
    return katalogOnbellek
}

exports.bul = function(kimlik){
    return exports.katalog().find(k => k.kimlik === kimlik) || null
}

function ayarDosyasi(oyunDizini){
    return path.join(oyunDizini, 'hapiclient', 'ayarlar.json')
}

function ayarlariOku(oyunDizini){
    try {
        const veri = JSON.parse(fs.readFileSync(ayarDosyasi(oyunDizini), 'utf8'))
        return veri != null && typeof veri === 'object' && !Array.isArray(veri) ? veri : {}
    } catch(_err){
        return {}
    }
}

/**
 * @param {string} oyunDizini Sunucunun oyun (instance) dizini.
 * @returns {Object<string,string>} yuva → kozmetik kimliği; yalnız katalogda olan ve yuvası tutan seçimler.
 */
exports.secimiOku = function(oyunDizini){
    const ham = ayarlariOku(oyunDizini).kozmetik
    const secim = {}
    if(ham != null && typeof ham === 'object'){
        for(const yuva of exports.YUVALAR){
            const k = exports.bul(ham[yuva])
            if(k != null && k.yuva === yuva){
                secim[yuva] = k.kimlik
            }
        }
    }
    return secim
}

/**
 * Seçimi oyunun ayar dosyasına yazar; dosyadaki diğer ayarlara dokunmaz.
 *
 * @param {string} oyunDizini Sunucunun oyun (instance) dizini.
 * @param {Object<string,string>} secim yuva → kozmetik kimliği (boş ya da eksik yuva = takılı değil).
 */
exports.secimiYaz = function(oyunDizini, secim){
    const ayarlar = ayarlariOku(oyunDizini)
    const temiz = {}
    for(const yuva of exports.YUVALAR){
        const k = exports.bul(secim[yuva])
        if(k != null && k.yuva === yuva){
            temiz[yuva] = k.kimlik
        }
    }
    ayarlar.kozmetik = temiz
    const dosya = ayarDosyasi(oyunDizini)
    fs.ensureDirSync(path.dirname(dosya))
    fs.writeFileSync(dosya + '.tmp', JSON.stringify(ayarlar, null, 2), 'utf8')
    fs.moveSync(dosya + '.tmp', dosya, { overwrite: true })
}

/**
 * Kanadın her ekleminin sırt düzlemine göre geriye açısı (radyan, kökten uca birikimli).
 * Moddaki KozmetikKatmani.kanatAcilari ile birebir aynı olmalıdır.
 */
exports.kanatAcilari = function(hareket, t, egik, yurume){
    const H = {
        zar:     [0.13, 0.62, 0.34, 0.05, 0.075, 0.42],
        kelebek: [0.21, 0.80, 0.55, 0.0, 0.012, 0.2],
        bocek:   [1.7, 0.55, 0.13, 0.0, 0.0, 0],
        mekanik: [0.11, 0.58, 0.32, 0.03, 0.06, 0.5],
        kristal: [0.06, 0.50, 0.14, 0.02, 0.03, 0.7]
    }
    let [hiz, acilma, genlik, bukum, bukumGenlik, gecikme] = H[hareket] || [0.14, 0.56, 0.30, 0.035, 0.06, 0.38]
    hiz *= 1 + 0.8 * yurume
    if(egik){
        acilma += 0.28
        genlik *= 0.45
    }
    const dalga = faz => {
        const s = Math.sin(faz)
        return hareket === 'mekanik' ? Math.round(s * 5) / 5 : s
    }
    const faz = t * hiz
    let toplam = acilma + genlik * dalga(faz)
    if(hareket === 'bocek'){
        toplam += 0.1 * Math.sin(t * 0.09)
    }
    const a = []
    for(let i = 0; i < exports.EKLEM; i++){
        if(i > 0){
            toplam += bukum + bukumGenlik * dalga(faz - gecikme - i * 0.3)
        }
        a.push(toplam)
    }
    return a
}

const YONLER = [[1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1]]

/**
 * Voksel modelini okur ve her parçayı dörtgenlere çevirir (aynı renkli açık yüzler birleştirilir).
 * Konumlar kafa kemiği uzayında model pikselidir (y aşağı). Moddaki VokselModel ile aynı sonuç.
 *
 * @returns {Array<{ad:string, isik:boolean, eksen:number[], anim:object, dortgenler:Array<{k:number[], renk:number, n:number[]}>}>}
 */
exports.modelYukle = function(ad){
    const ham = JSON.parse(fs.readFileSync(path.join(VARLIK_DIZINI, 'model', ad + '.json'), 'utf8'))
    return ham.parcalar.map(h => {
        const [nx, ny, nz] = h.boyut
        const v = zlib.inflateSync(Buffer.from(h.veri, 'base64'))
        if(v.length !== nx * ny * nz){
            throw new Error('eksik voksel verisi: ' + h.ad)
        }
        const palet = [0].concat(h.palet.map(p => parseInt(p, 16)))
        const n = [nx, ny, nz]
        const adim = 1 / h.olcek
        const dortgenler = []
        for(const d of YONLER){
            const a = d[0] !== 0 ? 0 : d[1] !== 0 ? 1 : 2, b = (a + 1) % 3, c = (a + 2) % 3
            const maske = new Int32Array(n[b] * n[c])
            const p = [0, 0, 0]
            for(p[a] = 0; p[a] < n[a]; p[a]++){
                for(p[c] = 0; p[c] < n[c]; p[c]++){
                    for(p[b] = 0; p[b] < n[b]; p[b]++){
                        const kendi = v[p[0] + nx * (p[1] + ny * p[2])]
                        const kx = p[0] + d[0], ky = p[1] + d[1], kz = p[2] + d[2]
                        const kapali = kx >= 0 && ky >= 0 && kz >= 0 && kx < nx && ky < ny && kz < nz
                            && v[kx + nx * (ky + ny * kz)] !== 0
                        maske[p[b] + n[b] * p[c]] = kendi === 0 || kendi === 255 || kapali ? 0 : kendi
                    }
                }
                for(let j = 0; j < n[c]; j++){
                    for(let i = 0; i < n[b];){
                        const renk = maske[i + n[b] * j]
                        if(renk === 0){
                            i++
                            continue
                        }
                        let g = 1
                        while(i + g < n[b] && maske[i + g + n[b] * j] === renk) g++
                        let yuk = 1
                        dis: for(; j + yuk < n[c]; yuk++){
                            for(let t = 0; t < g; t++){
                                if(maske[i + t + n[b] * (j + yuk)] !== renk) break dis
                            }
                        }
                        for(let jj = 0; jj < yuk; jj++){
                            for(let t = 0; t < g; t++) maske[i + t + n[b] * (j + jj)] = 0
                        }
                        const k = []
                        for(const [da, db] of [[0, 0], [g, 0], [g, yuk], [0, yuk]]){
                            const ijk = [0, 0, 0]
                            ijk[a] = p[a] + (d[a] > 0 ? 1 : 0)
                            ijk[b] = i + da
                            ijk[c] = j + db
                            k.push(h.koken[0] + ijk[0] * adim, h.koken[1] - ijk[1] * adim, h.koken[2] + ijk[2] * adim)
                        }
                        dortgenler.push({ k, renk: palet[Math.min(renk, palet.length - 1)], n: [d[0], -d[1], d[2]] })
                        i += g
                    }
                }
            }
        }
        return { ad: h.ad, isik: h.isik, eksen: h.eksen, anim: h.anim || {}, dortgenler }
    })
}
