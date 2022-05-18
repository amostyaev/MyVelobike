package com.raistlin.myvelobike.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.raistlin.myvelobike.R
import com.raistlin.myvelobike.databinding.ItemChooserBinding
import com.raistlin.myvelobike.databinding.ItemRideBinding
import com.raistlin.myvelobike.dto.Ride
import com.raistlin.myvelobike.util.asString
import com.raistlin.myvelobike.util.asTimeString

class RidesAdapter(private val scroller: () -> Unit) : ListAdapter<Data, RecyclerView.ViewHolder>(RideDiffCallback()) {

    fun addRides(rides: List<Ride>) {
        chooseType(
            Data.Chooser(
                Select.Date,
                mapOf(Select.Date to rides.sortedByDescending { it.startDate },
                    Select.Speed to rides.filter { it.duration > 0 }.sortedByDescending { it.distance / it.duration.toFloat() },
                    Select.Distance to rides.sortedByDescending { it.distance },
                    Select.Time to rides.sortedByDescending { it.duration })
            )
        )
    }

    private fun chooseType(chooser: Data.Chooser) {
        submitList(listOf(chooser) + chooser[chooser.selected].map { Data.Ride(it) }) {
            scroller()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Data.Chooser -> ITEM_VIEW_TYPE_CHOOSER
            is Data.Ride -> ITEM_VIEW_TYPE_RIDE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_CHOOSER -> ChooserViewHolder(ItemChooserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            ITEM_VIEW_TYPE_RIDE -> RideViewHolder(ItemRideBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChooserViewHolder -> holder.bind(getItem(position) as Data.Chooser, ::chooseType)
            is RideViewHolder -> holder.bind(getItem(position) as Data.Ride)
        }
    }

    companion object {
        private const val ITEM_VIEW_TYPE_CHOOSER = 1
        private const val ITEM_VIEW_TYPE_RIDE = 2
    }

}

class ChooserViewHolder(private val binding: ItemChooserBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(data: Data.Chooser, chooser: (Data.Chooser) -> Unit) {
        binding.chooserToggle.check(
            when (data.selected) {
                Select.Date -> R.id.chooser_date
                Select.Speed -> R.id.chooser_speed
                Select.Distance -> R.id.chooser_distance
                Select.Time -> R.id.chooser_time
            }
        )
        binding.chooserToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val select = when (checkedId) {
                    R.id.chooser_speed -> Select.Speed
                    R.id.chooser_distance -> Select.Distance
                    R.id.chooser_time -> Select.Time
                    else -> Select.Date
                }
                chooser(data.copy(selected = select))
            }
        }
    }

}

class RideViewHolder(private val binding: ItemRideBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(ride: Data.Ride) {
        val title = "${ride.data.startDate.date.asString()} ${ride.data.startDate.asTimeString()} - ${ride.data.endDate.asTimeString()} (${ride.data.startParking} - ${ride.data.endParking})"
        binding.rideDate.text = title
        binding.rideElectric.isVisible = ride.data.isElectric()
        binding.rideDistance.text = binding.root.context.getString(R.string.ride_distance, ride.data.distance / 1000f)
        binding.rideBike.text = ride.data.bikeId.toString()
        binding.rideTime.text = ride.data.duration.asTimeString(binding.root.context)
        binding.rideSpeed.text = binding.root.context.getString(R.string.ride_speed, ride.data.let { it.distance / it.duration.toFloat() } * 3600 / 1000)
    }

}

enum class Select {
    Date,
    Speed,
    Distance,
    Time
}

sealed class Data {

    data class Chooser(
        val selected: Select,
        val data: Map<Select, List<com.raistlin.myvelobike.dto.Ride>>
    ) : Data() {

        operator fun get(selected: Select) = data.getValue(selected)

    }

    class Ride(val data: com.raistlin.myvelobike.dto.Ride) : Data()
}

class RideDiffCallback : DiffUtil.ItemCallback<Data>() {
    override fun areItemsTheSame(oldItem: Data, newItem: Data): Boolean {
        return if (oldItem is Data.Ride && newItem is Data.Ride) {
            oldItem.data.id == newItem.data.id
        } else {
            false
        }
    }

    override fun areContentsTheSame(oldItem: Data, newItem: Data): Boolean {
        return oldItem == newItem
    }
}