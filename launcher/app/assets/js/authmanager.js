/**
 * AuthManager
 *
 * Giriş işlemlerinin tek kapısı. HapiCraft çevrimdışı (offline-mode) bir ağ olduğu için
 * hesap yalnız takma addan oluşur; UUID, sunucunun hesaplayacağı çevrimdışı UUID ile aynıdır.
 *
 * Faz 3 (şifre + API doğrulaması) için genişleme noktası `kimlikDogrula` fonksiyonudur:
 * arayüz ve ConfigManager yalnız bu modülün döndürdüğü hesap nesnesini kullanır.
 *
 * @module authmanager
 */
// Requirements
const ConfigManager = require('./configmanager')
const { LoggerUtil } = require('helios-core')
const Lang = require('./langloader')
const { gecerliTakmaAd, cevrimdisiUuid } = require('./hapi/cevrimdisi')

const log = LoggerUtil.getLogger('AuthManager')

// Çevrimdışı oturumda Mojang oturum sunucusu kullanılmaz; oyun yalnız boş olmayan bir değer ister.
const SAHTE_ACCESS_TOKEN = 'hapiclient-cevrimdisi'

/**
 * Kimlik doğrulama. Şimdilik yalnız takma adın biçimini denetler.
 * Faz 3'te burada `sifre` ile HapiCraft API'sine istek atılacak ve dönen oturum
 * anahtarı `accessToken` olarak kullanılacak.
 *
 * @param {string} takmaAd Takma ad.
 * @param {string|null} _sifre Faz 3'e kadar kullanılmaz.
 * @returns {Promise.<{uuid: string, accessToken: string, displayName: string}>}
 */
async function kimlikDogrula(takmaAd, _sifre = null){
    if(!gecerliTakmaAd(takmaAd)){
        return Promise.reject({
            title: Lang.queryJS('auth.error.invalidNameTitle'),
            desc: Lang.queryJS('auth.error.invalidNameDesc')
        })
    }
    return {
        uuid: cevrimdisiUuid(takmaAd),
        accessToken: SAHTE_ACCESS_TOKEN,
        displayName: takmaAd
    }
}

/**
 * Takma adla hesap ekler ve seçili hesap yapar.
 *
 * @param {string} takmaAd Takma ad (3–16 karakter, [A-Za-z0-9_]).
 * @param {string|null} sifre Faz 3'e kadar kullanılmaz.
 * @returns {Promise.<Object>} Eklenen hesap nesnesi.
 */
exports.addOfflineAccount = async function(takmaAd, sifre = null){
    const oturum = await kimlikDogrula(takmaAd, sifre)
    const ret = ConfigManager.addOfflineAuthAccount(oturum.uuid, oturum.accessToken, oturum.displayName)
    ConfigManager.save()
    log.info(`Hesap eklendi: ${oturum.displayName} (${oturum.uuid})`)
    return ret
}

/**
 * Hesabı kaldırır.
 *
 * @param {string} uuid Kaldırılacak hesabın UUID'si.
 * @returns {Promise.<void>}
 */
exports.removeAccount = async function(uuid){
    ConfigManager.removeAuthAccount(uuid)
    ConfigManager.save()
}

/**
 * Seçili hesabı doğrular. Çevrimdışı hesapta süresi dolan bir oturum yoktur.
 * Faz 3'te oturum anahtarı burada yenilenecek.
 *
 * @returns {Promise.<boolean>} Hesap kullanılabilir ise true.
 */
exports.validateSelected = async function(){
    return ConfigManager.getSelectedAccount() != null
}
