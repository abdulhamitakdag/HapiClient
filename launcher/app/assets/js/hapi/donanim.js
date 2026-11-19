/**
 * Donanım taraması için saf mantık: RAM önerisi, performans profili ve donanim.json kaydı.
 * Electron'a bağımlı değildir; ham ölçümler ana süreçten (index.js) gelir.
 */

exports.PROFILLER = ['ultrafps', 'fps', 'normal', 'kalite']

/**
 * Java'ya ayrılacak RAM önerisi (MB).
 * 4 GB sistem → 2 GB, 8 GB → 3 GB, 16 GB+ → 4 GB; asla sistemin yarısından fazla değil.
 * Eşikler, işletim sisteminin bildirdiği değer etiket değerinden biraz düşük olduğu için paylıdır.
 *
 * @param {number} toplamMB Sistemin toplam belleği (MB).
 * @returns {number} Önerilen -Xmx değeri (MB, 256'nın katı).
 */
exports.ramOner = function(toplamMB){
    let oneri
    if(toplamMB >= 15000){
        oneri = 4096
    } else if(toplamMB >= 7000){
        oneri = 3072
    } else if(toplamMB >= 5500){
        oneri = 2560
    } else {
        oneri = 2048
    }
    const yarisi = Math.floor(toplamMB / 2 / 256) * 256
    return Math.max(512, Math.min(oneri, yarisi))
}

/**
 * Ekran kartı adından kaba bir güç sınıfı çıkarır.
 * 0 = yazılımsal/çok eski tümleşik, 1 = tümleşik, 2 = giriş seviyesi harici,
 * 3 = orta, 4 = üst seviye.
 *
 * @param {string} gpu Ekran kartı adı.
 * @returns {number} 0–4 arası sınıf.
 */
exports.gpuSinifi = function(gpu){
    const ad = (gpu || '').toLowerCase()
    if(!ad){
        return 1
    }
    if(/swiftshader|llvmpipe|softpipe|basic render|software|vmware|virtualbox|parallels/.test(ad)){
        return 0
    }

    // NVIDIA
    let m = /(rtx|gtx)\s*(\d{3,4})/.exec(ad)
    if(m){
        const no = Number(m[2])
        const seviye = no % 100 // 3060 → 60, 1050 → 50, 970 → 70
        if(m[1] === 'rtx'){
            return seviye >= 70 ? 4 : 3
        }
        if(no >= 1000){
            return seviye >= 80 ? 4 : (seviye >= 60 ? 3 : 2)
        }
        return seviye >= 70 ? 3 : 2
    }
    if(/geforce|quadro|nvidia/.test(ad)){
        return 2 // GT, MX ve eski seriler
    }

    // Intel
    if(/\barc\b/.test(ad)){
        return 3
    }
    if(/iris/.test(ad)){
        return 1
    }
    if(/intel/.test(ad)){
        return /uhd|xe/.test(ad) ? 1 : 0 // HD Graphics, GMA vb. eski tümleşikler
    }

    // AMD
    if(/rx vega/.test(ad)){
        return 3
    }
    m = /rx\s*(\d{3,4})/.exec(ad)
    if(m){
        const no = Number(m[1])
        if(no >= 5000){
            const seviye = no % 1000 // 6700 → 700
            return seviye >= 800 ? 4 : (seviye >= 600 ? 3 : 2)
        }
        return no % 100 >= 70 ? 3 : 2 // RX 570/580/590 orta, 550/560 giriş
    }
    if(/radeon|amd|ati/.test(ad)){
        return 1 // Vega / Radeon Graphics (APU) ve eski kartlar
    }

    // Apple
    if(/apple m\d/.test(ad)){
        return 3
    }

    return 1
}

/**
 * Donanıma göre önerilen oyun modunu seçer. Saf fonksiyondur.
 *
 * Yüksek Kalite (gölgelendirici) yalnız her yönden güçlü sisteme önerilir: üst seviye ekran kartı,
 * 16 GB bellek ve 6'dan fazla iş parçacığı. Yazılımsal ya da çok eski ekran kartı, 4 GB'tan az
 * bellek, iki çekirdek veya tümleşik ekran kartı + 8 GB'tan az bellek Ultra FPS Boost'a gider.
 *
 * @param {{cekirdek: number, ramMB: number, gpu: string}} donanim
 * @returns {'ultrafps'|'fps'|'normal'|'kalite'}
 */
exports.profilSec = function({ cekirdek, ramMB, gpu }){
    const ekran = exports.gpuSinifi(gpu)

    if(ekran === 0 || ramMB < 3500 || cekirdek <= 2 || (ekran === 1 && ramMB < 7000)){
        return 'ultrafps'
    }
    if(ekran === 1 || ramMB < 7000 || cekirdek <= 4){
        return 'fps'
    }
    if(ekran === 4 && ramMB >= 15000 && cekirdek > 6){
        return 'kalite'
    }
    return 'normal'
}

/**
 * Ana süreçten gelen app.getGPUInfo sonucundan okunur bir ekran kartı adı çıkarır.
 *
 * @param {Object} gpuBilgisi app.getGPUInfo('complete') sonucu (null olabilir).
 * @returns {string} Ekran kartı adı; bulunamazsa boş dize.
 */
exports.gpuAdiCikar = function(gpuBilgisi){
    if(gpuBilgisi == null){
        return ''
    }
    const renderer = gpuBilgisi.auxAttributes?.glRenderer
    if(renderer){
        // Örn. "ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0, D3D11)"
        const m = /^ANGLE \([^,]*,\s*([^,]+?)(?:\s+Direct3D.*|\s+\(0x[0-9a-fA-F]+\).*)?(?:,.*)?\)$/.exec(renderer)
        return (m ? m[1] : renderer).trim()
    }
    const aygitlar = gpuBilgisi.gpuDevice || []
    const aygit = aygitlar.find(a => a.active) || aygitlar[0]
    if(aygit == null){
        return ''
    }
    if(aygit.deviceString){
        return aygit.deviceString
    }
    const ureticiler = { 0x10de: 'NVIDIA', 0x1002: 'AMD', 0x1022: 'AMD', 0x8086: 'Intel', 0x106b: 'Apple' }
    return ureticiler[aygit.vendorId] || ''
}

/**
 * Ham ölçümlerden donanim.json içeriğini üretir.
 *
 * @param {{cpu: string, cekirdek: number, ramMB: number, gpu: string, ekran?: string}} ham
 * @param {Date} tarih Kayıt zamanı.
 * @returns {Object} Modun okuyacağı kayıt.
 */
exports.donanimKaydi = function(ham, tarih = new Date()){
    return {
        cpu: ham.cpu,
        cekirdek: ham.cekirdek,
        ramMB: ham.ramMB,
        gpu: ham.gpu,
        ekran: ham.ekran || '',
        profil: exports.profilSec(ham),
        javaRamMB: exports.ramOner(ham.ramMB),
        tarih: tarih.toISOString()
    }
}
