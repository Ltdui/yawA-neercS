package com.example.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.AwayTimeApp
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.navigation.NavDestination
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.stats.StatsScreen
import com.example.ui.stats.StatsViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScaffold(
    app: AwayTimeApp,
    modifier: Modifier = Modifier
) {
    val prefs by app.preferencesRepository.userPreferencesFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.preferences.UserPreferences()
    )
    val scope = rememberCoroutineScope()

    if (!prefs.hasCompletedOnboarding) {
        OnboardingScreen(
            onFinished = {
                scope.launch {
                    app.preferencesRepository.setOnboardingCompleted(true)
                }
            }
        )
        return
    }

    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.repository)
    )
    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.Factory(app.repository)
    )
    val statsViewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(app.repository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.preferencesRepository, app.repository)
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline)
                    .testTag("main_bottom_navigation")
            ) {
                NavDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                letterSpacing = 0.5.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag(destination.testTag)
                    )
                }
            }
        }
    ) { paddingValues ->
        Crossfade(
            targetState = currentDestination,
            label = "screenTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { destination ->
            when (destination) {
                NavDestination.HOME -> HomeScreen(viewModel = homeViewModel)
                NavDestination.HISTORY -> HistoryScreen(viewModel = historyViewModel)
                NavDestination.STATISTICS -> StatsScreen(viewModel = statsViewModel)
                NavDestination.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
