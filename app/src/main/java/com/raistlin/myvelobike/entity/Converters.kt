package com.raistlin.myvelobike.entity

import androidx.room.TypeConverter
import com.raistlin.myvelobike.dto.StationType
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun dateTimeFromString(value: String) = LocalDateTime.parse(value)

    @TypeConverter
    fun dateTimeToString(value: LocalDateTime) = value.toString()

    @TypeConverter
    fun typeListFromString(value:String) = Json.decodeFromString<List<StationType>>(value)

    @TypeConverter
    fun typeListToString(value: List<StationType>) = Json.encodeToString(value)
}