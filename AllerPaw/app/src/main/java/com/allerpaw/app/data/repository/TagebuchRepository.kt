package com.allerpaw.app.data.repository

import com.allerpaw.app.data.local.dao.TagebuchDao
import com.allerpaw.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagebuchRepository @Inject constructor(private val dao: TagebuchDao) {

    // ── Umwelt ────────────────────────────────────────────────────────────
    fun umwelt(hundId: Long): Flow<List<TagebuchUmweltEntity>> = dao.getUmweltForHund(hundId)
    suspend fun saveUmwelt(e: TagebuchUmweltEntity): Long =
        if (e.id == 0L) dao.insertUmwelt(e) else { dao.updateUmwelt(e); e.id }
    suspend fun deleteUmwelt(id: Long) = dao.softDeleteUmwelt(id)

    // Pollen
    suspend fun getPollenForUmwelt(umweltId: Long) = dao.getPollenForUmwelt(umweltId)
    suspend fun savePollenLog(umweltId: Long, pollen: List<TagebuchPollenLogEntity>) {
        dao.deletePollenForUmwelt(umweltId)
        pollen.forEach { dao.insertPollenLog(it.copy(umweltId = umweltId)) }
    }
    fun eigenePollenarten(): Flow<List<EigenePollenartEntity>> = dao.getEigenePollenarten()
    suspend fun addEigenePollenart(name: String) =
        dao.insertEigenePollenart(EigenePollenartEntity(name = name))
    suspend fun deleteEigenePollenart(name: String) = dao.deleteEigenePollenart(name)

    // ── Symptom ───────────────────────────────────────────────────────────
    fun symptome(hundId: Long): Flow<List<TagebuchSymptomEntity>> = dao.getSymptomForHund(hundId)
    suspend fun saveSymptom(e: TagebuchSymptomEntity): Long =
        if (e.id == 0L) dao.insertSymptom(e) else { dao.updateSymptom(e); e.id }
    suspend fun deleteSymptom(id: Long) = dao.softDeleteSymptom(id)

    // ── Futter ────────────────────────────────────────────────────────────
    fun futter(hundId: Long): Flow<List<TagebuchFutterEntity>> = dao.getFutterForHund(hundId)
    suspend fun saveFutter(e: TagebuchFutterEntity, items: List<TagebuchFutterItemEntity>): Long {
        val id = if (e.id == 0L) dao.insertFutter(e) else { dao.updateFutter(e); e.id }
        dao.deleteFutterItems(id)
        items.forEach { dao.insertFutterItem(it.copy(futterId = id)) }
        return id
    }
    suspend fun getFutterItems(futterId: Long) = dao.getFutterItems(futterId)
    suspend fun deleteFutter(id: Long) = dao.softDeleteFutter(id)

    // ── Ausschluss ────────────────────────────────────────────────────────
    fun ausschluss(hundId: Long): Flow<List<TagebuchAusschlussEntity>> = dao.getAusschlussForHund(hundId)
    suspend fun saveAusschluss(e: TagebuchAusschlussEntity): Long =
        if (e.id == 0L) dao.insertAusschluss(e) else { dao.updateAusschluss(e); e.id }
    suspend fun deleteAusschluss(id: Long) = dao.softDeleteAusschluss(id)

    // ── Allergen ──────────────────────────────────────────────────────────
    fun allergene(hundId: Long): Flow<List<TagebuchAllergenEntity>> = dao.getAllergenForHund(hundId)
    suspend fun saveAllergen(e: TagebuchAllergenEntity): Long =
        if (e.id == 0L) dao.insertAllergen(e) else { dao.updateAllergen(e); e.id }
    suspend fun deleteAllergen(id: Long) = dao.softDeleteAllergen(id)

    // ── Tierarzt ──────────────────────────────────────────────────────────
    fun tierarzt(hundId: Long): Flow<List<TagebuchTierarztEntity>> = dao.getTierarztForHund(hundId)
    suspend fun saveTierarzt(e: TagebuchTierarztEntity): Long =
        if (e.id == 0L) dao.insertTierarzt(e) else { dao.updateTierarzt(e); e.id }
    suspend fun deleteTierarzt(id: Long) = dao.softDeleteTierarzt(id)

    // ── Medikament ────────────────────────────────────────────────────────
    fun medikamente(hundId: Long): Flow<List<TagebuchMedikamentEntity>> = dao.getMedikamentForHund(hundId)
    suspend fun saveMedikament(e: TagebuchMedikamentEntity): Long =
        if (e.id == 0L) dao.insertMedikament(e) else { dao.updateMedikament(e); e.id }
    suspend fun deleteMedikament(id: Long) = dao.softDeleteMedikament(id)

    // ── Phasen ────────────────────────────────────────────────────────────
    fun phasen(hundId: Long): Flow<List<AusschlussPhasEntity>> = dao.getPhasenForHund(hundId)
    suspend fun savePhase(e: AusschlussPhasEntity): Long =
        if (e.id == 0L) dao.insertPhase(e) else { dao.updatePhase(e); e.id }
    suspend fun deletePhase(id: Long) = dao.softDeletePhase(id)
}
