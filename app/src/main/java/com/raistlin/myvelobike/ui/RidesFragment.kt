package com.raistlin.myvelobike.ui

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.raistlin.myvelobike.R
import com.raistlin.myvelobike.adapter.RidesAdapter
import com.raistlin.myvelobike.databinding.FragmentRidesBinding
import com.raistlin.myvelobike.dto.Ride

class RidesFragment : Fragment() {

    private var records: RidesRecordsFragment? = null
    private var recordsShown = false
    private lateinit var rides: List<Ride>

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentRidesBinding.inflate(inflater, container, false)
        rides = arguments?.getSerializable(KEY_RIDES) as List<Ride>

        binding.list.adapter = RidesAdapter {
            binding.list.scrollToPosition(0)
        }.apply { addRides(rides) }
        binding.list.itemAnimator = null
        lifecycleScope.launchWhenStarted {
            records = binding.records.getFragment<RidesRecordsFragment>()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        records = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_rides, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_on_map -> {
                        findNavController().navigateUp()
                        findNavController().navigate(R.id.ridesPlacesFragment, bundleOf(RidesPlacesFragment.KEY_RIDES to arguments?.getSerializable(KEY_RIDES)))
                        true
                    }
                    R.id.action_statistics -> {
                        recordsShown = !recordsShown
                        if (recordsShown) {
                            records?.displayStats(rides)
                        } else {
                            records?.hideStats()
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    companion object {
        const val KEY_RIDES = "rides"
    }
}