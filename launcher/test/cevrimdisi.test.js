const test = require('node:test')
const assert = require('node:assert')
const { gecerliTakmaAd, cevrimdisiUuid } = require('../app/assets/js/hapi/cevrimdisi')

test('çevrimdışı UUID, Java UUID.nameUUIDFromBytes ile aynıdır', () => {
    // Beklenen değerler Java'da UUID.nameUUIDFromBytes(("OfflinePlayer:" + ad).getBytes(UTF_8)) çıktısıdır.
    assert.strictEqual(cevrimdisiUuid('Notch'), 'b50ad385-829d-3141-a216-7e7d7539ba7f')
    assert.strictEqual(cevrimdisiUuid('jeb_'), 'a762f560-4fce-3236-812a-b80efff0b62b')
    assert.strictEqual(cevrimdisiUuid('Hapi_Kurbaga16'), '1809f2a5-ebe2-33aa-9c0b-44a162701eaa')
})

test('UUID sürüm 3 ve IETF varyantıdır', () => {
    for(const ad of ['abc', 'A_1', 'x'.repeat(16)]){
        const uuid = cevrimdisiUuid(ad)
        assert.match(uuid, /^[0-9a-f]{8}-[0-9a-f]{4}-3[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
    }
})

test('büyük/küçük harf farklı UUID üretir', () => {
    assert.notStrictEqual(cevrimdisiUuid('notch'), cevrimdisiUuid('Notch'))
})

test('takma ad doğrulaması', () => {
    for(const ad of ['abc', 'Notch', 'a_b_c', '0123456789abcdef', '___']){
        assert.ok(gecerliTakmaAd(ad), ad)
    }
    for(const ad of ['', 'ab', '0123456789abcdefg', 'çılgın', 'a b', 'a-b', 'ad!', ' abc', 'abc\n', null, undefined, 123]){
        assert.ok(!gecerliTakmaAd(ad), String(ad))
    }
})
