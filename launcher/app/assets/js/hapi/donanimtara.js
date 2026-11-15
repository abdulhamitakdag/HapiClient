/**
 * Donanım taraması (ana süreç). Ham ölçümleri toplar; yorumlama donanim.js içindedir.
 */
const { app, screen } = require('electron')
const { execFile }    = require('child_process')
const os              = require('os')

const { gpuAdiCikar, gpuSinifi } = require('./donanim')

/**
 * Windows'ta ekran kartı adlarını işletim sisteminden sorar. Launcher donanım hızlandırmayı
 * kapattığı için Chromium çoğu zaman yalnız yazılımsal çiziciyi bildirir.
 *
 * @returns {Promise.<string[]>} Ekran kartı adları; sorgu başarısızsa boş dizi.
 */
function windowsGpuAdlari(){
    return new Promise(resolve => {
        execFile('powershell.exe', [
            '-NoProfile', '-NonInteractive', '-Command',
            '(Get-CimInstance Win32_VideoController).Name'
        ], { timeout: 8000, windowsHide: true }, (err, stdout) => {
            if(err){
                resolve([])
            } else {
                resolve(stdout.split(/\r?\n/).map(s => s.trim()).filter(s => s.length > 0))
            }
        })
    })
}

/**
 * @returns {Promise.<{cpu: string, cekirdek: number, ramMB: number, gpu: string, ekran: string}>}
 */
exports.donanimTara = async function(){
    const islemciler = os.cpus()

    let gpuBilgisi = null
    try {
        gpuBilgisi = await app.getGPUInfo('complete')
    } catch(_err) {
        try {
            gpuBilgisi = await app.getGPUInfo('basic')
        } catch(_err2) {
            // Ekran kartı bilinmiyor olarak devam edilir.
        }
    }
    let gpu = gpuAdiCikar(gpuBilgisi)

    if(process.platform === 'win32' && gpuSinifi(gpu) <= 1){
        const adlar = await windowsGpuAdlari()
        if(adlar.length > 0){
            // Birden çok kart varsa (tümleşik + harici) en güçlüsü esas alınır.
            gpu = adlar.sort((a, b) => gpuSinifi(b) - gpuSinifi(a))[0]
        }
    }

    const birincil = screen.getPrimaryDisplay()
    const genislik = Math.round(birincil.size.width * birincil.scaleFactor)
    const yukseklik = Math.round(birincil.size.height * birincil.scaleFactor)

    return {
        cpu: islemciler.length > 0 ? islemciler[0].model.trim() : '',
        cekirdek: islemciler.length,
        ramMB: Math.round(os.totalmem() / 1048576),
        gpu,
        ekran: `${genislik}x${yukseklik}`
    }
}
