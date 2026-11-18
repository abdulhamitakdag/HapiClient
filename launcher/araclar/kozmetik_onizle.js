/**
 * Kozmetik önizlemesini ekransız çizer ve PNG olarak kaydeder (gözle denetim için).
 *
 *   node_modules/electron/dist/electron --ozone-platform=headless --no-sandbox --disable-gpu \
 *     --enable-unsafe-swiftshader araclar/kozmetik_onizle.js <çıktı.png> [sütun sayısı] [yuva ya da kimlik]
 *
 * Katalogdaki her kozmetik için bir kare çizer; kanat ve pelerinler arkadan, diğerleri önden.
 */
const { app, BrowserWindow, ipcMain } = require('electron')
const fs = require('fs')
const path = require('path')

// Electron'un kendi bayrakları da argv'de durur; betikten sonrakiler bizimdir.
const arg = process.argv.slice(process.argv.findIndex(a => a.endsWith('kozmetik_onizle.js')) + 1)
const cikti = arg[0] || 'kozmetik-onizleme.png'
const sutun = parseInt(arg[1] || '8', 10)
const filtre = arg[2] || ''

app.whenReady().then(() => {
    const pencere = new BrowserWindow({ width: 600, height: 400, frame: false, show: false,
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
    pencere.webContents.on('console-message', (o, seviye, mesaj) => console.log('[sayfa]', mesaj))
    pencere.loadFile(path.join(__dirname, 'kozmetik_onizle.html'), { query: { sutun: String(sutun), filtre } })
})
