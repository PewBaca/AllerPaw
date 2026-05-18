package com.allerpaw.app.data.repository

import com.allerpaw.app.data.local.dao.ToleranzDao
import com.allerpaw.app.data.local.entity.ToleranzEntity
import com.allerpaw.app.domain.NaehrstoffKatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToleranzRepository @Inject constructor(
    private val dao: ToleranzDao
) {
    /**
     * Gibt Toleranzen für einen Hund zurück.
     * Für jeden Nährstoff ohne DB-Eintrag wird der NRC-Standard zurückgegeben:
     *   Min = 80%, Empfehlung = 100%, Max = 150% (oder UL-basiert wenn vorhanden)
     */
    fun beobachteToleranzMap(hundId: Long): Flow<Map<String, ToleranzEntity>> =
        dao.beobachteToleranzFuerHund(hundId).map { dbEintraege ->
            val map = dbEintraege.associateBy { it.naehrstoffKey }.toMutableMap()
            // Fehlende Nährstoffe mit NRC-Standard auffüllen
            NaehrstoffKatalog.alle.forEach { naehrstoff ->
                if (naehrstoff.key !in map) {
                    map[naehrstoff.key] = ToleranzEntity(
                        hundId             = hundId,
                        naehrstoffKey      = naehrstoff.key,
                        minProzent         = 80.0,
                        empfehlungProzent  = 100.0,
                        maxProzent         = 150.0
                    )
                }
            }
            map
        }

    suspend fun upsert(toleranz: ToleranzEntity) = dao.upsert(toleranz)

    suspend fun zuruecksetzenAufNrc(hundId: Long) = dao.loescheAlleForHund(hundId)
}
