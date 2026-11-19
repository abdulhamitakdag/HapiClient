/**
 * Oyun modları: Ultra FPS Boost, FPS Boost, Normal, Yüksek Kalite.
 *
 * Seçim <oyun dizini>/hapiclient/profil.json dosyasında durur; HapiClient modu da aynı dosyayı okur
 * ve oyunun kendi görüntü seçeneklerini ona göre kurar. Buradaki kısım oyun açılmadan önce yapılması
 * gerekenlerdir: hangi isteğe bağlı modların yükleneceği, gölgelendirici ve Sodium Extra ayarları.
 * Electron'a bağımlı değildir.
 */
const fs = require('fs')
const path = require('path')

exports.MODLAR = [
    {
        kimlik: 'ultrafps',
        ad: 'Ultra FPS Boost',
        aciklama: 'Zayıf bilgisayarlar için. Görüntü sadeleşir, FPS en yükseğe çıkar.'
    },
    {
        kimlik: 'fps',
        ad: 'FPS Boost',
        aciklama: 'Akıcı oyun. Gereksiz efektler kapalı, görüntü hâlâ temiz.'
    },
    {
        kimlik: 'normal',
        ad: 'Normal',
        aciklama: 'Görüntü ve akıcılık dengesi.'
    },
    {
        kimlik: 'kalite',
        ad: 'Yüksek Kalite',
        aciklama: 'Complementary Reimagined gölgelendiricisi açık. Güçlü ekran kartı ister.'
    }
]

const KIMLIKLER = exports.MODLAR.map(m => m.kimlik)
const ESKI_ADLAR = { patates: 'ultrafps', dusuk: 'fps', dengeli: 'normal', yuksek: 'normal', ultra: 'kalite' }

/**
 * Bir mod kimliğini (ya da eski sürümlerin profil adını) geçerli kimliğe çevirir.
 *
 * @param {string} deger Kimlik.
 * @returns {string|null} Geçerli kimlik; tanınmıyorsa null.
 */
exports.cevir = function(deger){
    const d = String(deger || '').toLowerCase().replace('_', '')
    if(KIMLIKLER.includes(d)){
        return d
    }
    return ESKI_ADLAR[d] || null
}

/**
 * Moda göre isteğe bağlı modların açık/kapalı durumu. Iris yalnız gölgelendiricili modda yüklenir;
 * dinamik ışık FPS modlarında kapalıdır.
 *
 * @param {string} profil Mod kimliği.
 * @returns {Object.<string, boolean>} Sürümsüz mod kimliği → açık mı.
 */
exports.modDurumlari = function(profil){
    return {
        'modrinth:iris': profil === 'kalite',
        'modrinth:lambdynamiclights': profil === 'normal' || profil === 'kalite'
    }
}

/**
 * Moda göre Sodium Extra ayarları (config/sodium-extra-options.json içine birleştirilir).
 * Oynanışı etkileyen çizimlere (zırh askısı, eşya çerçevesi, isim etiketi, işaret ışını) dokunulmaz;
 * sunucu bunları hologram ve dükkân için kullanır.
 *
 * @param {string} profil Mod kimliği.
 * @returns {Object} Ayar ağacı.
 */
exports.sodiumExtraAyari = function(profil){
    const ultra = profil === 'ultrafps'
    const hafif = ultra || profil === 'fps'
    return {
        animation_settings: {
            animation: !ultra,
            water: !ultra,
            lava: !ultra,
            fire: !ultra,
            portal: !hafif,
            block_animations: !hafif,
            sculk_sensor: !hafif
        },
        particle_settings: {
            particles: true,
            rain_splash: !hafif,
            block_break: !ultra,
            block_breaking: !ultra
        },
        detail_settings: {
            sky: true,
            sun: true,
            moon: true,
            stars: !ultra,
            rain_snow: !ultra
        }
    }
}

/**
 * Kaynaktaki alanları hedefe iç içe yazar; hedefteki öteki alanlara dokunmaz.
 *
 * @param {Object} hedef Var olan ayarlar.
 * @param {Object} kaynak Yazılacak alanlar.
 * @returns {Object} hedef.
 */
exports.birlestir = function(hedef, kaynak){
    for(const [anahtar, deger] of Object.entries(kaynak)){
        if(deger != null && typeof deger === 'object' && !Array.isArray(deger)){
            if(hedef[anahtar] == null || typeof hedef[anahtar] !== 'object' || Array.isArray(hedef[anahtar])){
                hedef[anahtar] = {}
            }
            exports.birlestir(hedef[anahtar], deger)
        } else {
            hedef[anahtar] = deger
        }
    }
    return hedef
}

/**
 * iris.properties içeriğini üretir; dosyadaki öteki satırlar korunur.
 *
 * @param {string} mevcut Dosyanın şimdiki içeriği (yoksa boş).
 * @param {string|null} shader Gölgelendirici dosyasının adı; null ise gölgelendirici kapatılır.
 * @returns {string} Yeni içerik.
 */
exports.irisOzellikleri = function(mevcut, shader){
    const yeni = { enableShaders: shader ? 'true' : 'false' }
    if(shader){
        yeni.shaderPack = shader
    }
    const satirlar = []
    for(const satir of String(mevcut || '').split(/\r?\n/)){
        const m = /^\s*([A-Za-z]+)\s*=/.exec(satir)
        if(m && Object.prototype.hasOwnProperty.call(yeni, m[1])){
            continue
        }
        if(satir.trim() !== ''){
            satirlar.push(satir)
        }
    }
    for(const [anahtar, deger] of Object.entries(yeni)){
        satirlar.push(`${anahtar}=${deger}`)
    }
    return satirlar.join('\n') + '\n'
}

function jsonOku(dosya){
    try {
        return JSON.parse(fs.readFileSync(dosya, 'utf-8'))
    } catch(_err) {
        return null
    }
}

/**
 * @param {string} oyunDizini Sunucunun oyun (instance) dizini.
 * @returns {string|null} Seçili mod; hiç seçilmemişse null.
 */
exports.oku = function(oyunDizini){
    const kayit = jsonOku(path.join(oyunDizini, 'hapiclient', 'profil.json'))
    return kayit ? exports.cevir(kayit.profil) : null
}

/**
 * Seçili modu yazar. HapiClient modu oyun açılırken bu dosyayı okur.
 *
 * @param {string} oyunDizini Sunucunun oyun (instance) dizini.
 * @param {string} profil Mod kimliği.
 */
exports.yaz = function(oyunDizini, profil){
    const dizin = path.join(oyunDizini, 'hapiclient')
    fs.mkdirSync(dizin, { recursive: true })
    fs.writeFileSync(path.join(dizin, 'profil.json'), JSON.stringify({ profil }) + '\n', 'utf-8')
}

/**
 * @param {string} oyunDizini Sunucunun oyun (instance) dizini.
 * @returns {string|null} shaderpacks klasöründeki Complementary Reimagined dosyasının adı.
 */
exports.shaderBul = function(oyunDizini){
    try {
        const adlar = fs.readdirSync(path.join(oyunDizini, 'shaderpacks'))
            .filter(a => /^ComplementaryReimagined_.*\.zip$/.test(a))
            .sort()
        return adlar.length > 0 ? adlar[adlar.length - 1] : null
    } catch(_err) {
        return null
    }
}

/**
 * Seçili mod son uygulanandan farklıysa oyun dizinindeki ayar dosyalarını ona göre kurar.
 * Yalnız mod değiştiğinde yazar; oyuncunun sonradan elle yaptığı ayarlar bir sonraki mod
 * değişikliğine kadar korunur.
 *
 * @param {string} oyunDizini Sunucunun oyun (instance) dizini.
 * @param {string} profil Mod kimliği.
 * @returns {Object.<string, boolean>|null} Değişiklik yapıldıysa isteğe bağlı modların yeni durumu,
 *     yapılmadıysa null.
 */
exports.uygula = function(oyunDizini, profil){
    const isaret = path.join(oyunDizini, 'hapiclient', 'profil-uygulanan.json')
    const onceki = jsonOku(isaret)
    if(onceki && onceki.profil === profil){
        return null
    }

    const ayarDizini = path.join(oyunDizini, 'config')
    fs.mkdirSync(ayarDizini, { recursive: true })

    const sodiumExtra = path.join(ayarDizini, 'sodium-extra-options.json')
    const mevcut = jsonOku(sodiumExtra)
    const ayar = exports.birlestir(mevcut != null && typeof mevcut === 'object' ? mevcut : {}, exports.sodiumExtraAyari(profil))
    fs.writeFileSync(sodiumExtra, JSON.stringify(ayar, null, 2) + '\n', 'utf-8')

    const iris = path.join(ayarDizini, 'iris.properties')
    let irisMetni = ''
    try {
        irisMetni = fs.readFileSync(iris, 'utf-8')
    } catch(_err) {
        // Henüz yok.
    }
    const shader = profil === 'kalite' ? exports.shaderBul(oyunDizini) : null
    fs.writeFileSync(iris, exports.irisOzellikleri(irisMetni, shader), 'utf-8')

    // Gölgelendirici dosyası henüz inmediyse işaret yazılmaz; bir sonraki açılışta yeniden denenir.
    if(profil !== 'kalite' || shader != null){
        fs.mkdirSync(path.dirname(isaret), { recursive: true })
        fs.writeFileSync(isaret, JSON.stringify({ profil }) + '\n', 'utf-8')
    }
    return exports.modDurumlari(profil)
}
