package com.srs.weather

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.os.AsyncTask
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import co.id.ssms.mobilepro.Login
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class WeatherWidgetProvider : AppWidgetProvider() {

    private val ACTION_UPDATE = "com.srs.weather.ACTION_UPDATE"

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                // Handle the click action here
                Log.d("WeatherWidgetProvider", "Widget clicked!")

                val views = RemoteViews(context.packageName, R.layout.weather_widget_layout)

                // Show the progress bar and hide the refresh button
                views.setViewVisibility(R.id.progressBar, View.VISIBLE)
                views.setViewVisibility(R.id.refresh, View.GONE)

                // Update the widget to reflect the changes
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)

                // Set the timer duration (in milliseconds)
                val timerDuration = 3000L

                // Start the timer
                val timer = object : CountDownTimer(timerDuration, timerDuration) {
                    override fun onTick(millisUntilFinished: Long) {
                        // Not used in this case
                    }

                    override fun onFinish() {
                        // Timer finished, switch the visibility of the refresh button and progress bar
                        views.setViewVisibility(R.id.progressBar, View.GONE)
                        views.setViewVisibility(R.id.refresh, View.VISIBLE)

                        // Update the widget to reflect the changes
                        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
                    }
                }
                timer.start()

                // Execute the FetchWeatherDataTask after the desired timer duration
                Handler().postDelayed(
                    {
                        FetchWeatherDataTask(context, appWidgetId).execute()
                        getAllDataStation(context, appWidgetId).execute()
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
        val stationPendingIntent = PendingIntent.getActivity(context, 0, stationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.srsLogo, stationPendingIntent)

        val appIntent = Intent(context, Login::class.java)
        appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val mainPendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.weather_widget_layout_id, mainPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createUpdateIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, WeatherWidgetProvider::class.java)
        intent.action = ACTION_UPDATE
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    private class FetchWeatherDataTask(
        private val context: Context,
        private val appWidgetId: Int
    ) : AsyncTask<Unit, Unit, WeatherData>() {
        var nameSt = ""

        @SuppressLint("Range")
        override fun doInBackground(vararg params: Unit?): WeatherData {
            var connection: HttpURLConnection? = null
            var reader: BufferedReader? = null

            try {
                val prefManager = PrefManager(context)
                val selectQueryStation = "SELECT * FROM ${DBHelper.dbTabStationList} WHERE ${DBHelper.db_id} = ${prefManager.idStation}"
                val db = DBHelper(context).readableDatabase
                val c: Cursor?
                try {
                    c = db.rawQuery(selectQueryStation, null)
                    if (c != null && c.moveToFirst()) {
                        nameSt = try {
                            c.getString(c.getColumnIndex("loc"))
                        } catch (e: Exception) {
                            ""
                        }
                    }
                } catch (e: SQLiteException) {
                    e.printStackTrace()
                }

                val url = URL("https://srs-ssms.com/aws_misol/get_aws_last_data.php?idws=" + prefManager.idStation)
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
                    return WeatherData.fromJson(JSONObject(response.toString()))
                } else {
                    // Handle error response
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Close resources
                reader?.close()
                connection?.disconnect()
            }

            return WeatherData("", "", "31 Desember, 23:23", "0", "", "0", "0", "0")
        }

        override fun onPostExecute(result: WeatherData) {
            super.onPostExecute(result)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val remoteViews =
                RemoteViews(context.packageName, R.layout.weather_widget_layout)
            remoteViews.setTextViewText(R.id.weatherTemperature, result.temperature)
            remoteViews.setTextViewText(R.id.rainfallRate, result.rainRate)
            remoteViews.setTextViewText(R.id.humidity, result.humidity)
            remoteViews.setTextViewText(R.id.windSpeed, result.windspeed)
            remoteViews.setTextViewText(R.id.date, result.date)
            remoteViews.setTextViewText(R.id.station, "Station: " + nameSt.ifEmpty { "Unknown" })
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
        val temperature: String
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

                return WeatherData(
                    id,
                    idws,
                    date,
                    windspeed,
                    winddir,
                    rainRate,
                    humidity,
                    temperature
                )
            }
        }
    }

    private class getAllDataStation(
        private val context: Context,
        private val appWidgetId: Int
    ) : AsyncTask<Unit, Unit, Boolean>() {

        override fun doInBackground(vararg params: Unit?): Boolean {
            var connection: HttpURLConnection? = null
            var reader: BufferedReader? = null

            try {
                val prefManager = PrefManager(context)
                val url = URL("https://srs-ssms.com/aws_misol/getListStation.php?version=" + prefManager.versionSt)
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
                    val jObj = JSONObject(response.toString())
                    val success = jObj.getInt("status")

                    val databaseHandler = DBHelper(context)
                    if (success == 1) {
                        val version = jObj.getInt("version")
                        databaseHandler.deleteDb()
                        val dataListStation = jObj.getJSONObject("listData")
                        val splitId = dataListStation.getJSONArray("id")
                        val splitLoc = dataListStation.getJSONArray("loc")

                        var idArray = ArrayList<Int>()
                        var locArray = ArrayList<String>()
                        for (i in 0 until splitId.length()) {
                            idArray.add(splitId.getInt(i))
                            locArray.add(splitLoc.getString(i))
                        }

                        var statusQuery = 1L
                        for (i in 0 until idArray.size) {
                            val status = databaseHandler.addWeatherStationList(
                                idws = idArray[i],
                                loc = locArray[i]
                            )
                            if (status == 0L) {
                                statusQuery = 0L
                            }
                        }

                        if (statusQuery > -1) {
                            Log.d("logStation", "Sukses insert!")
                            prefManager.versionSt = version
                        } else {
                            Log.d("logStation", "Terjadi kesalahan, hubungi pengembang")
                            databaseHandler.deleteDb()
                        }
                    }
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Close resources
                reader?.close()
                connection?.disconnect()
            }

            return false
        }

        override fun onPostExecute(result: Boolean) {
            super.onPostExecute(result)
            if (result) {
                Log.d("logStation", "Sukses insert!")
            } else {
                Log.d("logStation", "Gagal insert!")
            }
        }
    }
}