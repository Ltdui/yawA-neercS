package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.main.MainScaffold
import com.example.ui.theme.AwayTimeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AwayTimeApp
        com.example.widget.AwayTimeWidgetUpdater.updateAllWidgets(this)

        setContent {
            val prefs by app.preferencesRepository.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.preferences.UserPreferences()
            )

            AwayTimeTheme(themeMode = prefs.themeMode) {
                MainScaffold(
                    app = app,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.widget.AwayTimeWidgetUpdater.updateAllWidgets(this)
    }
}

