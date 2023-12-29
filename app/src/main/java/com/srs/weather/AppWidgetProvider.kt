@file:Suppress("DEPRECATION")

package com.srs.weather

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


@Suppress("DEPRECATION")
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)

            if (AppUtils.checkConnectionDevice(context)) {
                if (AppUtils.getCountDataStationWs(context) <= 0) {
                    AppUtils.checkDataStationWs(context, "first")
                }

                schedulePeriodicUpdate(context, appWidgetId)
                Handler().postDelayed({
                    FetchWeatherDataTask(context, appWidgetId).execute()
                }, 2000)
            } else {
                val prefManager = PrefManager(context)
                val storedData = prefManager.mainDataArray
                if (storedData != null) {
                    updateWidgetViewWithStoredData(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppUtils.ACTION_UPDATE) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                if (AppUtils.getCountDataStationWs(context) <= 0) {
                    AppUtils.checkDataStationWs(context, "first")
                }

                Log.d("WeatherWidgetProvider", "Refresh first widget clicked!")

                val views = RemoteViews(context.packageName, R.layout.weather_widget_layout)
                views.setViewVisibility(R.id.progressBar, View.VISIBLE)
                views.setViewVisibility(R.id.refresh, View.GONE)

                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)

                val timerDuration = 2000L
                val timer = object : CountDownTimer(timerDuration, timerDuration) {
                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        views.setViewVisibility(R.id.progressBar, View.GONE)
                        views.setViewVisibility(R.id.refresh, View.VISIBLE)
                        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
                    }
                }
                timer.start()

                Handler().postDelayed(
                    {
                        FetchWeatherDataTask(context, appWidgetId).execute()
                    },
                    timerDuration
                )
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.weather_widget_layout)

        val updateIntent = createUpdateIntent(context, appWidgetId)
        views.setOnClickPendingIntent(R.id.refresh, updateIntent)

        val stationIntent = Intent(context, StationList::class.java)
        stationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val stationPendingIntent = PendingIntent.getActivity(
            context,
            0,
            stationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.srsLogo, stationPendingIntent)

        val appIntent = Intent(context, Login::class.java)
        appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.weather_widget_layout_id, mainPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createUpdateIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, WeatherWidgetProvider::class.java)
        intent.action = AppUtils.ACTION_UPDATE
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    @SuppressLint("StaticFieldLeak")
    private class FetchWeatherDataTask(
        private val context: Context,
        private val appWidgetId: Int
    ) : AsyncTask<Unit, Unit, WeatherData>() {
        val prefManager = PrefManager(context)

        @Deprecated("Deprecated in Java")
        @SuppressLint("Range")
        override fun doInBackground(vararg params: Unit?): WeatherData {
            var connection: HttpURLConnection? = null
            var reader: BufferedReader? = null

            try {
                val url =
                    URL(AppUtils.mainServer + "aws_misol/get_aws_last_data.php?" +
                            "idws=${prefManager.idStation}")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    reader = BufferedReader(InputStreamReader(inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    val jsonResponse = JSONObject(response.toString())
                    if (jsonResponse.has("error")) {
                        throw Exception("Error: ${jsonResponse.getString("error")}")
                    } else {
                        return WeatherData.fromJson(jsonResponse)
                    }
                } else {
                    throw Exception("Error: $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            } finally {
                // Close resources
                reader?.close()
                connection?.disconnect()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: WeatherData) {
            super.onPostExecute(result)
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val rrMonth = try {
                result.rrMonth.toInt()
            } catch (e: Exception) {
                0
            }
            val resultRr = if (rrMonth in 60..300) {
                "Disarankan"
            } else {
                "Tidak Disarankan"
            }
            Log.d("cekData", "rrMonth: " + result.rrMonth)

            prefManager.mainDataArray = listOf(
                resultRr,
                result.temperature,
                result.rainRate,
                result.humidity,
                result.windspeed,
                result.date.replace(",", ";")
            )

            val remoteViews =
                RemoteViews(context.packageName, R.layout.weather_widget_layout)
            remoteViews.setTextViewText(R.id.weatherTemperature, prefManager.mainDataArray!![1])
            remoteViews.setTextViewText(R.id.rainfallRate, prefManager.mainDataArray!![2])
            remoteViews.setTextViewText(R.id.humidity, prefManager.mainDataArray!![3])
            remoteViews.setTextViewText(R.id.windSpeed, prefManager.mainDataArray!![4])
            remoteViews.setTextViewText(R.id.date, prefManager.mainDataArray!![5].replace(";", ","))
            remoteViews.setTextViewText(R.id.station, "Station: " + prefManager.locStation!!)
            remoteViews.setTextViewText(R.id.recom, "Pemupukan: ${prefManager.mainDataArray!![0]}")
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private data class WeatherData(
        val id: String,
        val idws: String,
        val date: String,
        val windspeed: String,
        val winddir: String,
        val rainRate: String,
        val humidity: String,
        val temperature: String,
        val rrMonth: String
    ) {
        companion object {
            fun fromJson(json: JSONObject): WeatherData {
                val id = json.getString("id")
                val idws = json.getString("idws")
                val date = json.getString("date")
                val windspeed = json.getString("ws")
                val winddir = json.getString("winddir")
                val rainRate = json.getString("rain_rate")
                val humidity = json.getString("hum")
                val temperature = json.getString("temp")
                val rrMonth = json.getString("rrMonth")

                return WeatherData(
                    id,
                    idws,
                    date,
                    windspeed,
                    winddir,
                    rainRate,
                    humidity,
                    temperature,
                    rrMonth
                )
            }
        }
    }

    private fun updateWidgetViewWithStoredData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefManager = PrefManager(context)
        val remoteViews =
            RemoteViews(context.packageName, R.layout.weather_widget_layout)
        remoteViews.setTextViewText(R.id.weatherTemperature, prefManager.mainDataArray!![1])
        remoteViews.setTextViewText(R.id.rainfallRate, prefManager.mainDataArray!![2])
        remoteViews.setTextViewText(R.id.humidity, prefManager.mainDataArray!![3])
        remoteViews.setTextViewText(R.id.windSpeed, prefManager.mainDataArray!![4])
        remoteViews.setTextViewText(R.id.date, prefManager.mainDataArray!![5].replace(";", ","))
        remoteViews.setTextViewText(R.id.station, "Station: " + prefManager.locStation!!)
        remoteViews.setTextViewText(R.id.recom, "Pemupukan: ${prefManager.mainDataArray!![0]}")
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    private fun schedulePeriodicUpdate(context: Context, appWidgetId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createUpdateIntent(context, appWidgetId)
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