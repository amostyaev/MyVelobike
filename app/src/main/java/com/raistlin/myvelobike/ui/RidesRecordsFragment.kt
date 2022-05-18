package com.raistlin.myvelobike.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.raistlin.myvelobike.R
import com.raistlin.myvelobike.databinding.FragmentRidesRecordsBinding
import com.raistlin.myvelobike.dto.Ride
import com.raistlin.myvelobike.util.asString
import com.raistlin.myvelobike.util.asTimeString
import kotlinx.datetime.LocalDate
import kotlin.math.max

class RidesRecordsFragment : Fragment() {

    private var binding: FragmentRidesRecordsBinding? = null

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentRidesRecordsBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    fun displayStats(rides: List<Ride>) {
        binding?.let { binding ->
            val (average, max) = calcStats(rides)
            showStats(binding, average, max)
            binding.root.isVisible = true
        }
    }

    fun hideStats() {
        binding?.root?.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun calcStats(rides: List<Ride>): Pair<Average, Max> {
        val average = Average(
            distance = rides.sumOf { it.distance } / max(rides.size, 1),
            time = rides.sumOf { it.duration } / max(rides.size, 1),
            speed = rides.filter { it.duration > 0 }.map { it.distance / it.duration.toFloat() }.sum() / max(rides.size, 1) * 3600 / 1000
        )
        val distanceByDay = rides.fold(mutableMapOf<LocalDate, Int>()) { acc, ride -> acc.apply { put(ride.startDate.date, getOrElse(ride.startDate.date) { 0 } + ride.distance) } }
        val timeByDay = rides.fold(mutableMapOf<LocalDate, Long>()) { acc, ride -> acc.apply { put(ride.startDate.date, getOrElse(ride.startDate.date) { 0 } + ride.duration) } }
        val ridesByDay = rides.fold(mutableMapOf<LocalDate, Int>()) { acc, ride -> acc.apply { put(ride.startDate.date, getOrElse(ride.startDate.date) { 0 } + 1) } }
        val max = Max(
            byDistance = rides.maxByOrNull { it.distance },
            byTime = rides.maxByOrNull { it.duration },
            bySpeed = rides.filter { it.duration > 0 }.maxByOrNull { it.distance / it.duration.toFloat() },
            dayDistance = distanceByDay.maxByOrNull { it.value }?.toPair(),
            dayTime = timeByDay.maxByOrNull { it.value }?.toPair(),
            dayRides = ridesByDay.maxByOrNull { it.value }?.toPair()
        )
        return average to max
    }

    private fun showStats(binding: FragmentRidesRecordsBinding, average: Average, max: Max) {
        with(binding.recordsAverage) {
            averageDistance.text = binding.root.context.getString(R.string.ride_distance, average.distance / 1000f)
            averageTime.text = average.time.asTimeString(binding.root.context)
            averageSpeed.text = binding.root.context.getString(R.string.ride_speed, average.speed)
        }
        with(binding.recordsMax) {
            maxDistance.text = binding.root.context.getString(R.string.ride_distance, max.byDistance?.distance?.div(1000f) ?: 0f)
            maxTime.text = max.byTime?.duration?.asTimeString(binding.root.context) ?: ""
            maxSpeed.text = binding.root.context.getString(R.string.ride_speed, max.bySpeed?.let { it.distance / it.duration.toFloat() }?.times(3600)?.div(1000) ?: 0f)
            max.byDistance?.let {
                maxDistanceHint.text = binding.root.context.getString(R.string.max_hint_route_date, it.startParking, it.endParking, it.startDate.date.asString())
            }
            max.byTime?.let {
                maxTimeHint.text = binding.root.context.getString(R.string.max_hint_route_date, it.startParking, it.endParking, it.startDate.date.asString())
            }
            max.bySpeed?.let {
                maxSpeedHint.text = binding.root.context.getString(R.string.max_hint_speed, it.distance / 1000f, it.duration.asTimeString(binding.root.context), it.startDate.date.asString())
            }
            max.dayDistance?.let {
                val (date, distance) = it
                maxDistanceDay.text = binding.root.context.getString(R.string.ride_distance, distance / 1000f)
                maxDistanceDayHint.text = date.asString()
            }
            max.dayTime?.let {
                val (date, duration) = it
                maxTimeDay.text = duration.asTimeString(binding.root.context)
                maxTimeDayHint.text = date.asString()
            }
            max.dayRides?.let {
                val (date, rides) = it
                maxRidesDay.text = rides.toString()
                maxRidesDayHint.text = date.asString()
            }
        }
    }

    data class Average(
        val distance: Int,
        val time: Long,
        val speed: Float
    )

    data class Max(
        val byDistance: Ride?,
        val byTime: Ride?,
        val bySpeed: Ride?,
        val dayDistance: Pair<LocalDate, Int>?,
        val dayTime: Pair<LocalDate, Long>?,
        val dayRides: Pair<LocalDate, Int>?
    )

}