const test = require('node:test')
const assert = require('node:assert')
const fs = require('fs')
const os = require('os')
const path = require('path')
const { oyunuTurkceYap } = require('../app/assets/js/hapi/dil')

function gecici(){
    return fs.mkdtempSync(path.join(os.tmpdir(), 'hapiclient-dil-'))
}

test('options.txt yoksa Türkçe olarak oluşturur', () => {
    const d = gecici()
    assert.strictEqual(oyunuTurkceYap(d), true)
    assert.strictEqual(fs.readFileSync(path.join(d, 'options.txt'), 'utf8'), 'lang:tr_tr\n')
    fs.rmSync(d, { recursive: true })
})

test('var olan dili Türkçeye çevirir, diğer ayarlara dokunmaz', () => {
    const d = gecici()
    fs.writeFileSync(path.join(d, 'options.txt'), 'fov:0.5\r\nlang:en_us\r\nmaxFps:120\r\n')
    assert.strictEqual(oyunuTurkceYap(d), true)
    assert.strictEqual(fs.readFileSync(path.join(d, 'options.txt'), 'utf8'), 'fov:0.5\nmaxFps:120\nlang:tr_tr\n')
    fs.rmSync(d, { recursive: true })
})

test('yalnız bir kez ayarlar; oyuncunun sonraki tercihi korunur', () => {
    const d = gecici()
    oyunuTurkceYap(d)
    fs.writeFileSync(path.join(d, 'options.txt'), 'lang:en_us\n')
    assert.strictEqual(oyunuTurkceYap(d), false)
    assert.strictEqual(fs.readFileSync(path.join(d, 'options.txt'), 'utf8'), 'lang:en_us\n')
    fs.rmSync(d, { recursive: true })
})
