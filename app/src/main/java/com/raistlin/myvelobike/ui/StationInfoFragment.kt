package com.raistlin.myvelobike.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.raistlin.myvelobike.R
import com.raistlin.myvelobike.databinding.FragmentStationInfoBinding
import com.raistlin.myvelobike.dto.Station
import com.raistlin.myvelobike.viewmodel.MapViewModel
import kotlinx.coroutines.launch

class StationInfoFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentStationInfoBinding.inflate(inflater, container, false)
        val viewModel by viewModels<MapViewModel>(ownerProducer = ::requireParentFragment)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedStation.collect { station ->
                    binding.root.isVisible = station != null
                    station?.let { showStationInfo(binding, it) }
                }
            }
        }
        binding.stationClose.setOnClickListener {
            viewModel.closeStation()
        }
        return binding.root
    }

    private fun showStationInfo(binding: FragmentStationInfoBinding, station: Station) {
        binding.stationTitle.text = getString(R.string.station_info_title, station.id)
        binding.stationElectricType.isVisible = station.isElectric()
        binding.stationLocked.isVisible = station.isLocked
        binding.stationAddress.text = station.address
        binding.stationOrdinaryBikesCount.setColoredCount(station.availableOrdinaryBikes)
        binding.stationOrdinaryPlacesCount.setColoredCount(station.freeOrdinaryPlaces)
        binding.stationElectricBikesCount.setColoredCount(station.availableElectricBikes)
        binding.stationElectricPlacesCount.setColoredCount(station.freeElectricPlaces)
        binding.stationOrdinaryGroup.isVisible = station.isOrdinary() || station.availableOrdinaryBikes > 0
        binding.stationElectricGroup.isVisible = station.isElectric() || station.availableElectricBikes > 0
    }

    private fun TextView.setColoredCount(count: Int) {
        isEnabled = count > 1
        text = count.toString()
    }

}