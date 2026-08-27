package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DaySummary
import com.example.util.TimeUtils

@Composable
fun MinimalBarChart(
    days: List<DaySummary>,
    selectedDateKey: String? = null,
    onDaySelected: (DaySummary) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return

    val maxDuration = days.maxOfOrNull { it.totalAwayMillis }?.coerceAtLeast(1L) ?: 1L

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(20.dp)
            .testTag("minimal_bar_chart")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WEEKLY TREND",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 2.0.sp
            )
            val selected = days.find { it.dateKey == selectedDateKey }
            if (selected != null) {
                Text(
                    text = "${selected.displayDate}: ${TimeUtils.formatDuration(selected.totalAwayMillis)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "LAST 7 DAYS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bars Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                val isSelected = day.dateKey == selectedDateKey
                val ratio = (day.totalAwayMillis.toFloat() / maxDuration.toFloat()).coerceIn(0.08f, 1.0f)

                val animatedHeight by animateFloatAsState(
                    targetValue = ratio,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    label = "barHeight"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onDaySelected(day) }
                        .padding(horizontal = 3.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Bar with rounded-t-sm
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedHeight)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    day.isToday -> MaterialTheme.colorScheme.primary
                                    day.totalAwayMillis > 0 -> Color(0x33FFFFFF) // white/20
                                    else -> Color(0x14FFFFFF) // white/8
                                }
                            )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day label
                    Text(
                        text = day.dayLabel.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (day.isToday || isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

