/**
 * Hesap şifresinin diskte saklanması. Şifre, işletim sisteminin anahtar kasasıyla
 * (Windows DPAPI, macOS Anahtar Zinciri, Linux libsecret) şifrelenir. Kasa kullanılamıyorsa
 * şifre saklanmaz; oyuncu şifresini eskisi gibi oyunda yazar.
 *
 * `kasa` parametresi Electron'un safeStorage nesnesidir (testte sahtesi verilir).
 */
const SIFRE_REGEX = /^\S{5,30}$/

/**
 * @param {string} sifre Şifre.
 * @returns {boolean} 5–30 karakter ve boşluksuz ise true (sunucunun şifre sınırları).
 */
exports.gecerliSifre = function(sifre){
    return typeof sifre === 'string' && SIFRE_REGEX.test(sifre)
}

/**
 * @returns {string|null} Saklanacak metin; kasa yoksa null.
 */
exports.sakla = function(kasa, sifre){
    if(!kasa || !kasa.isEncryptionAvailable()){
        return null
    }
    return kasa.encryptString(sifre).toString('base64')
}

/**
 * @returns {string|null} Açık şifre; saklanmamışsa ya da çözülemiyorsa null.
 */
exports.coz = function(kasa, sakli){
    if(!sakli || !kasa || !kasa.isEncryptionAvailable()){
        return null
    }
    try {
        return kasa.decryptString(Buffer.from(sakli, 'base64'))
    } catch(_err) {
        return null
    }
}
