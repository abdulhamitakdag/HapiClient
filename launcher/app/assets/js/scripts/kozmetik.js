/**
 * Kozmetik sayfası: yuvalar, kartlar ve 3B önizleme. Seçim, oyunun okuduğu ayar dosyasına yazılır;
 * böylece oyun başlamadan önce de kozmetik değiştirilebilir.
 */
const Kozmetik = require('./assets/js/hapi/kozmetik')
const { KozmetikSahnesi } = require('./assets/js/hapi/kozmetik3b')

let kozmetikSahnesi = null
let kozmetikYuva = 'kanat'
let kozmetikSecim = {}

function kozmetikOyunDizini(){
    return path.join(ConfigManager.getInstanceDirectory(), ConfigManager.getSelectedServer())
}

function kozmetikIkonu(kimlik){
    return `assets/kozmetik/doku/ikon/${kimlik}.png`
}

function kozmetikBilgiGoster(k){
    const ad = document.getElementById('kozmetikBilgiAd')
    const nadirlik = document.getElementById('kozmetikBilgiNadirlik')
    const aciklama = document.getElementById('kozmetikBilgiAciklama')
    if(k == null){
        ad.textContent = Kozmetik.YUVA_ADLARI[kozmetikYuva]
        ad.style.color = ''
        nadirlik.textContent = ''
        aciklama.textContent = 'Bir kartın üstüne gel: mankende dene. Tıkla: tak. Takılı karta yeniden tıkla: çıkar.'
        return
    }
    const n = Kozmetik.NADIRLIK[k.nadirlik] || Kozmetik.NADIRLIK.yaygin
    ad.textContent = k.ad
    ad.style.color = n.renk
    nadirlik.textContent = n.ad + (kozmetikSecim[k.yuva] === k.kimlik ? ' · Takılı' : '')
    aciklama.textContent = k.aciklama
}

function kozmetikYuvalariCiz(){
    const kap = document.getElementById('kozmetikYuvalar')
    kap.replaceChildren()
    for(const yuva of Kozmetik.YUVALAR){
        const d = document.createElement('button')
        d.className = 'kozmetikYuva'
        if(yuva === kozmetikYuva) d.setAttribute('secili', '')
        const ad = document.createElement('span')
        ad.textContent = Kozmetik.YUVA_ADLARI[yuva]
        d.appendChild(ad)
        const takili = Kozmetik.bul(kozmetikSecim[yuva])
        const alt = document.createElement('span')
        alt.className = 'kozmetikYuvaTakili'
        alt.textContent = takili ? takili.ad : 'Yok'
        if(takili) alt.setAttribute('dolu', '')
        d.appendChild(alt)
        d.onclick = () => {
            kozmetikYuva = yuva
            kozmetikSahnesi.yuvayaBak(yuva)
            kozmetikCiz()
        }
        kap.appendChild(d)
    }
    document.getElementById('kozmetikHepsiniCikar').disabled = Object.keys(kozmetikSecim).length === 0
}

function kozmetikKarti(k){
    const kart = document.createElement('button')
    kart.className = 'kozmetikKart'
    const secili = k == null ? kozmetikSecim[kozmetikYuva] == null : kozmetikSecim[kozmetikYuva] === k.kimlik
    if(secili) kart.setAttribute('secili', '')
    if(k != null){
        kart.style.setProperty('--nadirlik', (Kozmetik.NADIRLIK[k.nadirlik] || Kozmetik.NADIRLIK.yaygin).renk)
        const ikon = document.createElement('img')
        ikon.src = kozmetikIkonu(k.kimlik)
        ikon.draggable = false
        kart.appendChild(ikon)
    } else {
        const bos = document.createElement('div')
        bos.className = 'kozmetikKartBos'
        bos.textContent = '✕'
        kart.appendChild(bos)
    }
    const ad = document.createElement('span')
    ad.textContent = k == null ? 'Yok' : k.ad
    kart.appendChild(ad)
    kart.onmouseenter = () => {
        const deneme = Object.assign({}, kozmetikSecim)
        if(k == null) delete deneme[kozmetikYuva]
        else deneme[kozmetikYuva] = k.kimlik
        kozmetikSahnesi.secimAyarla(deneme)
        kozmetikBilgiGoster(k)
    }
    kart.onmouseleave = () => {
        kozmetikSahnesi.secimAyarla(kozmetikSecim)
        kozmetikBilgiGoster(Kozmetik.bul(kozmetikSecim[kozmetikYuva]))
    }
    kart.onclick = () => {
        if(k == null || kozmetikSecim[kozmetikYuva] === k.kimlik) delete kozmetikSecim[kozmetikYuva]
        else kozmetikSecim[kozmetikYuva] = k.kimlik
        kozmetikKaydet()
        kozmetikCiz()
    }
    return kart
}

function kozmetikKaydet(){
    try {
        Kozmetik.secimiYaz(kozmetikOyunDizini(), kozmetikSecim)
    } catch(err){
        console.error('Kozmetik seçimi kaydedilemedi', err)
    }
}

function kozmetikCiz(){
    kozmetikYuvalariCiz()
    const izgara = document.getElementById('kozmetikIzgara')
    izgara.replaceChildren(kozmetikKarti(null), ...Kozmetik.katalog().filter(k => k.yuva === kozmetikYuva).map(kozmetikKarti))
    kozmetikSahnesi.secimAyarla(kozmetikSecim)
    kozmetikBilgiGoster(Kozmetik.bul(kozmetikSecim[kozmetikYuva]))
}

/** Sayfayı açmadan önce çağrılır: seçimi diskten okur, sahneyi kurar. */
function prepareKozmetik(){
    if(kozmetikSahnesi == null){
        kozmetikSahnesi = new KozmetikSahnesi(document.getElementById('kozmetikTuval'))
        window.addEventListener('resize', () => kozmetikSahnesi.boyutla())
    }
    kozmetikSecim = Kozmetik.secimiOku(kozmetikOyunDizini())
    kozmetikSahnesi.yuvayaBak(kozmetikYuva)
    kozmetikCiz()
}

document.getElementById('kozmetikBitti').onclick = () => {
    switchView(getCurrentView(), VIEWS.landing, 500, 500, () => kozmetikSahnesi.durdur())
}
document.getElementById('kozmetikHepsiniCikar').onclick = () => {
    kozmetikSecim = {}
    kozmetikKaydet()
    kozmetikCiz()
}
document.getElementById('kozmetikCevir').onclick = () => kozmetikSahnesi.cevir()
