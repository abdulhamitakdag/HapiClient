/**
 * Mod bütünlüğü: Dağıtım dosyasındaki modlar oyuna --fabric.addMods ile ortak depodan verilir;
 * oyun dizinindeki mods klasörü hiç kullanılmaz. Orada bulunan her şey yabancıdır ve oyun
 * başlamadan önce karantinaya alınır.
 */
const fs   = require('fs-extra')
const path = require('path')

/**
 * @param {string} oyunDizini Sunucunun oyun (instance) dizini.
 * @param {Date} simdi Karantina klasörünün adı için zaman.
 * @returns {string[]} Karantinaya alınan girdilerin adları.
 */
exports.yabanciModlariKarantinayaAl = function(oyunDizini, simdi = new Date()){
    const modsDizini = path.join(oyunDizini, 'mods')
    if(!fs.existsSync(modsDizini)){
        return []
    }
    const girdiler = fs.readdirSync(modsDizini)
    if(girdiler.length === 0){
        return []
    }
    const hedef = path.join(oyunDizini, 'hapiclient', 'karantina', simdi.toISOString().replace(/[:.]/g, '-'))
    fs.ensureDirSync(hedef)
    for(const girdi of girdiler){
        fs.moveSync(path.join(modsDizini, girdi), path.join(hedef, girdi), { overwrite: true })
    }
    return girdiler
}
