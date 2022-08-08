package com.raistlin.myvelobike.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ktx.addMarker
import com.raistlin.myvelobike.R
import com.raistlin.myvelobike.dto.Place
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlacesFragment : MapFragment() {

    @SuppressLint("PotentialBehaviorOverride")
    override fun mapReady(map: GoogleMap) {
        val kremlin = LatLng(55.751999, 37.617734)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(kremlin, 10f))
        map.setOnInfoWindowClickListener { marker ->
            viewModel.selectStation(marker.tag as Int)
        }
        map.setOnMarkerClickListener {
            viewModel.closeStation()
            false
        }
        map.setOnMapClickListener {
            viewModel.closeStation()
        }
        lifecycleScope.launch {
            delay(500)
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.places.collect {
                    showPlaces(map, it)
                }
            }
        }
    }

    override suspend fun makeSync() {
        viewModel.syncAll().join()
    }

    private fun showPlaces(map: GoogleMap, stations: List<Place>) {
        if (stations.isEmpty()) return

        map.clear()
        stations.forEach { place ->
            map.addMarker {
                title("${place.id} ${place.visits}")
                position(place.position.toLatLng())
                icon(
                    BitmapDescriptorFactory.fromBitmap(
                        getMarkerBitmap(
                            when (place.visits) {
                                in 51..Int.MAX_VALUE -> R.drawable.ic_place_famous
                                in 16..50 -> R.drawable.ic_place_popular
                                in 1..15 -> R.drawable.ic_place_visited
                                else -> getStationIcon(place.electric, place.fillStatus)
                            }
                        )
                    )
                )
            }?.apply {
                tag = place.id
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_places, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_statistics -> {
                        val places = viewModel.places.replayCache.lastOrNull() ?: return true
                        Snackbar.make(requireView(), getString(R.string.places_statistics, places.count { it.visits > 0 }, places.size), Snackbar.LENGTH_LONG).show()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}