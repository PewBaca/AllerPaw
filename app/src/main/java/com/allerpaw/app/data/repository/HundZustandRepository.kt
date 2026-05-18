package com.allerpaw.app.data.repository

import com.allerpaw.app.data.local.dao.HundZustandDao
import com.allerpaw.app.data.local.entity.TagebuchHundZustandEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HundZustandRepository @Inject constructor(
    private val dao: HundZustandDao
) {
    fun verlauf(hundId: Long): Flow<List<TagebuchHundZustandEntity>> = dao.getForHund(hundId)

    suspend fun getHeute(hundId: Long): TagebuchHundZustandEntity? =
        dao.getForDate(hundId, LocalDate.now())

    suspend fun speichern(hundId: Long, zustand: Int, notizen: String = "") {
        val heute    = LocalDate.now()
        val existing = dao.getForDate(hundId, heute)
        if (existing != null) {
            dao.update(existing.copy(zustand = zustand, notizen = notizen))
        } else {
            dao.insert(TagebuchHundZustandEntity(
                hundId  = hundId,
                datum   = heute,
                zustand = zustand,
                notizen = notizen
            ))
        }
    }

    suspend fun delete(id: Long) = dao.softDelete(id)
}
