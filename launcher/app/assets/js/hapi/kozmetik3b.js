/**
 * Kozmetik önizlemesi: mankeni ve üstündeki kozmetikleri three.js ile çizer.
 *
 * Oyundaki çizimle aynı veriyi ve aynı hareket hesaplarını kullanır (mod: KozmetikKatmani, VokselModel).
 * "Model uzayı" Minecraft oyuncu modelinin uzayıdır: birim model pikseli, y aşağı, +x oyuncunun solu,
 * yüz -z tarafında, orijin boyun. `model` grubu bu uzayı sahneye (y yukarı) çevirir.
 */
/* global document, window, requestAnimationFrame */
const path  = require('path')
const THREE = require('three')
const K     = require('./kozmetik')

const TIK = 20 // saniyedeki oyun tiki
const AURA_RENK = {
    kalp: 0xff4f6d, alev: 0xff8a1e, buyu: 0xb28dff, nota: 0x4fc3ff, kiraz: 0xffb7d5,
    ruh: 0x40e0d0, yildiz: 0xffffff, kar: 0xe8ffff, hapi: 0x2bd35a, simsek: 0xfff066
}

function dokuYukle(goreli){
    const d = new THREE.TextureLoader().load(path.join(K.VARLIK_DIZINI, 'doku', goreli).replace(/\\/g, '/'))
    d.magFilter = THREE.NearestFilter
    d.minFilter = THREE.NearestFilter
    d.generateMipmaps = false
    d.colorSpace = THREE.SRGBColorSpace
    return d
}

function kutu(g, y, d, renk, x, yy, z){
    const m = new THREE.Mesh(new THREE.BoxGeometry(g, y, d), new THREE.MeshLambertMaterial({ color: renk }))
    m.position.set(x, yy, z)
    return m
}

/** 8x8 piksellik yüz/saç dokusu. satirlar: her karakter bir piksel. */
function pikselDoku(satirlar, renkler){
    const c = document.createElement('canvas')
    c.width = c.height = 8
    const ctx = c.getContext('2d')
    satirlar.forEach((s, y) => [...s].forEach((ch, x) => {
        ctx.fillStyle = renkler[ch]
        ctx.fillRect(x, y, 1, 1)
    }))
    const d = new THREE.CanvasTexture(c)
    d.magFilter = d.minFilter = THREE.NearestFilter
    d.generateMipmaps = false
    d.colorSpace = THREE.SRGBColorSpace
    return new THREE.MeshLambertMaterial({ map: d })
}

class KozmetikSahnesi {

    /** @param {HTMLCanvasElement} tuval */
    constructor(tuval){
        this.tuval = tuval
        this.cizer = new THREE.WebGLRenderer({ canvas: tuval, antialias: true, alpha: true })
        this.cizer.setPixelRatio(window.devicePixelRatio || 1)
        this.sahne = new THREE.Scene()
        this.kamera = new THREE.PerspectiveCamera(26, 1, 1, 600)
        this.sahne.add(new THREE.AmbientLight(0xffffff, 2.1))
        const gunes = new THREE.DirectionalLight(0xffffff, 1.5)
        gunes.position.set(-40, 70, 60)
        this.sahne.add(gunes)

        // donen: mankenin kendi ekseninde dönüşü. model: Minecraft model uzayı.
        this.donen = new THREE.Group()
        this.sahne.add(this.donen)
        this.model = new THREE.Group()
        this.model.scale.set(-1, -1, 1)
        this.model.position.y = 8
        this.donen.add(this.model)
        this.kafa = new THREE.Group()
        this.model.add(this.kafa)
        this._manken()

        this.kozmetik = new THREE.Group() // gövdeye bağlı kozmetikler (kanat, pelerin)
        this.model.add(this.kozmetik)
        this.kafaKozmetik = new THREE.Group()
        this.kafa.add(this.kafaKozmetik)
        this.auraGrubu = new THREE.Group()
        this.sahne.add(this.auraGrubu)

        this.canli = [] // her karede çağrılan güncelleyiciler: t (tik) alır
        this.yaw = 0.35
        this.hedefYaw = 0.35
        this.egim = 0.14
        this.uzaklik = 118
        this.elle = false
        this.calisiyor = false
        this._fare()
    }

    _manken(){
        const ten = 0xe2b08a, sac = 0x4a2f1b, yesil = 0x2bd35a, pantolon = 0x2d3340, ayakkabi = 0x1b1f27
        const g = new THREE.Group() // sahne uzayında: y yukarı, boyun y=8
        const R = { s: '#4a2f1b', t: '#e2b08a', b: '#ffffff', g: '#1f8f43', a: '#8a4b3a', k: '#c9966f' }
        const yuz = pikselDoku(['ssssssss', 'ssssssss', 'stttttts', 'tttttttt', 'tbgttgbt', 'tttkkttt', 'tttaattt', 'tttttttt'], R)
        const yan = pikselDoku(['ssssssss', 'ssssssss', 'ssssssss', 'sssttttt', 'sstttttt', 'tttttttt', 'tttttttt', 'tttttttt'], R)
        const arka = pikselDoku(['ssssssss', 'ssssssss', 'ssssssss', 'ssssssss', 'ssssssss', 'ssssssss', 'tsssssst', 'tttttttt'], R)
        const duz = c => new THREE.MeshLambertMaterial({ color: c })
        // BoxGeometry yüz sırası: +x, -x, +y, -y, +z, -z. Mankenin yüzü -z'ye bakar.
        const bas = new THREE.Mesh(new THREE.BoxGeometry(8, 8, 8), [yan, yan, duz(sac), duz(ten), arka, yuz])
        bas.position.set(0, 12, 0)
        g.add(bas)
        g.add(kutu(8, 12, 4, yesil, 0, 2, 0))
        g.add(kutu(8.2, 1.2, 4.2, 0xffffff, 0, -3.2, 0)) // kemer
        for(const s of [-1, 1]){
            g.add(kutu(4, 5, 4, yesil, s * 6, 5.5, 0))      // kol yeni
            g.add(kutu(4, 7, 4, ten, s * 6, -0.5, 0))        // kol
            g.add(kutu(4, 10, 4, pantolon, s * 2, -9, 0))    // bacak
            g.add(kutu(4.1, 2, 4.1, ayakkabi, s * 2, -15, 0))
        }
        this.donen.add(g)
    }

    _fare(){
        let basili = false, sx = 0, sy = 0
        this.tuval.addEventListener('pointerdown', e => {
            basili = true
            sx = e.clientX
            sy = e.clientY
            this.tuval.setPointerCapture(e.pointerId)
        })
        this.tuval.addEventListener('pointermove', e => {
            if(!basili) return
            this.elle = true
            this.yaw += (e.clientX - sx) * 0.012
            this.egim = Math.max(-0.25, Math.min(0.8, this.egim + (e.clientY - sy) * 0.006))
            sx = e.clientX
            sy = e.clientY
        })
        const birak = () => { basili = false }
        this.tuval.addEventListener('pointerup', birak)
        this.tuval.addEventListener('pointercancel', birak)
        this.tuval.addEventListener('wheel', e => {
            e.preventDefault()
            this.uzaklik = Math.max(70, Math.min(190, this.uzaklik + Math.sign(e.deltaY) * 8))
        }, { passive: false })
    }

    /** Mankeni yuvaya uygun yöne çevirir: pelerin ve kanat arkadan, diğerleri önden. */
    yuvayaBak(yuva){
        this.elle = false
        this.hedefYaw = yuva === 'pelerin' || yuva === 'kanat' ? Math.PI + 0.45 : 0.35
    }

    cevir(){
        this.elle = false
        const arkada = Math.cos(this.hedefYaw) < 0
        this.hedefYaw = arkada ? 0.35 : Math.PI + 0.45
    }

    _bosalt(grup){
        for(const o of [...grup.children]){
            grup.remove(o)
            o.traverse(n => {
                if(n.geometry) n.geometry.dispose()
                for(const m of [].concat(n.material || [])){
                    if(m.map) m.map.dispose()
                    m.dispose()
                }
            })
        }
    }

    /** @param {Object<string,string>} secim yuva → kozmetik kimliği */
    secimAyarla(secim){
        const anahtar = JSON.stringify(K.YUVALAR.map(y => secim[y] || ''))
        if(anahtar === this._anahtar) return
        this._anahtar = anahtar
        this._bosalt(this.kozmetik)
        this._bosalt(this.kafaKozmetik)
        this._bosalt(this.auraGrubu)
        this.canli = []
        const al = yuva => {
            const k = K.bul(secim[yuva])
            return k != null && k.yuva === yuva ? k : null
        }
        for(const yuva of ['sapka', 'hale']){
            const k = al(yuva)
            if(k != null) this._voksel(k)
        }
        if(al('kanat')) this._kanat(al('kanat'))
        if(al('pelerin')) this._pelerin(al('pelerin'))
        if(al('aura')) this._aura(al('aura'))
    }

    _voksel(k){
        for(const parca of K.modelYukle(k.model)){
            const n = parca.dortgenler.length
            const konum = new Float32Array(n * 12), normal = new Float32Array(n * 12), renk = new Float32Array(n * 12)
            const sira = new Uint32Array(n * 6)
            const c = new THREE.Color()
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
            ag.matrixAutoUpdate = false
            this.kafaKozmetik.add(ag)
            const a = parca.anim, e = parca.eksen
            if(a.don || a.sallan || a.sek || a.yorunge){
                const m = new THREE.Matrix4(), g = new THREE.Matrix4(), rad = Math.PI / 180
                // Sıra moddaki VokselModel.Parca.donustur ile aynı.
                this.canli.push(t => {
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
                    ag.matrix.copy(m)
                })
            }
        }
    }

    _kanat(k){
        const N = K.EKLEM
        const seg = k.en / N, ust = -k.kok * k.boy, alt = (1 - k.kok) * k.boy
        const doku = dokuYukle('kanat/' + k.doku + '.png')
        const ana = new THREE.MeshLambertMaterial({ map: doku, side: THREE.DoubleSide, alphaTest: k.saydam ? 0.05 : 0.5,
            transparent: !!k.saydam, depthWrite: !k.saydam })
        const isik = k.isik ? new THREE.MeshBasicMaterial({ map: dokuYukle('kanat/' + k.doku + '_isik.png'),
            side: THREE.DoubleSide, alphaTest: 0.5 }) : null
        const disliMalzeme = k.disliler.length ? new THREE.MeshLambertMaterial({ map: dokuYukle('disli.png'),
            side: THREE.DoubleSide, alphaTest: 0.5 }) : null
        for(const yon of [-1, 1]){
            const grup = new THREE.Group()
            grup.position.set(yon * 1.4, 3.2, 2.05)
            grup.rotation.z = -yon * 0.08
            this.kozmetik.add(grup)
            const konum = new Float32Array(N * 12), normal = new Float32Array(N * 12), uv = new Float32Array(N * 8)
            const sira = []
            for(let i = 0; i < N; i++){
                const u0 = i / N, u1 = (i + 1) / N
                uv.set([u0, 1, u1, 1, u1, 0, u0, 0], i * 8)
                sira.push(i * 4, i * 4 + 1, i * 4 + 2, i * 4, i * 4 + 2, i * 4 + 3)
            }
            const geo = new THREE.BufferGeometry()
            geo.setAttribute('position', new THREE.BufferAttribute(konum, 3))
            geo.setAttribute('normal', new THREE.BufferAttribute(normal, 3))
            geo.setAttribute('uv', new THREE.BufferAttribute(uv, 2))
            geo.setIndex(sira)
            const ag = new THREE.Mesh(geo, ana)
            ag.frustumCulled = false
            grup.add(ag)
            if(isik){
                const parlak = new THREE.Mesh(geo, isik)
                parlak.frustumCulled = false
                grup.add(parlak)
            }
            let disliGeo = null, disliKonum = null
            if(disliMalzeme){
                const adet = k.disliler.length * 2
                disliKonum = new Float32Array(adet * 12)
                const duv = new Float32Array(adet * 8), dsira = []
                for(let i = 0; i < adet; i++){
                    duv.set([0, 1, 1, 1, 1, 0, 0, 0], i * 8)
                    dsira.push(i * 4, i * 4 + 1, i * 4 + 2, i * 4, i * 4 + 2, i * 4 + 3)
                }
                disliGeo = new THREE.BufferGeometry()
                disliGeo.setAttribute('position', new THREE.BufferAttribute(disliKonum, 3))
                disliGeo.setAttribute('uv', new THREE.BufferAttribute(duv, 2))
                disliGeo.setIndex(dsira)
                const dag = new THREE.Mesh(disliGeo, disliMalzeme)
                dag.frustumCulled = false
                grup.add(dag)
            }
            this.canli.push(t => {
                const aci = K.kanatAcilari(k.hareket, t, false, 0)
                // Eklem uçları: moddaki kanatYaz ile aynı.
                const X = [0], Z = [0]
                for(let i = 0; i < N; i++){
                    const c = Math.cos(aci[i]), s = Math.sin(aci[i])
                    X.push(X[i] + yon * c * seg)
                    Z.push(Z[i] + s * seg)
                    konum.set([X[i], ust, Z[i], X[i + 1], ust, Z[i + 1], X[i + 1], alt, Z[i + 1], X[i], alt, Z[i]], i * 12)
                    for(let q = 0; q < 4; q++) normal.set([-yon * s, 0, c], i * 12 + q * 3)
                }
                geo.attributes.position.needsUpdate = true
                geo.attributes.normal.needsUpdate = true
                if(disliGeo){
                    k.disliler.forEach((d, di) => {
                        const hedef = Math.min(N - 1, Math.floor(d[0] * N))
                        const c = Math.cos(aci[hedef]), s = Math.sin(aci[hedef])
                        const kalan = d[0] * N - hedef
                        const ax = yon * c, az = s, nx = -yon * s, nz = c
                        const mx = X[hedef] + ax * seg * kalan, mz = Z[hedef] + az * seg * kalan, my = ust + d[1] * k.boy
                        const f = d[3] * t * Math.PI / 180 * yon
                        const co = Math.cos(f) * d[2], si = Math.sin(f) * d[2]
                        const kose = [[-co + si, -si - co], [co + si, si - co], [co - si, si + co], [-co - si, -si + co]]
                        ;[-1, 1].forEach((yuz, yi) => {
                            const ox = nx * yuz * 0.35, oz = nz * yuz * 0.35
                            kose.forEach((q, qi) => disliKonum.set(
                                [mx + ax * q[0] + ox, my + q[1], mz + az * q[0] + oz], (di * 2 + yi) * 12 + qi * 3))
                        })
                    })
                    disliGeo.attributes.position.needsUpdate = true
                }
            })
        }
    }

    _pelerin(k){
        const doku = dokuYukle('pelerin/' + k.doku + '.png')
        const grup = new THREE.Group()
        grup.position.set(0, 0, 2)
        this.kozmetik.add(grup)
        // Vanilla pelerin açılımı (64x32): dış yüz (1,1)-(11,17), iç yüz (12,1)-(22,17).
        const yuz = (z, u0, u1) => {
            const geo = new THREE.BufferGeometry()
            // Arkadan bakınca dokunun solu solda kalsın: model +x ↔ u0.
            geo.setAttribute('position', new THREE.BufferAttribute(new Float32Array([5, 0, z, -5, 0, z, -5, 16, z, 5, 16, z]), 3))
            geo.setAttribute('uv', new THREE.BufferAttribute(new Float32Array(
                [u0 / 64, 1 - 1 / 32, u1 / 64, 1 - 1 / 32, u1 / 64, 1 - 17 / 32, u0 / 64, 1 - 17 / 32]), 2))
            geo.setAttribute('normal', new THREE.BufferAttribute(new Float32Array([0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1]), 3))
            geo.setIndex([0, 1, 2, 0, 2, 3])
            return new THREE.Mesh(geo, new THREE.MeshLambertMaterial({ map: doku, side: THREE.DoubleSide }))
        }
        grup.add(yuz(1, 1, 11))
        grup.add(yuz(0, 22, 12))
        this.canli.push(t => {
            grup.rotation.x = (7 + Math.sin(t * 0.07) * 4) * Math.PI / 180
            grup.rotation.z = Math.sin(t * 0.05) * 1.5 * Math.PI / 180
        })
    }

    _aura(k){
        const adet = 14
        const konum = new Float32Array(adet * 3)
        const geo = new THREE.BufferGeometry()
        geo.setAttribute('position', new THREE.BufferAttribute(konum, 3))
        const nokta = new THREE.Points(geo, new THREE.PointsMaterial({ color: AURA_RENK[k.parcacik] || 0xffffff, size: 1.5 }))
        nokta.frustumCulled = false
        this.auraGrubu.add(nokta)
        // Sahne uzayı: ayak y=-16, baş y=16.
        const ayak = -16, bas = 17, yari = 11
        this.canli.push(t => {
            for(let i = 0; i < adet; i++){
                let x, y, z
                const sabit = i * 2.4
                if(k.desen === 'yorunge'){
                    const a = t * 0.22 + i * Math.PI
                    const h = ((t * 0.035 + Math.floor(i / 2) * 0.29) % 2) / 2
                    x = Math.cos(a) * yari
                    z = Math.sin(a) * yari
                    y = ayak + (bas - ayak) * h
                } else if(k.desen === 'yagan'){
                    const h = (t * 0.03 + i * 0.37) % 1
                    x = Math.sin(sabit) * yari
                    z = Math.cos(sabit * 1.7) * yari
                    y = bas + 8 - (bas + 8 - ayak) * h
                } else if(k.desen === 'halka'){
                    const a = t * 0.3 + i * Math.PI * 2 / adet
                    x = Math.cos(a) * yari
                    z = Math.sin(a) * yari
                    y = ayak + 1 + ((t * 0.05 + i * 0.3) % 1) * 4
                } else if(k.desen === 'sacilan'){
                    const tur = Math.floor(t * 0.08 + i * 0.5)
                    const h = (t * 0.08 + i * 0.5) % 1
                    x = Math.sin(i * 3.1 + tur) * yari
                    z = Math.cos(i * 4.3 + tur) * yari
                    y = Math.cos(i * 1.9 + tur) * 13 + h * 4
                } else {
                    const h = (t * 0.02 + i * 0.29) % 1
                    x = Math.sin(sabit) * yari * 0.8
                    z = Math.cos(sabit * 1.7) * yari * 0.8
                    y = k.parcacik === 'kalp' ? bas + 2 + h * 9 : ayak + (bas - ayak) * h
                }
                konum.set([x, y, z], i * 3)
            }
            geo.attributes.position.needsUpdate = true
        })
    }

    boyutla(){
        const g = this.tuval.clientWidth, y = this.tuval.clientHeight
        if(g === 0 || y === 0) return
        this.cizer.setSize(g, y, false)
        this.kamera.aspect = g / y
        this.kamera.updateProjectionMatrix()
    }

    /** Tek kare çizer. @param {number} t oyun tiki */
    ciz(t){
        if(!this.elle) this.yaw += (this.hedefYaw - this.yaw) * 0.1
        const sallanti = this.elle ? 0 : Math.sin(t * 0.035) * 0.16
        // Kamera +z'de durur; manken yaw=0'da yüzünü kameraya döner.
        this.donen.rotation.y = Math.PI + this.yaw + sallanti
        this.kamera.position.set(0, 4 + Math.sin(this.egim) * this.uzaklik, Math.cos(this.egim) * this.uzaklik)
        this.kamera.lookAt(0, 4, 0)
        for(const f of this.canli) f(t)
        this.cizer.render(this.sahne, this.kamera)
    }

    baslat(){
        if(this.calisiyor) return
        this.calisiyor = true
        this.boyutla()
        const dongu = () => {
            if(!this.calisiyor) return
            this.ciz(performance.now() / 1000 * TIK)
            requestAnimationFrame(dongu)
        }
        requestAnimationFrame(dongu)
    }

    durdur(){
        this.calisiyor = false
    }
}

exports.KozmetikSahnesi = KozmetikSahnesi
