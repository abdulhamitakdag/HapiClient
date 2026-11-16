/**
 * Oyun dili: HapiClient ilk açılışta oyunu Türkçe başlatır. Dil bir kez ayarlanır; oyuncu
 * sonradan oyunun içinden değiştirirse tercihi korunur.
 */
const fs   = require('fs-extra')
const path = require('path')

const ISARET = path.join('hapiclient', 'dil-ayarlandi')

/**
 * @param {string} oyunDizini Sunucunun oyun (instance) dizini.
 * @returns {boolean} options.txt bu çağrıda Türkçeye çevrildiyse true.
 */
exports.oyunuTurkceYap = function(oyunDizini){
    const isaret = path.join(oyunDizini, ISARET)
    if(fs.existsSync(isaret)){
        return false
    }
    const secenekler = path.join(oyunDizini, 'options.txt')
    const satirlar = fs.existsSync(secenekler)
        ? fs.readFileSync(secenekler, 'utf8').split(/\r?\n/).filter(s => s !== '' && !s.startsWith('lang:'))
        : []
    satirlar.push('lang:tr_tr')
    fs.writeFileSync(secenekler, satirlar.join('\n') + '\n', 'utf8')
    fs.ensureDirSync(path.dirname(isaret))
    fs.writeFileSync(isaret, '', 'utf8')
    return true
}
