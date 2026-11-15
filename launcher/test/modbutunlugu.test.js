const test = require('node:test')
const assert = require('node:assert')
const fs = require('fs')
const os = require('os')
const path = require('path')
const { yabanciModlariKarantinayaAl } = require('../app/assets/js/hapi/modbutunlugu')

function geciciOyunDizini(){
    return fs.mkdtempSync(path.join(os.tmpdir(), 'hapiclient-test-'))
}

test('mods klasörü yoksa hiçbir şey yapmaz', () => {
    const dizin = geciciOyunDizini()
    assert.deepStrictEqual(yabanciModlariKarantinayaAl(dizin), [])
    assert.ok(!fs.existsSync(path.join(dizin, 'hapiclient')))
    fs.rmSync(dizin, { recursive: true })
})

test('mods klasörü boşsa karantina klasörü açmaz', () => {
    const dizin = geciciOyunDizini()
    fs.mkdirSync(path.join(dizin, 'mods'))
    assert.deepStrictEqual(yabanciModlariKarantinayaAl(dizin), [])
    assert.ok(!fs.existsSync(path.join(dizin, 'hapiclient')))
    fs.rmSync(dizin, { recursive: true })
})

test('mods klasöründeki her şeyi karantinaya taşır', () => {
    const dizin = geciciOyunDizini()
    const mods = path.join(dizin, 'mods')
    fs.mkdirSync(path.join(mods, '1.21.11'), { recursive: true })
    fs.writeFileSync(path.join(mods, 'hile.jar'), 'x')
    fs.writeFileSync(path.join(mods, 'kapali.jar.disabled'), 'y')
    fs.writeFileSync(path.join(mods, '1.21.11', 'gizli.jar'), 'z')

    const tasinan = yabanciModlariKarantinayaAl(dizin, new Date('2026-11-16T10:00:00Z'))
    assert.deepStrictEqual(tasinan.sort(), ['1.21.11', 'hile.jar', 'kapali.jar.disabled'])
    assert.deepStrictEqual(fs.readdirSync(mods), [])

    const karantina = path.join(dizin, 'hapiclient', 'karantina', '2026-11-16T10-00-00-000Z')
    assert.strictEqual(fs.readFileSync(path.join(karantina, 'hile.jar'), 'utf8'), 'x')
    assert.strictEqual(fs.readFileSync(path.join(karantina, '1.21.11', 'gizli.jar'), 'utf8'), 'z')
    fs.rmSync(dizin, { recursive: true })
})
