package com.allerpaw.app.domain

import com.allerpaw.app.data.local.entity.RezeptZutatEntity
import com.allerpaw.app.data.local.entity.ZutatEntity
import com.allerpaw.app.data.local.entity.ZutatNaehrstoffEntity

/**
 * Löst ein Rezept rekursiv auf (Rezept-Mix).
 * - Max. 5 Ebenen Tiefe
 * - Zykluserkennung via visitedIds
 * - Keine Zwischenrundung bis zur finalen Ausgabe
 * - Kochverlust (Faktor 0.70) nur für B-Vitamine, konfigurierbar
 */
object RezeptResolver {

    private val B_VITAMINE = setOf(
        "vitamin_b1", "vitamin_b2", "vitamin_b3",
        "vitamin_b5", "vitamin_b6", "vitamin_b9", "vitamin_b12"
    )

    data class ResolvedZutat(
        val zutat: ZutatEntity,
        val mengeG: Double,
        val naehrstoffe: Map<String, Double>,   // key → wertPer100g (bereits kochverlust-korrigiert)
        val gekocht: Boolean
    )

    data class ResolveResult(
        val zutaten: List<ResolvedZutat>,
        val gesamtGrammRoh: Double,
        val kcalGesamt: Double
    )

    /**
     * @param rezeptId          Die aufzulösende Rezept-ID
     * @param skalierung        Multiplikator (z.B. 0.5 = halbe Portion)
     * @param kochverlustFaktor Standardmäßig 0.70 (= 30 % Verlust); konfigurierbar
     * @param loader            Suspending-Funktion zum Laden von Rezept-Positionen + Zutat-Daten
     */
    suspend fun resolve(
        rezeptId: Long,
        skalierung: Double = 1.0,
        gekocht: Boolean = false,
        kochverlustFaktor: Double = 0.70,
        loader: Loader,
        visitedIds: MutableSet<Long> = mutableSetOf(),
        tiefe: Int = 0
    ): ResolveResult {
        if (tiefe > 5) return ResolveResult(emptyList(), 0.0, 0.0)
        if (!visitedIds.add(rezeptId)) return ResolveResult(emptyList(), 0.0, 0.0) // Zyklus

        val positionen = loader.loadZutaten(rezeptId)
        val resolved = mutableListOf<ResolvedZutat>()

        for (pos in positionen) {
            val mengeG = pos.mengeG * skalierung

            if (pos.subRezeptId != null) {
                // Verschachteltes Rezept → rekursiv auflösen
                val subRezept = loader.loadRezept(pos.subRezeptId)
                val subGekocht = subRezept?.gekocht ?: gekocht
                val sub = resolve(
                    rezeptId          = pos.subRezeptId,
                    skalierung        = mengeG / 1.0,   // Sub-Rezept wird anteilig skaliert
                    gekocht           = subGekocht,
                    kochverlustFaktor = kochverlustFaktor,
                    loader            = loader,
                    visitedIds        = visitedIds.toMutableSet(),
                    tiefe             = tiefe + 1
                )
                resolved.addAll(sub.zutaten)
            } else if (pos.zutatId != null) {
                val zutat = loader.loadZutat(pos.zutatId) ?: continue
                val roheNaehrstoffe = loader.loadNaehrstoffe(pos.zutatId)
                    .associate { it.naehrstoffKey to it.wertPer100g }

                // Kochverlust anwenden (nur B-Vitamine, nur wenn gekocht)
                val korrigierteNaehrstoffe = if (gekocht) {
                    roheNaehrstoffe.mapValues { (key, wert) ->
                        if (key in B_VITAMINE) wert * kochverlustFaktor else wert
                    }
                } else roheNaehrstoffe

                resolved.add(ResolvedZutat(
                    zutat       = zutat,
                    mengeG      = mengeG,
                    naehrstoffe = korrigierteNaehrstoffe,
                    gekocht     = gekocht
                ))
            }
        }

        val gesamtG = resolved.sumOf { it.mengeG }
        val kcal    = resolved.sumOf { z ->
            val kcalPer100g = z.naehrstoffe["kcal"] ?: 0.0
            kcalPer100g * z.mengeG / 100.0
        }

        return ResolveResult(resolved, gesamtG, kcal)
    }

    interface Loader {
        suspend fun loadZutaten(rezeptId: Long): List<RezeptZutatEntity>
        suspend fun loadZutat(zutatId: Long): ZutatEntity?
        suspend fun loadNaehrstoffe(zutatId: Long): List<ZutatNaehrstoffEntity>
        suspend fun loadRezept(rezeptId: Long): com.allerpaw.app.data.local.entity.RezeptEntity?
    }
}
