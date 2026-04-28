package com.allerpaw.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

// ─────────────────────────────────────────────
// Umwelt
// ─────────────────────────────────────────────

@Entity(tableName = "tagebuch_umwelt")
data class TagebuchUmweltEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val datum: LocalDate,
    val tempMinC: Double? = null,
    val tempMaxC: Double? = null,
    val luftfeuchte: Int? = null,       // %
    val niederschlagMm: Double? = null,
    val raumtempC: Double? = null,
    val raumfeuchte: Int? = null,
    val bett: String = "unverändert",   // "unverändert" | "gewechselt"
    val notizen: String = "",
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

/** Pollen-Log: eine Zeile pro Pollenart pro Umwelt-Eintrag */
@Entity(
    tableName = "tagebuch_pollen_log",
    foreignKeys = [ForeignKey(
        entity = TagebuchUmweltEntity::class,
        parentColumns = ["id"],
        childColumns = ["umweltId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("umweltId")]
)
data class TagebuchPollenLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val umweltId: Long,
    val pollenart: String,  // z.B. "Birke", "Gräser"
    val staerke: Int,       // 0–5
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

/** Nutzerdefinierte Pollenarten (lokal gespeichert) */
@Entity(tableName = "eigene_pollenarten", primaryKeys = ["name"])
data class EigenePollenartEntity(
    val name: String,
    val createdAt: Instant = Instant.now()
)

// ─────────────────────────────────────────────
// Symptom
// ─────────────────────────────────────────────

/**
 * kategorie: "juckreiz" | "ohrentzuendung" | "hautrötung" | "pfoten_lecken"
 *            | "durchfall" | "erbrechen" | "schuetteln" | "sonstiges"
 * schweregrad: 0–5
 * koerperstelle: "ohren" | "pfoten" | "bauch" | "ruecken" | "beine" | "gesicht" | (Freitext)
 */
@Entity(tableName = "tagebuch_symptom")
data class TagebuchSymptomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val datum: LocalDate,
    val kategorie: String,
    val kategorieFreitext: String = "",
    val beschreibung: String = "",
    val schweregrad: Int,
    val koerperstelle: String = "",
    val koerperstelleFreitext: String = "",
    val notizen: String = "",
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

// ─────────────────────────────────────────────
// Futter
// ─────────────────────────────────────────────

/** Futter-Haupteintrag (enthält 1–n FutterItems) */
@Entity(tableName = "tagebuch_futter")
data class TagebuchFutterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val datum: LocalDate,
    val freitextErgaenzung: String = "",
    val produkt: String = "",
    val erstgabe: Boolean = false,
    val zweiWochenPhase: Boolean = false,
    val provokation: Boolean = false,
    val reaktion: Boolean = false,
    val notizen: String = "",
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

/** Eine Zeile pro Rezept-Position im Futter-Eintrag */
@Entity(
    tableName = "tagebuch_futter_item",
    foreignKeys = [ForeignKey(
        entity = TagebuchFutterEntity::class,
        parentColumns = ["id"],
        childColumns = ["futterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("futterId")]
)
data class TagebuchFutterItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val futterId: Long,
    val rezeptId: Long? = null,
    val rezeptName: String = "",   // Snapshot falls Rezept gelöscht wird
    val mengeG: Double,
    val kcalBerechnet: Double? = null,
    val reihenfolge: Int = 0
)

// ─────────────────────────────────────────────
// Ausschluss
// ─────────────────────────────────────────────

/** verdachtsstufe: 0 = Sicher · 1 = leicht · 2 = mittel · 3 = stark */
@Entity(tableName = "tagebuch_ausschluss")
data class TagebuchAusschlussEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val zutatId: Long? = null,
    val zutatName: String = "",   // Snapshot
    val verdachtsstufe: Int,
    val kategorie: String = "",
    val status: String = "",
    val erstmalsGegebenDatum: LocalDate? = null,
    val reaktion: String = "",
    val notizen: String = "",
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

// ─────────────────────────────────────────────
// Allergen
// ─────────────────────────────────────────────

/** reaktionsstaerke: 1–5 */
@Entity(tableName = "tagebuch_allergen")
data class TagebuchAllergenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val allergen: String,
    val kategorie: String = "",
    val reaktionsstaerke: Int,
    val symptome: String = "",
    val notizen: String = "",
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

// ─────────────────────────────────────────────
// Tierarzt
// ─────────────────────────────────────────────

@Entity(tableName = "tagebuch_tierarzt")
data class TagebuchTierarztEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val datum: LocalDate,
    val praxis: String = "",
    val anlass: String = "",
    val untersuchungen: String = "",
    val ergebnis: String = "",
    val empfehlung: String = "",
    val folgebesuchDatum: LocalDate? = null,
    val notizen: String = "",
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

// ─────────────────────────────────────────────
// Medikament
// ─────────────────────────────────────────────

@Entity(tableName = "tagebuch_medikament")
data class TagebuchMedikamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val name: String,
    val typ: String = "",
    val dosierung: String = "",
    val haeufigkeit: String = "",
    val vonDatum: LocalDate? = null,
    val bisDatum: LocalDate? = null,
    val verordnetVon: String = "",
    val notizen: String = "",
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

// ─────────────────────────────────────────────
// Ausschluss-Phasen
// ─────────────────────────────────────────────

/**
 * phasentyp: "elimination" | "provokation" | "ergebnis"
 * ergebnis:  "offen" | "vertraeglich" | "reaktion"
 */
@Entity(tableName = "ausschluss_phasen")
data class AusschlussPhasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val phasentyp: String,
    val getesteZutatId: Long? = null,   // Nur bei Provokation
    val getesteZutatName: String = "",
    val startdatum: LocalDate,
    val enddatum: LocalDate,
    val ergebnis: String = "offen",
    val notizen: String = "",
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

// ─────────────────────────────────────────────
// Allgemeiner Hundezustand (Smiley)
// ─────────────────────────────────────────────

/**
 * Täglicher Gesamtzustand des Hundes.
 * zustand: 1 = sehr gut 😄 · 2 = gut 🙂 · 3 = neutral 😐 · 4 = schlecht 😟 · 5 = sehr schlecht 😢
 */
@Entity(
    tableName = "tagebuch_hund_zustand",
    foreignKeys = [ForeignKey(
        entity = HundEntity::class,
        parentColumns = ["id"],
        childColumns = ["hundId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("hundId"), Index(value = ["hundId", "datum"], unique = true)]
)
data class TagebuchHundZustandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val datum: LocalDate,
    val zustand: Int,               // 1–5
    val notizen: String = "",
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

// ─────────────────────────────────────────────
// Task-System
// ─────────────────────────────────────────────

/**
 * wiederholung: "taeglich" | "woechentlich" | "intervall" | "einmalig"
 * wochentage: Komma-getrennte Zahlen 1–7 (1=Mo) für wöchentlich
 * intervallTage: Anzahl Tage für Intervall-Wiederholung
 * kategorie: "medikament" | "pflege" | "tierarzt" | "sonstiges"
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = HundEntity::class,
        parentColumns = ["id"],
        childColumns = ["hundId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("hundId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hundId: Long,
    val titel: String,
    val beschreibung: String = "",
    val kategorie: String = "sonstiges",
    val wiederholung: String = "taeglich",  // "taeglich"|"woechentlich"|"intervall"|"einmalig"
    val wochentage: String = "",            // "1,3,5" für Mo/Mi/Fr
    val intervallTage: Int = 1,
    val uhrzeit: String = "",               // "08:00" für Push-Notification
    val pushAktiv: Boolean = false,
    val aktiv: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val deletedAt: Instant? = null
)

/**
 * Erledigungs-Protokoll je Task.
 * Jede Bestätigung wird als eigene Zeile gespeichert.
 */
@Entity(
    tableName = "task_erledigungen",
    foreignKeys = [ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("taskId")]
)
data class TaskErledigung(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val datum: LocalDate,
    val erledigt: Boolean = true,
    val notizen: String = "",
    val createdAt: Instant = Instant.now()
)
