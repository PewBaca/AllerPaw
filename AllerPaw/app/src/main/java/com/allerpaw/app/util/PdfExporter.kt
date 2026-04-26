package com.allerpaw.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.allerpaw.app.data.local.entity.*
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Erstellt einen PDF-Tierarztbericht via Android PdfDocument API.
 * Kein externer Dependency nötig.
 *
 * Sektionen (konfigurierbar):
 * 1. Deckblatt
 * 2. Symptome
 * 3. Allergene
 * 4. Ausschlussdiät-Phasen
 * 5. Medikamente
 * 6. Futter-Protokoll
 * 7. Tierarzt-Besuche
 * 8. Pollen-/Umwelt-Log
 * 9. Korrelationshinweise
 */
object PdfExporter {

    data class ExportConfig(
        val hund: HundEntity,
        val vonDatum: LocalDate,
        val bisDatum: LocalDate,
        val sektionen: Set<Sektion> = Sektion.STANDARD,
        val symptome: List<TagebuchSymptomEntity> = emptyList(),
        val allergene: List<TagebuchAllergenEntity> = emptyList(),
        val phasen: List<AusschlussPhasEntity> = emptyList(),
        val medikamente: List<TagebuchMedikamentEntity> = emptyList(),
        val futter: List<TagebuchFutterEntity> = emptyList(),
        val tierarzt: List<TagebuchTierarztEntity> = emptyList(),
        val umwelt: List<TagebuchUmweltEntity> = emptyList()
    )

    enum class Sektion(val label: String) {
        DECKBLATT("Deckblatt"),
        SYMPTOME("Symptome"),
        ALLERGENE("Allergene"),
        PHASEN("Ausschlussdiät-Phasen"),
        MEDIKAMENTE("Medikamente"),
        FUTTER("Futter-Protokoll"),
        TIERARZT("Tierarzt-Besuche"),
        UMWELT("Pollen & Umwelt"),
        KORRELATION("Korrelationshinweise");

        companion object {
            val STANDARD = setOf(DECKBLATT, SYMPTOME, ALLERGENE, PHASEN,
                MEDIKAMENTE, FUTTER, TIERARZT, UMWELT)
        }
    }

    private const val PAGE_W = 595   // A4 @ 72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 48f
    private val datumFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun export(context: Context, config: ExportConfig): File {
        val pdfDoc = PdfDocument()
        val painter = PdfPainter(pdfDoc)

        if (Sektion.DECKBLATT in config.sektionen)
            painter.deckblatt(config)
        if (Sektion.SYMPTOME in config.sektionen && config.symptome.isNotEmpty())
            painter.symptomSektion(config.symptome)
        if (Sektion.ALLERGENE in config.sektionen && config.allergene.isNotEmpty())
            painter.allergenSektion(config.allergene)
        if (Sektion.PHASEN in config.sektionen && config.phasen.isNotEmpty())
            painter.phasenSektion(config.phasen)
        if (Sektion.MEDIKAMENTE in config.sektionen && config.medikamente.isNotEmpty())
            painter.medikamentSektion(config.medikamente)
        if (Sektion.FUTTER in config.sektionen && config.futter.isNotEmpty())
            painter.futterSektion(config.futter)
        if (Sektion.TIERARZT in config.sektionen && config.tierarzt.isNotEmpty())
            painter.tierarztSektion(config.tierarzt)
        if (Sektion.UMWELT in config.sektionen && config.umwelt.isNotEmpty())
            painter.umweltSektion(config.umwelt)

        painter.finish()

        val file = File(context.cacheDir, "AllergieReport_${config.hund.name}_" +
            "${config.vonDatum.format(DateTimeFormatter.BASIC_ISO_DATE)}.pdf")
        pdfDoc.writeTo(FileOutputStream(file))
        pdfDoc.close()
        return file
    }

    // ── Interne Zeichenklasse ──────────────────────────────────────────────

    private class PdfPainter(private val doc: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = MARGIN
        private var pageNum = 0

        private val titlePaint = Paint().apply {
            textSize = 20f; isFakeBoldText = true; color = Color.BLACK
        }
        private val h2Paint = Paint().apply {
            textSize = 14f; isFakeBoldText = true; color = Color.rgb(30, 80, 150)
        }
        private val bodyPaint = Paint().apply {
            textSize = 11f; color = Color.DKGRAY
        }
        private val smallPaint = Paint().apply {
            textSize = 9f; color = Color.GRAY
        }
        private val linePaint = Paint().apply {
            color = Color.LTGRAY; strokeWidth = 0.5f
        }

        init { newPage() }

        private fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNum++
            val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
            page = doc.startPage(info)
            canvas = page!!.canvas
            y = MARGIN
        }

        private fun checkNewPage(needed: Float = 40f) {
            if (y + needed > PAGE_H - MARGIN) newPage()
        }

        private fun text(s: String, paint: Paint, indent: Float = 0f) {
            canvas!!.drawText(s, MARGIN + indent, y, paint)
            y += paint.textSize * 1.4f
        }

        private fun line() {
            canvas!!.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
            y += 6f
        }

        private fun spacer(h: Float = 10f) { y += h }

        fun deckblatt(config: ExportConfig) {
            spacer(60f)
            text("AllerPaw – Allergie-Report", titlePaint)
            spacer(8f)
            text("Hund: ${config.hund.name}", h2Paint)
            if (config.hund.rasse.isNotBlank())
                text("Rasse: ${config.hund.rasse}", bodyPaint)
            text("Zeitraum: ${config.vonDatum.format(datumFmt)} – " +
                 "${config.bisDatum.format(datumFmt)}", bodyPaint)
            spacer(20f)
            line()
            text("Erstellt mit AllerPaw App · ${LocalDate.now().format(datumFmt)}", smallPaint)
        }

        fun symptomSektion(symptome: List<TagebuchSymptomEntity>) {
            newPage()
            text("Symptom-Verlauf", h2Paint); line()
            symptome.forEach { s ->
                checkNewPage()
                text("${s.datum.format(datumFmt)}  Schweregrad ${s.schweregrad}/5  ${s.kategorie}",
                    bodyPaint)
                if (s.beschreibung.isNotBlank())
                    text(s.beschreibung, smallPaint, 12f)
                spacer(4f)
            }
        }

        fun allergenSektion(allergene: List<TagebuchAllergenEntity>) {
            checkNewPage(60f); spacer(12f)
            text("Bekannte Allergene", h2Paint); line()
            allergene.forEach { a ->
                checkNewPage()
                text("${a.allergen}  (Stärke ${a.reaktionsstaerke}/5)", bodyPaint)
                if (a.symptome.isNotBlank())
                    text(a.symptome, smallPaint, 12f)
                spacer(4f)
            }
        }

        fun phasenSektion(phasen: List<AusschlussPhasEntity>) {
            checkNewPage(60f); spacer(12f)
            text("Ausschlussdiät-Phasen", h2Paint); line()
            phasen.forEach { p ->
                checkNewPage()
                val label = when (p.phasentyp) {
                    "elimination" -> "Elimination"
                    "provokation" -> "Provokation"
                    else          -> "Ergebnis"
                }
                val ergebnis = when (p.ergebnis) {
                    "vertraeglich" -> "✓ Verträglich"
                    "reaktion"     -> "✗ Reaktion"
                    else           -> "⏳ Offen"
                }
                text("$label  ${p.startdatum.format(datumFmt)} – ${p.enddatum.format(datumFmt)}  $ergebnis",
                    bodyPaint)
                if (p.getesteZutatName.isNotBlank())
                    text("Getestete Zutat: ${p.getesteZutatName}", smallPaint, 12f)
                spacer(4f)
            }
        }

        fun medikamentSektion(medikamente: List<TagebuchMedikamentEntity>) {
            checkNewPage(60f); spacer(12f)
            text("Medikamente", h2Paint); line()
            medikamente.forEach { m ->
                checkNewPage()
                text("${m.name}  ${m.dosierung}  ${m.haeufigkeit}", bodyPaint)
                val zeitraum = buildString {
                    m.vonDatum?.let { append(it.format(datumFmt)) }
                    m.bisDatum?.let { append(" – ${it.format(datumFmt)}") }
                }
                if (zeitraum.isNotBlank())
                    text(zeitraum, smallPaint, 12f)
                spacer(4f)
            }
        }

        fun futterSektion(futter: List<TagebuchFutterEntity>) {
            checkNewPage(60f); spacer(12f)
            text("Futter-Protokoll", h2Paint); line()
            futter.take(50).forEach { f ->   // max 50 Einträge im PDF
                checkNewPage()
                val flags = buildList {
                    if (f.erstgabe)    add("Erstgabe")
                    if (f.provokation) add("Provokation")
                    if (f.reaktion)    add("Reaktion")
                }.joinToString(" · ")
                text("${f.datum.format(datumFmt)}  $flags", bodyPaint)
                if (f.freitextErgaenzung.isNotBlank())
                    text(f.freitextErgaenzung, smallPaint, 12f)
                spacer(3f)
            }
        }

        fun tierarztSektion(tierarzt: List<TagebuchTierarztEntity>) {
            checkNewPage(60f); spacer(12f)
            text("Tierarzt-Besuche", h2Paint); line()
            tierarzt.forEach { t ->
                checkNewPage()
                text("${t.datum.format(datumFmt)}  ${t.praxis}", bodyPaint)
                if (t.anlass.isNotBlank())    text("Anlass: ${t.anlass}", smallPaint, 12f)
                if (t.ergebnis.isNotBlank())  text("Ergebnis: ${t.ergebnis}", smallPaint, 12f)
                if (t.empfehlung.isNotBlank()) text("Empfehlung: ${t.empfehlung}", smallPaint, 12f)
                spacer(4f)
            }
        }

        fun umweltSektion(umwelt: List<TagebuchUmweltEntity>) {
            checkNewPage(60f); spacer(12f)
            text("Pollen & Umwelt", h2Paint); line()
            umwelt.take(30).forEach { u ->
                checkNewPage()
                val temp = if (u.tempMinC != null && u.tempMaxC != null)
                    "${u.tempMinC}–${u.tempMaxC}°C" else ""
                val feuchte = if (u.luftfeuchte != null) "${u.luftfeuchte}%" else ""
                text("${u.datum.format(datumFmt)}  $temp  $feuchte", bodyPaint)
                spacer(3f)
            }
        }

        fun finish() {
            page?.let { doc.finishPage(it) }
        }
    }
}
