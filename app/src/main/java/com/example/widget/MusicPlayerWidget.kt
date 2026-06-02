package com.example.widget

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import com.example.MainActivity

class MusicPlayerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.powerplayer.ACTION_WIDGET_PLAY" ||
            intent.action == "com.example.powerplayer.ACTION_WIDGET_PREV" ||
            intent.action == "com.example.powerplayer.ACTION_WIDGET_NEXT") {
            
            // To simulate click action in a visual-only sandbox and real system update:
            val updateIntent = Intent(context, MusicPlayerWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, MusicPlayerWidget::class.java)
            )
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(updateIntent)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, com.example.R.layout.widget_layout)

            // Dynamic Pending Intents to interact with the main activity or widget receiver
            val playIntent = Intent(context, MusicPlayerWidget::class.java).apply {
                action = "com.example.powerplayer.ACTION_WIDGET_PLAY"
            }
            val prevIntent = Intent(context, MusicPlayerWidget::class.java).apply {
                action = "com.example.powerplayer.ACTION_WIDGET_PREV"
            }
            val nextIntent = Intent(context, MusicPlayerWidget::class.java).apply {
                action = "com.example.powerplayer.ACTION_WIDGET_NEXT"
            }

            val pPlay = PendingIntent.getBroadcast(context, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)
            val pPrev = PendingIntent.getBroadcast(context, 1, prevIntent, PendingIntent.FLAG_IMMUTABLE)
            val pNext = PendingIntent.getBroadcast(context, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE)

            views.setOnClickPendingIntent(com.example.R.id.btn_play, pPlay)
            views.setOnClickPendingIntent(com.example.R.id.btn_prev, pPrev)
            views.setOnClickPendingIntent(com.example.R.id.btn_next, pNext)

            // Clicking the title opens the Player UI
            val appIntent = Intent(context, MainActivity::class.java)
            val pApp = PendingIntent.getActivity(context, 3, appIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(com.example.R.id.widget_album_art, pApp)
            views.setOnClickPendingIntent(com.example.R.id.track_title, pApp)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
