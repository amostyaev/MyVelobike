package com.raistlin.myvelobike.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.raistlin.myvelobike.dao.RideDao
import com.raistlin.myvelobike.entity.Converters
import com.raistlin.myvelobike.entity.RideEntity
import com.raistlin.myvelobike.entity.StationEntity

@Database(entities = [RideEntity::class, StationEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun rideDao(): RideDao

    companion object {
        @Volatile
        private var instance: AppDb? = null

        fun getInstance(context: Context): AppDb {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, AppDb::class.java, "rides.db")
                .build()
    }
}