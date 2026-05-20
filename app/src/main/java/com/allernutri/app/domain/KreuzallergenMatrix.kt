package com.allernutri.app.domain

/**
 * Statische Kreuzreaktions-Matrix basierend auf gemeinsamen Proteinfamilien.
 *
 * Wissenschaftliche Grundlage:
 *   - Geflügel: Muskelproteine (Troponin, Myosin) ähnlich zwischen Huhn/Pute/Ente/Gans
 *   - Rind/Milch: Bovines Serumalbumin (BSA) und Kasein
 *   - Fisch: Parvalbumin (thermostabil)
 *   - Getreide: Prolamıne (Gluten-verwandt) zwischen Weizen/Roggen/Gerste/Hafer
 *   - Milbe/Schalentier: Tropomyosin (häufig unterschätzt)
 *   - Gras/Pollen: kreuzreaktive Proteine (PR-10, Profilin)
 *
 * Format: Gruppen-Name → Liste der Mitglieder (lowercase für Matching)
 *
 * Ausbaubar: neue Gruppen einfach als weiteren Eintrag ergänzen.
 */
object KreuzallergenMatrix {

    data class ProteinGruppe(
        val name: String,
        val beschreibung: String,
        val protein: String,          // Verantwortliches Protein
        val mitglieder: List<String>, // Keyword-Liste (lowercase) für Matching
        val quellen: String           // Wissenschaftliche Referenz
    )

    val gruppen: List<ProteinGruppe> = listOf(

        ProteinGruppe(
            name         = "Geflügel",
            beschreibung = "Ähnliche Muskelproteine (Troponin, Myosin) zwischen Geflügelarten",
            protein      = "Troponin / Myosin",
            mitglieder   = listOf(
                "huhn", "chicken", "hühnchen",
                "pute", "truthahn", "turkey",
                "ente", "duck",
                "gans", "goose",
                "taube", "pigeon",
                "wachtel", "quail",
                "strauss", "ostrich"
            ),
            quellen      = "Martín-Muñoz et al. (2004), Szépfalusi et al."
        ),

        ProteinGruppe(
            name         = "Rind & Milch",
            beschreibung = "Bovines Serumalbumin (BSA) und Kasein sind in Rind und Milch identisch",
            protein      = "Bovines Serumalbumin (BSA) / Kasein",
            mitglieder   = listOf(
                "rind", "rindfleisch", "beef",
                "kuh", "cow",
                "milch", "milk",
                "käse", "cheese",
                "butter",
                "sahne", "cream",
                "joghurt", "yogurt",
                "molke", "whey",
                "kalb", "veal"
            ),
            quellen      = "Fiocchi et al. (1999), NRC 2006"
        ),

        ProteinGruppe(
            name         = "Schwein & Wild",
            beschreibung = "Ähnliche Albumin-Strukturen zwischen Schwein und Haarwild",
            protein      = "Serumalbumin",
            mitglieder   = listOf(
                "schwein", "pork", "pig",
                "wildschwein", "wild boar",
                "speck", "bacon",
                "schinken", "ham"
            ),
            quellen      = "Restani et al. (2009)"
        ),

        ProteinGruppe(
            name         = "Fisch (Parvalbumin)",
            beschreibung = "Parvalbumin ist ein thermostabiles Kalziumbindungsprotein in fast allen Fischen",
            protein      = "Parvalbumin",
            mitglieder   = listOf(
                "fisch", "fish",
                "lachs", "salmon",
                "forelle", "trout",
                "hering", "herring",
                "makrele", "mackerel",
                "thunfisch", "tuna",
                "kabeljau", "cod",
                "tilapia",
                "pangasius",
                "sardine",
                "aal", "eel",
                "barsch", "perch",
                "zander", "pike-perch"
            ),
            quellen      = "Griesmeier et al. (2010), Kuehn et al."
        ),

        ProteinGruppe(
            name         = "Schalentiere & Milben",
            beschreibung = "Tropomyosin ist in Schalentieren und Milben strukturell nahezu identisch — häufig unterschätzte Kreuzreaktion",
            protein      = "Tropomyosin",
            mitglieder   = listOf(
                "garnele", "shrimp", "prawn",
                "krabbe", "crab",
                "hummer", "lobster",
                "muschel", "mussel",
                "tintenfisch", "squid",
                "milbe", "mite",
                "hausstaubmilbe", "house dust mite",
                "schabe", "cockroach",
                "insekt", "insect"
            ),
            quellen      = "Jeong et al. (2010), van Ree et al."
        ),

        ProteinGruppe(
            name         = "Gluten-Getreide",
            beschreibung = "Prolamine und Gluteline sind zwischen Weizen, Roggen, Gerste und Hafer verwandt",
            protein      = "Gliadın / Prolamin",
            mitglieder   = listOf(
                "weizen", "wheat",
                "roggen", "rye",
                "gerste", "barley",
                "hafer", "oat",
                "dinkel", "spelt",
                "emmer",
                "einkorn",
                "kamut",
                "grünkern"
            ),
            quellen      = "Battais et al. (2005)"
        ),

        ProteinGruppe(
            name         = "Hülsenfrüchte",
            beschreibung = "Vicillin und Legumin sind in Hülsenfrüchten verwandt",
            protein      = "Vicillin / Legumin",
            mitglieder   = listOf(
                "erdnuss", "peanut",
                "soja", "soy", "soybean",
                "erbse", "pea",
                "linse", "lentil",
                "bohne", "bean",
                "lupine",
                "kichererbse", "chickpea"
            ),
            quellen      = "Holzhauser et al. (2009)"
        ),

        ProteinGruppe(
            name         = "Gräser & Pollen",
            beschreibung = "PR-10 und Profilin-Proteine zwischen Graspollen und manchen Nahrungsmitteln",
            protein      = "PR-10 / Profilin",
            mitglieder   = listOf(
                "gräser", "grass pollen",
                "pollen",
                "birke", "birch",
                "hasel", "hazel",
                "erle", "alder",
                "beifuss", "mugwort",
                "sellerie", "celery",
                "karotte", "carrot",
                "apfel", "apple"
            ),
            quellen      = "Breiteneder & Clare Mills (2005)"
        )
    )

    /** Index: Keyword (lowercase) → ProteinGruppe */
    private val keywordIndex: Map<String, ProteinGruppe> by lazy {
        val map = mutableMapOf<String, ProteinGruppe>()
        gruppen.forEach { gruppe ->
            gruppe.mitglieder.forEach { keyword ->
                map[keyword] = gruppe
            }
        }
        map
    }

    /**
     * Findet die Protein-Gruppe für einen Allergen-Namen.
     * Prüft ob ein Keyword im Allergen-String enthalten ist (Teilstring-Match).
     */
    fun findeGruppe(allergenName: String): ProteinGruppe? {
        val lower = allergenName.lowercase()
        return keywordIndex.entries.firstOrNull { (keyword, _) ->
            lower.contains(keyword)
        }?.value
    }

    /**
     * Liefert alle Kreuzreaktions-Kandidaten für ein bestätigtes Allergen.
     * Gibt Mitglieder der gleichen Gruppe zurück, außer dem Allergen selbst.
     */
    fun kreuzreaktionsKandidaten(allergenName: String): List<String> {
        val gruppe = findeGruppe(allergenName) ?: return emptyList()
        return gruppe.mitglieder
            .filter { keyword -> !allergenName.lowercase().contains(keyword) }
            .map { keyword -> keyword.replaceFirstChar { it.uppercase() } }
    }
}
