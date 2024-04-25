package com.raistlin.myvelobike.entity

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDateTime

class Converters {

    @TypeConverter
    fun dateTimeFromString(value: String) = LocalDateTime.parse(value)

    @TypeConverter
    fun dateTimeToString(value: LocalDateTime) = value.toString()
}