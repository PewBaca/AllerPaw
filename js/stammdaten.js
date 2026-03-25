/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  MODULE: stammdaten.js                                       ║
 * ║  Hund Manager – CRUD für Stammdaten                          ║
 * ║                                                              ║
 * ║  Verantwortlich für:                                         ║
 * ║  - Hunde: Anzeigen, Anlegen, Bearbeiten, De-/Aktivieren      ║
 * ║  - Zutaten: Anzeigen, Anlegen                                ║
 * ║  - Parameter: Anzeigen, Bearbeiten (direkt im Sheet)         ║
 * ║                                                              ║
 * ║  Abhängigkeiten: sheets.js, config.js, ui.js, store.js       ║
 * ╚══════════════════════════════════════════════════════════════╝
 */

import { readSheet, writeRange, appendRow } from './sheets.js';
import { get as getCfg }                    from './config.js';
import { openModal, closeModal, setStatus,
         syncHundSelects, esc }             from './ui.js';
import { getHunde, getZutaten, addHund,
         updateHund, addZutat }             from './store.js';

// ── Aktuell geöffneter Tab ───────────────────────────────────────
let currentTab = 'hunde';

export function loadCurrentTab() { loadTab(currentTab); }

export function loadTab(tab) {
  currentTab = tab;
  if (tab === 'hunde')     loadHunde();
  if (tab === 'zutaten')   loadZutaten();
  if (tab === 'parameter') loadParameter();
}

// ════════════════════════════════════════════════════════════════
//  HUNDE
// ════════════════════════════════════════════════════════════════

export function loadHunde() {
  const el    = document.getElementById('sd-hunde-list');
  const hunde = getHunde();
  if (!hunde.length) {
    el.innerHTML = '<div class="view-empty"><div class="icon">🐕</div>Noch keine Hunde angelegt.</div>';
    return;
  }
  el.innerHTML = hunde.map(h => `
    <div class="card" style="margin-bottom:10px">
      <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px">
        <div style="flex:1">
          <div class="card-title" style="margin-bottom:6px">
            ${esc(h.name)}
            ${h.aktiv === 'nein'
              ? ' <span class="badge badge-warn" style="font-size:11px">inaktiv</span>'
              : ' <span class="badge badge-ok"  style="font-size:11px">aktiv</span>'}
          </div>
          <div style="font-size:13px;color:var(--sub);line-height:1.9">
            ${h.rasse        ? '🐾 ' + esc(h.rasse)        + '<br>' : ''}
            ${h.geburtsdatum ? '🎂 ' + esc(h.geburtsdatum) + '<br>' : ''}
            ${h.geschlecht   ? (h.geschlecht === 'm' ? '♂ männlich' : '♀ weiblich')
                               + (h.kastriert === 'ja' ? ' · kastriert' : '') + '<br>' : ''}
            ${h.notizen      ? '📝 ' + esc(h.notizen) : ''}
          </div>
        </div>
        <div style="display:flex;flex-direction:column;gap:6px;flex-shrink:0">
          <button class="edit-btn" onclick="STAMMDATEN.showHundModal(${h.hund_id})">✏️ Bearbeiten</button>
          <button class="del-small-btn"
            onclick="STAMMDATEN.toggleHundAktiv(${h.hund_id},'${esc(h.name)}','${h.aktiv || 'ja'}')">
            ${h.aktiv === 'nein' ? '✅ Aktivieren' : '🚫 Deaktivieren'}
          </button>
        </div>
      </div>
    </div>`).join('');
}

export function showHundModal(hundId) {
  const hunde  = getHunde();
  const h      = hundId ? (hunde.find(x => x.hund_id === hundId) || {}) : {};
  const isEdit = !!hundId;

  // DD.MM.YYYY → YYYY-MM-DD für input[type=date]
  let gebVal = h.geburtsdatum || '';
  if (gebVal.includes('.')) {
    const [d, m, y] = gebVal.split('.');
    gebVal = `${y}-${m?.padStart(2,'0')}-${d?.padStart(2,'0')}`;
  }

  openModal(isEdit ? `🐕 ${esc(h.name || '')} bearbeiten` : 'Neuen Hund anlegen', `
    <div class="field"><label>Name *</label>
      <input type="text" id="hund-name" value="${esc(h.name || '')}" placeholder="z.B. Milow"></div>
    <div class="field"><label>Rasse</label>
      <input type="text" id="hund-rasse" value="${esc(h.rasse || '')}" placeholder="z.B. Labrador-Mix"></div>
    <div class="field"><label>Geburtsdatum</label>
      <input type="date" id="hund-geb" value="${esc(gebVal)}"></div>
    <div class="field"><label>Geschlecht</label>
      <select id="hund-geschlecht">
        <option value="">– wählen –</option>
        <option value="m" ${h.geschlecht === 'm' ? 'selected' : ''}>♂ männlich</option>
        <option value="w" ${h.geschlecht === 'w' ? 'selected' : ''}>♀ weiblich</option>
      </select></div>
    <div class="field"><label>Kastriert</label>
      <select id="hund-kastriert">
        <option value="nein" ${(h.kastriert || 'nein') === 'nein' ? 'selected' : ''}>Nein</option>
        <option value="ja"   ${h.kastriert === 'ja' ? 'selected' : ''}>Ja</option>
      </select></div>
    <div class="field"><label>Status</label>
      <select id="hund-aktiv">
        <option value="ja"   ${(h.aktiv || 'ja') === 'ja' ? 'selected' : ''}>✅ Aktiv</option>
        <option value="nein" ${h.aktiv === 'nein' ? 'selected' : ''}>🚫 Inaktiv</option>
      </select></div>
    <div class="field"><label>Notizen</label>
      <textarea id="hund-notizen" placeholder="z.B. Besonderheiten…">${esc(h.notizen || '')}</textarea></div>
    <button class="btn-primary" onclick="STAMMDATEN.saveHund(${hundId || 'null'})">
      ${isEdit ? '💾 Änderungen speichern' : '+ Hund anlegen'}
    </button>
    <div class="status" id="status-hund"></div>
  `);
}

export async function saveHund(existingId) {
  const name = document.getElementById('hund-name')?.value.trim();
  if (!name) { setStatus('status-hund', 'err', 'Bitte Name eingeben.'); return; }

  const sid    = getCfg().stammdatenId;
  const gebRaw = document.getElementById('hund-geb')?.value || '';
  let gebFmt   = gebRaw;
  if (gebRaw.includes('-')) {
    const [y, m, d] = gebRaw.split('-');
    gebFmt = `${d}.${m}.${y}`;
  }

  const aktiv  = document.getElementById('hund-aktiv')?.value      || 'ja';
  const rasse  = document.getElementById('hund-rasse')?.value.trim()     || '';
  const gesch  = document.getElementById('hund-geschlecht')?.value        || '';
  const kastr  = document.getElementById('hund-kastriert')?.value         || 'nein';
  const notizen= document.getElementById('hund-notizen')?.value.trim()    || '';

  setStatus('status-hund', 'loading', 'Wird gespeichert…');
  try {
    if (existingId) {
      const rows = await readSheet('Hunde', sid);
      const idx  = rows.findIndex(r => String(r[0]).trim() === String(existingId));
      if (idx < 0) throw new Error('Zeile für Hund ' + existingId + ' nicht gefunden.');
      await writeRange('Hunde', `A${idx + 1}:H${idx + 1}`,
        [[existingId, name, rasse, gebFmt, gesch, kastr, aktiv, notizen]], sid);
      updateHund(existingId, { name, rasse, geburtsdatum: gebFmt, geschlecht: gesch, kastriert: kastr, aktiv, notizen });
    } else {
      const hunde = getHunde();
      const newId = Math.max(0, ...hunde.map(h => h.hund_id)) + 1;
      await appendRow('Hunde', [newId, name, rasse, gebFmt, gesch, kastr, aktiv, notizen], sid);
      addHund({ hund_id: newId, name, rasse, geburtsdatum: gebFmt, geschlecht: gesch, kastriert: kastr, aktiv, notizen });
    }
    setStatus('status-hund', 'ok', '✓ Gespeichert!');
    syncHundSelects();
    setTimeout(() => { closeModal(); loadHunde(); }, 900);
  } catch (e) { setStatus('status-hund', 'err', 'Fehler: ' + e.message); }
}

export async function toggleHundAktiv(hundId, name, aktuell) {
  const neuAktiv = aktuell === 'nein' ? 'ja' : 'nein';
  const verb     = neuAktiv === 'nein' ? 'deaktivieren' : 'aktivieren';
  if (!confirm(`"${name}" wirklich ${verb}?`)) return;

  const sid = getCfg().stammdatenId;
  try {
    const rows = await readSheet('Hunde', sid);
    const idx  = rows.findIndex(r => String(r[0]).trim() === String(hundId));
    if (idx < 0) throw new Error('Hund nicht gefunden');
    await writeRange('Hunde', `G${idx + 1}`, [[neuAktiv]], sid);
    updateHund(hundId, { aktiv: neuAktiv });
    syncHundSelects();
    loadHunde();
  } catch (e) { alert('Fehler: ' + e.message); }
}

// ════════════════════════════════════════════════════════════════
//  ZUTATEN
// ════════════════════════════════════════════════════════════════

export function loadZutaten() {
  const el      = document.getElementById('sd-zutaten-list');
  const zutaten = getZutaten().filter(z => String(z.deleted ?? '').toUpperCase() !== 'TRUE');
  if (!zutaten.length) {
    el.innerHTML = '<div class="view-empty"><div class="icon">🥩</div>Noch keine Zutaten.</div>';
    return;
  }
  const sorted = [...zutaten].sort(
    (a, b) => (a.kategorie || '').localeCompare(b.kategorie || '') || a.name.localeCompare(b.name, 'de')
  );
  el.innerHTML = `<table class="crud-table">
    <thead><tr><th>ID</th><th>Name</th><th>Hersteller</th><th>Kategorie</th><th></th></tr></thead>
    <tbody>
    ${sorted.map(z => `
      <tr>
        <td style="color:var(--sub);font-size:11px">${esc(String(z.zutaten_id))}</td>
        <td style="font-weight:500">${esc(z.name)}</td>
        <td style="color:var(--sub)">${esc(z.hersteller || '–')}</td>
        <td><span class="badge badge-ok" style="font-size:10px">${esc(z.kategorie || '–')}</span></td>
        <td style="white-space:nowrap">
          <button class="edit-btn" style="font-size:11px;padding:4px 8px"
            onclick="STAMMDATEN.showZutatModal(${z.zutaten_id})">✏️</button>
          <button class="del-small-btn" style="font-size:11px;padding:4px 8px;margin-left:4px"
            onclick="STAMMDATEN.deleteZutat(${z.zutaten_id},'${esc(z.name)}')">🗑</button>
        </td>
      </tr>`).join('')}
    </tbody>
  </table>`;
}

export function showZutatModal(zutatId) {
  const cats    = [...new Set(getZutaten().map(z => z.kategorie).filter(Boolean))].sort();
  const z       = zutatId ? (getZutaten().find(x => x.zutaten_id === zutatId) || {}) : {};
  const isEdit  = !!zutatId;

  openModal(isEdit ? `🥩 ${esc(z.name || '')} bearbeiten` : 'Neue Zutat', `
    <div class="field"><label>Name</label>
      <input type="text" id="zutat-name" value="${esc(z.name || '')}" placeholder="z.B. Pferd (Muskelfleisch)"></div>
    <div class="field"><label>Hersteller</label>
      <input type="text" id="zutat-hersteller" value="${esc(z.hersteller || '')}" placeholder="z.B. barfers"></div>
    <div class="field"><label>Kategorie</label>
      <select id="zutat-kat">
        <option value="">– wählen –</option>
        ${cats.map(c => `<option value="${esc(c)}" ${z.kategorie === c ? 'selected' : ''}>${esc(c)}</option>`).join('')}
      </select></div>
    <div class="field"><label>Status</label>
      <select id="zutat-aktiv">
        <option value="ja"   ${(z.aktiv || 'ja') === 'ja'   ? 'selected' : ''}>✅ Aktiv</option>
        <option value="nein" ${z.aktiv === 'nein'            ? 'selected' : ''}>🚫 Inaktiv</option>
      </select></div>
    ${isEdit ? '' : '<p style="font-size:12px;color:var(--sub);margin-bottom:.5rem">Nährstoffe können danach im Futterrechner über „Neue Zutat manuell erfassen" hinzugefügt werden.</p>'}
    <button class="btn-primary" onclick="STAMMDATEN.saveZutat(${zutatId || 'null'})">
      ${isEdit ? '💾 Änderungen speichern' : 'Speichern'}
    </button>
    <div class="status" id="status-zutat"></div>
  `);
}

export async function saveZutat(existingId) {
  const name = document.getElementById('zutat-name')?.value.trim();
  if (!name) { setStatus('status-zutat', 'err', 'Bitte Name eingeben.'); return; }

  const sid        = getCfg().stammdatenId;
  const hersteller = document.getElementById('zutat-hersteller')?.value.trim() || '';
  const kategorie  = document.getElementById('zutat-kat')?.value || 'Sonstiges';
  const aktiv      = document.getElementById('zutat-aktiv')?.value || 'ja';

  setStatus('status-zutat', 'loading', 'Wird gespeichert…');
  try {
    if (existingId) {
      // Bestehenede Zutat bearbeiten: Zeile im Sheet finden und überschreiben
      const rows = await readSheet('Zutaten', sid);
      const idx  = rows.findIndex(r => String(r[0]).trim() === String(existingId));
      if (idx < 0) throw new Error('Zutat ' + existingId + ' nicht gefunden.');
      // Spalten A–E überschreiben; Spalten F–H (created_at, deleted, deleted_at) bleiben unberührt
      await writeRange('Zutaten', `A${idx + 1}:E${idx + 1}`,
        [[existingId, name, hersteller, kategorie, aktiv]], sid);
      // Cache aktualisieren
      const z = getZutaten().find(x => x.zutaten_id === existingId);
      if (z) Object.assign(z, { name, hersteller, kategorie, aktiv });
    } else {
      // Neue Zutat anlegen
      const zutaten = getZutaten();
      const newId   = Math.max(0, ...zutaten.map(z => z.zutaten_id)) + 1;
      const now     = new Date().toISOString().slice(0, 19);
      await appendRow('Zutaten', [newId, name, hersteller, kategorie, aktiv, now, 'FALSE', ''], sid);
      addZutat({ zutaten_id: newId, name, hersteller, kategorie, aktiv });
    }
    setStatus('status-zutat', 'ok', '✓ Gespeichert!');
    // Futterrechner-Dropdown aktualisieren
    const { initIngredientSelect } = await import('./rechner.js');
    initIngredientSelect();
    setTimeout(() => { closeModal(); loadZutaten(); }, 900);
  } catch (e) { setStatus('status-zutat', 'err', 'Fehler: ' + e.message); }
}

// ── Undo-Stack für Zutaten-Löschungen ───────────────────────────
// Format: { zutatId, sheetRow, vorher: { aktiv, deleted, deleted_at } }
const _zutatUndoStack = [];
const _MAX_ZUTAT_UNDO = 5;

/**
 * Zutat per Soft-Delete deaktivieren.
 * Setzt deleted=TRUE in Spalte G (nach Migration) oder aktiv=nein als Fallback.
 * Legt Undo-Eintrag ab und zeigt Banner.
 *
 * @param {number} zutatId
 * @param {string} name
 */
export async function deleteZutat(zutatId, name) {
  if (!confirm(`Zutat "${name}" löschen?\n\nKann über den „Rückgängig"-Button wiederhergestellt werden.`)) return;

  const sid = getCfg().stammdatenId;
  try {
    const rows = await readSheet('Zutaten', sid);
    const idx  = rows.findIndex(r => String(r[0]).trim() === String(zutatId));
    if (idx < 0) { alert('Zutat nicht gefunden.'); return; }

    const sheetRow = idx + 1;
    const row      = rows[idx];
    const now      = new Date().toISOString().slice(0, 19);

    // Vorherigen Zustand für Undo merken
    const vorher = {
      aktiv:      String(row[4] ?? 'ja'),
      deleted:    String(row[6] ?? 'FALSE'),
      deleted_at: String(row[7] ?? ''),
    };

    if (row.length >= 7) {
      // Migrierte Tabelle: deleted=TRUE in G, deleted_at in H
      await writeRange('Zutaten', `G${sheetRow}:H${sheetRow}`, [['TRUE', now]], sid);
    } else {
      // Vor Migration: aktiv=nein setzen (Spalte E)
      await writeRange('Zutaten', `E${sheetRow}`, [['nein']], sid);
    }

    // Store-Cache aktualisieren
    const z = getZutaten().find(x => x.zutaten_id === zutatId);
    if (z) { z.aktiv = 'nein'; z.deleted = 'TRUE'; }

    // Undo-Stack befüllen
    _zutatUndoStack.unshift({ zutatId, zutatName: name, sheetRow, vorher, migriert: row.length >= 7 });
    if (_zutatUndoStack.length > _MAX_ZUTAT_UNDO) _zutatUndoStack.pop();

    // Futterrechner-Dropdown aktualisieren
    const { initIngredientSelect } = await import('./rechner.js');
    initIngredientSelect();

    loadZutaten();
    _showZutatUndoBanner(name);
  } catch (e) { alert('Fehler: ' + e.message); }
}

/**
 * Letzte Zutaten-Löschung rückgängig machen.
 */
export async function undoDeleteZutat() {
  const entry = _zutatUndoStack[0];
  if (!entry) return;

  const sid = getCfg().stammdatenId;
  try {
    if (entry.migriert) {
      await writeRange('Zutaten', `G${entry.sheetRow}:H${entry.sheetRow}`,
        [[entry.vorher.deleted, entry.vorher.deleted_at]], sid);
    } else {
      await writeRange('Zutaten', `E${entry.sheetRow}`, [[entry.vorher.aktiv]], sid);
    }

    // Store-Cache zurücksetzen
    const z = getZutaten().find(x => x.zutaten_id === entry.zutatId);
    if (z) { z.aktiv = entry.vorher.aktiv; z.deleted = entry.vorher.deleted; }

    _zutatUndoStack.shift();

    const { initIngredientSelect } = await import('./rechner.js');
    initIngredientSelect();

    document.getElementById('zutat-undo-banner')?.remove();
    loadZutaten();
  } catch (e) { alert('Fehler beim Wiederherstellen: ' + e.message); }
}

/** Undo-Banner für Zutaten-Löschung einblenden */
function _showZutatUndoBanner(name) {
  document.getElementById('zutat-undo-banner')?.remove();
  const el = document.createElement('div');
  el.id = 'zutat-undo-banner';
  el.style.cssText = `
    position:fixed;bottom:80px;left:50%;transform:translateX(-50%);
    background:var(--text);color:var(--bg);
    padding:10px 16px;border-radius:var(--radius);
    font-size:13px;font-weight:600;z-index:9999;
    display:flex;align-items:center;gap:12px;
    box-shadow:0 4px 20px rgba(0,0,0,.3);white-space:nowrap;
  `;
  el.innerHTML = `
    <span>„${name}" gelöscht</span>
    <button onclick="STAMMDATEN.undoDeleteZutat()"
      style="padding:5px 10px;font-size:12px;border:none;border-radius:4px;
        background:var(--c2);color:#fff;cursor:pointer;font-family:inherit;font-weight:700">
      ↺ Rückgängig
    </button>
  `;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 8000);
}

// ════════════════════════════════════════════════════════════════
//  PARAMETER
// ════════════════════════════════════════════════════════════════

export async function loadParameter() {
  const el = document.getElementById('sd-parameter-list');
  el.innerHTML = '<div class="view-loading"><div class="spinner"></div></div>';
  try {
    const rows   = await readSheet('Parameter', getCfg().stammdatenId);
    const params = rows.slice(2).filter(r => r && r[0]);

    el.innerHTML = params.map((r, i) => `
      <div class="card" style="margin-bottom:8px">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:10px">
          <div style="flex:1">
            <div style="font-size:13px;font-weight:600">${esc(r[0] || '')}</div>
            <div style="font-size:11px;color:var(--sub);margin-top:2px">${esc(r[3] || '')}</div>
          </div>
          <div style="display:flex;align-items:center;gap:8px;flex-shrink:0">
            <input type="text" id="param-val-${i}" value="${esc(r[1] || '')}"
              style="width:90px;padding:6px 10px;font-size:14px;font-weight:600;
                border:1px solid var(--border);border-radius:var(--radius-sm);
                background:var(--bg);color:var(--text);text-align:center">
            <span style="font-size:12px;color:var(--sub)">${esc(r[2] || '')}</span>
          </div>
        </div>
      </div>`).join('') +
      `<button class="btn-primary" onclick="STAMMDATEN.saveParameter(${params.length})">
        💾 Parameter speichern
      </button>
      <div class="status" id="status-param"></div>`;
  } catch (e) {
    el.innerHTML = `<div class="status err" style="display:block">Fehler: ${esc(e.message)}</div>`;
  }
}

export async function saveParameter(count) {
  setStatus('status-param', 'loading', 'Wird gespeichert…');
  try {
    const sid = getCfg().stammdatenId;
    for (let i = 0; i < count; i++) {
      const val = document.getElementById(`param-val-${i}`)?.value;
      if (val !== undefined) {
        await writeRange('Parameter', `B${i + 3}`, [[val]], sid);
      }
    }
    setStatus('status-param', 'ok', '✓ Gespeichert! Seite neu laden um Änderungen zu übernehmen.');
  } catch (e) { setStatus('status-param', 'err', 'Fehler: ' + e.message); }
}
