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
import com.srs.weather.BuildConfig
import com.srs.weather.R
import com.srs.weather.data.repository.DataWidgetAwsRepository
import com.srs.weather.ui.view.Login
import com.srs.weather.ui.view.StationList
import com.srs.weather.ui.viewModel.DataWidgetAwsViewModel
import com.srs.weather.utils.AppUtils
import com.srs.weather.utils.PrefManager
import org.json.JSONObject


class WidgetProviderSecond : AppWidgetProvider(), AppUtils.DataWidgetResponse {

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
                WidgetProviderSecond::class.java
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

        if (intent?.action == AppUtils.ACTION_REFRESH_CLICK_SCD) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_second)
            this.views = views
            views.apply {
                setViewVisibility(R.id.progressBarSc, View.VISIBLE)
                setViewVisibility(R.id.refreshSc, View.GONE)
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

                AppUtils.checkDataWidgetAws2(
                    app,
                    PrefManager(context),
                    dataWidgetAwsViewModel,
                    this
                )
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    views.apply {
                        setViewVisibility(R.id.refreshSc, View.VISIBLE)
                        setViewVisibility(R.id.progressBarSc, View.GONE)
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
                WidgetProviderSecond::class.java
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
        val views = RemoteViews(context.packageName, R.layout.widget_layout_second)
        this.views = views

        try {
            dataWidgetAwsViewModel.dataWidgetAwsList.observeForever { data ->
                for (dataWl in data) {
                    if (dataWl.widget == 2) {
                        val jsonObject = JSONObject(dataWl.response_data)

                        val lastDate = jsonObject.getString("lastDate")
                        views.setTextViewText(R.id.dateSc, lastDate)

                        val station1 = jsonObject.getJSONObject("station1")
                        updateStationViews(context, views, 1, station1, prefManager.locStation1!!)

                        val station2 = jsonObject.getJSONObject("station2")
                        updateStationViews(context, views, 2, station2, prefManager.locStation2!!)

                        val station3 = jsonObject.getJSONObject("station3")
                        updateStationViews(context, views, 3, station3, prefManager.locStation3!!)

                        val station4 = jsonObject.getJSONObject("station4")
                        updateStationViews(context, views, 4, station4, prefManager.locStation4!!)

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
        views.setOnClickPendingIntent(R.id.refreshSc, refreshPendingIntent)

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
        views.setOnClickPendingIntent(R.id.srsLogoSc, stationPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onDataUpdatedSuccessfully() {
        val context = this.context
        val dataWidgetAwsViewModel = this.dataWidgetAwsViewModel

        if (context != null && dataWidgetAwsViewModel != null) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_second)

            views.apply {
                setViewVisibility(R.id.refreshSc, View.VISIBLE)
                setViewVisibility(R.id.progressBarSc, View.GONE)
            }

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    WidgetProviderSecond::class.java
                )
            )
            appWidgetIds.forEach { appWidgetId ->
                updateWidgetUI(context, dataWidgetAwsViewModel)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            Log.d(AppUtils.LOG_WIDGET, "Success updated2!")
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
        val intent = Intent(context, WidgetProviderSecond::class.java)
        intent.action = AppUtils.ACTION_REFRESH_CLICK_SCD
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
            val prefManager = PrefManager(context)
            updateStationViews(context, views, 1, null, prefManager.locStation1!!)
            updateStationViews(context, views, 2, null, prefManager.locStation2!!)
            updateStationViews(context, views, 3, null, prefManager.locStation3!!)
            updateStationViews(context, views, 4, null, prefManager.locStation4!!)
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

    private fun updateStationViews(
        context: Context,
        views: RemoteViews,
        stationIndex: Int,
        stationData: JSONObject?,
        locStation: String,
    ) {
        val defaultText = "-"
        setTextViews(views, context, "locStation$stationIndex", locStation)

        if (stationData != null) {
            val uv = stationData.getString("uv")
            val ws = stationData.getString("ws")
            val rainRate = stationData.getString("rain_rate")
            val humidity = stationData.getString("hum")
            val temperature = stationData.getString("temp")
            val rrMonth = try {
                stationData.getInt("rrMonth")
            } catch (e: Exception) {
                0
            }
            val resultRr = if (rrMonth in 60..300) "✓" else "✕"

            setTextViews(views, context, "uvStation$stationIndex", uv)
            setTextViews(views, context, "tempStation$stationIndex", "$temperature°")
            setTextViews(views, context, "rainRateSecond$stationIndex", rainRate)
            setTextViews(views, context, "humSecond$stationIndex", humidity)
            setTextViews(views, context, "windSpeedSecond$stationIndex", ws)
            setTextViews(views, context, "rrMonthSecond$stationIndex", resultRr)
        } else {
            setTextViews(views, context, "uvStation$stationIndex", defaultText)
            setTextViews(views, context, "tempStation$stationIndex", defaultText)
            setTextViews(views, context, "rainRateSecond$stationIndex", defaultText)
            setTextViews(views, context, "humSecond$stationIndex", defaultText)
            setTextViews(views, context, "windSpeedSecond$stationIndex", defaultText)
            setTextViews(views, context, "rrMonthSecond$stationIndex", defaultText)
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun setTextViews(
        views: RemoteViews,
        context: Context,
        viewId: String,
        value: String
    ) {
        views.setTextViewText(
            context.resources.getIdentifier(
                viewId,
                "id",
                BuildConfig.APPLICATION_ID
            ),
            value
        )
    }
}