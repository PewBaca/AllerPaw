package com.allerpaw.app.domain

import com.allerpaw.app.data.local.entity.TagebuchAllergenEntity

/**
 * Kreuzallergen-Analyse UseCase.
 *
 * Nimmt eine Liste bestätigter Allergene eines Hundes und gibt
 * strukturierte Risikogruppen mit Kandidaten zurück.
 */
object KreuzallergenAnalyse {

    data class AllergenMitGruppe(
        val allergen: TagebuchAllergenEntity,
        val gruppe: KreuzallergenMatrix.ProteinGruppe?
    )

    data class Risikogruppe(
        val gruppe: KreuzallergenMatrix.ProteinGruppe,
        /** Bestätigte Allergene die in diese Gruppe fallen */
        val bestaetigteAllergene: List<TagebuchAllergenEntity>,
        /** Weitere Mitglieder der Gruppe, die noch nicht getestet wurden */
        val weitereKandidaten: List<String>
    )

    data class AnalyseErgebnis(
        /** Bestätigte Allergene mit ihrer zugehörigen Protein-Gruppe */
        val allergeneMitGruppe: List<AllergenMitGruppe>,
        /** Protein-Gruppen mit mindestens einem bestätigten Mitglied */
        val risikogruppen: List<Risikogruppe>,
        /** Allergene ohne bekannte Protein-Gruppe */
        val ohneGruppe: List<TagebuchAllergenEntity>
    )

    fun analysiere(allergene: List<TagebuchAllergenEntity>): AnalyseErgebnis {
        // Jedem Allergen seine Gruppe zuordnen
        val mitGruppe = allergene.map { allergen ->
            AllergenMitGruppe(allergen, KreuzallergenMatrix.findeGruppe(allergen.allergen))
        }

        val ohneGruppe = mitGruppe.filter { it.gruppe == null }.map { it.allergen }

        // Gruppen zusammenführen (mehrere Allergene aus gleicher Gruppe)
        val gruppenMap = mitGruppe
            .filter { it.gruppe != null }
            .groupBy { it.gruppe!! }

        val risikogruppen = gruppenMap.map { (gruppe, eintraege) ->
            val bestaetigte = eintraege.map { it.allergen }
            val bestaetigteKeywords = bestaetigte.map { it.allergen.lowercase() }

            // Kandidaten: Gruppen-Mitglieder die noch nicht als Allergen erfasst sind
            val kandidaten = gruppe.mitglieder
                .filter { keyword ->
                    bestaetigteKeywords.none { confirmed -> confirmed.contains(keyword) }
                }
                .map { keyword -> keyword.replaceFirstChar { it.uppercase() } }
                .distinct()

            Risikogruppe(
                gruppe               = gruppe,
                bestaetigteAllergene = bestaetigte,
                weitereKandidaten    = kandidaten
            )
        }.sortedByDescending { rg ->
            // Sortierung: höchste Reaktionsstärke der bestätigten Allergene zuerst
            rg.bestaetigteAllergene.maxOfOrNull { it.reaktionsstaerke } ?: 0
        }

        return AnalyseErgebnis(mitGruppe, risikogruppen, ohneGruppe)
    }
}
