package com.raistlin.myvelobike.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.raistlin.myvelobike.entity.RideEntity
import com.raistlin.myvelobike.entity.StationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {

    @Query("SELECT * FROM RideEntity ")
    fun getAllRides(): Flow<List<RideEntity>>

    @Query("SELECT EXISTS(SELECT * FROM RideEntity WHERE id = :id)")
    fun hasRide(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRide(ride: RideEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRides(rides: List<RideEntity>)

    @Query("SELECT * FROM StationEntity ")
    fun getAllStations(): Flow<List<StationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStations(rides: List<StationEntity>)

}