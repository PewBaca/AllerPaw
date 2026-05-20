package com.allernutri.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.allernutri.app.data.local.dao.ToleranzDao
import com.allernutri.app.data.local.dao.*
import com.allernutri.app.data.local.entity.*

@Database(
    entities = [
        HundEntity::class,
        HundGewichtEntity::class,
        ZutatEntity::class,
        ZutatNaehrstoffEntity::class,
        RezeptEntity::class,
        RezeptZutatEntity::class,
        ParameterEntity::class,
        ToleranzEntity::class,
        TagebuchUmweltEntity::class,
        TagebuchSymptomEntity::class,
        TagebuchFutterEntity::class,
        TagebuchFutterItemEntity::class,
        TagebuchAusschlussEntity::class,
        TagebuchAllergenEntity::class,
        TagebuchTierarztEntity::class,
        TagebuchMedikamentEntity::class,
        TagebuchPollenLogEntity::class,
        AusschlussPhasEntity::class,
        EigenePollenartEntity::class,
        TagebuchHundZustandEntity::class,
        TaskEntity::class,
        TaskErledigung::class,
        SymptomMediaEntity::class,
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hundDao(): HundDao
    abstract fun zutatenDao(): ZutatenDao
    abstract fun rezeptDao(): RezeptDao
    abstract fun tagebuchDao(): TagebuchDao
    abstract fun parameterDao(): ParameterDao
    abstract fun hundZustandDao(): HundZustandDao
    abstract fun taskDao(): TaskDao
    abstract fun symptomMediaDao(): SymptomMediaDao
    abstract fun toleranzDao(): ToleranzDao
}
