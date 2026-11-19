/**
 * Sahne çizimi için kozmetik verisi: modun kaynaklarındaki katalog ve voksel modelleri okur.
 * Hareket ve model hesapları moddaki KozmetikKatmani / VokselModel ile aynıdır.
 */
const fs   = require('fs')
const path = require('path')
const zlib = require('zlib')

const MOD = path.join(__dirname, '..', '..', '..', 'mod', 'src', 'main', 'resources', 'assets', 'hapiclient')
exports.DOKU_DIZINI = path.join(MOD, 'textures', 'kozmetik')
const MODEL_DIZINI = path.join(MOD, 'kozmetik', 'model')
exports.EKLEM = 8

const katalog = JSON.parse(fs.readFileSync(path.join(MOD, 'kozmetik', 'katalog.json'), 'utf8')).kozmetikler
exports.bul = kimlik => katalog.find(k => k.kimlik === kimlik) || null

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
    const ham = JSON.parse(fs.readFileSync(path.join(MODEL_DIZINI, ad + '.json'), 'utf8'))
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
