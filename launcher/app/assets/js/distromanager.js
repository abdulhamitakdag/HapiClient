const { DistributionAPI } = require('helios-core/common')
const path = require('path')

const ConfigManager = require('./configmanager')

// Dağıtım dosyasının adresi. Başka hiçbir yerde tekrarlanmaz.
exports.REMOTE_DISTRO_URL = 'https://client.hapicraft.com.tr/dagitim/distribution.json'

const api = new DistributionAPI(
    ConfigManager.getLauncherDirectory(),
    null, // Injected forcefully by the preloader.
    null, // Injected forcefully by the preloader.
    exports.REMOTE_DISTRO_URL,
    false
)

// Geliştirme: HAPICLIENT_DAGITIM_DOSYASI ortam değişkeni bir dosya yolu gösteriyorsa
// dağıtım uzak adres yerine o yerel dosyadan okunur.
if(process.env.HAPICLIENT_DAGITIM_DOSYASI){
    api.distroDevPath = path.resolve(process.env.HAPICLIENT_DAGITIM_DOSYASI)
    api.toggleDevMode(true)
}

exports.DistroAPI = api
