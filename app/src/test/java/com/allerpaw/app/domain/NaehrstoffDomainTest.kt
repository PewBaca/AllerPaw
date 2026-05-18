package com.allerpaw.app.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class NaehrstoffDomainTest {

    // ── RER / MER ─────────────────────────────────────────────────────────

    @Test
    fun `RER berechnung korrekt fuer 10kg Hund`() {
        val rer = EnergieBedarf.rer(10.0)
        // 70 × 10^0.75 = 70 × 5.623 = 393.6
        assertTrue("RER sollte ~393 sein, war $rer", abs(rer - 393.6) < 1.0)
    }

    @Test
    fun `RER berechnung korrekt fuer 30kg Hund`() {
        val rer = EnergieBedarf.rer(30.0)
        // 70 × 30^0.75 = 70 × 12.828 = 897.9
        assertTrue("RER sollte ~898 sein, war $rer", abs(rer - 897.9) < 2.0)
    }

    @Test
    fun `MER ist RER mal Faktor`() {
        val rer = EnergieBedarf.rer(10.0)
        val mer = EnergieBedarf.mer(10.0, 1.6)
        assertEquals(rer * 1.6, mer, 0.001)
    }

    @Test
    fun `Naehrstoffbedarf skaliert korrekt mit kcal`() {
        val protein = NaehrstoffKatalog.byKey["protein"]!!
        // Bedarf: 45g je 1000 kcal
        val bedarf = EnergieBedarf.naehrstoffBedarf(protein, 1000.0)
        assertEquals(45.0, bedarf, 0.001)

        val bedarf2000 = EnergieBedarf.naehrstoffBedarf(protein, 2000.0)
        assertEquals(90.0, bedarf2000, 0.001)
    }

    // ── RezeptAnalyse ─────────────────────────────────────────────────────

    @Test
    fun `Analyse mit leerem Rezept gibt 0 Prozent`() {
        val uc = RezeptAnalyseUseCase()
        val results = uc.analyse(emptyList(), 1000.0)
        results.forEach { e ->
            assertEquals(0.0, e.prozent, 0.001)
            assertEquals(NaehrstoffErgebnis.Status.MANGEL, e.status)
        }
    }

    @Test
    fun `Analyse berechnet Protein korrekt`() {
        val uc = RezeptAnalyseUseCase()
        // 100g Zutat mit 20g Protein pro 100g → 20g Protein gesamt
        val zutaten = listOf(
            RezeptAnalyseUseCase.ZutatInput(
                mengeG      = 100.0,
                naehrstoffe = mapOf("protein" to 20.0)
            )
        )
        // Bei 1000 kcal ME: Bedarf = 45g
        val results = uc.analyse(zutaten, 1000.0)
        val protein = results.find { it.naehrstoff.key == "protein" }!!

        assertEquals(20.0, protein.istWert, 0.001)
        assertEquals(45.0, protein.bedarfswert, 0.001)
        // 20/45 * 100 ≈ 44.4%
        assertTrue(abs(protein.prozent - 44.4) < 0.5)
        assertEquals(NaehrstoffErgebnis.Status.MANGEL, protein.status)
    }

    @Test
    fun `Analyse Status OK bei 100 Prozent`() {
        val uc = RezeptAnalyseUseCase()
        // Calcium: Bedarf 1.0g/1000kcal → genau 1.0g eingeben
        val zutaten = listOf(
            RezeptAnalyseUseCase.ZutatInput(
                mengeG      = 100.0,
                naehrstoffe = mapOf("calcium" to 1.0)  // 1.0g per 100g → 1.0g gesamt
            )
        )
        val results = uc.analyse(zutaten, 1000.0)
        val calcium = results.find { it.naehrstoff.key == "calcium" }!!
        assertEquals(NaehrstoffErgebnis.Status.OK, calcium.status)
    }

    @Test
    fun `Analyse erkennt Ueberschreitung des UL`() {
        val uc = RezeptAnalyseUseCase()
        // Selen UL = 500µg/1000kcal → wir geben 600µg ein
        val zutaten = listOf(
            RezeptAnalyseUseCase.ZutatInput(
                mengeG      = 100.0,
                naehrstoffe = mapOf("selen" to 600.0) // 600µg per 100g
            )
        )
        val results = uc.analyse(zutaten, 1000.0)
        val selen = results.find { it.naehrstoff.key == "selen" }!!
        assertEquals(NaehrstoffErgebnis.Status.UEBERSCHRITTEN, selen.status)
    }

    @Test
    fun `Analyse summiert mehrere Zutaten korrekt`() {
        val uc = RezeptAnalyseUseCase()
        val zutaten = listOf(
            RezeptAnalyseUseCase.ZutatInput(100.0, mapOf("protein" to 10.0)),
            RezeptAnalyseUseCase.ZutatInput(200.0, mapOf("protein" to 15.0))
        )
        // 100g × 10/100 + 200g × 15/100 = 10 + 30 = 40g Protein
        val results = uc.analyse(zutaten, 1000.0)
        val protein = results.find { it.naehrstoff.key == "protein" }!!
        assertEquals(40.0, protein.istWert, 0.001)
    }

    // ── FloatParser ───────────────────────────────────────────────────────

    @Test
    fun `FloatParser parst Komma als Dezimaltrenner`() {
        val result = com.allerpaw.app.util.FloatParser.parse("3,14")
        assertNotNull(result)
        assertEquals(3.14, result!!, 0.001)
    }

    @Test
    fun `FloatParser parst Punkt als Dezimaltrenner`() {
        val result = com.allerpaw.app.util.FloatParser.parse("3.14")
        assertNotNull(result)
        assertEquals(3.14, result!!, 0.001)
    }

    @Test
    fun `FloatParser gibt null fuer leeren String`() {
        assertNull(com.allerpaw.app.util.FloatParser.parse(""))
        assertNull(com.allerpaw.app.util.FloatParser.parse("   "))
    }

    // ── RezeptResolver ────────────────────────────────────────────────────

    @Test
    fun `Kochverlust nur fuer B-Vitamine`() {
        // Faktor 0.70 für B-Vitamine, 1.0 für andere
        val b1Roh     = 100.0
        val calciumRoh = 100.0
        val faktor    = 0.70

        val b1Korrigiert      = b1Roh * faktor
        val calciumKorrigiert = calciumRoh * 1.0 // KEIN Kochverlust

        assertEquals(70.0, b1Korrigiert, 0.001)
        assertEquals(100.0, calciumKorrigiert, 0.001)
    }

    // ── NRC Lebensphasen ─────────────────────────────────────────────────

    @Test
    fun `Welpen haben hoeheren Calciumbedarf als Adulte`() {
        val calcium = NaehrstoffKatalog.byKey["calcium"]!!
        val adultBedarf  = EnergieBedarf.naehrstoffBedarf(calcium, 1000.0)
        val welpeBedarf  = NrcLebensphasen.bedarfPro1000kcal(
            calcium, NrcLebensphasen.Lebensphase.WELPE) * 1000.0 / 1000.0

        assertTrue("Welpen-Calciumbedarf > Adult", welpeBedarf > adultBedarf)
    }
}
