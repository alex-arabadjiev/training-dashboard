package com.example.trainingdashboard.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {

    /**
     * Triggers a data refresh for all TrainingWidget instances on the home screen.
     * Safe to call from any thread; launches on IO dispatcher internally.
     */
    fun update(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(TrainingWidget::class.java)
            glanceIds.forEach { id ->
                TrainingWidget().update(context, id)
            }
        }
    }
}
