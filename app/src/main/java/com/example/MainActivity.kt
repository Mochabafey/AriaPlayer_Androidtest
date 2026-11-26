package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AriaApp
import com.example.ui.theme.AriaPlayerTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.DiscoverViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.SearchViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as AriaApplication).container
        val factory = AriaViewModelFactory(application as AriaApplication, container)

        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            val authViewModel: AuthViewModel = viewModel(factory = factory)
            val playerViewModel: PlayerViewModel = viewModel(factory = factory)
            val discoverViewModel: DiscoverViewModel = viewModel(factory = factory)
            val searchViewModel: SearchViewModel = viewModel(factory = factory)

            val authState by authViewModel.uiState.collectAsStateWithLifecycle()

            AriaPlayerTheme(themeMode = authState.settings.themeMode) {
                AriaApp(
                    widthSizeClass = widthSizeClass,
                    playerViewModel = playerViewModel,
                    discoverViewModel = discoverViewModel,
                    searchViewModel = searchViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}

private class AriaViewModelFactory(
    private val application: AriaApplication,
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> {
                PlayerViewModel(application, container.repository, container.playbackController) as T
            }
            modelClass.isAssignableFrom(DiscoverViewModel::class.java) -> {
                DiscoverViewModel(container.repository) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(container.repository) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(container.repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
