package com.execos.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.execos.MainActivity
import com.execos.R
import com.execos.data.repo.GoalRepository
import com.execos.data.repo.SessionStore
import com.execos.data.repo.TaskRepository
import com.execos.data.model.GoalPeriod
import com.execos.util.Dates
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

class DailyGoalsWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = buildRemoteViews(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId,
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val views = buildRemoteViews(context, appWidgetManager, appWidgetId)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        refreshAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE || intent.action == Intent.ACTION_DATE_CHANGED) {
            refreshAll(context)
        }
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ): RemoteViews {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val isCompact = minWidth in 1..220
        val layoutId = if (isCompact) R.layout.widget_daily_goals_compact else R.layout.widget_daily_goals
        val views = RemoteViews(context.packageName, layoutId)
        val widgetData = loadWidgetData(context)
        val goals = widgetData.items
        val score = widgetData.score
        DailyGoalLockscreenNotifier.showOrCancel(context, widgetData.items.firstOrNull())
        val badge = when {
            score >= 80 -> "On Fire"
            score >= 60 -> "In Control"
            score >= 40 -> "At Risk"
            else -> "Reset Needed"
        }
        val scoreColor = when {
            score >= 80 -> R.color.widget_score_good
            score >= 60 -> R.color.widget_score_ok
            else -> R.color.widget_score_risk
        }

        views.setTextViewText(
            R.id.widgetSubtitle,
            if (goals.isEmpty()) "Set your daily plan in ExecOS." else "Stay locked on what matters today.",
        )
        views.setTextViewText(R.id.widgetScore, "$score")
        views.setTextViewText(R.id.widgetBadge, badge)
        views.setInt(R.id.widgetScore, "setTextColor", context.getColor(scoreColor))
        views.setTextViewText(R.id.widgetGoal1, goals.getOrNull(0)?.let { "1. $it" } ?: "1. Define your first high-impact goal")
        if (!isCompact) {
            views.setTextViewText(R.id.widgetGoal2, goals.getOrNull(1)?.let { "2. $it" } ?: "2. Protect your deep-work block")
            views.setTextViewText(R.id.widgetGoal3, goals.getOrNull(2)?.let { "3. $it" } ?: "3. Evaluate your day tonight")
        }

        val launchIntent = Intent(context, MainActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, flags)
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

        return views
    }

    private fun loadWidgetData(context: Context): WidgetData = runBlocking {
        val entryPoint = EntryPointAccessors.fromApplication(context, DailyGoalsWidgetEntryPoint::class.java)
        val uid = entryPoint.sessionStore().uidFlow.first().orEmpty().ifBlank { "local" }

        val todayTasks = entryPoint.taskRepository()
            .observeTasksForDate(uid, Dates.todayIso())
            .first()
            .asSequence()
            .map { it.copy(title = it.title.trim()) }
            .filter { it.title.isNotBlank() }
            .toList()

        if (todayTasks.isNotEmpty()) {
            val completed = todayTasks.count { it.completed }
            val total = todayTasks.size
            val completionRate = completed.toFloat() / total.toFloat()
            // Match Home screen execution score model for consistency.
            val progressScore = ((completionRate * 70f) + 30f).roundToInt().coerceIn(0, 100)
            val topItems = todayTasks
                .sortedWith(compareBy({ it.completed }, { it.title.lowercase() }))
                .map { it.title }
                .take(3)
            return@runBlocking WidgetData(items = topItems, score = progressScore)
        }

        val weekGoals = entryPoint.goalRepository()
            .observeGoals(uid, GoalPeriod.WEEK, Dates.weekStartIso())
            .first()
            .map { it.title }
            .filter { it.isNotBlank() }

        if (weekGoals.isNotEmpty()) {
            return@runBlocking WidgetData(
                items = weekGoals.take(3),
                score = scoreFromPlannedCount(weekGoals.size),
            )
        }

        val monthGoals = entryPoint.goalRepository()
                .observeGoals(uid, GoalPeriod.MONTH, Dates.monthKey())
                .first()
                .map { it.title }
                .filter { it.isNotBlank() }

        if (monthGoals.isNotEmpty()) {
            return@runBlocking WidgetData(
                items = monthGoals.take(3),
                score = scoreFromPlannedCount(monthGoals.size),
            )
        }

        WidgetData(items = emptyList(), score = 0)
    }

    companion object {
        fun refreshAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, DailyGoalsWidgetReceiver::class.java)
            val ids = appWidgetManager.getAppWidgetIds(provider)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, DailyGoalsWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    private fun scoreFromPlannedCount(count: Int): Int {
        val capped = count.coerceIn(0, 3)
        return when (capped) {
            0 -> 0
            1 -> 34
            2 -> 67
            else -> 100
        }
    }
    private data class WidgetData(
        val items: List<String>,
        val score: Int,
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyGoalsWidgetEntryPoint {
    fun sessionStore(): SessionStore
    fun goalRepository(): GoalRepository
    fun taskRepository(): TaskRepository
}
