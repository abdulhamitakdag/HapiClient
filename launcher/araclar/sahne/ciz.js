/**
 * Launcher arka planını çizer: oyunun kendi dokularından kurulan bir manzara ve HapiClient kozmetikleri
 * takmış oyuncular. Ekransız çalışır, sonucu PNG olarak yazar.
 *
 *   node_modules/electron/dist/electron --ozone-platform=headless --no-sandbox --disable-gpu \
 *     --enable-unsafe-swiftshader araclar/sahne/ciz.js <çıktı.png> <minecraft jar'ından açılmış dizin> [genişlik] [yükseklik]
 *
 * Doku dizini: client jar'ındaki `assets/minecraft/textures` (block/, entity/, environment/ yeter).
 */
const { app, BrowserWindow, ipcMain } = require('electron')
const fs = require('fs')
const path = require('path')

const arg = process.argv.slice(process.argv.findIndex(a => a.endsWith('ciz.js')) + 1)
const [cikti, dokular, genislik = '1960', yukseklik = '1104'] = arg

app.whenReady().then(() => {
    // frame: false olmazsa ekransız ortamda pencere açılırken çöker.
    const pencere = new BrowserWindow({ width: 400, height: 300, show: false, frame: false,
        webPreferences: { nodeIntegration: true, contextIsolation: false, backgroundThrottling: false } })
    ipcMain.on('bitti', (olay, veri) => {
        fs.writeFileSync(cikti, Buffer.from(veri.split(',')[1], 'base64'))
        console.log('yazıldı:', cikti)
        app.quit()
    })
    ipcMain.on('hata', (olay, mesaj) => {
        console.error('HATA:', mesaj)
        app.exit(1)
    })
    pencere.loadFile(path.join(__dirname, 'sahne.html'), { query: { dokular: path.resolve(dokular), g: genislik, y: yukseklik } })
})
