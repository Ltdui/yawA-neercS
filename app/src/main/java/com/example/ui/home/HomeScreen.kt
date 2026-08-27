package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MinimalBarChart
import com.example.ui.components.PositiveInsightCard
import com.example.ui.components.SessionCard
import com.example.ui.components.StatMetricCard
import com.example.ui.components.TimeHeroCounter
import com.example.util.TimeUtils

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDateKey by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Minimalist concentric ring brand icon
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.onBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground)
                        )
                    }

                    Text(
                        text = "AWAY TIME",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        letterSpacing = 2.4.sp
                    )
                }

                // Automatic Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.isAway) Color(0x2234C759) else Color(0x14FFFFFF))
                        .border(
                            1.dp,
                            if (state.isAway) Color(0xFF34C759).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("auto_tracking_status_badge"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (state.isAway) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    Text(
                        text = if (state.isAway) "AWAY ACTIVE" else "AUTO-TRACKING ON",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.isAway) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.0.sp
                    )
                }
            }
        }

        // Hero Away Time Counter
        item {
            TimeHeroCounter(
                isAway = state.isAway,
                activeSession = state.activeSession,
                liveDurationMillis = state.liveDurationMillis,
                lastBreakDurationMillis = state.lastBreakDurationMillis
            )
        }

        // Summary Metric Cards (Today, Longest Break)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatMetricCard(
                    label = "Today Total",
                    value = TimeUtils.formatDuration(state.todayTotalMillis),
                    modifier = Modifier.weight(1f),
                    testTag = "today_total_metric_card"
                )
                StatMetricCard(
                    label = "Longest Break",
                    value = TimeUtils.formatDuration(state.longestBreakMillis),
                    modifier = Modifier.weight(1f),
                    testTag = "longest_break_metric_card"
                )
            }
        }

        // Positive Insight
        if (state.insightText.isNotBlank()) {
            item {
                PositiveInsightCard(insightText = state.insightText)
            }
        }

        // 7-Day Trend Chart
        if (state.weeklyStats != null && state.weeklyStats!!.days.isNotEmpty()) {
            item {
                MinimalBarChart(
                    days = state.weeklyStats!!.days,
                    selectedDateKey = selectedDateKey,
                    onDaySelected = { selectedDateKey = it.dateKey }
                )
            }
        }

        // Recent Away Sessions Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT SESSIONS",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 2.0.sp
                )
                if (state.recentSessions.isNotEmpty()) {
                    Text(
                        text = "${state.recentSessions.size} logged",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        letterSpacing = 1.0.sp
                    )
                }
            }
        }

        // Recent Away Sessions List or Empty State
        if (state.recentSessions.isEmpty() && !state.isAway) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Away Time yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Put your phone down and your first break will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = state.recentSessions,
                key = { it.id }
            ) { session ->
                SessionCard(
                    session = session,
                    onDelete = { viewModel.deleteSession(session) }
                )
            }
        }
    }
}
