/**
 * AuthManager
 *
 * Giriş işlemlerinin tek kapısı. HapiCraft çevrimdışı (offline-mode) bir ağ olduğu için
 * hesap takma ad ve sunucu şifresinden oluşur; UUID, sunucunun hesaplayacağı çevrimdışı UUID ile
 * aynıdır. Şifre burada doğrulanmaz: işletim sisteminin anahtar kasasıyla şifrelenip saklanır ve
 * oyuna girişte mod tarafından sunucunun giriş penceresine yazılır.
 *
 * @module authmanager
 */
// Requirements
const ConfigManager = require('./configmanager')
const { LoggerUtil } = require('helios-core')
const Lang = require('./langloader')
const { gecerliTakmaAd, cevrimdisiUuid } = require('./hapi/cevrimdisi')
const { gecerliSifre, sakla, coz } = require('./hapi/sifrekasasi')

function kasa(){
    return require('@electron/remote').safeStorage
}

const log = LoggerUtil.getLogger('AuthManager')

// Çevrimdışı oturumda Mojang oturum sunucusu kullanılmaz; oyun yalnız boş olmayan bir değer ister.
const SAHTE_ACCESS_TOKEN = 'hapiclient-cevrimdisi'

/**
 * Takma ad ve şifreyle hesap ekler ve seçili hesap yapar.
 *
 * @param {string} takmaAd Takma ad (3–16 karakter, [A-Za-z0-9_]).
 * @param {string} sifre Sunucu şifresi (5–30 karakter).
 * @returns {Promise.<Object>} Eklenen hesap nesnesi.
 */
exports.addOfflineAccount = async function(takmaAd, sifre){
    if(!gecerliTakmaAd(takmaAd)){
        return Promise.reject({
            title: Lang.queryJS('auth.error.invalidNameTitle'),
            desc: Lang.queryJS('auth.error.invalidNameDesc')
        })
    }
    if(!gecerliSifre(sifre)){
        return Promise.reject({
            title: Lang.queryJS('auth.error.invalidPasswordTitle'),
            desc: Lang.queryJS('auth.error.invalidPasswordDesc')
        })
    }
    const uuid = cevrimdisiUuid(takmaAd)
    const sakli = sakla(kasa(), sifre)
    if(sakli == null){
        log.warn('Anahtar kasası kullanılamıyor; şifre saklanmadı, oyunda sorulacak.')
    }
    const ret = ConfigManager.addOfflineAuthAccount(uuid, SAHTE_ACCESS_TOKEN, takmaAd, sakli)
    ConfigManager.save()
    log.info(`Hesap eklendi: ${takmaAd} (${uuid})`)
    return ret
}

/**
 * @param {Object} hesap ConfigManager'daki hesap nesnesi.
 * @returns {string|null} Hesabın açık şifresi; saklanmamışsa null.
 */
exports.hesapSifresi = function(hesap){
    return hesap ? coz(kasa(), hesap.sifre) : null
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
 *
 * @returns {Promise.<boolean>} Hesap kullanılabilir ise true.
 */
exports.validateSelected = async function(){
    return ConfigManager.getSelectedAccount() != null
}
