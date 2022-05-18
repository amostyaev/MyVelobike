package com.raistlin.myvelobike.ui

import android.annotation.SuppressLint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.addMarker
import com.raistlin.myvelobike.dto.Station
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class RealtimeFragment : MapFragment() {

    @SuppressLint("PotentialBehaviorOverride")
    override fun mapReady(map: GoogleMap) {
        val kremlin = LatLng(55.751999, 37.617734)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(kremlin, 10f))
        map.setOnMarkerClickListener { marker ->
            viewModel.selectStation(marker.tag as Int)
            false
        }
        map.setOnMapClickListener {
            viewModel.closeStation()
        }
        lifecycleScope.launch {
            delay(500)
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stations.collect {
                    showStations(map, it)
                }
            }
        }
    }

    override suspend fun makeSync() {
        viewModel.syncStations().join()
    }

    private fun showStations(map: GoogleMap, stations: List<Station>) {
        map.clear()
        stations.forEach { station ->
            map.addMarker {
                title(station.id.toString())
                position(station.position.toLatLng())
                icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmap(getStationIcon(station.isElectric(), station.fillStatus()))))
            }?.apply {
                tag = station.id
            }
        }
    }
}