package com.raistlin.myvelobike.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.max

@Serializable
data class Station(

    @SerialName("Id")
    @Serializable(with = StationIdSerializer::class)
    val id: Int,

    @SerialName("Address")
    val address: String,

    @SerialName("AvailableElectricBikes")
    val availableElectricBikes: Int,

    @SerialName("AvailableOrdinaryBikes")
    val availableOrdinaryBikes: Int,

    @SerialName("FreeElectricPlaces")
    val freeElectricPlaces: Int,

    @SerialName("FreeOrdinaryPlaces")
    val freeOrdinaryPlaces: Int,

    @SerialName("TotalElectricPlaces")
    val totalElectricPlaces: Int,

    @SerialName("TotalOrdinaryPlaces")
    val totalOrdinaryPlaces: Int,

    @SerialName("Position")
    val position: Position,

    @SerialName("IsLocked")
    val isLocked: Boolean,

    @SerialName("IsOverflow")
    val isOverflow: Boolean,

    @SerialName("StationTypes")
    val types: List<StationType>
) {
    fun fillStatus() = when (isLocked) {
        true -> -1
        false -> (availableElectricBikes + availableOrdinaryBikes) * 100 / max(totalElectricPlaces + totalOrdinaryPlaces, 1)
    }

    fun isOrdinary() = types.contains(StationType.Ordinary)

    fun isElectric() = types.contains(StationType.Electric)
}

@Serializable
data class Position(

    @SerialName("Lat")
    val lat: Double,

    @SerialName("Lon")
    val lon: Double
)

@Serializable
enum class StationType {

    @SerialName("ordinary")
    Ordinary,

    @SerialName("electric")
    Electric
}

object StationIdSerializer : KSerializer<Int> {

    override val descriptor = PrimitiveSerialDescriptor("Bike.Id", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        return decoder.decodeString()
            .replace("c", "10")
            .replace("m", "10")
            .replace("ot", "1000")
            .toInt()
    }
}