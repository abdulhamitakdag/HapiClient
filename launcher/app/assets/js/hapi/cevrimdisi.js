/**
 * Çevrimdışı (takma adla) hesap yardımcıları. Saf fonksiyonlar; Electron'a bağımlı değildir.
 */
const crypto = require('crypto')

const TAKMA_AD_REGEX = /^[A-Za-z0-9_]{3,16}$/

/**
 * @param {string} ad Takma ad.
 * @returns {boolean} 3–16 karakter ve yalnız [A-Za-z0-9_] ise true.
 */
exports.gecerliTakmaAd = function(ad){
    return typeof ad === 'string' && TAKMA_AD_REGEX.test(ad)
}

/**
 * Minecraft'ın çevrimdışı UUID'si. Java'daki
 * UUID.nameUUIDFromBytes(("OfflinePlayer:" + ad).getBytes(UTF_8)) ile birebir aynı sonucu verir
 * (MD5 tabanlı, sürüm 3).
 *
 * @param {string} ad Takma ad.
 * @returns {string} Tireli, küçük harfli UUID.
 */
exports.cevrimdisiUuid = function(ad){
    const md5 = crypto.createHash('md5').update('OfflinePlayer:' + ad, 'utf8').digest()
    md5[6] = (md5[6] & 0x0f) | 0x30 // sürüm 3
    md5[8] = (md5[8] & 0x3f) | 0x80 // IETF varyantı
    const hex = md5.toString('hex')
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}
