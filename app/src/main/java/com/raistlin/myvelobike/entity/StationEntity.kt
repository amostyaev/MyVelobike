package com.raistlin.myvelobike.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.raistlin.myvelobike.dto.Position
import com.raistlin.myvelobike.dto.Station
import com.raistlin.myvelobike.dto.StationType

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
    @Embedded
    val position: Position,
    val isLocked: Boolean,
    val isOverflow: Boolean,
    val types: List<StationType>
) {
    fun toDto() = Station(
        id, address, availableElectricBikes, availableOrdinaryBikes, freeElectricPlaces, freeOrdinaryPlaces, totalElectricPlaces, totalOrdinaryPlaces, position, isLocked, isOverflow, types
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
            station.position,
            station.isLocked,
            station.isOverflow,
            station.types
        )
    }
}

fun List<Station>.toEntity() = map(StationEntity::fromDto)
fun List<StationEntity>.toDto() = map(StationEntity::toDto)