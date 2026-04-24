package com.allerpaw.app.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromInstant(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun toInstant(v: Long?): Instant? = v?.let { Instant.ofEpochMilli(it) }

    @TypeConverter fun fromLocalDate(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun toLocalDate(v: String?): LocalDate? = v?.let { LocalDate.parse(it) }
}
