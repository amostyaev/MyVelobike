package com.raistlin.myvelobike.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.raistlin.myvelobike.R
import com.raistlin.myvelobike.databinding.ActivityMainBinding
import com.raistlin.myvelobike.store.getLoginData
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(R.id.historyFragment, R.id.realtimeFragment, R.id.placesFragment),
            fallbackOnNavigateUpListener = ::onSupportNavigateUp
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val subMode = destination.id == R.id.ridesFragment || destination.id == R.id.ridesPlacesFragment || destination.id == R.id.authFragment
            binding.bottomNavigation.isVisible = !subMode
        }
        binding.mainToolbar.setupWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        lifecycleScope.launch {
            if (getLoginData().value.login.isEmpty()) {
                navController.navigate(R.id.authFragment)
            }
        }
    }
}