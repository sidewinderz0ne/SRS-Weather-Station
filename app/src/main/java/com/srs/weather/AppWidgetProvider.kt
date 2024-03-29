package com.srs.weather

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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


class WeatherWidgetProvider : AppWidgetProvider() {

    private val ACTION_UPDATE = "com.srs.weather.ACTION_UPDATE"

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)

            if (hasNetworkConnection(context)) {
                FetchWeatherDataTask(context, appWidgetId).execute()
                getAllDataStation(context, appWidgetId).execute()
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
        if (intent.action == ACTION_UPDATE) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
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
                val timerDuration = 2000L

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
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (appWidgetIds != null) {
                Log.d("WeatherWidgetProvider", "Station saved!")
                for (appWidgetId in appWidgetIds) {
                    FetchWeatherDataTask(context, appWidgetId).execute()
                }
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
        val prefManager = PrefManager(context)

        @SuppressLint("Range")
        override fun doInBackground(vararg params: Unit?): WeatherData {
            var connection: HttpURLConnection? = null
            var reader: BufferedReader? = null

            try {
                val url =
                    URL("https://srs-ssms.com/aws_misol/get_aws_last_data.php?idws=${prefManager.idStation}")
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

        override fun onPostExecute(result: WeatherData) {
            super.onPostExecute(result)
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val resultRr = if (result.rrMonth.toInt() in 60..300) {
                "Disarankan"
            } else {
                "Tidak Disarankan"
            }

            Log.d("cekData", result.rrMonth)

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

    private class getAllDataStation(
        private val context: Context,
        private val appWidgetId: Int
    ) : AsyncTask<Unit, Unit, Boolean>() {

        override fun doInBackground(vararg params: Unit?): Boolean {
            var connection: HttpURLConnection? = null
            var reader: BufferedReader? = null

            try {
                val prefManager = PrefManager(context)
                val url =
                    URL("https://srs-ssms.com/aws_misol/getListStation.php?version=${prefManager.versionSt}")
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

    @SuppressLint("ServiceCast")
    fun hasNetworkConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            // For other device-attached transports like Ethernet, Bluetooth, etc.
            else -> false
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
}