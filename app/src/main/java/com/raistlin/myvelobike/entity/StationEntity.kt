package com.raistlin.myvelobike.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.raistlin.myvelobike.dto.Station

@Entity
data class StationEntity(
    @PrimaryKey
    val id: Int,
    val address: String,
    val availableElectricBikes: Int,
    val availableOrdinaryBikes: Int,
    val freeElectricPlaces: Int,
    val freeOrdinaryPlaces: Int,
    val totalElectricPlaces: Int,
    val totalOrdinaryPlaces: Int,
    val latitude: Double,
    val longitude: Double,
    val isLocked: Boolean,
    val isOverflow: Boolean,
    val type: Int
) {
    fun toDto() = Station(
        id, address, availableElectricBikes, availableOrdinaryBikes, freeElectricPlaces, freeOrdinaryPlaces, totalElectricPlaces, totalOrdinaryPlaces, latitude, longitude, isLocked, isOverflow, type
    )

    companion object {
        fun fromDto(station: Station) = StationEntity(
            station.id,
            station.address,
            station.availableElectricBikes,
            station.availableOrdinaryBikes,
            station.freeElectricPlaces,
            station.freeOrdinaryPlaces,
            station.totalElectricPlaces,
            station.totalOrdinaryPlaces,
            station.latitude,
            station.longitude,
            station.isLocked,
            station.isOverflow,
            station.type
        )
    }
}

fun List<Station>.toEntity() = map(StationEntity::fromDto)
fun List<StationEntity>.toDto() = map(StationEntity::toDto)