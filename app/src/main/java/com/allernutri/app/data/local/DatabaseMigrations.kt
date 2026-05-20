package com.allernutri.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room-Datenbank-Migrationen für AllerPaw.
 *
 * Version 1 → 2:
 *   - zutaten: tropfenGewichtG, tropfenVolumenMl hinzugefügt
 *   - rezept_zutaten: anzahlTropfen, inhaltsstoffeFreitext hinzugefügt
 *
 * Version 2 → 3:
 *   - tagebuch_hund_zustand neu (Smiley 1–5)
 *   - tasks neu (Task-System)
 *   - task_erledigungen neu (Erledigungs-Protokoll)
 */
object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // zutaten: Tropfen-Felder ergänzen
            db.execSQL(
                "ALTER TABLE zutaten ADD COLUMN tropfenGewichtG REAL NOT NULL DEFAULT 0.0"
            )
            db.execSQL(
                "ALTER TABLE zutaten ADD COLUMN tropfenVolumenMl REAL NOT NULL DEFAULT 0.0"
            )

            // rezept_zutaten: Tropfen-Anzahl + Freitext
            db.execSQL(
                "ALTER TABLE rezept_zutaten ADD COLUMN anzahlTropfen REAL"
            )
            db.execSQL(
                "ALTER TABLE rezept_zutaten ADD COLUMN inhaltsstoffeFreitext TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {

            // tagebuch_hund_zustand
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS tagebuch_hund_zustand (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    hundId      INTEGER NOT NULL,
                    datum       TEXT NOT NULL,
                    zustand     INTEGER NOT NULL,
                    notizen     TEXT NOT NULL DEFAULT '',
                    createdAt   INTEGER NOT NULL,
                    deleted     INTEGER NOT NULL DEFAULT 0,
                    deletedAt   INTEGER,
                    FOREIGN KEY (hundId) REFERENCES hunde(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_tagebuch_hund_zustand_hundId ON tagebuch_hund_zustand(hundId)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_tagebuch_hund_zustand_hundId_datum ON tagebuch_hund_zustand(hundId, datum)"
            )

            // tasks
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    hundId          INTEGER NOT NULL,
                    titel           TEXT NOT NULL,
                    beschreibung    TEXT NOT NULL DEFAULT '',
                    kategorie       TEXT NOT NULL DEFAULT 'sonstiges',
                    wiederholung    TEXT NOT NULL DEFAULT 'taeglich',
                    wochentage      TEXT NOT NULL DEFAULT '',
                    intervallTage   INTEGER NOT NULL DEFAULT 1,
                    uhrzeit         TEXT NOT NULL DEFAULT '',
                    pushAktiv       INTEGER NOT NULL DEFAULT 0,
                    aktiv           INTEGER NOT NULL DEFAULT 1,
                    createdAt       INTEGER NOT NULL,
                    deleted         INTEGER NOT NULL DEFAULT 0,
                    deletedAt       INTEGER,
                    FOREIGN KEY (hundId) REFERENCES hunde(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_tasks_hundId ON tasks(hundId)"
            )

            // task_erledigungen
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS task_erledigungen (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    taskId      INTEGER NOT NULL,
                    datum       TEXT NOT NULL,
                    erledigt    INTEGER NOT NULL DEFAULT 1,
                    notizen     TEXT NOT NULL DEFAULT '',
                    createdAt   INTEGER NOT NULL,
                    FOREIGN KEY (taskId) REFERENCES tasks(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_task_erledigungen_taskId ON task_erledigungen(taskId)"
            )
        }
    }

    /** Alle Migrationen in korrekter Reihenfolge */
    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // zutaten: Datenquelle + externe ID für USDA/OFF/Edamam-Import
            db.execSQL("ALTER TABLE zutaten ADD COLUMN quelle TEXT NOT NULL DEFAULT 'manuell'")
            db.execSQL("ALTER TABLE zutaten ADD COLUMN quelleId TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {

            // rezepte: kategorie-Feld ergänzen
            db.execSQL(
                "ALTER TABLE rezepte ADD COLUMN kategorie TEXT NOT NULL DEFAULT ''"
            )

            // symptom_media: Foto/Video-Anhänge
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS symptom_media (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    symptomId           INTEGER NOT NULL,
                    pfad                TEXT NOT NULL,
                    typ                 TEXT NOT NULL DEFAULT 'foto',
                    dateigroesseBytes   INTEGER NOT NULL DEFAULT 0,
                    createdAt           INTEGER NOT NULL,
                    FOREIGN KEY (symptomId) REFERENCES tagebuch_symptom(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_symptom_media_symptomId ON symptom_media(symptomId)"
            )
        }
    }
}
