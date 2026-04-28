package com.allerpaw.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

// ─────────────────────────────────────────────
// Hund
// ─────────────────────────────────────────────

@Entity(tableName = "hunde")
data class HundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rasse: String = "",
    val geburtsdatum: LocalDate? = null,
    val geschlecht: String = "",        // "m" | "w"
    val kastriert: Boolean = false,
    val gewichtKg: Double = 0.0,        // Aktuell; wird durch Gewichtsverlauf ergänzt
    val kcalBedarfManuell: Double? = null, // null → RER-Berechnung
    val notizen: String = "",
    val aktiv: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

@Entity(
    tableName = "hund_gewicht",
    foreignKeys = [ForeignKey(
        entity = HundEntity::class,
        parentColumns = ["id"],
        childColumns = ["hundId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("hundId")]
)
data class HundGewichtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val datum: LocalDate,
    val gewichtKg: Double,
    val notizen: String = "",
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

// ─────────────────────────────────────────────
// Zutat / Supplement
// ─────────────────────────────────────────────

/**
 * perMode: "100g" | "1g" | "tablette" | "tropfen" | "pulver"
 * Alle Nährstoffe werden INTERN als Äquivalent pro 100g gespeichert.
 * vitaminEForm: "natuerlich" | "synthetisch" | "acetat_natuerlich" | "acetat_synthetisch"
 */
@Entity(tableName = "zutaten")
data class ZutatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hersteller: String = "",
    val kategorie: String = "",
    val typ: String = "lebensmittel",       // "lebensmittel" | "supplement"
    val perMode: String = "100g",
    // Tablette
    val tabletteGewichtG: Double = 0.0,     // Gewicht je Tablette in Gramm
    // Tropfen
    val tropfenGewichtG: Double = 0.0,      // Gewicht je Tropfen in Gramm (z.B. 0.05g)
    val tropfenVolumenMl: Double = 0.0,     // Volumen je Tropfen in ml (z.B. 0.05ml)
    // Pulver: Eingabe immer pro 100g → kein Extra-Feld nötig
    val vitaminEForm: String = "natuerlich",
    val aktiv: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

/**
 * Speichert alle 39 NRC-Nährstoffe + kcal je Zutat.
 * Wert ist IMMER als Äquivalent pro 100g gespeichert.
 * einheit: "g" | "mg" | "µg" | "kcal"  (IE bereits umgerechnet)
 */
@Entity(
    tableName = "zutat_naehrstoffe",
    foreignKeys = [ForeignKey(
        entity = ZutatEntity::class,
        parentColumns = ["id"],
        childColumns = ["zutatId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("zutatId")],
    primaryKeys = ["zutatId", "naehrstoffKey"]
)
data class ZutatNaehrstoffEntity(
    val zutatId: Long,
    val naehrstoffKey: String,   // z.B. "protein", "epa_dha", "vitamin_a"
    val wertPer100g: Double,
    val einheit: String          // "g" | "mg" | "µg" | "kcal"
)

// ─────────────────────────────────────────────
// Rezept
// ─────────────────────────────────────────────

@Entity(
    tableName = "rezepte",
    foreignKeys = [ForeignKey(
        entity = HundEntity::class,
        parentColumns = ["id"],
        childColumns = ["hundId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("hundId")]
)
data class RezeptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val name: String,
    val notizen: String = "",
    val gekocht: Boolean = false,
    val portionenProTag: Int = 2,
    val skalierungsFaktor: Double = 1.0,
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

/**
 * Eine Zeile pro Zutat ODER Sub-Rezept in einem Rezept.
 * Wenn subRezeptId != null → verschachteltes Rezept (Rezept-Mix)
 */
@Entity(
    tableName = "rezept_zutaten",
    foreignKeys = [
        ForeignKey(
            entity = RezeptEntity::class,
            parentColumns = ["id"],
            childColumns = ["rezeptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ZutatEntity::class,
            parentColumns = ["id"],
            childColumns = ["zutatId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("rezeptId"), Index("zutatId")]
)
data class RezeptZutatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rezeptId: Long,
    val zutatId: Long? = null,           // null wenn subRezeptId gesetzt
    val subRezeptId: Long? = null,       // null wenn zutatId gesetzt
    val mengeG: Double,                  // Immer in Gramm (Anzahl × Gewicht umgerechnet)
    val anzahlTabletten: Double? = null, // Anzeige: Stückzahl Tabletten
    val anzahlTropfen: Double? = null,   // Anzeige: Anzahl Tropfen
    val inhaltsstoffeFreitext: String = "", // Freie Beschreibung der Inhaltsstoffe/Zusammensetzung
    val reihenfolge: Int = 0
)

// ─────────────────────────────────────────────
// Parameter & Toleranzen
// ─────────────────────────────────────────────

/** Globale Rechenwerte (kochverlust_b_vitamine, portionen_pro_tag, rer_faktor …) */
@Entity(tableName = "parameter", primaryKeys = ["schluessel"])
data class ParameterEntity(
    val schluessel: String,
    val wert: String    // Als String gespeichert; Parsing via FloatParser
)

/** Individuelle Nährstoff-Toleranzen je Hund */
@Entity(
    tableName = "toleranzen",
    foreignKeys = [ForeignKey(
        entity = HundEntity::class,
        parentColumns = ["id"],
        childColumns = ["hundId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("hundId")],
    primaryKeys = ["hundId", "naehrstoffKey"]
)
data class ToleranzEntity(
    val hundId: Long,
    val naehrstoffKey: String,
    val minProzent: Double = 80.0,
    val maxProzent: Double = 150.0,
    val empfehlungProzent: Double = 100.0
)
