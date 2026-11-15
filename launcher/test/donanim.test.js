const test = require('node:test')
const assert = require('node:assert')
const { ramOner, gpuSinifi, profilSec, gpuAdiCikar, donanimKaydi, PROFILLER } = require('../app/assets/js/hapi/donanim')

test('RAM önerisi: 4 GB → 2 GB, 8 GB → 3 GB, 16 GB+ → 4 GB', () => {
    assert.strictEqual(ramOner(4096), 2048)
    assert.strictEqual(ramOner(3900), 1792) // yarısını aşamaz
    assert.strictEqual(ramOner(8192), 3072)
    assert.strictEqual(ramOner(7800), 3072)
    assert.strictEqual(ramOner(6000), 2560)
    assert.strictEqual(ramOner(16384), 4096)
    assert.strictEqual(ramOner(15900), 4096)
    assert.strictEqual(ramOner(32768), 4096)
    assert.strictEqual(ramOner(65536), 4096)
})

test('RAM önerisi asla sistemin yarısını aşmaz', () => {
    for(let mb = 1024; mb <= 70000; mb += 111){
        const oneri = ramOner(mb)
        assert.ok(oneri <= mb / 2, `${mb} → ${oneri}`)
        assert.strictEqual(oneri % 256, 0)
    }
    assert.strictEqual(ramOner(2048), 1024)
})

test('ekran kartı sınıfı', () => {
    const beklenen = {
        'Google SwiftShader': 0,
        'llvmpipe (LLVM 15.0.7, 256 bits)': 0,
        'Microsoft Basic Render Driver': 0,
        'Intel(R) HD Graphics 4000': 0,
        'Intel(R) UHD Graphics 620': 1,
        'Intel(R) Iris(R) Xe Graphics': 1,
        'AMD Radeon(TM) Vega 8 Graphics': 1,
        'AMD Radeon(TM) Graphics': 1,
        '': 1,
        'Bilinmeyen Kart 9000': 1,
        'NVIDIA GeForce GT 710': 2,
        'NVIDIA GeForce MX150': 2,
        'NVIDIA GeForce GTX 750 Ti': 2,
        'NVIDIA GeForce GTX 1050 Ti': 2,
        'NVIDIA GeForce GTX 1650': 2,
        'AMD Radeon RX 550': 2,
        'NVIDIA GeForce GTX 970': 3,
        'NVIDIA GeForce GTX 1060 6GB': 3,
        'NVIDIA GeForce GTX 1660 SUPER': 3,
        'NVIDIA GeForce RTX 3050': 3,
        'NVIDIA GeForce RTX 4060': 3,
        'AMD Radeon RX 580 Series': 3,
        'AMD Radeon RX 6600 XT': 3,
        'Radeon RX Vega 56': 3,
        'Intel(R) Arc(TM) A750 Graphics': 3,
        'Apple M2': 3,
        'NVIDIA GeForce GTX 1080 Ti': 4,
        'NVIDIA GeForce RTX 2070 SUPER': 4,
        'NVIDIA GeForce RTX 4090': 4,
        'AMD Radeon RX 6800 XT': 4,
        'AMD Radeon RX 7900 XTX': 4
    }
    for(const [ad, sinif] of Object.entries(beklenen)){
        assert.strictEqual(gpuSinifi(ad), sinif, ad)
    }
})

test('profil seçimi', () => {
    // Eski dizüstü: 4 GB, 2 çekirdek, eski tümleşik.
    assert.strictEqual(profilSec({ cekirdek: 2, ramMB: 3900, gpu: 'Intel(R) HD Graphics 4000' }), 'patates')
    // Çok az bellek her şeyi patatese çeker.
    assert.strictEqual(profilSec({ cekirdek: 8, ramMB: 3000, gpu: 'NVIDIA GeForce RTX 3060' }), 'patates')
    // Tümleşik kart, 8 GB.
    assert.strictEqual(profilSec({ cekirdek: 4, ramMB: 7900, gpu: 'Intel(R) UHD Graphics 620' }), 'dusuk')
    // Giriş seviyesi harici kart.
    assert.strictEqual(profilSec({ cekirdek: 4, ramMB: 7900, gpu: 'NVIDIA GeForce GTX 1050 Ti' }), 'dengeli')
    // Orta kart ama 8 GB: dengeliyle sınırlı.
    assert.strictEqual(profilSec({ cekirdek: 6, ramMB: 7900, gpu: 'NVIDIA GeForce RTX 3060' }), 'dengeli')
    // Orta kart, 16 GB.
    assert.strictEqual(profilSec({ cekirdek: 6, ramMB: 16000, gpu: 'NVIDIA GeForce RTX 3060' }), 'yuksek')
    // Üst seviye kart, 32 GB, 16 çekirdek.
    assert.strictEqual(profilSec({ cekirdek: 16, ramMB: 32000, gpu: 'NVIDIA GeForce RTX 4080' }), 'ultra')
    // Üst seviye kart ama 6 çekirdek: yüksekle sınırlı.
    assert.strictEqual(profilSec({ cekirdek: 6, ramMB: 32000, gpu: 'NVIDIA GeForce RTX 4080' }), 'yuksek')
    // Güçlü kart ama 2 çekirdek: düşükle sınırlı.
    assert.strictEqual(profilSec({ cekirdek: 2, ramMB: 16000, gpu: 'NVIDIA GeForce RTX 4080' }), 'dusuk')
    // Ekran kartı bilinmiyor: tümleşik varsayılır.
    assert.strictEqual(profilSec({ cekirdek: 8, ramMB: 16000, gpu: '' }), 'dusuk')
})

test('profil her girdide geçerli bir değer döndürür', () => {
    for(const cekirdek of [1, 2, 4, 6, 8, 32]){
        for(const ramMB of [1000, 4000, 8000, 12000, 16000, 64000]){
            for(const gpu of ['', 'x', 'NVIDIA GeForce RTX 4090', 'Intel(R) HD Graphics']){
                assert.ok(PROFILLER.includes(profilSec({ cekirdek, ramMB, gpu })))
            }
        }
    }
})

test('getGPUInfo çıktısından ekran kartı adı', () => {
    assert.strictEqual(gpuAdiCikar(null), '')
    assert.strictEqual(gpuAdiCikar({}), '')
    assert.strictEqual(gpuAdiCikar({
        auxAttributes: { glRenderer: 'ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0, D3D11)' }
    }), 'NVIDIA GeForce RTX 3060')
    assert.strictEqual(gpuAdiCikar({
        auxAttributes: { glRenderer: 'ANGLE (Intel, Intel(R) UHD Graphics 620 (0x00003EA0) Direct3D11 vs_5_0 ps_5_0, D3D11)' }
    }), 'Intel(R) UHD Graphics 620')
    assert.strictEqual(gpuAdiCikar({
        auxAttributes: { glRenderer: 'Mesa Intel(R) UHD Graphics 620 (KBL GT2)' }
    }), 'Mesa Intel(R) UHD Graphics 620 (KBL GT2)')
    assert.strictEqual(gpuAdiCikar({
        gpuDevice: [{ active: false, vendorId: 0x8086, deviceId: 1 }, { active: true, vendorId: 0x10de, deviceId: 2 }]
    }), 'NVIDIA')
    assert.strictEqual(gpuAdiCikar({
        gpuDevice: [{ active: true, vendorId: 0x10de, deviceString: 'NVIDIA GeForce GTX 1650' }]
    }), 'NVIDIA GeForce GTX 1650')
})

test('donanim.json kaydı', () => {
    const kayit = donanimKaydi({
        cpu: 'AMD Ryzen 5 5600', cekirdek: 12, ramMB: 16000, gpu: 'NVIDIA GeForce RTX 3060', ekran: '1920x1080'
    }, new Date('2026-11-16T10:00:00Z'))
    assert.deepStrictEqual(kayit, {
        cpu: 'AMD Ryzen 5 5600',
        cekirdek: 12,
        ramMB: 16000,
        gpu: 'NVIDIA GeForce RTX 3060',
        ekran: '1920x1080',
        profil: 'yuksek',
        javaRamMB: 4096,
        tarih: '2026-11-16T10:00:00.000Z'
    })
})
