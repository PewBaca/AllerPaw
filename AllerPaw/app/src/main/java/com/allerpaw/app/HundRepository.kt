package com.allerpaw.app.data.repository

import com.allerpaw.app.data.local.dao.HundDao
import com.allerpaw.app.data.local.entity.HundEntity
import com.allerpaw.app.data.local.entity.HundGewichtEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HundRepository @Inject constructor(private val dao: HundDao) {

    fun alleHunde(): Flow<List<HundEntity>> = dao.getAlleHunde()

    suspend fun getById(id: Long): HundEntity? = dao.getById(id)

    suspend fun upsert(hund: HundEntity): Long {
        return if (hund.id == 0L) dao.insert(hund)
        else { dao.update(hund); hund.id }
    }

    suspend fun delete(id: Long) = dao.softDelete(id)

    // Gewicht
    suspend fun letzteGewichte(hundId: Long): List<HundGewichtEntity> =
        dao.getLetzteGewichte(hundId)

    suspend fun addGewicht(hundId: Long, kg: Double, datum: LocalDate): Long =
        dao.insertGewicht(HundGewichtEntity(hundId = hundId, datum = datum, gewichtKg = kg))

    suspend fun deleteGewicht(id: Long) = dao.softDeleteGewicht(id)
}
