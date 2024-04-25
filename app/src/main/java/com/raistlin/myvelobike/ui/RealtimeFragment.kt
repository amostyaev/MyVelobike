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
import com.google.maps.android.ktx.addMarker
import com.raistlin.myvelobike.R
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
                viewModel.filteredStations.collect {
                    showStations(map, it)
                }
            }
        }
    }

    override suspend fun makeSync() {
        viewModel.syncStations().join()
    }

    private fun showStations(map: GoogleMap, stations: List<Station>) {
        if (stations.isEmpty()) return

        map.clear()
        stations.forEach { station ->
            map.addMarker {
                title(station.id.toString())
                position(station.toLatLng())
                icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmap(getStationIcon(station.isElectric(), station.fillStatus()))))
            }?.apply {
                tag = station.id
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_realtime, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.action_electric)?.setIcon(if (viewModel.isFiltered()) R.drawable.ic_action_full else R.drawable.ic_action_electric)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_electric -> {
                        viewModel.filterStations(!viewModel.isFiltered())
                        requireActivity().invalidateOptionsMenu()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}