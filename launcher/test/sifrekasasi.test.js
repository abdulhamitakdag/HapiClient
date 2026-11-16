const test = require('node:test')
const assert = require('node:assert')
const { gecerliSifre, sakla, coz } = require('../app/assets/js/hapi/sifrekasasi')

// safeStorage sahtesi: baytları ters çevirir.
const kasa = {
    isEncryptionAvailable: () => true,
    encryptString: (s) => Buffer.from(s, 'utf8').reverse(),
    decryptString: (b) => Buffer.from(b).reverse().toString('utf8')
}
const kasasiz = { isEncryptionAvailable: () => false }

test('şifre 5–30 karakter ve boşluksuz olmalı', () => {
    assert.ok(gecerliSifre('12345'))
    assert.ok(gecerliSifre('a'.repeat(30)))
    assert.ok(!gecerliSifre('1234'))
    assert.ok(!gecerliSifre('a'.repeat(31)))
    assert.ok(!gecerliSifre('abc def'))
    assert.ok(!gecerliSifre(null))
})

test('saklanan şifre geri çözülür ve açık metin olarak durmaz', () => {
    const sakli = sakla(kasa, 'gizliŞifre1')
    assert.notStrictEqual(sakli, 'gizliŞifre1')
    assert.strictEqual(coz(kasa, sakli), 'gizliŞifre1')
})

test('kasa yoksa şifre saklanmaz', () => {
    assert.strictEqual(sakla(kasasiz, 'gizli123'), null)
    assert.strictEqual(coz(kasasiz, 'abc'), null)
    assert.strictEqual(coz(kasa, null), null)
})
