package com.allerpaw.app.data.repository

import com.allerpaw.app.data.local.dao.ParameterDao
import com.allerpaw.app.data.local.dao.RezeptDao
import com.allerpaw.app.data.local.dao.ZutatenDao
import com.allerpaw.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RezeptRepository @Inject constructor(
    private val dao: RezeptDao,
    private val zutatenDao: ZutatenDao,
    private val parameterDao: ParameterDao
) {
    fun rezepteForHund(hundId: Long): Flow<List<RezeptEntity>> = dao.getRezepteForHund(hundId)

    suspend fun alleRezepte(): List<RezeptEntity> = dao.getAlleRezepte()

    suspend fun getById(id: Long): RezeptEntity? = dao.getById(id)

    suspend fun upsert(rezept: RezeptEntity): Long =
        if (rezept.id == 0L) dao.insert(rezept)
        else { dao.update(rezept); rezept.id }

    suspend fun delete(id: Long) = dao.softDelete(id)

    suspend fun getZutaten(rezeptId: Long): List<RezeptZutatEntity> =
        dao.getZutatenForRezept(rezeptId)

    suspend fun saveZutaten(rezeptId: Long, items: List<RezeptZutatEntity>) {
        dao.deleteAllZutatenForRezept(rezeptId)
        items.forEachIndexed { i, item ->
            dao.insertZutat(item.copy(rezeptId = rezeptId, reihenfolge = i))
        }
    }

    suspend fun getNaehrstoffeForZutat(zutatId: Long): List<ZutatNaehrstoffEntity> =
        zutatenDao.getNaehrstoffe(zutatId)

    suspend fun getZutatById(id: Long): ZutatEntity? = zutatenDao.getById(id)

    suspend fun getParameter(key: String, default: String): String =
        parameterDao.get(key) ?: default
}
