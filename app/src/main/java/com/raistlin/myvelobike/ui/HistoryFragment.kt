package com.raistlin.myvelobike.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.raistlin.myvelobike.R
import com.raistlin.myvelobike.databinding.FragmentHistoryBinding
import com.raistlin.myvelobike.dto.Ride
import com.raistlin.myvelobike.util.asTimeString
import com.raistlin.myvelobike.util.utcDate
import com.raistlin.myvelobike.viewmodel.BikeViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class HistoryFragment : Fragment() {

    private val viewModel by viewModels<BikeViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentHistoryBinding.inflate(inflater, container, false)

        binding.historySync.setOnClickListener {
            lifecycleScope.launchWhenResumed {
                viewModel.syncStats().collect()
                Snackbar.make(binding.root, R.string.action_sync_completed, Snackbar.LENGTH_LONG).show()
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rides.collect { rides ->
                    showStats(binding, rides)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dateRides.collect { dateRides ->
                    showDateStata(binding, dateRides)
                }
            }
        }

        binding.historyDatesChooser.setOnClickListener {
            val constraintsBuilder = CalendarConstraints.Builder().setOpenAt(viewModel.dates.value.second)
            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.history_select_dates))
                .setSelection(androidx.core.util.Pair(viewModel.dates.value.first, viewModel.dates.value.second))
                .setCalendarConstraints(constraintsBuilder.build())
                .build()
            dateRangePicker.addOnPositiveButtonClickListener { chosen ->
                viewModel.updateDates(chosen.first, chosen.second + 86399 * 1000)
            }
            dateRangePicker.show(parentFragmentManager, TAG_DATES_CHOOSER)
        }

        return binding.root
    }

    private fun showStats(binding: FragmentHistoryBinding, rides: List<Ride>) {
        binding.historyTotalRides.text = rides.size.toString()
        binding.historyTotalDistance.text = getString(R.string.ride_distance, rides.sumOf { it.distance } / 1000f)
        binding.historyTotalTime.text = rides.sumOf { it.duration }.asTimeString(requireContext())

        binding.historyAverage.averageDistance.text = getString(R.string.ride_distance, rides.sumOf { it.distance } / 1000f / max(rides.size, 1))
        binding.historyAverage.averageTime.text = (rides.sumOf { it.duration } / max(rides.size, 1)).asTimeString(requireContext())
        binding.historyAverage.averageSpeed.text = getString(R.string.ride_speed, rides.filter { it.duration > 0 }.map { it.distance / it.duration.toFloat() }.sum() / max(rides.size, 1) * 3600 / 1000)

        binding.historyBikesCount.text = rides.map { it.bikeId }.toSet().size.toString()
        binding.historyStationsCount.text = rides.fold(mutableSetOf<Int>()) { acc, ride -> acc.addAll(listOf(ride.startParking, ride.endParking)); acc }.toSet().size.toString()

        binding.historyCardTotal.setOnClickListener {
            findNavController().navigate(R.id.ridesFragment, bundleOf(RidesFragment.KEY_RIDES to rides))
        }
    }

    private fun showDateStata(binding: FragmentHistoryBinding, rides: List<Ride>) {
        binding.historyDatesRides.text = rides.size.toString()
        binding.historyDatesDistance.text = getString(R.string.ride_distance, rides.sumOf { it.distance } / 1000f)
        binding.historyDatesTime.text = rides.sumOf { it.duration }.asTimeString(requireContext())

        val format = SimpleDateFormat("dd.MM.yy", Locale.US)
        val dates = "${format.format(viewModel.dates.value.first.utcDate())} - ${format.format(viewModel.dates.value.second.utcDate())}"
        binding.historyDatesChooser.text = dates

        binding.historyCardDates.setOnClickListener {
            findNavController().navigate(R.id.ridesFragment, bundleOf(RidesFragment.KEY_RIDES to rides))
        }
    }

    companion object {
        const val TAG_DATES_CHOOSER = "dates_chooser"
    }
}