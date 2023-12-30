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
import android.os.Handler
import android.os.Looper
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

class WeatherWidgetProvider : AppWidgetProvider(), AppUtils.DataWidgetResponse {

    private var views: RemoteViews? = null
    private var context: Context? = null
    private var dataWidgetAwsViewModel: DataWidgetAwsViewModel? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        this.context = context

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context!!,
                WeatherWidgetProvider::class.java
            )
        )

        if (AppUtils.checkConnectionDevice(context)) {
            if (AppUtils.getCountDataStationWs(context) <= 0) {
                AppUtils.checkDataStationWs(context, "first")
            } else {
                appWidgetIds.forEach { appWidgetId ->
                    createUpdatePendingIntent(context, appWidgetId)
                }
            }
        }

        if (intent?.action == AppUtils.ACTION_REFRESH_CLICK) {
            val views = RemoteViews(context.packageName, R.layout.weather_widget_layout)
            this.views = views
            views.apply {
                setViewVisibility(R.id.progressBar, View.VISIBLE)
                setViewVisibility(R.id.refresh, View.GONE)
            }
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            if (AppUtils.checkConnectionDevice(context)) {
                val app = context.applicationContext as Application
                val viewModelFactory =
                    DataWidgetAwsViewModel.Factory(app, DataWidgetAwsRepository(context))
                val dataWidgetAwsViewModel = ViewModelProvider(
                    ViewModelStore(),
                    viewModelFactory
                )[DataWidgetAwsViewModel::class.java]
                this.dataWidgetAwsViewModel = dataWidgetAwsViewModel

                AppUtils.checkDataWidgetAws1(
                    app,
                    PrefManager(context),
                    dataWidgetAwsViewModel,
                    this
                )
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    views.apply {
                        setViewVisibility(R.id.refresh, View.VISIBLE)
                        setViewVisibility(R.id.progressBar, View.GONE)
                    }
                    appWidgetIds.forEach { appWidgetId ->
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }, 1000)
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
        val dataWidgetAwsViewModel = ViewModelProvider(
            ViewModelStore(),
            viewModelFactory
        )[DataWidgetAwsViewModel::class.java]

        appWidgetIds.forEach { appWidgetId ->
            schedulePeriodicUpdate(context, appWidgetId)
        }
        updateWidgetUI(context, dataWidgetAwsViewModel)
    }

    private fun updateWidgetUI(context: Context?, dataWidgetAwsViewModel: DataWidgetAwsViewModel) {
        context?.applicationContext as Application
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context,
                WeatherWidgetProvider::class.java
            )
        )

        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId, dataWidgetAwsViewModel)
        }
    }

    @SuppressLint("RemoteViewLayout")
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        dataWidgetAwsViewModel: DataWidgetAwsViewModel
    ) {
        val prefManager = PrefManager(context)
        val views = RemoteViews(context.packageName, R.layout.weather_widget_layout)
        this.views = views

        try {
            dataWidgetAwsViewModel.dataWidgetAwsList.observeForever { data ->
                for (dataWl in data) {
                    if (dataWl.widget == 1) {
                        val jsonObject = JSONObject(dataWl.response_data)
                        val date = jsonObject.getString("date")
                        val ws = jsonObject.getString("ws")
                        val rainRate = jsonObject.getString("rain_rate")
                        val humidity = jsonObject.getString("hum")
                        val temperature = jsonObject.getString("temp")
                        val rrMonth = try {
                            jsonObject.getInt("rrMonth")
                        } catch (e: Exception) {
                            0
                        }
                        val resultRr = if (rrMonth in 60..300) {
                            "Disarankan"
                        } else {
                            "Tidak Disarankan"
                        }

                        views.setTextViewText(R.id.weatherTemperature, temperature)
                        views.setTextViewText(R.id.rainfallRate, rainRate)
                        views.setTextViewText(R.id.humidity, humidity)
                        views.setTextViewText(R.id.windSpeed, ws)
                        views.setTextViewText(R.id.date, date)
                        views.setTextViewText(R.id.station, "Station: " + prefManager.locStation!!)
                        views.setTextViewText(R.id.recom, "Pemupukan: $resultRr")

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        break
                    }
                }
            }
            dataWidgetAwsViewModel.loadDataWidgetAws()
        } catch (e: Exception) {
            defaultErrorData(views)
        }

        val refreshPendingIntent = createUpdatePendingIntent(context, appWidgetId)
        views.setOnClickPendingIntent(R.id.refresh, refreshPendingIntent)

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
        views.setOnClickPendingIntent(R.id.srsLogo, stationPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onDataUpdatedSuccessfully() {
        val context = this.context
        val dataWidgetAwsViewModel = this.dataWidgetAwsViewModel

        if (context != null && dataWidgetAwsViewModel != null) {
            val views = RemoteViews(context.packageName, R.layout.weather_widget_layout)

            views.apply {
                setViewVisibility(R.id.refresh, View.VISIBLE)
                setViewVisibility(R.id.progressBar, View.GONE)
            }

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    WeatherWidgetProvider::class.java
                )
            )
            appWidgetIds.forEach { appWidgetId ->
                updateWidgetUI(context, dataWidgetAwsViewModel)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            Log.d(AppUtils.LOG_WIDGET, "Success updated1!")
        } else {
            Log.e(AppUtils.LOG_WIDGET, "Context or dataWidgetAwsViewModel is null")
        }
    }

    override fun onDataUpdateFailed() {
        val views = this.views
        if (views != null) {
            defaultErrorData(views)
        }
    }

    private fun createUpdatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, WeatherWidgetProvider::class.java)
        intent.action = AppUtils.ACTION_REFRESH_CLICK
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
            views.apply {
                setViewVisibility(R.id.refresh, View.VISIBLE)
                setViewVisibility(R.id.progressBar, View.GONE)
            }

            views.setTextViewText(R.id.weatherTemperature, "-")
            views.setTextViewText(R.id.rainfallRate, "-")
            views.setTextViewText(R.id.humidity, "-")
            views.setTextViewText(R.id.windSpeed, "-")
            views.setTextViewText(R.id.date, "-")
            views.setTextViewText(R.id.station, "Station: -")
            views.setTextViewText(R.id.recom, "Pemupukan: -")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    WeatherWidgetProvider::class.java
                )
            )
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun schedulePeriodicUpdate(context: Context, appWidgetId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createUpdatePendingIntent(context, appWidgetId)
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
}