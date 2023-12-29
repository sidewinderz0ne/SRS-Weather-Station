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
class WidgetProviderSecond : AppWidgetProvider() {

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
                updateWidgetViewWithStoredData(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppUtils.ACTION_UPDATE + "_SECOND") {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                if (AppUtils.getCountDataStationWs(context) <= 0) {
                    AppUtils.checkDataStationWs(context, "first")
                }

                Log.d("WeatherWidgetProvider", "Refresh second widget clicked!")
                val views = RemoteViews(context.packageName, R.layout.widget_layout_second)
                views.setViewVisibility(R.id.progressBarSc, View.VISIBLE)
                views.setViewVisibility(R.id.refreshSc, View.GONE)

                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)

                val timerDuration = 2000L
                val timer = object : CountDownTimer(timerDuration, timerDuration) {
                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        views.setViewVisibility(R.id.progressBarSc, View.GONE)
                        views.setViewVisibility(R.id.refreshSc, View.VISIBLE)
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
        val views = RemoteViews(context.packageName, R.layout.widget_layout_second)

        val updateIntent = createUpdateIntent(context, appWidgetId)
        views.setOnClickPendingIntent(R.id.refreshSc, updateIntent)

        val stationIntent = Intent(context, StationList::class.java)
        stationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val stationPendingIntent = PendingIntent.getActivity(
            context,
            0,
            stationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.srsLogoSc, stationPendingIntent)

        val appIntent = Intent(context, Login::class.java)
        appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_layout_second_id, mainPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createUpdateIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, WidgetProviderSecond::class.java)
        intent.action = AppUtils.ACTION_UPDATE + "_SECOND"
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
                    URL(
                        AppUtils.mainServer + "aws_misol/getDataAwsLocation.php?" +
                                "idws1=${prefManager.idStation1}&idws2=${prefManager.idStation2}" +
                                "&idws3=${prefManager.idStation3}&idws4=${prefManager.idStation4}"
                    )
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

            val resultRr1 = if (try {
                    result.rrMonth1.toInt()
                } catch (e: Exception) {
                    0
                } in 60..300
            ) {
                "✓"
            } else {
                "✕"
            }
            val resultRr2 = if (try {
                    result.rrMonth2.toInt()
                } catch (e: Exception) {
                    0
                } in 60..300
            ) {
                "✓"
            } else {
                "✕"
            }
            val resultRr3 = if (try {
                    result.rrMonth3.toInt()
                } catch (e: Exception) {
                    0
                } in 60..300
            ) {
                "✓"
            } else {
                "✕"
            }
            val resultRr4 = if (try {
                    result.rrMonth4.toInt()
                } catch (e: Exception) {
                    0
                } in 60..300
            ) {
                "✓"
            } else {
                "✕"
            }

            prefManager.secondDataArray1 = listOf(
                resultRr1,
                result.uv1,
                result.temperature1,
                result.rainRate1,
                result.humidity1,
                result.windspeed1,
                result.date.replace(",", ";")
            )

            prefManager.secondDataArray2 = listOf(
                resultRr2,
                result.uv2,
                result.temperature2,
                result.rainRate2,
                result.humidity2,
                result.windspeed2
            )

            prefManager.secondDataArray3 = listOf(
                resultRr3,
                result.uv3,
                result.temperature3,
                result.rainRate3,
                result.humidity3,
                result.windspeed3
            )

            prefManager.secondDataArray4 = listOf(
                resultRr4,
                result.uv4,
                result.temperature4,
                result.rainRate4,
                result.humidity4,
                result.windspeed4
            )

            val remoteViews =
                RemoteViews(context.packageName, R.layout.widget_layout_second)
            remoteViews.setTextViewText(
                R.id.dateSc,
                prefManager.secondDataArray1!![6].replace(";", ",")
            )
            remoteViews.setTextViewText(
                R.id.locStation1,
                prefManager.locStation1!!
            )
            remoteViews.setTextViewText(R.id.uvStation1, prefManager.secondDataArray1!![1])
            remoteViews.setTextViewText(R.id.tempStation1, prefManager.secondDataArray1!![2])
            remoteViews.setTextViewText(R.id.rainRateSecond1, prefManager.secondDataArray1!![3])
            remoteViews.setTextViewText(R.id.humSecond1, prefManager.secondDataArray1!![4])
            remoteViews.setTextViewText(R.id.windSpeedSecond1, prefManager.secondDataArray1!![5])
            remoteViews.setTextViewText(R.id.rrMonthSecond1, prefManager.secondDataArray1!![0])
            remoteViews.setTextViewText(
                R.id.locStation2,
                prefManager.locStation2!!
            )
            remoteViews.setTextViewText(R.id.uvStation2, prefManager.secondDataArray2!![1])
            remoteViews.setTextViewText(R.id.tempStation2, prefManager.secondDataArray2!![2])
            remoteViews.setTextViewText(R.id.rainRateSecond2, prefManager.secondDataArray2!![3])
            remoteViews.setTextViewText(R.id.humSecond2, prefManager.secondDataArray2!![4])
            remoteViews.setTextViewText(R.id.windSpeedSecond2, prefManager.secondDataArray2!![5])
            remoteViews.setTextViewText(R.id.rrMonthSecond2, prefManager.secondDataArray2!![0])
            remoteViews.setTextViewText(
                R.id.locStation3,
                prefManager.locStation3!!
            )
            remoteViews.setTextViewText(R.id.uvStation3, prefManager.secondDataArray3!![1])
            remoteViews.setTextViewText(R.id.tempStation3, prefManager.secondDataArray3!![2])
            remoteViews.setTextViewText(R.id.rainRateSecond3, prefManager.secondDataArray3!![3])
            remoteViews.setTextViewText(R.id.humSecond3, prefManager.secondDataArray3!![4])
            remoteViews.setTextViewText(R.id.windSpeedSecond3, prefManager.secondDataArray3!![5])
            remoteViews.setTextViewText(R.id.rrMonthSecond3, prefManager.secondDataArray3!![0])
            remoteViews.setTextViewText(
                R.id.locStation4,
                prefManager.locStation4!!
            )
            remoteViews.setTextViewText(R.id.uvStation4, prefManager.secondDataArray4!![1])
            remoteViews.setTextViewText(R.id.tempStation4, prefManager.secondDataArray4!![2])
            remoteViews.setTextViewText(R.id.rainRateSecond4, prefManager.secondDataArray4!![3])
            remoteViews.setTextViewText(R.id.humSecond4, prefManager.secondDataArray4!![4])
            remoteViews.setTextViewText(R.id.windSpeedSecond4, prefManager.secondDataArray4!![5])
            remoteViews.setTextViewText(R.id.rrMonthSecond4, prefManager.secondDataArray4!![0])
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private data class WeatherData(
        val date: String,
        val id1: String,
        val id2: String,
        val id3: String,
        val id4: String,
        val idws1: String,
        val idws2: String,
        val idws3: String,
        val idws4: String,
        val uv1: String,
        val uv2: String,
        val uv3: String,
        val uv4: String,
        val temperature1: String,
        val temperature2: String,
        val temperature3: String,
        val temperature4: String,
        val rainRate1: String,
        val rainRate2: String,
        val rainRate3: String,
        val rainRate4: String,
        val humidity1: String,
        val humidity2: String,
        val humidity3: String,
        val humidity4: String,
        val windspeed1: String,
        val windspeed2: String,
        val windspeed3: String,
        val windspeed4: String,
        val rrMonth1: String,
        val rrMonth2: String,
        val rrMonth3: String,
        val rrMonth4: String
    ) {
        companion object {
            fun fromJson(json: JSONObject): WeatherData {
                val dataStation1 = json.getJSONObject("station1")
                val date = dataStation1.getString("date")
                val id1 = dataStation1.getString("id1")
                val idws1 = dataStation1.getString("idws1")
                val uv1 = dataStation1.getString("uv1")
                val temperature1 = dataStation1.getString("temp1") + "°"
                val rainRate1 = dataStation1.getString("rain_rate1")
                val humidity1 = dataStation1.getString("hum1")
                val windspeed1 = dataStation1.getString("ws1")
                val rrMonth1 = dataStation1.getString("rrMonth1")

                val dataStation2 = json.getJSONObject("station2")
                val id2 = dataStation2.getString("id2")
                val idws2 = dataStation2.getString("idws2")
                val uv2 = dataStation2.getString("uv2")
                val temperature2 = dataStation2.getString("temp2") + "°"
                val rainRate2 = dataStation2.getString("rain_rate2")
                val humidity2 = dataStation2.getString("hum2")
                val windspeed2 = dataStation2.getString("ws2")
                val rrMonth2 = dataStation2.getString("rrMonth2")

                val dataStation3 = json.getJSONObject("station3")
                val id3 = dataStation3.getString("id3")
                val idws3 = dataStation3.getString("idws3")
                val uv3 = dataStation3.getString("uv3")
                val temperature3 = dataStation3.getString("temp3") + "°"
                val rainRate3 = dataStation3.getString("rain_rate3")
                val humidity3 = dataStation3.getString("hum3")
                val windspeed3 = dataStation3.getString("ws3")
                val rrMonth3 = dataStation3.getString("rrMonth3")

                val dataStation4 = json.getJSONObject("station4")
                val id4 = dataStation4.getString("id4")
                val idws4 = dataStation4.getString("idws4")
                val uv4 = dataStation4.getString("uv4")
                val temperature4 = dataStation4.getString("temp4") + "°"
                val rainRate4 = dataStation4.getString("rain_rate4")
                val humidity4 = dataStation4.getString("hum4")
                val windspeed4 = dataStation4.getString("ws4")
                val rrMonth4 = dataStation4.getString("rrMonth4")

                return WeatherData(
                    date,
                    id1,
                    id2,
                    id3,
                    id4,
                    idws1,
                    idws2,
                    idws3,
                    idws4,
                    uv1,
                    uv2,
                    uv3,
                    uv4,
                    temperature1,
                    temperature2,
                    temperature3,
                    temperature4,
                    rainRate1,
                    rainRate2,
                    rainRate3,
                    rainRate4,
                    humidity1,
                    humidity2,
                    humidity3,
                    humidity4,
                    windspeed1,
                    windspeed2,
                    windspeed3,
                    windspeed4,
                    rrMonth1,
                    rrMonth2,
                    rrMonth3,
                    rrMonth4
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
            RemoteViews(context.packageName, R.layout.widget_layout_second)
        remoteViews.setTextViewText(
            R.id.dateSc,
            prefManager.secondDataArray1!![6].replace(";", ",")
        )
        remoteViews.setTextViewText(
            R.id.locStation1,
            prefManager.locStation1!!
        )
        remoteViews.setTextViewText(R.id.uvStation1, prefManager.secondDataArray1!![1])
        remoteViews.setTextViewText(R.id.tempStation1, prefManager.secondDataArray1!![2])
        remoteViews.setTextViewText(R.id.rainRateSecond1, prefManager.secondDataArray1!![3])
        remoteViews.setTextViewText(R.id.humSecond1, prefManager.secondDataArray1!![4])
        remoteViews.setTextViewText(R.id.windSpeedSecond1, prefManager.secondDataArray1!![5])
        remoteViews.setTextViewText(R.id.rrMonthSecond1, prefManager.secondDataArray1!![0])
        remoteViews.setTextViewText(
            R.id.locStation2,
            prefManager.locStation2!!
        )
        remoteViews.setTextViewText(R.id.uvStation2, prefManager.secondDataArray2!![1])
        remoteViews.setTextViewText(R.id.tempStation2, prefManager.secondDataArray2!![2])
        remoteViews.setTextViewText(R.id.rainRateSecond2, prefManager.secondDataArray2!![3])
        remoteViews.setTextViewText(R.id.humSecond2, prefManager.secondDataArray2!![4])
        remoteViews.setTextViewText(R.id.windSpeedSecond2, prefManager.secondDataArray2!![5])
        remoteViews.setTextViewText(R.id.rrMonthSecond2, prefManager.secondDataArray2!![0])
        remoteViews.setTextViewText(
            R.id.locStation3,
            prefManager.locStation3!!
        )
        remoteViews.setTextViewText(R.id.uvStation3, prefManager.secondDataArray3!![1])
        remoteViews.setTextViewText(R.id.tempStation3, prefManager.secondDataArray3!![2])
        remoteViews.setTextViewText(R.id.rainRateSecond3, prefManager.secondDataArray3!![3])
        remoteViews.setTextViewText(R.id.humSecond3, prefManager.secondDataArray3!![4])
        remoteViews.setTextViewText(R.id.windSpeedSecond3, prefManager.secondDataArray3!![5])
        remoteViews.setTextViewText(R.id.rrMonthSecond3, prefManager.secondDataArray3!![0])
        remoteViews.setTextViewText(
            R.id.locStation4,
            prefManager.locStation4!!
        )
        remoteViews.setTextViewText(R.id.uvStation4, prefManager.secondDataArray4!![1])
        remoteViews.setTextViewText(R.id.tempStation4, prefManager.secondDataArray4!![2])
        remoteViews.setTextViewText(R.id.rainRateSecond4, prefManager.secondDataArray4!![3])
        remoteViews.setTextViewText(R.id.humSecond4, prefManager.secondDataArray4!![4])
        remoteViews.setTextViewText(R.id.windSpeedSecond4, prefManager.secondDataArray4!![5])
        remoteViews.setTextViewText(R.id.rrMonthSecond4, prefManager.secondDataArray4!![0])
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