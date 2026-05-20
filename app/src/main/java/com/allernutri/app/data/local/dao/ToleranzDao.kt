package com.allernutri.app.data.local.dao

import androidx.room.*
import com.allernutri.app.data.local.entity.ToleranzEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToleranzDao {

    @Query("SELECT * FROM toleranzen WHERE hundId = :hundId")
    fun beobachteToleranzFuerHund(hundId: Long): Flow<List<ToleranzEntity>>

    @Query("SELECT * FROM toleranzen WHERE hundId = :hundId AND naehrstoffKey = :key")
    suspend fun getToleranz(hundId: Long, key: String): ToleranzEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(toleranz: ToleranzEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlle(toleranzen: List<ToleranzEntity>)

    @Query("DELETE FROM toleranzen WHERE hundId = :hundId AND naehrstoffKey = :key")
    suspend fun delete(hundId: Long, key: String)

    /** Setzt alle Toleranzen eines Hundes auf NRC-Standard zurück */
    @Query("DELETE FROM toleranzen WHERE hundId = :hundId")
    suspend fun loescheAlleForHund(hundId: Long)
}
