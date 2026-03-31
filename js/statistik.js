/**
 * MODULE: statistik.js  (v4 – Pollen-Popup, Symptome als rotes Band, Ausschlussdiät zurück)
 *
 * Konfigurierbarer Mixed-Chart.
 * Y-Links  (y):  Temperatur °C, Luftfeuchtigkeit %, Gewicht kg
 * Y-Rechts (y2): Schweregrad 0–5 (rotes Flächenband), Pollen-Stufe 0–5
 *
 * Neu in v4:
 * - Schweregrad Symptome: gefülltes rotes Band (fill:'origin') statt Balken
 * - Pollen-Typen: Popup-Dialog statt Inline-Buttons; inkl. Custom-Pollen aus localStorage
 * - Ausschlussdiät: wieder als Liste in der Statistik (wie Medikamente), nur wenn Daten vorhanden
 */

import { getSheet, invalidateAll, getAge } from './cache.js';
import { getHunde }                         from './store.js';
import { esc }                              from './ui.js';

const C = {
  blue:'#3b82f6',   blueL:'rgba(59,130,246,.15)',
  orange:'#f97316', orangeL:'rgba(249,115,22,.15)',
  amber:'#f59e0b',  amberL:'rgba(245,158,11,.15)',
  green:'#22c55e',  greenL:'rgba(34,197,94,.15)',
  red:'#ef4444',    redL:'rgba(239,68,68,.18)',
  purple:'#a855f7', purpleL:'rgba(168,85,247,.15)',
  teal:'#14b8a6',   tealL:'rgba(20,184,166,.15)',
  sky:'#0ea5e9',    skyL:'rgba(14,165,233,.15)',
};

let _chart      = null;
let _selected   = new Set(['temp_band','symptome']);
let _cachedData = null;
// Bekannte Allergene (Pollenarten) aus dem Sheet – für Pollen-Vorauswahl
let _knownAllergenPollen = new Set();

const POLLEN_COLORS = [C.green,C.amber,C.teal,C.sky,C.orange,C.purple,'#10b981','#6366f1'];
let _pollenTypes    = [];
let _selPollenTypes = new Set();

function _getCustomPollen() {
  try { return JSON.parse(localStorage.getItem('hundapp_custom_pollen') || '[]'); }
  catch { return []; }
}
function _getAllPollenTypes() {
  return [...new Set([..._pollenTypes, ..._getCustomPollen()])].sort();
}

const PARAM_DEFS = [
  {
    key:'temp_band', label:'Temp. Band (Min–Max)', emoji:'🌡',
    color:C.orange, colorL:'rgba(249,115,22,.12)', yAxis:'y', chartType:'band',
    extract:({umw})=>({
      max:_byDate(umw,1,r=>parseFloat(g(r,3)),Math.max),
      min:_byDate(umw,1,r=>parseFloat(g(r,2)),Math.min),
    }),
  },
  {
    key:'temp_in', label:'Temp. innen (°C)', emoji:'🏠',
    color:C.amber, colorL:C.amberL, yAxis:'y',
    extract:({umw})=>_byDate(umw,1,r=>parseFloat(g(r,7))),
  },
  {
    key:'feuchte_aus', label:'Feuchte außen (%)', emoji:'💧',
    color:C.sky, colorL:C.skyL, yAxis:'y', dashed:true,
    extract:({umw})=>_byDate(umw,1,r=>parseFloat(g(r,4))),
  },
  {
    key:'feuchte_in', label:'Feuchte innen (%)', emoji:'🏠',
    color:C.teal, colorL:C.tealL, yAxis:'y', dashed:true,
    extract:({umw})=>_byDate(umw,1,r=>parseFloat(g(r,8))),
  },
  {
    key:'regen', label:'Niederschlag (mm)', emoji:'🌧',
    color:'#3b82f6', colorL:'rgba(59,130,246,.18)', yAxis:'y', chartType:'bar_param',
    extract:({umw})=>_byDate(umw,1,r=>parseFloat(g(r,5)),((a,b)=>a+b)),
  },
  {
    key:'symptome', label:'Schweregrad Symptome (0–5)', emoji:'🔍',
    color:C.red, colorL:'rgba(239,68,68,.22)', yAxis:'y2', chartType:'area',
    extract:({sym})=>_byDate(sym,1,r=>parseInt(g(r,4)),Math.max),
  },
  {
    key:'gewicht', label:'Gewicht (kg)', emoji:'⚖️',
    color:C.purple, colorL:C.purpleL, yAxis:'y',
    extract:({gew})=>_byDate(gew||[],2,r=>parseFloat(String(g(r,3)).replace(',','.'))),
  },
];

export async function load() {
  const panel = document.getElementById('panel-statistik');
  if (!panel) return;
  panel.innerHTML = _buildShell();
  const hundSel = document.getElementById('stat-hund');
  getHunde().forEach(h=>{
    const opt=document.createElement('option');
    opt.value=h.hund_id; opt.textContent=h.name;
    hundSel?.appendChild(opt);
  });
  _buildParamButtons();
  refresh();
}

export async function refresh(forceRefresh=false) {
  const hundId    = parseInt(document.getElementById('stat-hund')?.value)||1;
  const rangeDays = parseInt(document.getElementById('stat-range')?.value)||90;
  const content   = document.getElementById('stat-content');
  const cacheEl   = document.getElementById('stat-cache-status');
  if(!content) return;
  content.innerHTML='<div class="view-loading"><div class="spinner"></div>Lade Daten…</div>';
  try {
    const [rSym,rUmw,rFut,rAll,rMed,rAus] = await Promise.all([
      getSheet('Symptomtagebuch',   'tagebuch',forceRefresh),
      getSheet('Umweltagebuch',     'tagebuch',forceRefresh),
      getSheet('Futtertagebuch',    'tagebuch',forceRefresh),
      getSheet('Bekannte Allergene','tagebuch',forceRefresh),
      getSheet('Medikamente',       'tagebuch',forceRefresh),
      getSheet('Ausschlussdiät',    'tagebuch',forceRefresh).catch(()=>[]),
    ]);
    const rGew=await getSheet('Hund_Gewicht','tagebuch',forceRefresh).catch(()=>[]);
    const rPol=await getSheet('Pollen_Log',  'tagebuch',forceRefresh).catch(()=>[]);

    const age=getAge('Symptomtagebuch');
    if(cacheEl) cacheEl.textContent = age!==null&&age<30
      ? `✅ Gerade geladen · Aktualisierung in ~${Math.round((600-age)/60)} Min`
      : age!==null
      ? `📦 Cache vor ${age<60?age+'s':Math.round(age/60)+' Min'} · ↺ für neue Daten`
      : '✅ Frisch geladen';

    const cutoff = rangeDays>0 ? new Date(Date.now()-rangeDays*86_400_000) : new Date(0);
    const notDel = idx=>r=>String(r[idx]??'').toUpperCase()!=='TRUE';

    const _pr=(raw,skip)=>_parseRows(raw,skip);
    const allSym=_pr(rSym,2); const allUmw=_pr(rUmw,2);
    const allFut=_pr(rFut,2); const allAll=_pr(rAll,2);
    const allMed=_pr(rMed,2); const allAus=_pr(rAus,2);
    const allGew=_pr(rGew,2); const allPol=_pr(rPol,2);

    const sym=allSym.filter(r=>_matchH(r,hundId)&&_inRange(g(r,1),cutoff)&&notDel(9)(r));
    const umw=allUmw.filter(r=>_matchH(r,hundId)&&_inRange(g(r,1),cutoff)&&notDel(13)(r));
    const fut=allFut.filter(r=>_matchH(r,hundId)&&_inRange(g(r,1),cutoff)&&notDel(11)(r));
    const all=allAll.filter(r=>_matchH(r,hundId)&&notDel(8)(r));
    const med=allMed.filter(r=>_matchH(r,hundId)&&notDel(11)(r));
    const aus=allAus.filter(r=>_matchH(r,hundId)&&notDel(10)(r));
    const gew=allGew.filter(r=>g(r,1)===String(hundId)&&_inRange(g(r,2),cutoff));
    const pol=allPol.filter(r=>g(r,1)===String(hundId)&&_inRange(g(r,2),cutoff));

    _cachedData={sym,umw,fut,all,med,aus,gew,pol};

    const discoveredPollen=[...new Set(pol.map(r=>g(r,3)).filter(Boolean))].sort();
    _pollenTypes=discoveredPollen;
    const allPollenTypes=_getAllPollenTypes();

    // Bekannte Allergene (Futterallergene ausschließen): Pollen-Namen normiert
    _knownAllergenPollen = new Set(
      all.filter(r => {
        const kat = g(r,2).toLowerCase();
        return kat.includes('umwelt') || kat.includes('pollen') || kat === '';
      }).map(r => g(r,1))
    );

    // Pollen-Vorauswahl: nur Pollenarten die in bekannten Allergenen stehen,
    // alles andere standardmäßig deaktiviert. Nur beim ersten Laden setzen.
    if(_selPollenTypes.size === 0) {
      allPollenTypes.forEach(t => {
        if(_knownAllergenPollen.has(t)) _selPollenTypes.add(t);
      });
    }
    _buildParamButtons();

    const schweList=sym.map(r=>parseInt(g(r,4))||0).filter(v=>v>0);
    const avgSchw=schweList.length?(schweList.reduce((a,b)=>a+b,0)/schweList.length).toFixed(1):'–';
    const symDays=new Set(sym.map(r=>g(r,1))).size;
    const polDays=pol.length
      ? new Set(pol.map(r=>g(r,2))).size
      : umw.filter(r=>{const p=g(r,6);return p&&p!=='keine erhöhte Belastung'&&p.trim();}).length;

    content.innerHTML=`
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-bottom:1rem">
        ${_kpi('Symptomtage',symDays,C.red)}
        ${_kpi('Ø Schweregrad',avgSchw,parseFloat(avgSchw)>=3?C.red:C.green)}
        ${_kpi('Pollentage',polDays,C.amber)}
      </div>
      <div style="background:var(--bg2);border:1px solid var(--border);border-radius:var(--radius);
        padding:14px;margin-bottom:1rem">
        <canvas id="ch-konfig" height="240"></canvas>
      </div>
      ${_box('⚠️ Bekannte Allergene','<div id="st-allergene"></div>')}
      ${aus.length?_box('🍽️ Ausschlussdiät','<div id="st-aus"></div>'):''}
      ${_box('🥩 Futter-Reaktionen','<div id="st-futter"></div>')}
      ${_box('💊 Medikamente','<div id="st-medis"></div>')}
    `;

    await _buildChart(_cachedData);
    _renderAllergene(all);
    if(aus.length) _renderAusschluss(aus);
    _renderFutter(fut);
    _renderMedis(med);

  } catch(e) {
    content.innerHTML=`<div class="status err" style="display:block">Fehler: ${esc(e.message)}</div>`;
  }
}

export function forceRefresh() { invalidateAll(); _cachedData=null; refresh(true); }

export async function toggleParam(key) {
  _selected.has(key)?_selected.delete(key):_selected.add(key);
  document.querySelectorAll('.stat-param-btn[data-group="param"]').forEach(btn=>{
    btn.classList.toggle('sel',_selected.has(btn.dataset.key));
  });
  if(_cachedData) await _buildChart(_cachedData);
}

// ── Pollen-Popup ─────────────────────────────────────────────────
export function showPollenPopup() {
  document.getElementById('pollen-select-popup')?.remove();
  const allTypes=_getAllPollenTypes();
  if(!allTypes.length){
    alert('Keine Pollen-Daten vorhanden.\nZuerst Pollen im Tagebuch eintragen oder im Wetter-Bereich eigene Pollenarten anlegen (⚙️ Verwalten).');
    return;
  }
  const overlay=document.createElement('div');
  overlay.id='pollen-select-popup';
  overlay.style.cssText='position:fixed;inset:0;background:rgba(0,0,0,.55);z-index:8000;display:flex;align-items:flex-end;justify-content:center;';
  const sheet=document.createElement('div');
  sheet.style.cssText='background:var(--bg);border-radius:var(--radius) var(--radius) 0 0;padding:20px 16px 32px;width:100%;max-width:540px;max-height:75vh;overflow-y:auto;box-shadow:0 -4px 24px rgba(0,0,0,.3);';
  const customLS=_getCustomPollen();
  const rows=allTypes.map((t,i)=>{
    const isData=_pollenTypes.includes(t);
    const col=POLLEN_COLORS[i%POLLEN_COLORS.length];
    const badge=isData
      ?`<span style="font-size:10px;padding:2px 6px;border-radius:10px;background:${col}22;color:${col};border:1px solid ${col}44">Daten</span>`
      :`<span style="font-size:10px;padding:2px 6px;border-radius:10px;background:var(--bg2);color:var(--sub);border:1px solid var(--border)">Manuell</span>`;
    const checked=_selPollenTypes.has(t)?'checked':'';
    return `<label style="display:flex;align-items:center;gap:10px;padding:10px 0;border-bottom:1px solid var(--border);cursor:pointer">
      <input type="checkbox" data-pollen="${esc(t)}" ${checked} style="width:18px;height:18px;accent-color:${col};cursor:pointer;flex-shrink:0">
      <span style="flex:1;font-size:14px;font-weight:500">🌿 ${esc(t)}</span>${badge}</label>`;
  }).join('');
  sheet.innerHTML=`
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px">
      <div style="font-size:15px;font-weight:700">🌿 Pollen im Chart anzeigen</div>
      <button id="ppc-close" style="padding:6px 12px;font-size:13px;border:1px solid var(--border);border-radius:var(--radius-sm);background:var(--bg2);color:var(--sub);cursor:pointer;font-family:inherit">✕</button>
    </div>
    <div style="font-size:12px;color:var(--sub);margin-bottom:12px">„Daten" = im Pollen_Log gefunden · „Manuell" = eigene Pollenart aus Wetter-Tab</div>
    <div style="display:flex;gap:8px;margin-bottom:12px">
      <button id="ppc-all" style="flex:1;padding:7px;font-size:12px;border:1px solid var(--border);border-radius:var(--radius-sm);background:var(--bg2);color:var(--sub);cursor:pointer;font-family:inherit">✓ Alle</button>
      <button id="ppc-none" style="flex:1;padding:7px;font-size:12px;border:1px solid var(--border);border-radius:var(--radius-sm);background:var(--bg2);color:var(--sub);cursor:pointer;font-family:inherit">✗ Keine</button>
    </div>
    <div id="ppc-rows">${rows}</div>
    <button id="ppc-apply" style="width:100%;margin-top:16px;padding:12px;font-size:14px;font-weight:700;border:none;border-radius:var(--radius-sm);background:var(--c2);color:#fff;cursor:pointer;font-family:inherit">✓ Übernehmen & Chart aktualisieren</button>
  `;
  overlay.appendChild(sheet);
  document.body.appendChild(overlay);
  const close=()=>document.getElementById('pollen-select-popup')?.remove();
  overlay.addEventListener('click',e=>{if(e.target===overlay)close();});
  document.getElementById('ppc-close').addEventListener('click',close);
  document.getElementById('ppc-all').addEventListener('click',()=>{
    sheet.querySelectorAll('input[data-pollen]').forEach(cb=>cb.checked=true);
  });
  document.getElementById('ppc-none').addEventListener('click',()=>{
    sheet.querySelectorAll('input[data-pollen]').forEach(cb=>cb.checked=false);
  });
  document.getElementById('ppc-apply').addEventListener('click',async()=>{
    _selPollenTypes.clear();
    sheet.querySelectorAll('input[data-pollen]:checked').forEach(cb=>_selPollenTypes.add(cb.dataset.pollen));
    _updatePollenBtnLabel();
    close();
    if(_cachedData) await _buildChart(_cachedData);
  });
}

function _updatePollenBtnLabel() {
  const btn=document.getElementById('stat-pollen-btn'); if(!btn) return;
  const all=_getAllPollenTypes();
  const active=[..._selPollenTypes].filter(t=>all.includes(t)).length;
  btn.textContent=`🌿 Pollen (${active}/${all.length})`;
  btn.classList.toggle('sel',active>0);
}

// ════════════════════════════════════════════════════════════════
function _buildShell() {
  return `
  <div style="padding:1rem">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1rem">
      <div class="section-title" style="margin-bottom:0">📊 Statistik</div>
      <button onclick="STATISTIK.forceRefresh()"
        style="padding:7px 12px;font-size:12px;border:1px solid var(--border);
          border-radius:var(--radius-sm);background:var(--bg2);color:var(--sub);
          cursor:pointer;font-family:inherit">↺ Aktualisieren</button>
    </div>
    <div style="display:flex;gap:8px;margin-bottom:8px">
      <select id="stat-hund" onchange="STATISTIK.refresh()"
        style="flex:1;padding:10px 12px;font-size:14px;border:1px solid var(--border);
          border-radius:var(--radius-sm);background:var(--bg);color:var(--text);font-family:inherit">
      </select>
      <select id="stat-range" onchange="STATISTIK.refresh()"
        style="width:110px;padding:10px 12px;font-size:14px;border:1px solid var(--border);
          border-radius:var(--radius-sm);background:var(--bg);color:var(--text);font-family:inherit">
        <option value="30">30 Tage</option>
        <option value="90" selected>90 Tage</option>
        <option value="180">6 Monate</option>
        <option value="365">1 Jahr</option>
        <option value="0">Alles</option>
      </select>
    </div>
    <div id="stat-cache-status"
      style="font-size:11px;color:var(--sub);margin-bottom:1rem;padding:6px 10px;
        background:var(--bg2);border-radius:var(--radius-sm);border:1px solid var(--border)">
      Wird geladen…
    </div>
    <div style="background:var(--bg2);border:1px solid var(--border);border-radius:var(--radius);
      padding:12px;margin-bottom:1rem">
      <div style="font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:.05em;
        color:var(--c2);margin-bottom:10px">Parameter auswählen</div>
      <div id="stat-param-btns" style="display:flex;flex-wrap:wrap;gap:6px"></div>
    </div>
    <div id="stat-content">
      <div class="view-loading"><div class="spinner"></div>Daten werden geladen…</div>
    </div>
  </div>`;
}

function _buildParamButtons() {
  const c=document.getElementById('stat-param-btns'); if(!c) return;
  let html=PARAM_DEFS.map(p=>`
    <button class="stat-param-btn tog-btn${_selected.has(p.key)?' sel':''}"
      data-key="${p.key}" data-group="param"
      onclick="STATISTIK.toggleParam('${p.key}')"
      style="font-size:12px;padding:6px 10px;border-color:${p.color}">
      ${p.emoji} ${p.label}
    </button>`).join('');
  const all=_getAllPollenTypes();
  const active=[..._selPollenTypes].filter(t=>all.includes(t)).length;
  html+=`
    <div style="width:100%;height:1px;background:var(--border);margin:4px 0"></div>
    <button id="stat-pollen-btn"
      class="stat-param-btn tog-btn${active>0?' sel':''}"
      onclick="STATISTIK.showPollenPopup()"
      style="font-size:12px;padding:6px 10px;border-color:${C.green}">
      🌿 Pollen (${active}/${all.length})
    </button>`;
  c.innerHTML=html;
}

async function _buildChart(data) {
  if(!window.Chart) await _loadScript('https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js');
  const canvas=document.getElementById('ch-konfig'); if(!canvas) return;
  if(_chart){try{_chart.destroy();}catch(e){} _chart=null;}

  const active=PARAM_DEFS.filter(p=>_selected.has(p.key));
  const allPolTypes=_getAllPollenTypes();
  const activePoll=allPolTypes.filter(t=>_selPollenTypes.has(t));

  if(!active.length&&!activePoll.length){
    canvas.style.display='none';
    if(!canvas.nextElementSibling?.classList?.contains('stat-no-data'))
      canvas.insertAdjacentHTML('afterend','<p class="stat-no-data" style="text-align:center;color:var(--sub);font-size:13px;margin-top:8px">Bitte oben mindestens einen Parameter auswählen.</p>');
    return;
  }
  canvas.style.display='';
  document.querySelector('.stat-no-data')?.remove();

  const datasets=[];

  // Pollen-Balken (y2)
  activePoll.forEach((t,i)=>{
    const col=POLLEN_COLORS[(allPolTypes.indexOf(t))%POLLEN_COLORS.length];
    const polMap=_byDate(data.pol.filter(r=>g(r,3)===t),2,r=>parseInt(g(r,4)),Math.max);
    datasets.push({
      label:`🌿 ${t}`,data:null,_map:polMap,type:'bar',
      backgroundColor:col+'aa',borderColor:col,borderWidth:1,yAxisID:'y2',
    });
  });

  // Standard-Parameter
  for(const p of active){
    if(p.chartType==='band'){
      const maps=p.extract(data);
      datasets.push({
        label:'🌡 Temp. Max',data:null,_map:maps.max,type:'line',
        borderColor:C.orange,backgroundColor:'rgba(249,115,22,.12)',
        borderWidth:1.5,pointRadius:0,tension:0.3,fill:'+1',yAxisID:'y',spanGaps:true,
      });
      datasets.push({
        label:'🌡 Temp. Min',data:null,_map:maps.min,type:'line',
        borderColor:C.blue,backgroundColor:'transparent',
        borderWidth:1.5,pointRadius:0,tension:0.3,fill:false,
        borderDash:[3,3],yAxisID:'y',spanGaps:true,
      });
    } else if(p.chartType==='area'){
      // Rotes gefülltes Band: fill von y=0 bis zur Linie
      const map=p.extract(data);
      datasets.push({
        label:`${p.emoji} ${p.label}`,data:null,_map:map,type:'line',
        borderColor:p.color,backgroundColor:p.colorL,
        borderWidth:2,pointRadius:3,pointHoverRadius:5,
        pointBackgroundColor:p.color,
        tension:0.1,fill:'origin',
        yAxisID:p.yAxis,spanGaps:false,
      });
    } else if(p.chartType==='bar_param'){
      const map=p.extract(data);
      datasets.push({
        label:`${p.emoji} ${p.label}`,data:null,_map:map,type:'bar',
        backgroundColor:p.colorL,borderColor:p.color,borderWidth:1,
        yAxisID:p.yAxis,spanGaps:false,
      });
    } else {
      const map=p.extract(data);
      datasets.push({
        label:`${p.emoji} ${p.label}`,data:null,_map:map,type:'line',
        borderColor:p.color,backgroundColor:p.colorL,
        borderWidth:2,pointRadius:undefined,pointHoverRadius:5,
        tension:0.3,fill:false,
        borderDash:p.dashed?[4,4]:undefined,
        yAxisID:p.yAxis,spanGaps:true,
      });
    }
  }

  const allDates=[...new Set(datasets.flatMap(d=>Object.keys(d._map||{})))].sort();
  if(!allDates.length){
    canvas.style.display='none';
    canvas.insertAdjacentHTML('afterend','<p class="stat-no-data" style="text-align:center;color:var(--sub);font-size:13px">Keine Daten im gewählten Zeitraum.</p>');
    return;
  }
  datasets.forEach(d=>{
    d.data=allDates.map(date=>{const v=d._map?.[date];return(v!==undefined&&!isNaN(v))?v:null;});
    if(allDates.length>60&&d.type==='line') d.pointRadius=0;
    delete d._map;
  });

  const hasY=datasets.some(d=>d.yAxisID==='y');
  const hasY2=datasets.some(d=>d.yAxisID==='y2');
  const scales={};
  if(hasY)  scales.y ={type:'linear',position:'left',ticks:{font:{size:10},maxTicksLimit:6},grid:{color:'rgba(150,150,150,.1)'}};
  if(hasY2) scales.y2={type:'linear',position:'right',min:0,max:5,
    ticks:{font:{size:10},stepSize:1,callback:v=>(['–','gering','gering–m.','mittel','m.–stark','stark'][v]||v)},
    grid:{drawOnChartArea:false}};
  scales.x={
    ticks:{font:{size:10},maxTicksLimit:allDates.length>30?8:allDates.length,callback:(_,i)=>_fmtLabel(allDates[i])},
    grid:{color:'rgba(150,150,150,.05)'},
  };

  _chart=new window.Chart(canvas.getContext('2d'),{
    type:'bar',
    data:{labels:allDates,datasets},
    options:{
      responsive:true,
      interaction:{mode:'index',intersect:false},
      plugins:{
        legend:{display:true,labels:{boxWidth:10,font:{size:11},padding:8}},
        tooltip:{callbacks:{
          title:items=>_fmtLabel(items[0]?.label||''),
          label:item=>{const v=item.raw;if(v===null||v===undefined)return null;
            return ` ${item.dataset.label}: ${typeof v==='number'?v.toFixed(v<10?1:0):v}`;}
        }},
      },
      scales,
    },
  });
}

function _renderAllergene(all) {
  const el=document.getElementById('st-allergene'); if(!el) return;
  el.innerHTML=all.length?all.map(r=>{
    const reakt=parseInt(g(r,3))||0;
    const color=reakt>=4?C.red:reakt>=3?C.amber:C.green;
    return `<div style="display:flex;justify-content:space-between;align-items:center;
      padding:10px 0;border-bottom:1px solid var(--border)">
      <div><div style="font-size:14px;font-weight:600">${esc(g(r,1))}</div>
        <div style="font-size:12px;color:var(--sub)">${esc(g(r,2))} · ${esc(g(r,4))}</div></div>
      <div style="font-size:18px;color:${color};letter-spacing:2px">
        ${'●'.repeat(reakt)}${'○'.repeat(5-reakt)}</div></div>`;
  }).join(''):'<p style="color:var(--sub);font-size:13px">Keine Allergene erfasst.</p>';
}

function _renderAusschluss(aus) {
  const el=document.getElementById('st-aus'); if(!el) return;
  const statusColor=s=>{
    if(!s) return C.amber;
    const sl=s.toLowerCase();
    if(sl.includes('vertr')) return C.green;
    if(sl.includes('reaktion')||sl.includes('gesperrt')) return C.red;
    return C.amber;
  };
  const sorted=[...aus].sort((a,b)=>(g(a,4)||'').localeCompare(g(b,4)||''));
  el.innerHTML=sorted.map(r=>{
    const status=g(r,4)||'–';
    const verdacht=parseInt(g(r,2))||0;
    const col=statusColor(status);
    return `<div style="display:flex;justify-content:space-between;align-items:center;
      padding:9px 0;border-bottom:1px solid var(--border)">
      <div style="flex:1;min-width:0">
        <div style="font-size:13px;font-weight:600">${esc(g(r,1))}</div>
        ${g(r,3)?`<div style="font-size:11px;color:var(--sub)">${esc(g(r,3))}</div>`:''}
        ${g(r,6)?`<div style="font-size:11px;color:var(--sub);margin-top:2px">↳ ${esc(g(r,6))}</div>`:''}
      </div>
      <div style="display:flex;flex-direction:column;align-items:flex-end;gap:3px;flex-shrink:0;margin-left:8px">
        <span style="font-size:11px;font-weight:700;padding:2px 8px;border-radius:10px;
          background:${col}22;color:${col};border:1px solid ${col}44">${esc(status)}</span>
        ${verdacht>0?`<span style="font-size:10px;color:var(--sub)">Verdacht: ${'▲'.repeat(verdacht)}${'△'.repeat(3-verdacht)}</span>`:''}
      </div></div>`;
  }).join('')||'<p style="color:var(--sub);font-size:13px">Keine Ausschlussdiät-Einträge.</p>';
}

function _renderFutter(fut) {
  const el=document.getElementById('st-futter'); if(!el) return;
  const reakt=fut.filter(r=>g(r,7)||g(r,6)==='Ja');
  el.innerHTML=reakt.length?reakt.map(r=>`
    <div style="padding:10px 0;border-bottom:1px solid var(--border)">
      <div style="display:flex;justify-content:space-between;align-items:flex-start">
        <div style="font-size:13px;font-weight:600">${esc(g(r,1))}</div>
        ${g(r,6)==='Ja'?'<span class="badge badge-warn">⚠️ Provokation</span>':g(r,4)==='Ja'?'<span class="badge badge-ok">Erste Gabe</span>':''}
      </div>
      ${g(r,3)?`<div style="font-size:12px;color:var(--sub)">${esc(g(r,3))}</div>`:''}
      ${g(r,7)?`<div style="font-size:13px;margin-top:4px">${esc(g(r,7))}</div>`:''}
    </div>`).join(''):'<p style="color:var(--sub);font-size:13px">Keine Reaktionen im Zeitraum.</p>';
}

function _renderMedis(med) {
  const el=document.getElementById('st-medis'); if(!el) return;
  el.innerHTML=med.length?med.map(r=>`
    <div style="display:flex;justify-content:space-between;align-items:center;
      padding:10px 0;border-bottom:1px solid var(--border)">
      <div>
        <div style="font-size:14px;font-weight:600">${esc(g(r,1))}</div>
        <div style="font-size:12px;color:var(--sub)">${esc(g(r,2))} · ${esc(g(r,3))}</div>
        ${g(r,4)?`<div style="font-size:12px;color:var(--sub)">${esc(g(r,4))}</div>`:''}
      </div>
      <div style="font-size:12px;color:var(--sub);text-align:right">
        ${esc(g(r,5)||'?')}<br>bis ${esc(g(r,6)||'laufend')}</div>
    </div>`).join(''):'<p style="color:var(--sub);font-size:13px">Keine Medikamente erfasst.</p>';
}

// ── Hilfsfunktionen ──────────────────────────────────────────────
const g=(row,i)=>(row[i]??'').toString().trim();
function _parseRows(rawRows,skipRows){
  if(!rawRows?.length) return [];
  return rawRows.slice(skipRows).filter(r=>r?.some(v=>v!==null&&v!==undefined&&String(v).trim()!==''));
}
function _matchH(row,hundId){return !g(row,0)||g(row,0)===String(hundId);}
function _parseDate(str){
  if(!str) return null;
  if(str.includes('.')){const[d,m,y]=str.split('.');const yr=y?.length===2?'20'+y:y;const date=new Date(parseInt(yr),parseInt(m)-1,parseInt(d));return isNaN(date.getTime())?null:date;}
  if(str.includes('-')){const date=new Date(str);return isNaN(date.getTime())?null:date;}
  return null;
}
function _toISO(str){const d=_parseDate(str);return d?d.toISOString().slice(0,10):null;}
function _inRange(datum,cutoff){const d=_parseDate(datum);return d&&d>=cutoff;}
function _fmtLabel(iso){if(!iso)return'';const d=new Date(iso);return`${String(d.getDate()).padStart(2,'0')}.${String(d.getMonth()+1).padStart(2,'0')}`;}
function _byDate(rows,dateCol,valFn,aggFn){
  const map={};
  rows.forEach(r=>{
    const iso=_toISO(g(r,dateCol));if(!iso)return;
    const v=valFn(r);if(v===undefined||v===null||isNaN(v))return;
    if(map[iso]===undefined)map[iso]=v;
    else if(aggFn)map[iso]=aggFn(map[iso],v);
    else map[iso]=v;
  });
  return map;
}
function _kpi(label,value,color){
  return `<div style="background:var(--bg2);border:1px solid var(--border);border-radius:var(--radius);
    padding:12px;text-align:center">
    <div style="font-size:24px;font-weight:700;color:${color}">${value}</div>
    <div style="font-size:11px;color:var(--sub);text-transform:uppercase;letter-spacing:.04em;margin-top:2px">${label}</div>
  </div>`;
}
function _box(title,body){
  return `<div style="background:var(--bg2);border:1px solid var(--border);
    border-radius:var(--radius);padding:14px;margin-bottom:1rem">
    <div style="font-size:14px;font-weight:700;margin-bottom:12px">${title}</div>${body}</div>`;
}
function _loadScript(src){
  return new Promise((resolve,reject)=>{
    if(document.querySelector(`script[src="${src}"]`)){resolve();return;}
    const s=document.createElement('script');
    s.src=src;s.onload=resolve;s.onerror=reject;
    document.head.appendChild(s);
  });
}
