package com.raistlin.myvelobike.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.max

@Serializable
data class Station(

    @SerialName("static_external_id")
    @Serializable(with = StationIdSerializer::class)
    val id: Int,

    @SerialName("addr")
    val address: String,

    @SerialName("num_of_available_electric_bikes")
    @Serializable(with = NumNullableSerializer::class)
    val availableElectricBikes: Int,

    @SerialName("num_of_available_non_electric_bikes")
    @Serializable(with = NumNullableSerializer::class)
    val availableOrdinaryBikes: Int,

    @SerialName("num_of_free_electric_slots")
    @Serializable(with = NumNullableSerializer::class)
    val freeElectricPlaces: Int,

    @SerialName("num_of_free_non_electric_slots")
    @Serializable(with = NumNullableSerializer::class)
    val freeOrdinaryPlaces: Int,

    @SerialName("num_of_electric_slots")
    @Serializable(with = NumNullableSerializer::class)
    val totalElectricPlaces: Int,

    @SerialName("num_of_non_electric_slots")
    @Serializable(with = NumNullableSerializer::class)
    val totalOrdinaryPlaces: Int,

    @SerialName("latitude")
    val latitude: Double,

    @SerialName("longitude")
    val longitude: Double,

    @SerialName("station_status")
    @Serializable(with = IsLockedSerializer::class)
    val isLocked: Boolean,

    @SerialName("station_overflowed")
    val isOverflow: Boolean,

    @SerialName("type")
    val type: Int
) {
    fun fillStatus() = when (isLocked) {
        true -> -1
        false -> (availableElectricBikes + availableOrdinaryBikes) * 100 / max(totalElectricPlaces + totalOrdinaryPlaces, 1)
    }

    fun isOrdinary() = type == 0 || type == 2

    fun isElectric() = type == 1 || type == 2
}

object StationIdSerializer : KSerializer<Int> {

    override val descriptor = PrimitiveSerialDescriptor("Bike.Id", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        return decoder.decodeString()
            .replace("undefined", "-1")
            .replace("p", "")
            .replace("c", "10")
            .replace("m", "10")
            .replace("ot", "1000")
            .replace("MS", "1000")
            .toInt()
    }
}

object NumNullableSerializer : KSerializer<Int> {

    private val delegate = Int.serializer().nullable

    override val descriptor = PrimitiveSerialDescriptor("Bike.Num", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        return delegate.deserialize(decoder) ?: 0
    }
}

object IsLockedSerializer : KSerializer<Boolean> {

    private val delegate = Int.serializer()

    override val descriptor = PrimitiveSerialDescriptor("Bike.Locked", PrimitiveKind.BOOLEAN)

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        return delegate.deserialize(decoder) != 1
    }
}