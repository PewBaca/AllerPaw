package com.allernutri.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Leichtgewichtiger Google Sheets v4 REST-Client.
 * Verwendet OkHttp + JSONObject direkt (kein extra Retrofit-Interface nötig).
 *
 * Endpoints:
 * POST   /spreadsheets                          → neues Sheet erstellen
 * GET    /spreadsheets/{id}/values/{range}      → Daten lesen
 * PUT    /spreadsheets/{id}/values/{range}      → Daten schreiben
 * POST   /spreadsheets/{id}/values/{range}:append → Daten anhängen
 */
class SheetsApiClient(private val oauthToken: String) {

    private val base   = "https://sheets.googleapis.com/v4/spreadsheets"
    private val client = OkHttpClient()
    private val json   = "application/json; charset=utf-8".toMediaType()

    // ── Spreadsheet erstellen ─────────────────────────────────────────────

    suspend fun createSpreadsheet(title: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("properties", JSONObject().put("title", title))
        }.toString().toRequestBody(json)

        val request = Request.Builder()
            .url(base)
            .addHeader("Authorization", "Bearer $oauthToken")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw Exception("Leere Antwort beim Erstellen des Spreadsheets")

        if (!response.isSuccessful) {
            throw Exception("Sheets API Fehler ${response.code}: $responseBody")
        }

        JSONObject(responseBody).getString("spreadsheetId")
    }

    // ── Daten lesen ───────────────────────────────────────────────────────

    suspend fun readSheet(
        spreadsheetId: String,
        sheetName: String,
        range: String = "A1:Z"
    ): List<List<String>> = withContext(Dispatchers.IO) {
        val encodedRange = "${encodeSheetName(sheetName)}!$range"
        val url = "$base/$spreadsheetId/values/$encodedRange"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $oauthToken")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext emptyList()

        if (!response.isSuccessful) {
            throw Exception("Sheets API Fehler ${response.code}: $responseBody")
        }

        val obj    = JSONObject(responseBody)
        val values = obj.optJSONArray("values") ?: return@withContext emptyList()

        (0 until values.length()).map { i ->
            val row = values.getJSONArray(i)
            (0 until row.length()).map { j -> row.optString(j, "") }
        }
    }

    // ── Daten schreiben (kompletter Tab) ──────────────────────────────────

    suspend fun writeSheet(
        spreadsheetId: String,
        sheetName: String,
        headersDe: List<String>,
        headersApi: List<String>,
        dataRows: List<List<String>>
    ) = withContext(Dispatchers.IO) {
        // Sicherstellen dass das Sheet existiert
        ensureSheetExists(spreadsheetId, sheetName)

        // Alle Zeilen: Zeile 1 = DE-Header, Zeile 2 = API-Keys, dann Daten
        val allRows = mutableListOf<List<String>>().apply {
            add(headersDe)
            add(headersApi)
            addAll(dataRows)
        }

        val valuesArray = JSONArray()
        allRows.forEach { row ->
            valuesArray.put(JSONArray(row))
        }

        val body = JSONObject().apply {
            put("range", "${encodeSheetName(sheetName)}!A1")
            put("majorDimension", "ROWS")
            put("values", valuesArray)
        }.toString().toRequestBody(json)

        val encodedRange = "${encodeSheetName(sheetName)}!A1"
        val url = "$base/$spreadsheetId/values/$encodedRange" +
                  "?valueInputOption=USER_ENTERED"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $oauthToken")
            .put(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val err = response.body?.string() ?: ""
            throw Exception("Sheets API Fehler ${response.code}: $err")
        }
    }

    // ── Zeilen anhängen ───────────────────────────────────────────────────

    suspend fun appendRows(
        spreadsheetId: String,
        sheetName: String,
        rows: List<List<String>>
    ) = withContext(Dispatchers.IO) {
        val valuesArray = JSONArray()
        rows.forEach { row -> valuesArray.put(JSONArray(row)) }

        val body = JSONObject().apply {
            put("values", valuesArray)
        }.toString().toRequestBody(json)

        val encodedRange = "${encodeSheetName(sheetName)}!A1"
        val url = "$base/$spreadsheetId/values/$encodedRange:append" +
                  "?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $oauthToken")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val err = response.body?.string() ?: ""
            throw Exception("Sheets API Fehler ${response.code}: $err")
        }
    }

    // ── Sheet-Tab erstellen falls nicht vorhanden ─────────────────────────

    private suspend fun ensureSheetExists(
        spreadsheetId: String,
        sheetName: String
    ) = withContext(Dispatchers.IO) {
        val metaUrl  = "$base/$spreadsheetId?fields=sheets.properties.title"
        val metaReq  = Request.Builder()
            .url(metaUrl)
            .addHeader("Authorization", "Bearer $oauthToken")
            .get().build()
        val metaResp = client.newCall(metaReq).execute()
        val metaBody = metaResp.body?.string() ?: return@withContext

        val sheets = JSONObject(metaBody).optJSONArray("sheets") ?: return@withContext
        val exists = (0 until sheets.length()).any { i ->
            sheets.getJSONObject(i)
                .optJSONObject("properties")
                ?.optString("title") == sheetName
        }

        if (!exists) {
            val addBody = JSONObject().apply {
                put("requests", JSONArray().put(
                    JSONObject().put("addSheet",
                        JSONObject().put("properties",
                            JSONObject().put("title", sheetName)
                        )
                    )
                ))
            }.toString().toRequestBody(json)

            val req = Request.Builder()
                .url("$base/$spreadsheetId:batchUpdate")
                .addHeader("Authorization", "Bearer $oauthToken")
                .post(addBody).build()
            client.newCall(req).execute()
        }
    }

    // ── Hilfsfunktionen ───────────────────────────────────────────────────

    /**
     * URL-Enkodierung für Sheet-Namen mit Sonderzeichen.
     * Leerzeichen → %20, Umlaute werden direkt übergeben.
     */
    private fun encodeSheetName(name: String): String =
        "'${name.replace("'", "\\'")}'"
}
