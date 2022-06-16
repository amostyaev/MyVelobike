package com.raistlin.myvelobike.ui

import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.addMarker
import com.raistlin.myvelobike.R
import com.raistlin.myvelobike.databinding.FragmentMapBinding
import com.raistlin.myvelobike.dto.Ride
import com.raistlin.myvelobike.dto.Station
import kotlinx.coroutines.launch

class RidesPlacesFragment : MapFragment() {

    override fun setupBinding(binding: FragmentMapBinding) {
        super.setupBinding(binding)
        binding.mapSync.isVisible = false
    }

    @Suppress("UNCHECKED_CAST")
    override fun mapReady(map: GoogleMap) {
        val rides = arguments?.getSerializable(RidesFragment.KEY_RIDES) as List<Ride>
        val visited = rides.fold(mutableSetOf<Int>()) { acc, ride -> acc.apply { add(ride.startParking); add(ride.endParking) } }

        val kremlin = LatLng(55.751999, 37.617734)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(kremlin, 10f))
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stations.collect {
                    showPlaces(map, it.filter { station -> station.id in visited })
                }
            }
        }
    }

    private fun showPlaces(map: GoogleMap, stations: List<Station>) {
        map.clear()
        stations.forEach { station ->
            map.addMarker {
                title("${station.id}")
                position(station.position.toLatLng())
                icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmap(R.drawable.ic_place_visited)))
            }?.apply {
                tag = station.id
            }
        }
    }

    override suspend fun makeSync() {
    }

    companion object {
        const val KEY_RIDES = "rides"
    }
}