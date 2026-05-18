package com.allerpaw.app.data.repository

import android.content.Context
import android.net.Uri
import com.allerpaw.app.data.local.dao.SymptomMediaDao
import com.allerpaw.app.data.local.entity.SymptomMediaEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val dao: SymptomMediaDao
) {
    private val mediaDir: File get() = File(ctx.filesDir, "media").also { it.mkdirs() }

    fun getForSymptom(symptomId: Long): Flow<List<SymptomMediaEntity>> =
        dao.getForSymptom(symptomId)

    suspend fun getForSymptomOnce(symptomId: Long) =
        dao.getForSymptomOnce(symptomId)

    /**
     * Kopiert eine URI (Kamera/Galerie) in das App-interne media/-Verzeichnis
     * und speichert den Pfad in der Datenbank.
     *
     * @param symptomId  Zugehöriger Symptom-Eintrag
     * @param uri        URI der gewählten Datei (content://)
     * @param typ        "foto" | "video"
     */
    suspend fun saveMedia(
        symptomId: Long,
        uri: Uri,
        typ: String = "foto"
    ): SymptomMediaEntity = withContext(Dispatchers.IO) {
        val ext      = if (typ == "video") "mp4" else "jpg"
        val dateiname = "symptom_${symptomId}_${UUID.randomUUID()}.$ext"
        val ziel     = File(mediaDir, dateiname)

        // URI → App-internes Verzeichnis kopieren
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            ziel.outputStream().use { output -> input.copyTo(output) }
        } ?: throw Exception("Datei konnte nicht gelesen werden")

        val entity = SymptomMediaEntity(
            symptomId         = symptomId,
            pfad              = ziel.absolutePath,
            typ               = typ,
            dateigroesseBytes = ziel.length(),
            createdAt         = Instant.now()
        )
        val id = dao.insert(entity)
        entity.copy(id = id)
    }

    /**
     * Erstellt eine leere Datei für die Kamera (CameraX / Intent).
     * Die URI wird zurückgegeben damit sie dem Camera-Intent übergeben werden kann.
     */
    suspend fun createMediaFile(typ: String = "foto"): File = withContext(Dispatchers.IO) {
        val ext = if (typ == "video") "mp4" else "jpg"
        File(mediaDir, "temp_${UUID.randomUUID()}.$ext")
    }

    /**
     * Speichert eine bereits vorhandene Datei (z.B. nach Kamera-Aufnahme).
     */
    suspend fun saveMediaFile(
        symptomId: Long,
        datei: File,
        typ: String = "foto"
    ): SymptomMediaEntity = withContext(Dispatchers.IO) {
        val entity = SymptomMediaEntity(
            symptomId         = symptomId,
            pfad              = datei.absolutePath,
            typ               = typ,
            dateigroesseBytes = datei.length(),
            createdAt         = Instant.now()
        )
        val id = dao.insert(entity)
        entity.copy(id = id)
    }

    suspend fun deleteMedia(id: Long, pfad: String) = withContext(Dispatchers.IO) {
        File(pfad).delete()
        dao.delete(id)
    }

    suspend fun deleteAllForSymptom(symptomId: Long) = withContext(Dispatchers.IO) {
        val medien = dao.getForSymptomOnce(symptomId)
        medien.forEach { File(it.pfad).delete() }
        dao.deleteAllForSymptom(symptomId)
    }

    fun mediaDirPath(): String = mediaDir.absolutePath
}
