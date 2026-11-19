/* global document, Image */
/**
 * Launcher arka plan sahnesi. Blok birimi 1; y yukarı. Kamera +z'den -z'ye bakar.
 * Bloklar oyundaki gibi ışıksız çizilir: yüz yönüne göre sabit gölge + köşe karartması.
 */
const path  = require('path')
const THREE = require('three')
const K     = require('./kozmetik')

let DOKULAR = ''
const SU = 6        // su yüzeyi yüksekliği
const PX = 0.058    // oyuncu modelinde bir pikselin blok karşılığı (1,86 blok boy)

/* ---------- yardımcılar ---------- */

function resim(dosya){
    return new Promise((coz, red) => {
        const r = new Image()
        r.onload = () => coz(r)
        r.onerror = () => red(new Error('yüklenemedi: ' + dosya))
        r.src = 'file://' + dosya
    })
}

/** Resmi tuvale alır; renk verilirse çarpar (çim/yaprak gibi gri dokular için), kare verilirse ilk kareyi keser. */
function tuval(r, renk, kare){
    const c = document.createElement('canvas')
    c.width = r.width
    c.height = kare ? r.width : r.height
    const x = c.getContext('2d')
    x.drawImage(r, 0, 0)
    if(renk){
        const v = x.getImageData(0, 0, c.width, c.height)
        for(let i = 0; i < v.data.length; i += 4){
            v.data[i] = v.data[i] * renk[0] / 255
            v.data[i + 1] = v.data[i + 1] * renk[1] / 255
            v.data[i + 2] = v.data[i + 2] * renk[2] / 255
        }
        x.putImageData(v, 0, 0)
    }
    return c
}

function doku(kaynak, mip){
    const d = new THREE.CanvasTexture(kaynak)
    d.magFilter = THREE.NearestFilter
    d.minFilter = mip ? THREE.NearestMipmapLinearFilter : THREE.NearestFilter
    d.generateMipmaps = !!mip
    d.colorSpace = THREE.SRGBColorSpace
    return d
}

/** Tekrarlanabilir gürültü. */
function rast(x, z){
    const s = Math.sin(x * 127.1 + z * 311.7) * 43758.5453
    return s - Math.floor(s)
}
function gurultu(x, z){
    const xi = Math.floor(x), zi = Math.floor(z), fx = x - xi, fz = z - zi
    const u = fx * fx * (3 - 2 * fx), v = fz * fz * (3 - 2 * fz)
    const a = rast(xi, zi), b = rast(xi + 1, zi), c = rast(xi, zi + 1), d = rast(xi + 1, zi + 1)
    return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v
}
const yumusak = (a, b, x) => {
    const t = Math.max(0, Math.min(1, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)
}

/* ---------- arazi ---------- */

/** Ön planda oyuncuların durduğu düzlük, arkasında göl ve vadi, uzakta dağlar. */
function yukseklik(x, z){
    const duzluk = 1 - yumusak(4, 20, Math.hypot((x - 3) * 0.6, Math.min(0, z + 3)))
    const tepe = gurultu(x / 44 + 3.1, z / 44 + 7.7) * 6.5 + gurultu(x / 13, z / 13) * 1.3 - 0.5
    const dag = yumusak(-95, -190, z) * (6 + Math.pow(gurultu(x / 24 + 9, z / 24), 2) * 62 + gurultu(x / 8, z / 8) * 7)
    const gol = yumusak(17, 6, Math.hypot((x + 15) * 0.6, z + 36)) * 9
    return Math.round(6 + duzluk * 3.4 + tepe * (1 - duzluk * 0.94) + dag - gol * (1 - duzluk))
}

class Bloklar {
    constructor(){
        this.yuzler = {}   // doku adı → {k:[], uv:[], r:[]}
        this.ek = new Map() // "x,y,z" → blok türü (ağaç, ev)
    }

    koy(x, y, z, tur){
        this.ek.set(x + ',' + y + ',' + z, tur)
    }

    /** Bir dörtgen ekler. k: 4 köşe (saat yönünün tersine), golge: yüz parlaklığı, ao: köşe karartmaları. */
    yuz(ad, k, golge, ao){
        const y = this.yuzler[ad] || (this.yuzler[ad] = { k: [], uv: [], r: [] })
        const uv = [[0, 0], [1, 0], [1, 1], [0, 1]]
        for(const i of [0, 1, 2, 0, 2, 3]){
            y.k.push(...k[i])
            y.uv.push(...uv[i])
            const s = golge * (ao ? ao[i] : 1)
            y.r.push(s, s, s)
        }
    }

    aglar(malzemeler){
        const c = new THREE.Color()
        return Object.entries(this.yuzler).map(([ad, y]) => {
            const g = new THREE.BufferGeometry()
            // Gölge sRGB uzayında çarpılır ki oyundaki ton tutsun.
            const r = new Float32Array(y.r.length)
            for(let i = 0; i < y.r.length; i += 3){
                c.setRGB(y.r[i], y.r[i + 1], y.r[i + 2], THREE.SRGBColorSpace)
                r.set([c.r, c.g, c.b], i)
            }
            g.setAttribute('position', new THREE.Float32BufferAttribute(y.k, 3))
            g.setAttribute('uv', new THREE.Float32BufferAttribute(y.uv, 2))
            g.setAttribute('color', new THREE.BufferAttribute(r, 3))
            const ag = new THREE.Mesh(g, malzemeler[ad])
            ag.renderOrder = ad === 'su' ? 5 : 0
            return ag
        })
    }
}

const YAN = [[1, 0, 0.6], [-1, 0, 0.6], [0, 1, 0.8], [0, -1, 0.8]]

function araziKur(b, x0, x1, z0, z1){
    const h = (x, z) => yukseklik(x, z)
    const ustTur = (y, x, z) => y >= 40 + rast(x, z) * 4 ? 'kar' : y >= 26 + rast(z, x) * 5 ? 'tas' : y <= SU ? 'kum' : 'cim_ust'
    for(let x = x0; x <= x1; x++){
        for(let z = z0; z <= z1; z++){
            const y = h(x, z), tur = ustTur(y, x, z)
            // Üst yüz + köşe karartması: köşeye komşu üç sütundan yüksek olan sayısı.
            const ao = [[0, 0], [1, 0], [1, 1], [0, 1]].map(([cx, cz]) => {
                const sx = cx ? 1 : -1, sz = cz ? 1 : -1
                const say = (h(x + sx, z) > y) + (h(x, z + sz) > y) + (h(x + sx, z + sz) > y)
                return 1 - say * 0.17
            })
            b.yuz(tur, [[x, y + 1, z + 1], [x + 1, y + 1, z + 1], [x + 1, y + 1, z], [x, y + 1, z]], 1, [ao[3], ao[2], ao[1], ao[0]])
            if(y < SU){
                b.yuz('su', [[x, SU + 0.88, z + 1], [x + 1, SU + 0.88, z + 1], [x + 1, SU + 0.88, z], [x, SU + 0.88, z]], 1)
            }
            // Uzakta hafif eğimli yamaçların basamak yanları da çim çizilir; yoksa tepeler kahverengi şeritli görünür.
            for(const [dx, dz, golge] of YAN){
                const komsu = h(x + dx, z + dz)
                for(let yy = y; yy > komsu; yy--){
                    const ad = tur === 'cim_ust' ? (yy === y ? (z < -25 && y - komsu <= 2 ? 'cim_ust' : 'cim_yan') : y - yy < 4 ? (z < -25 && y - komsu <= 2 ? 'cim_ust' : 'toprak') : 'tas') : tur === 'kum' ? 'kum' : tur === 'kar' && yy === y ? 'kar' : 'tas'
                    b.yuz(ad, yanKose(x, yy, z, dx, dz), golge * (yy === komsu + 1 ? 0.86 : 1))
                }
            }
        }
    }
}

/** Bloğun (dx,dz) yönündeki yan yüzünün köşeleri; doku dik durur. */
function yanKose(x, y, z, dx, dz){
    if(dx === 1) return [[x + 1, y, z + 1], [x + 1, y, z], [x + 1, y + 1, z], [x + 1, y + 1, z + 1]]
    if(dx === -1) return [[x, y, z], [x, y, z + 1], [x, y + 1, z + 1], [x, y + 1, z]]
    if(dz === 1) return [[x, y, z + 1], [x + 1, y, z + 1], [x + 1, y + 1, z + 1], [x, y + 1, z + 1]]
    return [[x + 1, y, z], [x, y, z], [x, y + 1, z], [x + 1, y + 1, z]]
}

/** Ağaç, ev gibi tek tek konan blokların açıkta kalan yüzleri. */
function ekBloklariKur(b){
    const DOKU = {
        kutuk: ['kutuk', 'kutuk_ust'], yaprak: ['yaprak', 'yaprak'], hus: ['hus', 'hus_ust'], hus_yaprak: ['hus_yaprak', 'hus_yaprak'],
        tahta: ['tahta', 'tahta'], kirik: ['kirik', 'kirik'], cam: ['cam', 'cam'], koyu: ['koyu_tahta', 'koyu_tahta']
    }
    const saydam = t => t === 'yaprak' || t === 'hus_yaprak' || t === 'cam'
    for(const [anahtar, tur] of b.ek){
        const [x, y, z] = anahtar.split(',').map(Number)
        const acik = (dx, dy, dz) => {
            const k = b.ek.get((x + dx) + ',' + (y + dy) + ',' + (z + dz))
            return k == null || saydam(k)
        }
        const [yan, ust] = DOKU[tur]
        if(acik(0, 1, 0)) b.yuz(ust, [[x, y + 1, z + 1], [x + 1, y + 1, z + 1], [x + 1, y + 1, z], [x, y + 1, z]], 1)
        if(acik(0, -1, 0)) b.yuz(ust, [[x, y, z], [x + 1, y, z], [x + 1, y, z + 1], [x, y, z + 1]], 0.5)
        for(const [dx, dz, golge] of YAN){
            if(acik(dx, 0, dz)) b.yuz(yan, yanKose(x, y, z, dx, dz), golge)
        }
    }
}

function agac(b, x, z, boy, hus){
    const y0 = yukseklik(x, z) + 1
    const govde = hus ? 'hus' : 'kutuk', yaprak = hus ? 'hus_yaprak' : 'yaprak'
    for(let i = 0; i < boy; i++) b.koy(x, y0 + i, z, govde)
    for(let dy = -2; dy <= 1; dy++){
        const r = dy < 0 ? 2 : 1
        for(let dx = -r; dx <= r; dx++){
            for(let dz = -r; dz <= r; dz++){
                const kose = Math.abs(dx) === r && Math.abs(dz) === r
                if(kose && (dy >= 0 || rast(x + dx * 3 + dy, z + dz * 5) < 0.5)) continue
                if(dx === 0 && dz === 0 && dy < 1) continue
                b.koy(x + dx, y0 + boy + dy, z + dz, yaprak)
            }
        }
    }
}

function ev(b, x0, z0){
    const y0 = Math.max(yukseklik(x0, z0), yukseklik(x0 + 6, z0 + 5), yukseklik(x0 + 6, z0), yukseklik(x0, z0 + 5)) + 1
    for(let x = 0; x <= 6; x++){
        for(let z = 0; z <= 5; z++){
            for(let y = yukseklik(x0 + x, z0 + z) + 1; y < y0; y++) b.koy(x0 + x, y, z0 + z, 'kirik')
            const kenar = x === 0 || x === 6 || z === 0 || z === 5
            for(let y = 0; y < 4; y++){
                if(!kenar) continue
                const kose = (x === 0 || x === 6) && (z === 0 || z === 5)
                const pencere = y === 1 || y === 2 ? ((z === 5 || z === 0) && (x === 2 || x === 4)) || ((x === 0 || x === 6) && (z === 2 || z === 3)) : false
                const kapi = z === 5 && x === 3 && y < 2
                b.koy(x0 + x, y0 + y, z0 + z, kapi ? 'koyu' : pencere ? 'cam' : kose ? 'kutuk' : y === 0 ? 'kirik' : 'tahta')
            }
        }
    }
    // Basamaklı çatı.
    for(let k = 0; k < 4; k++){
        for(let x = -1 + k; x <= 7 - k; x++){
            for(let z = -1; z <= 6; z++){
                if(k === 3 && x !== 3) continue
                b.koy(x0 + x, y0 + 4 + k, z0 + z, x === -1 + k || x === 7 - k || k === 3 ? 'koyu' : (z === -1 || z === 6 ? 'tahta' : null) || 'koyu')
            }
        }
    }
}

/** Çiçek ve ot: çapraz iki düzlem. */
function bitkiler(b, x0, x1, z0, z1, bos){
    const TUR = ['ot', 'ot', 'ot', 'ot', 'ot', 'gelincik', 'karahindiba', 'peygamber', 'papatya', 'ot', 'lale', 'ot']
    for(let x = x0; x <= x1; x++){
        for(let z = z0; z <= z1; z++){
            const y = yukseklik(x, z)
            if(y <= SU || y >= 24 || rast(x * 1.7 + 5, z * 2.3) > (z > 4 ? 0.05 : 0.2) || z > 8 || bos(x, z) || b.ek.has(x + ',' + (y + 1) + ',' + z)) continue
            const ad = TUR[Math.floor(rast(x + 31, z + 17) * TUR.length)]
            const g = 0.92
            b.yuz(ad, [[x + 0.08, y + 1, z + 0.08], [x + 0.92, y + 1, z + 0.92], [x + 0.92, y + 2, z + 0.92], [x + 0.08, y + 2, z + 0.08]], g)
            b.yuz(ad, [[x + 0.08, y + 1, z + 0.92], [x + 0.92, y + 1, z + 0.08], [x + 0.92, y + 2, z + 0.08], [x + 0.08, y + 2, z + 0.92]], g)
        }
    }
}

/* ---------- varlık modelleri (kutu açılımı) ---------- */

/**
 * Minecraft'ın kutu açılımıyla dokulanmış kutu. Koordinatlar model pikseli (y aşağı).
 * @param {number[]} o kutunun en küçük köşesi, @param {number[]} b boyut, @param {number} sis her yöne şişirme
 */
function kutu(malzeme, dg, dy, u, v, o, b, sis = 0){
    const [dx, dyy, dz] = b
    const x0 = o[0] - sis, y0 = o[1] - sis, z0 = o[2] - sis, x1 = o[0] + dx + sis, y1 = o[1] + dyy + sis, z1 = o[2] + dz + sis
    // [köşeler (sol üst, sağ üst, sağ alt, sol alt), u0, v0, u1, v1, normal]
    const yuzler = [
        [[x0, y0, z0], [x1, y0, z0], [x1, y1, z0], [x0, y1, z0], u + dz, v + dz, u + dz + dx, v + dz + dyy, [0, 0, -1]],
        [[x1, y0, z1], [x0, y0, z1], [x0, y1, z1], [x1, y1, z1], u + dz * 2 + dx, v + dz, u + dz * 2 + dx * 2, v + dz + dyy, [0, 0, 1]],
        [[x0, y0, z1], [x0, y0, z0], [x0, y1, z0], [x0, y1, z1], u, v + dz, u + dz, v + dz + dyy, [-1, 0, 0]],
        [[x1, y0, z0], [x1, y0, z1], [x1, y1, z1], [x1, y1, z0], u + dz + dx, v + dz, u + dz * 2 + dx, v + dz + dyy, [1, 0, 0]],
        [[x0, y0, z1], [x1, y0, z1], [x1, y0, z0], [x0, y0, z0], u + dz, v, u + dz + dx, v + dz, [0, -1, 0]],
        [[x0, y1, z0], [x1, y1, z0], [x1, y1, z1], [x0, y1, z1], u + dz + dx, v, u + dz + dx * 2, v + dz, [0, 1, 0]]
    ]
    const k = [], uv = [], n = []
    for(const [a, bb, c, d, u0, v0, u1, v1, nn] of yuzler){
        let kose = [a, bb, c, d], uvk = [[u0, v0], [u1, v0], [u1, v1], [u0, v1]]
        // Sarım dış normale göre saat yönünün tersine olsun (çift yüzlü malzemede ışık doğru hesaplansın).
        const e1 = [bb[0] - a[0], bb[1] - a[1], bb[2] - a[2]], e2 = [c[0] - a[0], c[1] - a[1], c[2] - a[2]]
        const cap = [e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]]
        if(cap[0] * nn[0] + cap[1] * nn[1] + cap[2] * nn[2] < 0){
            kose = [a, d, c, bb]
            uvk = [uvk[0], uvk[3], uvk[2], uvk[1]]
        }
        for(const i of [0, 1, 2, 0, 2, 3]){
            k.push(...kose[i])
            uv.push(uvk[i][0] / dg, 1 - uvk[i][1] / dy)
            n.push(...nn)
        }
    }
    const g = new THREE.BufferGeometry()
    g.setAttribute('position', new THREE.Float32BufferAttribute(k, 3))
    g.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2))
    g.setAttribute('normal', new THREE.Float32BufferAttribute(n, 3))
    return new THREE.Mesh(g, malzeme)
}

function kemik(ust, x, y, z){
    const g = new THREE.Group()
    g.position.set(x, y, z)
    ust.add(g)
    return g
}

/** Model uzayını (piksel, y aşağı, ayak y=24) sahneye yerleştirir; yaw=0 kameraya (+z) bakar. */
function yerlestir(sahne, x, z, yaw, olcek, yEk = 0){
    const dis = new THREE.Group()
    dis.position.set(x, yukseklik(Math.floor(x), Math.floor(z)) + 1 + 24 * olcek + yEk, z)
    dis.rotation.y = Math.PI + yaw
    const model = new THREE.Group()
    model.scale.set(-olcek, -olcek, olcek)
    dis.add(model)
    sahne.add(dis)
    return model
}

function varlikMalzemesi(r){
    return new THREE.MeshLambertMaterial({ map: doku(tuval(r)), side: THREE.DoubleSide, alphaTest: 0.5 })
}

async function oyuncu(sahne, o){
    const m = varlikMalzemesi(await resim(path.join(DOKULAR, 'entity', 'player', 'wide', o.gorunum + '.png')))
    const model = yerlestir(sahne, o.x, o.z, o.yaw || 0, PX)
    const kb = (ust, u, v, k, b, u2, v2, sis) => {
        ust.add(kutu(m, 64, 64, u, v, k, b))
        ust.add(kutu(m, 64, 64, u2, v2, k, b, sis))
    }
    const govde = kemik(model, 0, 0, 0)
    kb(govde, 16, 16, [-4, 0, -2], [8, 12, 4], 16, 32, 0.25)
    const kafa = kemik(model, 0, 0, 0)
    kb(kafa, 0, 0, [-4, -8, -4], [8, 8, 8], 32, 0, 0.5)
    const sagKol = kemik(model, -5, 2, 0)
    kb(sagKol, 40, 16, [-3, -2, -2], [4, 12, 4], 40, 32, 0.25)
    const solKol = kemik(model, 5, 2, 0)
    kb(solKol, 32, 48, [-1, -2, -2], [4, 12, 4], 48, 48, 0.25)
    const sagBacak = kemik(model, -1.9, 12, 0)
    kb(sagBacak, 0, 16, [-2, 0, -2], [4, 12, 4], 0, 32, 0.25)
    const solBacak = kemik(model, 1.9, 12, 0)
    kb(solBacak, 16, 48, [-2, 0, -2], [4, 12, 4], 0, 48, 0.25)

    const p = o.poz || {}
    kafa.rotation.set(p.kafaX || 0, p.kafaY || 0, p.kafaZ || 0, 'YXZ')
    sagKol.rotation.set(p.sagKolX || 0, 0, p.sagKolZ == null ? 0.06 : p.sagKolZ)
    solKol.rotation.set(p.solKolX || 0, 0, p.solKolZ == null ? -0.06 : p.solKolZ)
    sagBacak.rotation.x = p.sagBacakX || 0
    solBacak.rotation.x = p.solBacakX || 0

    for(const kimlik of o.kozmetik || []){
        const k = K.bul(kimlik)
        if(k.yuva === 'sapka' || k.yuva === 'hale') voksel(kafa, k, o.t || 40)
        else if(k.yuva === 'kanat') kanat(govde, k, o.t || enAcik(k))
        else if(k.yuva === 'pelerin') await pelerin(govde, k, o.pelerinAci || 0.2)
    }
    golge(sahne, o.x, o.z, 0.75)
}

/** Varlıkların altındaki yuvarlak gölge (oyundaki gibi). */
function golge(sahne, x, z, r){
    const c = document.createElement('canvas')
    c.width = c.height = 32
    const g = c.getContext('2d')
    const d = g.createRadialGradient(16, 16, 2, 16, 16, 16)
    d.addColorStop(0, 'rgba(0,0,0,0.5)')
    d.addColorStop(1, 'rgba(0,0,0,0)')
    g.fillStyle = d
    g.fillRect(0, 0, 32, 32)
    const ag = new THREE.Mesh(new THREE.PlaneGeometry(r * 2, r * 2),
        new THREE.MeshBasicMaterial({ map: new THREE.CanvasTexture(c), transparent: true, depthWrite: false }))
    ag.rotation.x = -Math.PI / 2
    ag.position.set(x, yukseklik(Math.floor(x), Math.floor(z)) + 1.02, z)
    ag.renderOrder = 2
    sahne.add(ag)
}

/* ---------- kozmetikler (moddaki çizimin karşılığı) ---------- */

function kozmetikDokusu(goreli){
    const d = new THREE.TextureLoader().load('file://' + path.join(K.DOKU_DIZINI, goreli))
    d.magFilter = d.minFilter = THREE.NearestFilter
    d.generateMipmaps = false
    d.colorSpace = THREE.SRGBColorSpace
    return d
}

function voksel(kafa, k, t){
    const rad = Math.PI / 180
    for(const parca of K.modelYukle(k.model)){
        const n = parca.dortgenler.length
        const konum = new Float32Array(n * 12), normal = new Float32Array(n * 12), renk = new Float32Array(n * 12)
        const sira = new Uint32Array(n * 6), c = new THREE.Color()
        parca.dortgenler.forEach((q, i) => {
            konum.set(q.k, i * 12)
            c.setHex(q.renk)
            for(let s = 0; s < 4; s++){
                normal.set(q.n, i * 12 + s * 3)
                renk.set([c.r, c.g, c.b], i * 12 + s * 3)
            }
            sira.set([i * 4, i * 4 + 1, i * 4 + 2, i * 4, i * 4 + 2, i * 4 + 3], i * 6)
        })
        const geo = new THREE.BufferGeometry()
        geo.setAttribute('position', new THREE.BufferAttribute(konum, 3))
        geo.setAttribute('normal', new THREE.BufferAttribute(normal, 3))
        geo.setAttribute('color', new THREE.BufferAttribute(renk, 3))
        geo.setIndex(new THREE.BufferAttribute(sira, 1))
        const ayar = { vertexColors: true, side: THREE.DoubleSide }
        const ag = new THREE.Mesh(geo, parca.isik ? new THREE.MeshBasicMaterial(ayar) : new THREE.MeshLambertMaterial(ayar))
        const a = parca.anim, e = parca.eksen, m = new THREE.Matrix4(), g = new THREE.Matrix4()
        m.makeTranslation(e[0], e[1], e[2])
        if(a.yorunge){
            m.multiply(g.makeRotationY((a.yorunge[1] * t + a.yorunge[2]) * rad))
            m.multiply(g.makeTranslation(a.yorunge[0], 0, 0))
        }
        if(a.sek) m.multiply(g.makeTranslation(0, Math.sin(t * a.sek[1] + a.sek[2]) * a.sek[0], 0))
        if(a.don){
            if(a.don[0]) m.multiply(g.makeRotationX(a.don[0] * t * rad))
            if(a.don[1]) m.multiply(g.makeRotationY(a.don[1] * t * rad))
            if(a.don[2]) m.multiply(g.makeRotationZ(a.don[2] * t * rad))
        }
        if(a.sallan){
            const aci = Math.sin(t * a.sallan[2] + a.sallan[3]) * a.sallan[1] * rad
            m.multiply(a.sallan[0] === 0 ? g.makeRotationX(aci) : a.sallan[0] === 1 ? g.makeRotationY(aci) : g.makeRotationZ(aci))
        }
        m.multiply(g.makeTranslation(-e[0], -e[1], -e[2]))
        ag.matrixAutoUpdate = false
        ag.matrix.copy(m)
        kafa.add(ag)
    }
}

/** Kanadın yana en çok açıldığı an. */
function enAcik(k){
    let iyi = 0, en = Infinity
    for(let t = 0; t < 120; t += 0.5){
        const a = K.kanatAcilari(k.hareket, t, false, 0)
        const toplam = a[0] + a[K.EKLEM - 1]
        if(toplam < en){
            en = toplam
            iyi = t
        }
    }
    return iyi
}

function kanat(govde, k, t){
    const N = K.EKLEM, seg = k.en / N, ust = -k.kok * k.boy, alt = (1 - k.kok) * k.boy
    const ana = new THREE.MeshLambertMaterial({ map: kozmetikDokusu('kanat/' + k.doku + '.png'), side: THREE.DoubleSide,
        alphaTest: k.saydam ? 0.05 : 0.5, transparent: !!k.saydam, depthWrite: !k.saydam })
    const isik = k.isik ? new THREE.MeshBasicMaterial({ map: kozmetikDokusu('kanat/' + k.doku + '_isik.png'), side: THREE.DoubleSide, alphaTest: 0.5 }) : null
    const aci = K.kanatAcilari(k.hareket, t, false, 0)
    for(const yon of [-1, 1]){
        const grup = new THREE.Group()
        grup.position.set(yon * 1.4, 3.2, 2.05)
        grup.rotation.z = -yon * 0.08
        govde.add(grup)
        const konum = [], normal = [], uv = [], sira = []
        const X = [0], Z = [0]
        for(let i = 0; i < N; i++){
            const c = Math.cos(aci[i]), s = Math.sin(aci[i])
            X.push(X[i] + yon * c * seg)
            Z.push(Z[i] + s * seg)
            konum.push(X[i], ust, Z[i], X[i + 1], ust, Z[i + 1], X[i + 1], alt, Z[i + 1], X[i], alt, Z[i])
            for(let q = 0; q < 4; q++) normal.push(-yon * s, 0, c)
            uv.push(i / N, 1, (i + 1) / N, 1, (i + 1) / N, 0, i / N, 0)
            sira.push(i * 4, i * 4 + 1, i * 4 + 2, i * 4, i * 4 + 2, i * 4 + 3)
        }
        const geo = new THREE.BufferGeometry()
        geo.setAttribute('position', new THREE.Float32BufferAttribute(konum, 3))
        geo.setAttribute('normal', new THREE.Float32BufferAttribute(normal, 3))
        geo.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2))
        geo.setIndex(sira)
        const ag = new THREE.Mesh(geo, ana)
        ag.renderOrder = k.saydam ? 6 : 0
        grup.add(ag)
        if(isik) grup.add(new THREE.Mesh(geo, isik))
    }
}

async function pelerin(govde, k, aci){
    const d = kozmetikDokusu('pelerin/' + k.doku + '.png')
    const grup = new THREE.Group()
    grup.position.set(0, 0, 2)
    grup.rotation.x = aci
    govde.add(grup)
    const yuz = (z, u0, u1) => {
        const geo = new THREE.BufferGeometry()
        geo.setAttribute('position', new THREE.Float32BufferAttribute([5, 0, z, -5, 0, z, -5, 16, z, 5, 16, z], 3))
        geo.setAttribute('uv', new THREE.Float32BufferAttribute([u0 / 64, 1 - 1 / 32, u1 / 64, 1 - 1 / 32, u1 / 64, 1 - 17 / 32, u0 / 64, 1 - 17 / 32], 2))
        geo.setAttribute('normal', new THREE.Float32BufferAttribute([0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1], 3))
        geo.setIndex([0, 1, 2, 0, 2, 3])
        return new THREE.Mesh(geo, new THREE.MeshLambertMaterial({ map: d, side: THREE.DoubleSide }))
    }
    grup.add(yuz(1, 1, 11))
    grup.add(yuz(0, 22, 12))
}

/* ---------- hayvanlar ---------- */

async function domuz(sahne, x, z, yaw){
    const m = varlikMalzemesi(await resim(path.join(DOKULAR, 'entity', 'pig', 'temperate_pig.png')))
    const model = yerlestir(sahne, x, z, yaw, 1 / 16)
    const kafa = kemik(model, 0, 12, -6)
    kafa.add(kutu(m, 64, 64, 0, 0, [-4, -4, -8], [8, 8, 8]))
    kafa.add(kutu(m, 64, 64, 16, 16, [-2, 0, -9], [4, 3, 1]))
    kafa.rotation.set(0.1, 0.35, 0, 'YXZ')
    const govde = kemik(model, 0, 11, 2)
    govde.rotation.x = Math.PI / 2
    govde.add(kutu(m, 64, 64, 28, 8, [-5, -10, -7], [10, 16, 8]))
    for(const [bx, bz] of [[-3, 7], [3, 7], [-3, -5], [3, -5]]){
        kemik(model, bx, 18, bz).add(kutu(m, 64, 64, 0, 16, [-2, 0, -2], [4, 6, 4]))
    }
    golge(sahne, x, z, 0.7)
}

async function tavuk(sahne, x, z, yaw){
    const m = varlikMalzemesi(await resim(path.join(DOKULAR, 'entity', 'chicken', 'temperate_chicken.png')))
    const model = yerlestir(sahne, x, z, yaw, 1 / 16)
    const kafa = kemik(model, 0, 15, -4)
    kafa.add(kutu(m, 64, 32, 0, 0, [-2, -6, -2], [4, 6, 3]))
    kafa.add(kutu(m, 64, 32, 14, 0, [-2, -4, -4], [4, 2, 2]))
    kafa.add(kutu(m, 64, 32, 14, 4, [-1, -2, -3], [2, 2, 2]))
    const govde = kemik(model, 0, 16, 0)
    govde.rotation.x = Math.PI / 2
    govde.add(kutu(m, 64, 32, 0, 9, [-3, -4, -3], [6, 8, 6]))
    kemik(model, -2, 19, 1).add(kutu(m, 64, 32, 26, 0, [-1, 0, -3], [3, 5, 3]))
    kemik(model, 1, 19, 1).add(kutu(m, 64, 32, 26, 0, [-1, 0, -3], [3, 5, 3]))
    kemik(model, -4, 13, 0).add(kutu(m, 64, 32, 24, 13, [0, 0, -3], [1, 4, 6]))
    kemik(model, 4, 13, 0).add(kutu(m, 64, 32, 24, 13, [-1, 0, -3], [1, 4, 6]))
    golge(sahne, x, z, 0.4)
}

/* ---------- gök ---------- */

function gok(g, y){
    const c = document.createElement('canvas')
    c.width = 4
    c.height = 512
    const x = c.getContext('2d')
    const d = x.createLinearGradient(0, 0, 0, 512)
    d.addColorStop(0, '#4f9bff')
    d.addColorStop(0.45, '#86c2ff')
    d.addColorStop(0.72, '#cfe9ff')
    d.addColorStop(1, '#eaf6ff')
    x.fillStyle = d
    x.fillRect(0, 0, 4, 512)
    const t = new THREE.CanvasTexture(c)
    t.colorSpace = THREE.SRGBColorSpace
    return t
}

async function bulutlar(sahne){
    const r = await resim(path.join(DOKULAR, 'environment', 'clouds.png'))
    const c = tuval(r)
    const v = c.getContext('2d').getImageData(0, 0, 256, 256).data
    const b = new Bloklar()
    const H = 7, Y = 46, KAL = 2.4
    const dolu = (i, j) => v[(((j % 256) + 256) % 256 * 256 + ((i % 256) + 256) % 256) * 4 + 3] > 128
    for(let i = 0; i < 56; i++){
        for(let j = 0; j < 40; j++){
            if(!dolu(i + 20, j + 96)) continue
            const x = (i - 28) * H, z = -30 - j * H
            b.yuz('bulut', [[x, Y + KAL, z + H], [x + H, Y + KAL, z + H], [x + H, Y + KAL, z], [x, Y + KAL, z]], 1)
            b.yuz('bulut', [[x, Y, z], [x + H, Y, z], [x + H, Y, z + H], [x, Y, z + H]], 0.9)
            if(!dolu(i + 21, j + 96)) b.yuz('bulut', [[x + H, Y, z + H], [x + H, Y, z], [x + H, Y + KAL, z], [x + H, Y + KAL, z + H]], 0.93)
            if(!dolu(i + 19, j + 96)) b.yuz('bulut', [[x, Y, z], [x, Y, z + H], [x, Y + KAL, z + H], [x, Y + KAL, z]], 0.93)
            if(!dolu(i + 20, j + 95)) b.yuz('bulut', [[x, Y, z + H], [x + H, Y, z + H], [x + H, Y + KAL, z + H], [x, Y + KAL, z + H]], 0.96)
            if(!dolu(i + 20, j + 97)) b.yuz('bulut', [[x + H, Y, z], [x, Y, z], [x, Y + KAL, z], [x + H, Y + KAL, z]], 0.96)
        }
    }
    const beyaz = document.createElement('canvas')
    beyaz.width = beyaz.height = 2
    const g = beyaz.getContext('2d')
    g.fillStyle = '#fff'
    g.fillRect(0, 0, 2, 2)
    for(const ag of b.aglar({ bulut: new THREE.MeshBasicMaterial({ map: doku(beyaz), vertexColors: true, fog: false, transparent: true, opacity: 0.93 }) })){
        ag.renderOrder = 1
        sahne.add(ag)
    }
}

/* ---------- sahne ---------- */

exports.ciz = async function(p){
    DOKULAR = p.get('dokular')
    const G = parseInt(p.get('g'), 10), Y = parseInt(p.get('y'), 10)
    const tuv = document.createElement('canvas')
    const cizer = new THREE.WebGLRenderer({ canvas: tuv, antialias: true, preserveDrawingBuffer: true })
    cizer.setSize(G, Y, false)
    const sahne = new THREE.Scene()
    sahne.background = gok()
    sahne.fog = new THREE.Fog(0xcfe6fb, 70, 300)
    sahne.add(new THREE.AmbientLight(0xffffff, 2.0))
    const gunes = new THREE.DirectionalLight(0xfff4dc, 1.7)
    gunes.position.set(-30, 60, 40)
    sahne.add(gunes)

    // Blok dokuları.
    const blok = ad => resim(path.join(DOKULAR, 'block', ad + '.png'))
    const CIM = [124, 196, 86], YAPRAK = [92, 170, 52], HUS = [128, 167, 85]
    const yan = tuval(await blok('grass_block_side'))
    yan.getContext('2d').drawImage(tuval(await blok('grass_block_side_overlay'), CIM), 0, 0)
    const T = {
        cim_ust: tuval(await blok('grass_block_top'), CIM), cim_yan: yan, toprak: tuval(await blok('dirt')), tas: tuval(await blok('stone')),
        kum: tuval(await blok('sand')), kar: tuval(await blok('snow')), su: tuval(await blok('water_still'), [70, 140, 235], true),
        kutuk: tuval(await blok('oak_log')), kutuk_ust: tuval(await blok('oak_log_top')), yaprak: tuval(await blok('oak_leaves'), YAPRAK),
        hus: tuval(await blok('birch_log')), hus_ust: tuval(await blok('birch_log_top')), hus_yaprak: tuval(await blok('birch_leaves'), HUS),
        tahta: tuval(await blok('oak_planks')), kirik: tuval(await blok('cobblestone')), cam: tuval(await blok('glass')),
        koyu_tahta: tuval(await blok('spruce_planks')), ot: tuval(await blok('short_grass'), CIM), gelincik: tuval(await blok('poppy')),
        karahindiba: tuval(await blok('dandelion')), peygamber: tuval(await blok('cornflower')), papatya: tuval(await blok('oxeye_daisy')),
        lale: tuval(await blok('pink_tulip'))
    }
    const kesik = new Set(['yaprak', 'hus_yaprak', 'cam', 'ot', 'gelincik', 'karahindiba', 'peygamber', 'papatya', 'lale'])
    const malzeme = {}
    for(const [ad, c] of Object.entries(T)){
        malzeme[ad] = new THREE.MeshBasicMaterial({ map: doku(c, !kesik.has(ad)), vertexColors: true, side: THREE.DoubleSide,
            alphaTest: kesik.has(ad) ? 0.5 : 0, transparent: ad === 'su', opacity: ad === 'su' ? 0.78 : 1, depthWrite: ad !== 'su' })
    }

    const OYUNCULAR = [
        { gorunum: 'alex', x: 3.3, z: 3.7, yaw: -0.12, kozmetik: ['k_anka', 's_tac'],
            poz: { sagKolX: -2.75, sagKolZ: 0.35, kafaY: 0.1, kafaX: -0.08, kafaZ: 0.06 } },
        { gorunum: 'steve', x: 5.3, z: 3.0, yaw: -0.3, kozmetik: ['p_hapi', 's_kep'], pelerinAci: 0.28,
            poz: { solKolX: -1.45, solKolZ: -0.25, kafaY: 0.2, sagBacakX: 0.12, solBacakX: -0.12 } },
        { gorunum: 'efe', x: 7.1, z: 1.6, yaw: -0.4, kozmetik: ['k_mekanik', 's_pervane'],
            poz: { kafaY: 0.2, kafaX: -0.1, sagKolZ: 0.5, solKolZ: -0.5, sagKolX: -0.2, solKolX: -0.2 } },
        { gorunum: 'zuri', x: 1.2, z: 2.2, yaw: 0.15, kozmetik: ['k_peri', 's_cicek'],
            poz: { kafaY: -0.15, kafaZ: -0.1, solKolX: -2.6, solKolZ: -0.4 } },
        { gorunum: 'kai', x: 5.4, z: -0.4, yaw: -0.25, kozmetik: ['k_melek', 's_buyucu'],
            poz: { kafaY: 0.15, sagKolX: -1.1, sagKolZ: 0.2 } },
        { gorunum: 'noor', x: 2.6, z: -0.6, yaw: 0.1, kozmetik: ['k_ejderha', 'h_ates'],
            poz: { kafaY: -0.1, sagKolZ: 0.3, solKolZ: -0.3 } }
    ]

    const b = new Bloklar()
    araziKur(b, -170, 190, -240, 15)
    for(const [x, z, boy, hus] of [[12, -5, 6, 0], [-14, -13, 5, 1], [16, 4, 5, 1], [-26, -14, 6, 0], [-3, -27, 5, 0], [9, -34, 6, 0], [-34, -30, 5, 1], [24, -30, 5, 0], [-22, -10, 6, 0], [22, -15, 5, 0],
        [9, -18, 5, 1], [30, -6, 6, 0], [-30, -3, 5, 0], [-4, -19, 5, 0], [36, -22, 5, 1], [-36, -18, 6, 0], [3, -40, 5, 0],
        [18, -44, 6, 0], [-28, -48, 5, 1], [42, -40, 5, 0], [-48, -36, 6, 0], [30, -58, 5, 0], [-10, -60, 5, 0], [52, -55, 6, 1],
        [-55, -60, 5, 0], [12, -70, 5, 0], [-36, -72, 6, 0], [60, -30, 5, 0], [-62, -22, 5, 1]]){
        if(yukseklik(x, z) > SU) agac(b, x, z, boy, hus)
    }
    ev(b, -22, -27)
    ekBloklariKur(b)
    const kalabalik = (x, z) => OYUNCULAR.some(o => Math.abs(o.x - x - 0.5) < 1.7 && o.z - z - 0.5 > -3 && o.z - z - 0.5 < 1.5)
    bitkiler(b, -45, 50, -60, 12, kalabalik)
    for(const ag of b.aglar(malzeme)) sahne.add(ag)
    await bulutlar(sahne)

    // Kare güneş.
    const gunesAg = new THREE.Mesh(new THREE.PlaneGeometry(26, 26), new THREE.MeshBasicMaterial({ color: 0xfffbe0, fog: false }))
    gunesAg.position.set(-95, 118, -260)
    gunesAg.lookAt(0, 20, 10)
    sahne.add(gunesAg)

    for(const o of OYUNCULAR) await oyuncu(sahne, o)
    await domuz(sahne, 9.6, 3.4, -0.9)
    await tavuk(sahne, 7.6, 5.2, -0.5)
    await tavuk(sahne, -1.0, 3.6, 0.5)

    const kamera = new THREE.PerspectiveCamera(36, G / Y, 0.5, 600)
    const yer = yukseklik(3, 3)
    kamera.position.set(0.2, yer + 2.7, 11.8)
    kamera.lookAt(2.7, yer + 2.55, 0)
    await new Promise(c => setTimeout(c, 800)) // kozmetik dokuları yüklensin
    cizer.render(sahne, kamera)
    return tuv.toDataURL('image/png')
}
