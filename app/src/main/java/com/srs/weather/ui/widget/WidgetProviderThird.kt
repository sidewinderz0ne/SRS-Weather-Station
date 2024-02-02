package com.srs.weather.ui.widget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.srs.weather.R
import com.srs.weather.data.repository.DataWidgetAwsRepository
import com.srs.weather.ui.view.Login
import com.srs.weather.ui.view.StationList
import com.srs.weather.ui.viewModel.DataWidgetAwsViewModel
import com.srs.weather.utils.AppUtils
import com.srs.weather.utils.PrefManager
import org.json.JSONObject

class WidgetProviderThird : AppWidgetProvider(), AppUtils.DataWidgetResponse {

    private var views: RemoteViews? = null
    private var context: Context? = null
    private var widgetAwsViewModel: DataWidgetAwsViewModel? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        this.context = context

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context!!,
                WidgetProviderThird::class.java
            )
        )

        if (AppUtils.checkConnectionDevice(context)) {
            if (AppUtils.getCountDataStationWs(context) <= 0) {
                AppUtils.checkDataStationWs(context, "first")
            } else {
                appWidgetIds.forEach { appWidgetId ->
                    createUpdatePendingIntent(context, appWidgetId, "update")
                }
            }
        }

        val views = RemoteViews(context.packageName, R.layout.widget_layout_third)
        this.views = views

        val app = context.applicationContext as Application
        val viewModelFactory =
            DataWidgetAwsViewModel.Factory(app, DataWidgetAwsRepository(context))
        val dataWidgetAwsViewModel = ViewModelProvider(
            ViewModelStore(),
            viewModelFactory
        )[DataWidgetAwsViewModel::class.java]
        this.widgetAwsViewModel = dataWidgetAwsViewModel

        if (intent?.action == AppUtils.ACTION_REFRESH_CLICK_THR) {
            views.apply {
                setViewVisibility(R.id.pbRefreshWidget, View.VISIBLE)
                setViewVisibility(R.id.ivRefreshWidget, View.GONE)
            }
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            if (AppUtils.checkConnectionDevice(context)) {
                AppUtils.checkDataWidgetAws3(
                    app,
                    PrefManager(context),
                    dataWidgetAwsViewModel,
                    this
                )
                AppUtils.updateViewsWidget(
                    views,
                    appWidgetManager,
                    appWidgetIds,
                    R.id.pbRefreshWidget,
                    R.id.ivRefreshWidget,
                    5000
                )
            } else {
                AppUtils.updateViewsWidget(
                    views,
                    appWidgetManager,
                    appWidgetIds,
                    R.id.pbRefreshWidget,
                    R.id.ivRefreshWidget,
                    1000
                )
            }
        } else if (intent?.action == AppUtils.ACTION_UPDATE_INTERVAL_THR) {
            if (AppUtils.checkConnectionDevice(context)) {
                AppUtils.checkDataWidgetAws3(
                    app,
                    PrefManager(context),
                    dataWidgetAwsViewModel,
                    this,
                    "update"
                )
                AppUtils.updateViewsWidget(
                    views,
                    appWidgetManager,
                    appWidgetIds,
                    R.id.pbRefreshWidget,
                    R.id.ivRefreshWidget,
                    5000
                )
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as Application
        val viewModelFactory = DataWidgetAwsViewModel.Factory(app, DataWidgetAwsRepository(context))
        val widgetAwsViewModel = ViewModelProvider(
            ViewModelStore(),
            viewModelFactory
        )[DataWidgetAwsViewModel::class.java]

        appWidgetIds.forEach { appWidgetId ->
            schedulePeriodicUpdate(context, appWidgetId)
        }
        updateWidgetUI(context, widgetAwsViewModel)
    }

    private fun updateWidgetUI(context: Context?, widgetAwsViewModel: DataWidgetAwsViewModel) {
        context?.applicationContext as Application
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context,
                WidgetProviderThird::class.java
            )
        )

        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId, widgetAwsViewModel)
        }
    }

    @SuppressLint("RemoteViewLayout")
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        widgetAwsViewModel: DataWidgetAwsViewModel
    ) {
        val prefManager = PrefManager(context)
        val views = RemoteViews(context.packageName, R.layout.widget_layout_third)
        this.views = views

        try {
            widgetAwsViewModel.dataWidgetAwsList.observeForever { data ->
                views.removeAllViews(R.id.llHeaderThird)
                views.removeAllViews(R.id.llRow1Third)
                views.removeAllViews(R.id.llRow2Third)
                views.removeAllViews(R.id.llRow3Third)
                views.removeAllViews(R.id.llRow4Third)

                for (dataWl in data) {
                    if (dataWl.widget == 3) {
                        val jObj = JSONObject(dataWl.response_data)
                        val lastSixHours = jObj.getJSONObject("lastSixHours")

                        val timeKeys = lastSixHours.keys()
                        while (timeKeys.hasNext()) {
                            val timeKey = timeKeys.next()
                            val timeObject = lastSixHours.getJSONObject(timeKey)
                            val timeTemp = timeObject.getString("temp")
                            val timeRr = timeObject.getString("rainrate")
                            val timeHum = timeObject.getString("hum")
                            val timeWs = timeObject.getString("ws")

                            val textViewLayout =
                                RemoteViews(context.packageName, R.layout.text_widget_layout)
                            val spannableString = SpannableString(timeKey)
                            spannableString.setSpan(
                                StyleSpan(Typeface.BOLD),
                                0,
                                spannableString.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            textViewLayout.setTextViewText(R.id.row_text_view, spannableString)
                            views.addView(R.id.llHeaderThird, textViewLayout)

                            addTextViewToLayout(
                                context,
                                views,
                                timeTemp,
                                R.id.llRow1Third
                            )
                            addTextViewToLayout(
                                context,
                                views,
                                timeRr,
                                R.id.llRow2Third
                            )
                            addTextViewToLayout(
                                context,
                                views,
                                timeHum,
                                R.id.llRow3Third
                            )
                            addTextViewToLayout(
                                context,
                                views,
                                timeWs,
                                R.id.llRow4Third
                            )
                        }

                        val lastData = jObj.getJSONObject("lastData")
                        val lastTemp = lastData.getString("temp")
                        val lastRr = lastData.getString("rainrate")
                        val lastHum = lastData.getString("hum")
                        val lastWs = lastData.getString("ws")
                        val lastDate = lastData.getString("date")

                        views.setTextViewText(R.id.tvLocationWidget, prefManager.locStation)
                        views.setTextViewText(R.id.tvTempWidget, lastTemp)
                        views.setTextViewText(R.id.tvRrWidget, lastRr)
                        views.setTextViewText(R.id.tvHumWidget, lastHum)
                        views.setTextViewText(R.id.tvWsWidget, lastWs)
                        views.setTextViewText(
                            R.id.tvLastWidget,
                            AppUtils.formatDate(lastDate)
                        )

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        break
                    }
                }
            }
            widgetAwsViewModel.loadDataWidgetAws()
        } catch (e: Exception) {
            defaultErrorData(views)
        }

        val refreshPendingIntent = createUpdatePendingIntent(context, appWidgetId)
        views.setOnClickPendingIntent(R.id.ivRefreshWidget, refreshPendingIntent)

        val stationIntent: Intent
        if (prefManager.session) {
            stationIntent = Intent(context, StationList::class.java)
        } else {
            prefManager.isFromWidget = true
            stationIntent = Intent(context, Login::class.java)
        }
        stationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val stationPendingIntent = PendingIntent.getActivity(
            context,
            0,
            stationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.ivLocationWidget, stationPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onDataUpdatedSuccessfully(arg: String?) {
        val context = this.context
        val widgetAwsViewModel = this.widgetAwsViewModel

        if (context != null && widgetAwsViewModel != null) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_third)

            views.removeAllViews(R.id.llHeaderThird)
            views.removeAllViews(R.id.llRow1Third)
            views.removeAllViews(R.id.llRow2Third)
            views.removeAllViews(R.id.llRow3Third)
            views.removeAllViews(R.id.llRow4Third)

            if (arg!!.isEmpty()) {
                views.apply {
                    setViewVisibility(R.id.ivRefreshWidget, View.VISIBLE)
                    setViewVisibility(R.id.pbRefreshWidget, View.GONE)
                }
            }

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    WidgetProviderThird::class.java
                )
            )
            appWidgetIds.forEach { appWidgetId ->
                updateWidgetUI(context, widgetAwsViewModel)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            Log.d(AppUtils.LOG_WIDGET, "Success updated3!")
        } else {
            Log.e(AppUtils.LOG_WIDGET, "Context or widgetAwsViewModel is null3")
        }
    }

    override fun onDataUpdateFailed() {
        val views = this.views
        if (views != null) {
            defaultErrorData(views)
        }
    }

    private fun createUpdatePendingIntent(context: Context, appWidgetId: Int, arg: String? = ""): PendingIntent {
        val intent = Intent(context, WidgetProviderThird::class.java)
        intent.action = if (arg!!.isEmpty()) AppUtils.ACTION_REFRESH_CLICK_THR else AppUtils.ACTION_UPDATE_INTERVAL_THR
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun defaultErrorData(views: RemoteViews) {
        val context = this.context
        if (context != null) {
            views.removeAllViews(R.id.llHeaderThird)
            views.removeAllViews(R.id.llRow1Third)
            views.removeAllViews(R.id.llRow2Third)
            views.removeAllViews(R.id.llRow3Third)
            views.removeAllViews(R.id.llRow4Third)

            views.apply {
                setViewVisibility(R.id.ivRefreshWidget, View.VISIBLE)
                setViewVisibility(R.id.pbRefreshWidget, View.GONE)
            }

            for (i in 0 until 7) {
                val textViewLayout =
                    RemoteViews(context.packageName, R.layout.text_widget_layout)
                textViewLayout.setTextViewText(R.id.row_text_view, "-")
                views.addView(R.id.llHeaderThird, textViewLayout)

                addTextViewToLayout(context, views, "-", R.id.llRow1Third)
                addTextViewToLayout(context, views, "-", R.id.llRow2Third)
                addTextViewToLayout(context, views, "-", R.id.llRow3Third)
                addTextViewToLayout(context, views, "-", R.id.llRow4Third)
            }

            views.setTextViewText(R.id.tvLocationWidget, "No data found")
            views.setTextViewText(R.id.tvTempWidget, "-")
            views.setTextViewText(R.id.tvRrWidget, "-")
            views.setTextViewText(R.id.tvHumWidget, "-")
            views.setTextViewText(R.id.tvWsWidget, "-")
            views.setTextViewText(R.id.tvLastWidget, "-")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    WidgetProviderThird::class.java
                )
            )
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun schedulePeriodicUpdate(context: Context, appWidgetId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createUpdatePendingIntent(context, appWidgetId, "update")
        val updateIntervalMillis = try {
            AppWidgetManager.getInstance(context)
                .getAppWidgetInfo(appWidgetId)
                .updatePeriodMillis
        } catch (e: Exception) {
            0
        }
        if (updateIntervalMillis > 0) {
            alarmManager.setRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + updateIntervalMillis,
                updateIntervalMillis.toLong(),
                pendingIntent
            )
        }
    }

    private fun addTextViewToLayout(context: Context, views: RemoteViews, text: String, viewId: Int) {
        val textViewLayout = RemoteViews(context.packageName, R.layout.text_widget_layout)
        textViewLayout.setTextViewText(R.id.row_text_view, text)
        views.addView(viewId, textViewLayout)
    }
}