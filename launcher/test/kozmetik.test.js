const test   = require('node:test')
const assert = require('node:assert')
const fs     = require('fs-extra')
const os     = require('os')
const path   = require('path')

const K = require('../app/assets/js/hapi/kozmetik')

function geciciDizin(){
    return fs.mkdtempSync(path.join(os.tmpdir(), 'hapi-kozmetik-'))
}

test('katalog her yuvada dolu, ikon ve dokular yerinde', () => {
    for(const yuva of K.YUVALAR){
        const liste = K.katalog().filter(k => k.yuva === yuva)
        assert.ok(liste.length >= 5, yuva)
        for(const k of liste){
            assert.ok(fs.existsSync(path.join(K.VARLIK_DIZINI, 'doku', 'ikon', k.kimlik + '.png')), k.kimlik)
            if(yuva === 'kanat' || yuva === 'pelerin'){
                assert.ok(fs.existsSync(path.join(K.VARLIK_DIZINI, 'doku', yuva, k.doku + '.png')), k.kimlik)
            }
        }
    }
})

test('seçim oyunun ayar dosyasına yazılır, diğer ayarlara dokunulmaz', () => {
    const dizin = geciciDizin()
    const dosya = path.join(dizin, 'hapiclient', 'ayarlar.json')
    fs.ensureDirSync(path.dirname(dosya))
    fs.writeFileSync(dosya, JSON.stringify({ sessiz: true, modul: { zoom: false }, kozmetik: { sapka: 's_fes' } }))
    assert.deepStrictEqual(K.secimiOku(dizin), { sapka: 's_fes' })
    K.secimiYaz(dizin, { kanat: 'k_melek', sapka: 's_tac' })
    const yazilan = JSON.parse(fs.readFileSync(dosya, 'utf8'))
    assert.deepStrictEqual(yazilan.kozmetik, { kanat: 'k_melek', sapka: 's_tac' })
    assert.strictEqual(yazilan.sessiz, true)
    assert.deepStrictEqual(yazilan.modul, { zoom: false })
})

test('ayar dosyası yokken ve bozukken seçim boş okunur, yazınca oluşur', () => {
    const dizin = geciciDizin()
    assert.deepStrictEqual(K.secimiOku(dizin), {})
    fs.ensureDirSync(path.join(dizin, 'hapiclient'))
    fs.writeFileSync(path.join(dizin, 'hapiclient', 'ayarlar.json'), '{bozuk')
    assert.deepStrictEqual(K.secimiOku(dizin), {})
    K.secimiYaz(dizin, { hale: 'h_altin' })
    assert.deepStrictEqual(K.secimiOku(dizin), { hale: 'h_altin' })
})

test('bilinmeyen kimlik ve yanlış yuva yok sayılır', () => {
    const dizin = geciciDizin()
    K.secimiYaz(dizin, { kanat: 's_fes', sapka: 'yok_boyle_bir_sey', aura: 'a_kalp' })
    assert.deepStrictEqual(K.secimiOku(dizin), { aura: 'a_kalp' })
})

test('şapka ve hale modelleri dörtgene çevrilir', () => {
    for(const k of K.katalog().filter(k => k.yuva === 'sapka' || k.yuva === 'hale')){
        const parcalar = K.modelYukle(k.model)
        assert.ok(parcalar.length > 0, k.kimlik)
        for(const p of parcalar){
            assert.ok(p.dortgenler.length > 0, k.kimlik + '/' + p.ad)
            assert.strictEqual(p.dortgenler[0].k.length, 12)
        }
    }
})

test('kanat açıları modla aynı değerleri verir', () => {
    // Beklenen değerler moddaki KozmetikKatmani.kanatAcilari çıktısıdır (KozmetikTest aynı sayıları sınar).
    const a = K.kanatAcilari('tuy', 37.5, false, 0.4)
    const b = K.kanatAcilari('mekanik', 120, true, 0)
    assert.strictEqual(a.length, K.EKLEM)
    assert.deepStrictEqual(a.map(v => +v.toFixed(3)), PARITE.tuy)
    assert.deepStrictEqual(b.map(v => +v.toFixed(3)), PARITE.mekanik)
})

const PARITE = require('./kozmetik_parite.json')
