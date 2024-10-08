package com.raistlin.myvelobike.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ktx.awaitMap
import com.raistlin.myvelobike.R
import com.raistlin.myvelobike.databinding.FragmentMapBinding
import com.raistlin.myvelobike.dto.Station
import com.raistlin.myvelobike.store.getLastSync
import com.raistlin.myvelobike.util.dp
import com.raistlin.myvelobike.viewmodel.MapViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

abstract class MapFragment : Fragment() {

    protected val viewModel by viewModels<MapViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentMapBinding.inflate(inflater, container, false)

        lifecycleScope.launch {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            val map = mapFragment.awaitMap().apply {
                uiSettings.apply {
                    isZoomControlsEnabled = true
                }
            }
            mapReady(map)
        }

        setupBinding(binding)
        return binding.root
    }

    protected open fun setupBinding(binding: FragmentMapBinding) {
        binding.mapSync.setOnClickListener {
            lifecycleScope.launch {
                try {
                    makeSync()
                    Snackbar.make(binding.root, R.string.action_sync_completed, Snackbar.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Snackbar.make(binding.root, R.string.action_sync_error, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_retry) {
                            binding.mapSync.performClick()
                        }.show()
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                requireContext().getLastSync().collect { lastSync ->
                    when (lastSync) {
                        0L -> binding.mapLastSync.setText(R.string.map_not_synced)
                        else -> {
                            val date = SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date(lastSync))
                            binding.mapLastSync.text = date
                        }
                    }
                }
            }
        }
    }

    abstract fun mapReady(map: GoogleMap)

    abstract suspend fun makeSync()

    @DrawableRes
    protected fun getStationIcon(electric: Boolean, fillStatus: Int) =
        when (electric) {
            true -> when (fillStatus) {
                -1 -> R.drawable.ic_station_locked
                in 0..33 -> R.drawable.ic_station_electric_red
                in 34..66 -> R.drawable.ic_station_electric_yellow
                else -> R.drawable.ic_station_electric_green
            }
            false -> when (fillStatus) {
                -1 -> R.drawable.ic_station_locked
                in 0..33 -> R.drawable.ic_station_red
                in 34..66 -> R.drawable.ic_station_yellow
                else -> R.drawable.ic_station_green
            }
        }

    protected fun getMarkerBitmap(@DrawableRes resourceId: Int) =
        requireNotNull(ContextCompat.getDrawable(requireContext(), resourceId))
            .toBitmap(width = 16.dp(requireContext()), height = 16.dp(requireContext()))

    protected fun Station.toLatLng() = LatLng(latitude, longitude)
}