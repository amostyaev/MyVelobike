package com.raistlin.myvelobike.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raistlin.myvelobike.api.ApiService
import com.raistlin.myvelobike.db.AppDb
import com.raistlin.myvelobike.entity.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.util.*

class BikeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>()
    private val dao = AppDb.getInstance(context).rideDao()
    private val service by lazy { ApiService(context, dao) }

    private val _dates: MutableStateFlow<Pair<Long, Long>>
    val dates: StateFlow<Pair<Long, Long>>
        get() = _dates

    init {
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar[Calendar.HOUR_OF_DAY] = 23
        calendar[Calendar.MINUTE] = 59
        val end = calendar.timeInMillis
        calendar[Calendar.MONTH] = Calendar.APRIL
        calendar[Calendar.DAY_OF_MONTH] = 1
        val start = calendar.timeInMillis
        _dates = MutableStateFlow(start to end)
    }

    val rides = dao.getAllRides().map { it.toDto() }.shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val dateRides = rides.combineTransform(dates) { rides, dates ->
        emit(rides.filter { it.startDate.toInstant(TimeZone.UTC).epochSeconds >= dates.first / 1000 && it.endDate.toInstant(TimeZone.UTC).epochSeconds <= dates.second / 1000 })
    }

    fun syncStats() = service.syncStats().catch {  }.flowOn(Dispatchers.IO)

    fun updateDates(start: Long, end: Long) {
        _dates.value = start to end
    }

    override fun onCleared() {
        super.onCleared()
        service.close()
    }
}
