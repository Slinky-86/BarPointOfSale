// Copyright 2024 anyone-Hub
@file:Suppress("unused")

package com.anyonehub.barpos.data

import androidx.room.TypeConverter
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class Converters {
    @TypeConverter
    fun fromInstant(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @TypeConverter
    fun instantToLong(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }
}
