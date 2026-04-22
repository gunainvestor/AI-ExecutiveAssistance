package com.execos.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onOpenDateDetails: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        when {
            state.loading -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            }
            state.error != null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                ) { Text(state.error ?: "") }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    MonthHeader(
                        label = state.monthLabel,
                        onPrev = viewModel::prevMonth,
                        onNext = viewModel::nextMonth,
                    )
                    CalendarGrid(
                        yearMonth = state.yearMonth,
                        selectedDateIso = state.selectedDateIso,
                        dayTaskCount = state.dayTaskCount,
                        dayDecisionCount = state.dayDecisionCount,
                        dayReflectionCount = state.dayReflectionCount,
                        onSelectDateIso = { iso ->
                            viewModel.selectDate(iso)
                            onOpenDateDetails(iso)
                        },
                    )
                    SelectedDaySummary(
                        selectedDateIso = state.selectedDateIso,
                        tasks = state.dayTaskCount[state.selectedDateIso] ?: 0,
                        decisions = state.dayDecisionCount[state.selectedDateIso] ?: 0,
                        reflections = state.dayReflectionCount[state.selectedDateIso] ?: 0,
                    )
                    DailyGoalsCard(
                        selectedDateIso = state.selectedDateIso,
                        plannedGoals = state.selectedDayPlannedGoals,
                        achievedGoals = state.selectedDayAchievedGoals,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month") }
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, contentDescription = "Next month") }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDateIso: String,
    dayTaskCount: Map<String, Int>,
    dayDecisionCount: Map<String, Int>,
    dayReflectionCount: Map<String, Int>,
    onSelectDateIso: (String) -> Unit,
) {
    val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val first = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startDow = first.dayOfWeek
    val offset = ((startDow.value - DayOfWeek.MONDAY.value) + 7) % 7
    val totalCells = ((offset + daysInMonth + 6) / 7) * 7

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        val spacing = 8.dp
        val cellWidth = (maxWidth - spacing * 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                    Box(modifier = Modifier.width(cellWidth), contentAlignment = Alignment.Center) {
                        Text(
                            d,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            for (row in 0 until (totalCells / 7)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    for (col in 0 until 7) {
                        val cell = row * 7 + col
                        val day = cell - offset + 1
                        val inMonth = day in 1..daysInMonth
                        val iso = if (inMonth) yearMonth.atDay(day).toString() else ""
                        DayCell(
                            modifier = Modifier.width(cellWidth),
                            dayNumber = if (inMonth) day else null,
                            selected = iso == selectedDateIso,
                            isToday = iso == todayIso,
                            dots = if (inMonth) {
                                val t = dayTaskCount[iso] ?: 0
                                val d = dayDecisionCount[iso] ?: 0
                                val r = dayReflectionCount[iso] ?: 0
                                DotPack(tasks = t, decisions = d, reflections = r)
                            } else DotPack(0, 0, 0),
                            onClick = { if (inMonth) onSelectDateIso(iso) },
                        )
                    }
                }
            }
        }
    }
}

private data class DotPack(val tasks: Int, val decisions: Int, val reflections: Int)

@Composable
private fun DayCell(
    modifier: Modifier = Modifier,
    dayNumber: Int?,
    selected: Boolean,
    isToday: Boolean,
    dots: DotPack,
    onClick: () -> Unit,
) {
    val base = modifier
        .padding(vertical = 6.dp)

    if (dayNumber == null) {
        Box(modifier = base.height(46.dp))
        return
    }

    val bg = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = base
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(dayNumber.toString(), style = MaterialTheme.typography.labelLarge, color = fg)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (dots.tasks > 0) Dot(MaterialTheme.colorScheme.primary)
            if (dots.decisions > 0) Dot(MaterialTheme.colorScheme.tertiary)
            if (dots.reflections > 0) Dot(MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun SelectedDaySummary(
    selectedDateIso: String,
    tasks: Int,
    decisions: Int,
    reflections: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(selectedDateIso, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("✅", tasks, "Tasks")
                MiniStat("🧠", decisions, "Decisions")
                MiniStat("📝", reflections, "Reflections")
            }
            Text(
                "Dots: tasks (primary), decisions (tertiary), reflections (secondary).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MiniStat(emoji: String, value: Int, label: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("$emoji  $value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DailyGoalsCard(
    selectedDateIso: String,
    plannedGoals: List<String>,
    achievedGoals: List<String>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Daily goals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                selectedDateIso,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))

            GoalsSection(
                title = "Planned",
                goals = plannedGoals,
                emptyText = "No planned goals for this day.",
            )
            GoalsSection(
                title = "Achieved",
                goals = achievedGoals,
                emptyText = "No achieved goals for this day yet.",
            )
        }
    }
}

@Composable
private fun GoalsSection(
    title: String,
    goals: List<String>,
    emptyText: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        if (goals.isEmpty()) {
            Text(
                emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.forEachIndexed { idx, goalTitle ->
                    GoalRow(rank = idx + 1, title = goalTitle)
                }
            }
        }
    }
}

@Composable
private fun GoalRow(rank: Int, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "$rank.",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

