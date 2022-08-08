package com.raistlin.myvelobike.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raistlin.myvelobike.api.ApiService
import com.raistlin.myvelobike.db.AppDb
import com.raistlin.myvelobike.dto.Place
import com.raistlin.myvelobike.entity.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>()
    private val dao = AppDb.getInstance(context).rideDao()
    private val service by lazy { ApiService(context, dao) }

    private val selectedId = MutableStateFlow<Int?>(null)

    private val electricFilter = MutableStateFlow(false)

    private val rides = dao.getAllRides().map { it.toDto() }.shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val stations = dao.getAllStations().map { it.toDto() }.shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val places = rides.combineTransform(stations) { rides, stations ->
        val visits = rides.fold(mutableMapOf<Int, Int>()) { acc, ride ->
            acc.apply {
                put(ride.startParking, acc.getOrElse(ride.startParking) { 0 } + 1)
                put(ride.endParking, acc.getOrElse(ride.endParking) { 0 } + 1)
            }
        }
        emit(stations.map { Place(it.id, it.position, it.fillStatus(), it.isElectric(), visits.getOrElse(it.id) { 0 }) })
    }.shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val filteredStations = stations.combineTransform(electricFilter) { stations, electric ->
        emit(if (electric) stations.filter { it.isElectric() || it.availableElectricBikes > 0 } else stations)
    }

    val selectedStation = stations.combineTransform(selectedId) { stations, selected ->
        emit(stations.find { it.id == selected })
    }

    fun syncStations() = viewModelScope.launch {
        service.syncStations()
    }

    fun syncAll() = viewModelScope.launch {
        service.syncStations()
        service.syncStats().catch {  }.flowOn(Dispatchers.IO).collect()
    }

    fun filterStations(electric: Boolean) {
        electricFilter.value = electric
    }

    fun isFiltered() = electricFilter.value

    fun selectStation(id: Int) {
        selectedId.value = id
    }

    fun closeStation() {
        selectedId.value = null
    }
}