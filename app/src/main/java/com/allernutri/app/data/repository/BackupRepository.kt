package com.allernutri.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.RoomDatabase
import com.allernutri.app.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupResult {
    data class Success(val file: File)      : BackupResult()
    data class Error(val message: String)   : BackupResult()
}

sealed class RestoreResult {
    object Success                          : RestoreResult()
    data class WrongVersion(
        val dbVersion: Int,
        val appVersion: Int
    )                                       : RestoreResult()
    data class Error(val message: String)   : RestoreResult()
}

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val db: AppDatabase
) {
    companion object {
        /** Aktuelle Room-DB-Version — muss mit AppDatabase.version übereinstimmen */
        const val CURRENT_DB_VERSION = 5
    }

    // ── Export ────────────────────────────────────────────────────────────

    suspend fun exportBackup(): BackupResult = withContext(Dispatchers.IO) {
        try {
            // WAL-Checkpoint: alle uncommitted Daten in die Hauptdatei schreiben
            db.query("PRAGMA wal_checkpoint(FULL)", null)

            val dbPath  = ctx.getDatabasePath("allernutri.db")
            val outFile = File(
                ctx.cacheDir,
                "allernutri_backup_${LocalDate.now()}_v${CURRENT_DB_VERSION}.db"
            )
            dbPath.copyTo(outFile, overwrite = true)
            BackupResult.Success(outFile)
        } catch (e: Exception) {
            BackupResult.Error("Export fehlgeschlagen: ${e.message}")
        }
    }

    // ── Import / Restore ──────────────────────────────────────────────────

    /**
     * Stellt eine Backup-Datei wieder her.
     *
     * Ablauf:
     * 1. URI → cache kopieren
     * 2. SQLite-Version der Backup-Datei prüfen
     * 3. Room-DB vollständig schließen
     * 4. Backup über allernutri.db kopieren
     * 5. App muss danach neu gestartet werden (Caller verantwortlich)
     */
    suspend fun importBackup(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        try {
            // Schritt 1: URI in Cache kopieren
            val cacheFile = File(ctx.cacheDir, "restore_temp.db")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext RestoreResult.Error("Datei konnte nicht geöffnet werden")

            // Schritt 2: SQLite-Version der Backup-Datei prüfen
            val backupVersion = readUserVersion(cacheFile)
            if (backupVersion != CURRENT_DB_VERSION) {
                cacheFile.delete()
                return@withContext RestoreResult.WrongVersion(
                    dbVersion  = backupVersion,
                    appVersion = CURRENT_DB_VERSION
                )
            }

            // Schritt 3: Room-DB schließen
            db.close()

            // Schritt 4: Backup überschreiben
            val dbPath = ctx.getDatabasePath("allernutri.db")
            cacheFile.copyTo(dbPath, overwrite = true)

            // WAL und SHM auch löschen damit Room sauber neu öffnet
            File("${dbPath.path}-wal").delete()
            File("${dbPath.path}-shm").delete()

            cacheFile.delete()
            RestoreResult.Success

        } catch (e: Exception) {
            RestoreResult.Error("Wiederherstellung fehlgeschlagen: ${e.message}")
        }
    }

    /**
     * Liest die SQLite user_version aus dem Datei-Header (Byte 60–63, Big-Endian).
     * Diese entspricht der Room-DB-Version.
     */
    private fun readUserVersion(file: File): Int {
        return try {
            file.inputStream().use { stream ->
                stream.skip(60)  // user_version steht bei Offset 60
                val b = ByteArray(4)
                stream.read(b)
                ((b[0].toInt() and 0xFF) shl 24) or
                ((b[1].toInt() and 0xFF) shl 16) or
                ((b[2].toInt() and 0xFF) shl 8)  or
                (b[3].toInt() and 0xFF)
            }
        } catch (e: Exception) {
            -1
        }
    }
}
