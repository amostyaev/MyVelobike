package com.raistlin.myvelobike.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.raistlin.myvelobike.dto.BikeType
import com.raistlin.myvelobike.dto.Ride
import kotlinx.datetime.LocalDateTime

@Entity
data class RideEntity(
    @PrimaryKey
    val id: String,
    val bikeId: Int,
    val bikeType: BikeType,
    val distance: Int,
    val duration: Long,
    val startBikeParkingNumber: Int,
    val endBikeParkingNumber: Int,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
) {

    fun toDto() = Ride(
        id, bikeId, bikeType, distance, duration, startBikeParkingNumber, endBikeParkingNumber, startDate, endDate
    )

    companion object {
        fun fromDto(ride: Ride) = RideEntity(
            ride.id,
            ride.bikeId,
            ride.bikeType,
            ride.distance,
            ride.duration,
            ride.startParking,
            ride.endParking,
            ride.startDate,
            ride.endDate
        )
    }
}

fun List<Ride>.toEntity() = map(RideEntity::fromDto)
fun List<RideEntity>.toDto() = map(RideEntity::toDto)