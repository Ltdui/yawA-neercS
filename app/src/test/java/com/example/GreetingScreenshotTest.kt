package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.components.TimeHeroCounter
import com.example.ui.theme.AwayTimeTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun awayTimeHero_screenshot() {
        composeTestRule.setContent {
            AwayTimeTheme(themeMode = "DARK") {
                TimeHeroCounter(
                    isAway = false,
                    activeSession = null,
                    liveDurationMillis = 0L,
                    lastBreakDurationMillis = 45 * 60 * 1000L
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/hero_away_time.png")
    }
}

