package com.allerpaw.app.data.local.dao

import androidx.room.*
import com.allerpaw.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// ─────────────────────────────────────────────
// HundDao
// ─────────────────────────────────────────────

@Dao
interface HundDao {
    @Query("SELECT * FROM hunde WHERE deleted = 0 ORDER BY name ASC")
    fun getAlleHunde(): Flow<List<HundEntity>>

    @Query("SELECT * FROM hunde WHERE id = :id AND deleted = 0")
    suspend fun getById(id: Long): HundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hund: HundEntity): Long

    @Update
    suspend fun update(hund: HundEntity)

    @Query("UPDATE hunde SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    // Gewicht
    @Query("SELECT * FROM hund_gewicht WHERE hundId = :hundId AND deleted = 0 ORDER BY datum DESC LIMIT 15")
    suspend fun getLetzteGewichte(hundId: Long): List<HundGewichtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGewicht(gewicht: HundGewichtEntity): Long

    @Query("UPDATE hund_gewicht SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDeleteGewicht(id: Long, now: Long = System.currentTimeMillis())
}

// ─────────────────────────────────────────────
// ZutatenDao
// ─────────────────────────────────────────────

@Dao
interface ZutatenDao {
    @Query("SELECT * FROM zutaten WHERE deleted = 0 ORDER BY name ASC")
    fun getAlleZutaten(): Flow<List<ZutatEntity>>

    @Query("SELECT * FROM zutaten WHERE id = :id AND deleted = 0")
    suspend fun getById(id: Long): ZutatEntity?

    @Query("SELECT * FROM zutaten WHERE deleted = 0 AND typ = :typ ORDER BY name ASC")
    suspend fun getByTyp(typ: String): List<ZutatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(zutat: ZutatEntity): Long

    @Update
    suspend fun update(zutat: ZutatEntity)

    @Query("UPDATE zutaten SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    // Nährstoffe
    @Query("SELECT * FROM zutat_naehrstoffe WHERE zutatId = :zutatId")
    suspend fun getNaehrstoffe(zutatId: Long): List<ZutatNaehrstoffEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNaehrstoff(naehrstoff: ZutatNaehrstoffEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNaehrstoffe(naehrstoffe: List<ZutatNaehrstoffEntity>)

    @Query("DELETE FROM zutat_naehrstoffe WHERE zutatId = :zutatId")
    suspend fun deleteNaehrstoffe(zutatId: Long)
}

// ─────────────────────────────────────────────
// RezeptDao
// ─────────────────────────────────────────────

@Dao
interface RezeptDao {
    @Query("SELECT * FROM rezepte WHERE hundId = :hundId AND deleted = 0 ORDER BY name ASC")
    fun getRezepteForHund(hundId: Long): Flow<List<RezeptEntity>>

    @Query("SELECT * FROM rezepte WHERE deleted = 0 ORDER BY name ASC")
    suspend fun getAlleRezepte(): List<RezeptEntity>

    @Query("SELECT * FROM rezepte WHERE id = :id AND deleted = 0")
    suspend fun getById(id: Long): RezeptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rezept: RezeptEntity): Long

    @Update
    suspend fun update(rezept: RezeptEntity)

    @Query("UPDATE rezepte SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    // Rezept-Zutaten
    @Query("SELECT * FROM rezept_zutaten WHERE rezeptId = :rezeptId ORDER BY reihenfolge ASC")
    suspend fun getZutatenForRezept(rezeptId: Long): List<RezeptZutatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZutat(item: RezeptZutatEntity): Long

    @Update
    suspend fun updateZutat(item: RezeptZutatEntity)

    @Query("DELETE FROM rezept_zutaten WHERE id = :id")
    suspend fun deleteZutat(id: Long)

    @Query("DELETE FROM rezept_zutaten WHERE rezeptId = :rezeptId")
    suspend fun deleteAllZutatenForRezept(rezeptId: Long)
}

// ─────────────────────────────────────────────
// TagebuchDao
// ─────────────────────────────────────────────

@Dao
interface TagebuchDao {

    // Umwelt
    @Query("SELECT * FROM tagebuch_umwelt WHERE hundId = :hundId AND deleted = 0 ORDER BY datum DESC")
    fun getUmweltForHund(hundId: Long): Flow<List<TagebuchUmweltEntity>>

    @Query("SELECT * FROM tagebuch_umwelt WHERE hundId = :hundId AND datum BETWEEN :von AND :bis AND deleted = 0")
    suspend fun getUmweltRange(hundId: Long, von: LocalDate, bis: LocalDate): List<TagebuchUmweltEntity>

    @Insert suspend fun insertUmwelt(e: TagebuchUmweltEntity): Long
    @Update suspend fun updateUmwelt(e: TagebuchUmweltEntity)
    @Query("UPDATE tagebuch_umwelt SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDeleteUmwelt(id: Long, now: Long = System.currentTimeMillis())

    // Pollen-Log
    @Query("SELECT * FROM tagebuch_pollen_log WHERE umweltId = :umweltId AND deleted = 0")
    suspend fun getPollenForUmwelt(umweltId: Long): List<TagebuchPollenLogEntity>

    @Query("SELECT * FROM tagebuch_pollen_log WHERE umweltId IN (SELECT id FROM tagebuch_umwelt WHERE hundId = :hundId AND datum BETWEEN :von AND :bis AND deleted = 0) AND deleted = 0")
    suspend fun getPollenRange(hundId: Long, von: LocalDate, bis: LocalDate): List<TagebuchPollenLogEntity>

    @Insert suspend fun insertPollenLog(e: TagebuchPollenLogEntity): Long
    @Query("DELETE FROM tagebuch_pollen_log WHERE umweltId = :umweltId")
    suspend fun deletePollenForUmwelt(umweltId: Long)

    // Eigene Pollenarten
    @Query("SELECT * FROM eigene_pollenarten ORDER BY name ASC")
    fun getEigenePollenarten(): Flow<List<EigenePollenartEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEigenePollenart(e: EigenePollenartEntity)
    @Query("DELETE FROM eigene_pollenarten WHERE name = :name") suspend fun deleteEigenePollenart(name: String)

    // Symptom
    @Query("SELECT * FROM tagebuch_symptom WHERE hundId = :hundId AND deleted = 0 ORDER BY datum DESC")
    fun getSymptomForHund(hundId: Long): Flow<List<TagebuchSymptomEntity>>

    @Query("SELECT * FROM tagebuch_symptom WHERE hundId = :hundId AND datum BETWEEN :von AND :bis AND deleted = 0")
    suspend fun getSymptomRange(hundId: Long, von: LocalDate, bis: LocalDate): List<TagebuchSymptomEntity>

    @Insert suspend fun insertSymptom(e: TagebuchSymptomEntity): Long
    @Update suspend fun updateSymptom(e: TagebuchSymptomEntity)
    @Query("UPDATE tagebuch_symptom SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDeleteSymptom(id: Long, now: Long = System.currentTimeMillis())

    // Futter
    @Query("SELECT * FROM tagebuch_futter WHERE hundId = :hundId AND deleted = 0 ORDER BY datum DESC")
    fun getFutterForHund(hundId: Long): Flow<List<TagebuchFutterEntity>>

    @Query("SELECT * FROM tagebuch_futter WHERE hundId = :hundId AND datum BETWEEN :von AND :bis AND deleted = 0")
    suspend fun getFutterRange(hundId: Long, von: LocalDate, bis: LocalDate): List<TagebuchFutterEntity>

    @Insert suspend fun insertFutter(e: TagebuchFutterEntity): Long
    @Update suspend fun updateFutter(e: TagebuchFutterEntity)
    @Query("UPDATE tagebuch_futter SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDeleteFutter(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM tagebuch_futter_item WHERE futterId = :futterId ORDER BY reihenfolge ASC")
    suspend fun getFutterItems(futterId: Long): List<TagebuchFutterItemEntity>
    @Insert suspend fun insertFutterItem(e: TagebuchFutterItemEntity): Long
    @Query("DELETE FROM tagebuch_futter_item WHERE futterId = :futterId") suspend fun deleteFutterItems(futterId: Long)

    // Ausschluss
    @Query("SELECT * FROM tagebuch_ausschluss WHERE hundId = :hundId AND deleted = 0 ORDER BY createdAt DESC")
    fun getAusschlussForHund(hundId: Long): Flow<List<TagebuchAusschlussEntity>>
    @Insert suspend fun insertAusschluss(e: TagebuchAusschlussEntity): Long
    @Update suspend fun updateAusschluss(e: TagebuchAusschlussEntity)
    @Query("UPDATE tagebuch_ausschluss SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDeleteAusschluss(id: Long, now: Long = System.currentTimeMillis())

    // Allergen
    @Query("SELECT * FROM tagebuch_allergen WHERE hundId = :hundId AND deleted = 0 ORDER BY allergen ASC")
    fun getAllergenForHund(hundId: Long): Flow<List<TagebuchAllergenEntity>>
    @Insert suspend fun insertAllergen(e: TagebuchAllergenEntity): Long
    @Update suspend fun updateAllergen(e: TagebuchAllergenEntity)
    @Query("UPDATE tagebuch_allergen SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDeleteAllergen(id: Long, now: Long = System.currentTimeMillis())

    // Tierarzt
    @Query("SELECT * FROM tagebuch_tierarzt WHERE hundId = :hundId AND deleted = 0 ORDER BY datum DESC")
    fun getTierarztForHund(hundId: Long): Flow<List<TagebuchTierarztEntity>>
    @Insert suspend fun insertTierarzt(e: TagebuchTierarztEntity): Long
    @Update suspend fun updateTierarzt(e: TagebuchTierarztEntity)
    @Query("UPDATE tagebuch_tierarzt SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDeleteTierarzt(id: Long, now: Long = System.currentTimeMillis())

    // Medikament
    @Query("SELECT * FROM tagebuch_medikament WHERE hundId = :hundId AND deleted = 0 ORDER BY vonDatum DESC")
    fun getMedikamentForHund(hundId: Long): Flow<List<TagebuchMedikamentEntity>>
    @Insert suspend fun insertMedikament(e: TagebuchMedikamentEntity): Long
    @Update suspend fun updateMedikament(e: TagebuchMedikamentEntity)
    @Query("UPDATE tagebuch_medikament SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDeleteMedikament(id: Long, now: Long = System.currentTimeMillis())

    // Ausschluss-Phasen
    @Query("SELECT * FROM ausschluss_phasen WHERE hundId = :hundId AND deleted = 0 ORDER BY startdatum DESC")
    fun getPhasenForHund(hundId: Long): Flow<List<AusschlussPhasEntity>>
    @Insert suspend fun insertPhase(e: AusschlussPhasEntity): Long
    @Update suspend fun updatePhase(e: AusschlussPhasEntity)
    @Query("UPDATE ausschluss_phasen SET deleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDeletePhase(id: Long, now: Long = System.currentTimeMillis())
}

// ─────────────────────────────────────────────
// ParameterDao
// ─────────────────────────────────────────────

@Dao
interface ParameterDao {
    @Query("SELECT * FROM parameter")
    suspend fun getAll(): List<ParameterEntity>

    @Query("SELECT wert FROM parameter WHERE schluessel = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ParameterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(params: List<ParameterEntity>)
}
