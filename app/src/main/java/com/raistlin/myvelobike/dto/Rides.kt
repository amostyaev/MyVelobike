package com.raistlin.myvelobike.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.*


@Serializable(with = EventSerializer::class)
sealed class Event

@Serializable
enum class BikeType {

    @SerialName("ordinary")
    Ordinary,

    @SerialName("electric")
    Electric
}

@Serializable
data class Ride(

    @SerialName("Id")
    val id: String,

    @SerialName("BikeId")
    val bikeId: Int,

    @SerialName("BikeType")
    val bikeType: BikeType,

    @SerialName("CoveredDistance")
    val distance: Int,

    @SerialName("Duration")
    @Serializable(with = DurationAsLongSerializer::class)
    val duration: Long,

    @SerialName("StartBikeParkingNumber")
    val startParking: Int,

    @SerialName("EndBikeParkingNumber")
    val endParking: Int,

    @SerialName("StartDate")
    val startDate: LocalDateTime,

    @SerialName("EndDate")
    val endDate: LocalDateTime
) : Event() {
    fun isElectric() = bikeType == BikeType.Electric
}

@Serializable
object Pay : Event() {
    override fun toString(): String {
        return javaClass.simpleName
    }
}

@Serializable
data class Events(
    @SerialName("HasMore")
    val hasMore: Boolean,
    @SerialName("TotalRides")
    val totalRides: Int,
    @SerialName("Items")
    val items: List<Event>
)

object EventSerializer : JsonContentPolymorphicSerializer<Event>(Event::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Event> {
        val value = element as JsonObject?
        val type = value?.get("Type") as JsonPrimitive?
        return when (type?.content) {
            "Ride" -> Ride.serializer()
            "Pay" -> Pay.serializer()
            else -> throw RuntimeException("Unknown event type: ${type?.content}")
        }
    }
}

object DurationAsLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Bike.Duration", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Long {
        val value = decoder.decodeString()
        val parser = SimpleDateFormat("dd.HH:mm:ss", Locale.US)
        val result = parser.parse(value) ?: throw SerializationException("Illegal duration data: $value")
        // add one day and time zone offset as the numbering starts from zero
        return (result.time + with(Calendar.getInstance()) { get(Calendar.ZONE_OFFSET) + get(Calendar.DST_OFFSET) }) / 1000 + 86400
    }
}