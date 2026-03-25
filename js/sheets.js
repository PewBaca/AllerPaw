/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  MODULE: sheets.js                                           ║
 * ║  Hund Manager – Google Sheets API Layer                      ║
 * ║                                                              ║
 * ║  Verantwortlich für:                                         ║
 * ║  - Alle Google Sheets REST API v4 Aufrufe                    ║
 * ║  - Zeilen anhängen (appendRow)                               ║
 * ║  - Blätter lesen (readSheet)                                 ║
 * ║  - Bereiche schreiben (writeRange)                           ║
 * ║  - Tabellenblätter erstellen / löschen                       ║
 * ║  - Tabellenblatt-Liste abrufen                               ║
 * ║                                                              ║
 * ║  WICHTIG: Kein anderes Modul greift direkt auf die           ║
 * ║  Sheets API zu – immer über dieses Modul!                    ║
 * ║                                                              ║
 * ║  Abhängigkeiten: auth.js                                     ║
 * ║  Wird importiert von: store.js, tagebuch.js, ansicht.js,     ║
 * ║    rechner.js, stammdaten.js, config.js (dynamisch)          ║
 * ╚══════════════════════════════════════════════════════════════╝
 */

import { getToken, handleExpired } from './auth.js';

// ── Sheets API Base URL ──────────────────────────────────────────
const BASE = 'https://sheets.googleapis.com/v4/spreadsheets';

/**
 * Gemeinsame Fehlerbehandlung für alle API-Calls.
 * Bei 401 → Session abgelaufen.
 * @param {Response} res
 */
async function handleResponse(res) {
  if (res.status === 401) {
    handleExpired();
    throw new Error('Sitzung abgelaufen – bitte neu anmelden.');
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body?.error?.message || `HTTP ${res.status}`);
  }
  return res.json();
}

/**
 * Auth-Header erzeugen. Wirft wenn kein Token vorhanden.
 */
function authHeaders(extra = {}) {
  const token = getToken();
  if (!token) throw new Error('Nicht angemeldet – bitte zuerst einloggen.');
  return { Authorization: 'Bearer ' + token, ...extra };
}

// ════════════════════════════════════════════════════════════════
//  LESEN
// ════════════════════════════════════════════════════════════════

/**
 * Gesamtes Tabellenblatt lesen.
 * Gibt alle Zeilen als string[][] zurück (Zeilen × Spalten).
 * Leeres Blatt → [].
 *
 * @param {string} sheet         - Name des Tabellenblatts, z.B. 'Hunde'
 * @param {string} spreadsheetId - ID des Spreadsheets
 * @returns {Promise<string[][]>}
 */
export async function readSheet(sheet, spreadsheetId) {
  const range = encodeURIComponent(sheet);
  const url   = `${BASE}/${spreadsheetId}/values/${range}` +
                `?majorDimension=ROWS&valueRenderOption=FORMATTED_VALUE`;

  const res  = await fetch(url, { headers: authHeaders() });
  const data = await handleResponse(res);
  return data.values || [];
}

// ════════════════════════════════════════════════════════════════
//  SCHREIBEN
// ════════════════════════════════════════════════════════════════

/**
 * Eine Zeile ans Ende eines Tabellenblatts anhängen.
 *
 * @param {string}  sheet         - Name des Tabellenblatts
 * @param {Array}   values        - Flaches Array der Zellenwerte
 * @param {string}  spreadsheetId - ID des Spreadsheets
 * @returns {Promise<Object>}     - Sheets API Antwort
 */
export async function appendRow(sheet, values, spreadsheetId) {
  const range = encodeURIComponent(`${sheet}!A:ZZ`);
  const url   = `${BASE}/${spreadsheetId}/values/${range}:append` +
                `?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS`;

  const res = await fetch(url, {
    method:  'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body:    JSON.stringify({ values: [values] }),
  });
  return handleResponse(res);
}

/**
 * Einen bestimmten Bereich überschreiben (PUT).
 * Nützlich für Updates einzelner Zeilen oder Zellen.
 *
 * @param {string}   sheet         - Name des Tabellenblatts
 * @param {string}   range         - A1-Notation, z.B. 'A5:H5' oder 'B10'
 * @param {Array[][]}values        - 2D Array (Zeilen × Spalten)
 * @param {string}   spreadsheetId - ID des Spreadsheets
 * @returns {Promise<Object>}
 */
export async function writeRange(sheet, range, values, spreadsheetId) {
  const r   = encodeURIComponent(`${sheet}!${range}`);
  const url = `${BASE}/${spreadsheetId}/values/${r}?valueInputOption=USER_ENTERED`;

  const res = await fetch(url, {
    method:  'PUT',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body:    JSON.stringify({ values }),
  });
  return handleResponse(res);
}

// ════════════════════════════════════════════════════════════════
//  TABELLENBLÄTTER VERWALTEN
// ════════════════════════════════════════════════════════════════

/**
 * Neues Tabellenblatt im Spreadsheet anlegen.
 *
 * @param {string} sheetName      - Name des neuen Blatts
 * @param {string} spreadsheetId
 * @returns {Promise<Object>}
 */
export async function createSheet(sheetName, spreadsheetId) {
  const url = `${BASE}/${spreadsheetId}:batchUpdate`;
  const res = await fetch(url, {
    method:  'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body:    JSON.stringify({
      requests: [{ addSheet: { properties: { title: sheetName } } }],
    }),
  });
  return handleResponse(res);
}

/**
 * Neues Tabellenblatt erstellen und sofort Anzeige- + API-Header setzen.
 *
 * Zeile 1: displayHeaders (deutsch / für Menschen)
 * Zeile 2: apiHeaders     (englisch, snake_case – für JS-Code)
 * Daten ab Zeile 3.
 *
 * Falls das Sheet bereits existiert, wird es NICHT neu erstellt (idempotent).
 * Gibt true zurück wenn neu erstellt, false wenn bereits vorhanden.
 *
 * @param {string}   sheetName      - Name des neuen Blatts
 * @param {string[]} displayHeaders - Zeile 1
 * @param {string[]} apiHeaders     - Zeile 2
 * @param {string}   spreadsheetId
 * @returns {Promise<boolean>}      - true = neu erstellt, false = bereits vorhanden
 */
export async function createSheetWithHeaders(sheetName, displayHeaders, apiHeaders, spreadsheetId) {
  // Idempotenz: prüfen ob Sheet schon existiert
  const existing = await getSheetsList(spreadsheetId);
  if (existing.includes(sheetName)) {
    console.info(`sheets.js: "${sheetName}" bereits vorhanden, übersprungen.`);
    return false;
  }

  // Sheet erstellen
  await createSheet(sheetName, spreadsheetId);

  // Kurze Pause – Google API braucht einen Moment bevor der erste Write möglich ist
  await new Promise(r => setTimeout(r, 600));

  // Beide Header-Zeilen schreiben
  await writeRange(sheetName, 'A1', [displayHeaders, apiHeaders], spreadsheetId);

  console.info(`sheets.js: "${sheetName}" erstellt mit ${displayHeaders.length} Spalten.`);
  return true;
}

/**
 * Hilfsfunktion: Sheet sicherstellen (erstellen falls nicht vorhanden).
 * Kein Fehler wenn Sheet bereits existiert.
 *
 * @param {string}   sheetName
 * @param {string}   spreadsheetId
 * @returns {Promise<boolean>}  true = neu erstellt, false = bereits vorhanden
 */
export async function ensureSheet(sheetName, spreadsheetId) {
  const existing = await getSheetsList(spreadsheetId);
  if (existing.includes(sheetName)) return false;
  await createSheet(sheetName, spreadsheetId);
  await new Promise(r => setTimeout(r, 600));
  return true;
}

/**
 * Tabellenblatt löschen (per interner sheetId, nicht per Name).
 * sheetId aus getSheetsWithIds() holen.
 *
 * @param {number} sheetId        - Interne Sheet-ID (aus Sheets API)
 * @param {string} spreadsheetId
 * @returns {Promise<Object>}
 */
export async function deleteSheet(sheetId, spreadsheetId) {
  const url = `${BASE}/${spreadsheetId}:batchUpdate`;
  const res = await fetch(url, {
    method:  'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body:    JSON.stringify({
      requests: [{ deleteSheet: { sheetId } }],
    }),
  });
  return handleResponse(res);
}

/**
 * Liste aller Tabellenblatt-Namen zurückgeben.
 * Verwendet für den Verbindungstest in config.js.
 *
 * @param {string} spreadsheetId
 * @returns {Promise<string[]>} Array von Blattnamen
 */
export async function getSheetsList(spreadsheetId) {
  const url  = `${BASE}/${spreadsheetId}?fields=sheets.properties`;
  const res  = await fetch(url, { headers: authHeaders() });
  const data = await handleResponse(res);
  return (data.sheets || []).map(s => s.properties.title);
}

/**
 * Liste aller Tabellenblätter mit interner sheetId zurückgeben.
 * Benötigt für deleteSheet().
 *
 * @param {string} spreadsheetId
 * @returns {Promise<Array<{title: string, sheetId: number}>>}
 */
export async function getSheetsWithIds(spreadsheetId) {
  const url  = `${BASE}/${spreadsheetId}?fields=sheets.properties`;
  const res  = await fetch(url, { headers: authHeaders() });
  const data = await handleResponse(res);
  return (data.sheets || []).map(s => ({
    title:   s.properties.title,
    sheetId: s.properties.sheetId,
  }));
}
