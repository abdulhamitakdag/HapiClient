const test = require('node:test')
const assert = require('node:assert')
const fs = require('fs')
const os = require('os')
const path = require('path')
const profil = require('../app/assets/js/hapi/profil')
const { PROFILLER } = require('../app/assets/js/hapi/donanim')

function geciciDizin(){
    return fs.mkdtempSync(path.join(os.tmpdir(), 'hapi-profil-'))
}

test('mod listesi donanım önerisiyle aynı kimlikleri kullanır', () => {
    assert.deepStrictEqual(profil.MODLAR.map(m => m.kimlik), PROFILLER)
})

test('eski profil adları yeni modlara çevrilir', () => {
    assert.strictEqual(profil.cevir('patates'), 'ultrafps')
    assert.strictEqual(profil.cevir('dusuk'), 'fps')
    assert.strictEqual(profil.cevir('dengeli'), 'normal')
    assert.strictEqual(profil.cevir('ultra'), 'kalite')
    assert.strictEqual(profil.cevir('ULTRA_FPS'), 'ultrafps')
    assert.strictEqual(profil.cevir('kalite'), 'kalite')
    assert.strictEqual(profil.cevir('yok'), null)
    assert.strictEqual(profil.cevir(undefined), null)
})

test('Iris yalnız gölgelendiricili modda yüklenir', () => {
    for(const m of profil.MODLAR){
        assert.strictEqual(profil.modDurumlari(m.kimlik)['modrinth:iris'], m.kimlik === 'kalite')
    }
    assert.strictEqual(profil.modDurumlari('ultrafps')['modrinth:lambdynamiclights'], false)
    assert.strictEqual(profil.modDurumlari('normal')['modrinth:lambdynamiclights'], true)
})

test('Sodium Extra ayarı: Ultra en sade, Normal her şey açık', () => {
    const ultra = profil.sodiumExtraAyari('ultrafps')
    assert.strictEqual(ultra.animation_settings.water, false)
    assert.strictEqual(ultra.detail_settings.rain_snow, false)
    assert.strictEqual(ultra.detail_settings.sky, true)
    const fps = profil.sodiumExtraAyari('fps')
    assert.strictEqual(fps.animation_settings.water, true)
    assert.strictEqual(fps.animation_settings.block_animations, false)
    for(const kimlik of ['normal', 'kalite']){
        for(const bolum of Object.values(profil.sodiumExtraAyari(kimlik))){
            for(const [anahtar, deger] of Object.entries(bolum)){
                assert.strictEqual(deger, true, `${kimlik}.${anahtar}`)
            }
        }
    }
})

test('birleştirme öteki alanlara dokunmaz', () => {
    const hedef = { render_settings: { item_frame: false }, detail_settings: { sky: false, biome_colors: false } }
    profil.birlestir(hedef, { detail_settings: { sky: true } })
    assert.deepStrictEqual(hedef, { render_settings: { item_frame: false }, detail_settings: { sky: true, biome_colors: false } })
})

test('iris.properties: öteki satırlar korunur', () => {
    const eski = '#Iris\nmaxShadowRenderDistance=24\nshaderPack=Eski.zip\nenableShaders=false\n'
    assert.strictEqual(profil.irisOzellikleri(eski, 'Yeni.zip'), '#Iris\nmaxShadowRenderDistance=24\nenableShaders=true\nshaderPack=Yeni.zip\n')
    assert.strictEqual(profil.irisOzellikleri(eski, null), '#Iris\nmaxShadowRenderDistance=24\nshaderPack=Eski.zip\nenableShaders=false\n')
    assert.strictEqual(profil.irisOzellikleri('', null), 'enableShaders=false\n')
})

test('seçim yazılır ve okunur', () => {
    const dizin = geciciDizin()
    assert.strictEqual(profil.oku(dizin), null)
    profil.yaz(dizin, 'fps')
    assert.strictEqual(profil.oku(dizin), 'fps')
    // HapiClient modunun yazdığı biçim de okunur.
    fs.writeFileSync(path.join(dizin, 'hapiclient', 'profil.json'), '{"profil":"ultrafps"}\n')
    assert.strictEqual(profil.oku(dizin), 'ultrafps')
})

test('uygulama: yalnız mod değişince yazar, gölgelendiriciyi yalnız Yüksek Kalite açar', () => {
    const dizin = geciciDizin()
    fs.mkdirSync(path.join(dizin, 'shaderpacks'))
    fs.writeFileSync(path.join(dizin, 'shaderpacks', 'ComplementaryReimagined_r5.10.zip'), 'x')
    fs.mkdirSync(path.join(dizin, 'config'))
    const sodiumExtra = path.join(dizin, 'config', 'sodium-extra-options.json')
    fs.writeFileSync(sodiumExtra, JSON.stringify({ extra_settings: { show_fps: true } }))

    assert.deepStrictEqual(profil.uygula(dizin, 'kalite'), { 'modrinth:iris': true, 'modrinth:lambdynamiclights': true })
    const iris = path.join(dizin, 'config', 'iris.properties')
    assert.match(fs.readFileSync(iris, 'utf-8'), /enableShaders=true\nshaderPack=ComplementaryReimagined_r5\.10\.zip\n/)
    assert.strictEqual(JSON.parse(fs.readFileSync(sodiumExtra, 'utf-8')).extra_settings.show_fps, true)

    // Aynı mod yeniden uygulanmaz; oyuncunun elle yaptığı ayar korunur.
    fs.writeFileSync(iris, 'enableShaders=false\n')
    assert.strictEqual(profil.uygula(dizin, 'kalite'), null)
    assert.strictEqual(fs.readFileSync(iris, 'utf-8'), 'enableShaders=false\n')

    assert.deepStrictEqual(profil.uygula(dizin, 'ultrafps'), { 'modrinth:iris': false, 'modrinth:lambdynamiclights': false })
    assert.strictEqual(fs.readFileSync(iris, 'utf-8'), 'enableShaders=false\n')
    const ayar = JSON.parse(fs.readFileSync(sodiumExtra, 'utf-8'))
    assert.strictEqual(ayar.animation_settings.water, false)
    assert.strictEqual(ayar.extra_settings.show_fps, true)
})

test('gölgelendirici dosyası yoksa Yüksek Kalite gölgelendiricisiz açılır', () => {
    const dizin = geciciDizin()
    assert.notStrictEqual(profil.uygula(dizin, 'kalite'), null)
    assert.strictEqual(fs.readFileSync(path.join(dizin, 'config', 'iris.properties'), 'utf-8'), 'enableShaders=false\n')
    // Dosya inince bir sonraki açılışta gölgelendirici açılır.
    fs.mkdirSync(path.join(dizin, 'shaderpacks'))
    fs.writeFileSync(path.join(dizin, 'shaderpacks', 'ComplementaryReimagined_r5.10.zip'), 'x')
    assert.notStrictEqual(profil.uygula(dizin, 'kalite'), null)
    assert.match(fs.readFileSync(path.join(dizin, 'config', 'iris.properties'), 'utf-8'), /enableShaders=true/)
})
