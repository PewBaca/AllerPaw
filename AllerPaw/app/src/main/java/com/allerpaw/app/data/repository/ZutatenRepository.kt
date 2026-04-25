package com.allerpaw.app.data.repository

import com.allerpaw.app.data.local.dao.ZutatenDao
import com.allerpaw.app.data.local.entity.ZutatEntity
import com.allerpaw.app.data.local.entity.ZutatNaehrstoffEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZutatenRepository @Inject constructor(private val dao: ZutatenDao) {

    fun alleZutaten(): Flow<List<ZutatEntity>> = dao.getAlleZutaten()

    suspend fun getById(id: Long): ZutatEntity? = dao.getById(id)

    suspend fun upsert(zutat: ZutatEntity): Long =
        if (zutat.id == 0L) dao.insert(zutat)
        else { dao.update(zutat); zutat.id }

    suspend fun delete(id: Long) = dao.softDelete(id)

    suspend fun getNaehrstoffe(zutatId: Long): List<ZutatNaehrstoffEntity> =
        dao.getNaehrstoffe(zutatId)

    suspend fun saveNaehrstoffe(zutatId: Long, naehrstoffe: List<ZutatNaehrstoffEntity>) {
        dao.deleteNaehrstoffe(zutatId)
        dao.upsertNaehrstoffe(naehrstoffe)
    }
}
