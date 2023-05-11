package com.srs.weather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
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
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.weather_widget_layout2)
        val rootView = views.layoutId

        // Set the click listener for the root view
        views.setOnClickPendingIntent(rootView, createUpdateIntent(context))
        views.setOnClickPendingIntent(R.id.weather_widget_layout_id, createUpdateIntent(context))
        // Fetch weather data asynchronously
        val dataCallback = object : WeatherDataCallback {
            override fun onWeatherDataFetched(weatherData: WeatherData) {
                views.setTextViewText(R.id.weatherTemperature, weatherData.temperature)
                // Update other widget views with the available data
                views.setTextViewText(R.id.rainfallRate, weatherData.rainRate)
                views.setTextViewText(R.id.humidity, weatherData.humidity)
                views.setTextViewText(R.id.windSpeed, weatherData.windspeed)
                views.setTextViewText(R.id.date, weatherData.date)

                // Update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        // Execute the task to fetch weather data
        val fetchWeatherDataTask = FetchWeatherDataTask(dataCallback)
        fetchWeatherDataTask.execute()
    }

    private interface WeatherDataCallback {
        fun onWeatherDataFetched(weatherData: WeatherData)
    }

    private fun createUpdateIntent(context: Context): PendingIntent {
        Log.d("testUpdate", "masuk createUpdateIntent")
        val intent = Intent(context, WeatherWidgetProvider::class.java)
        intent.action = ACTION_UPDATE
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    private class FetchWeatherDataTask(private val callback: WeatherDataCallback) :
        AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void): String? {
            var connection: HttpURLConnection? = null
            var reader: BufferedReader? = null

            try {
                val url = URL("https://srs-ssms.com/aws_misol/get_aws_last_data.php")
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
                    return response.toString()
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

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != null) {
                val jsonObject = JSONObject(result)
                val weatherData = WeatherData.fromJson(jsonObject)
                callback.onWeatherDataFetched(weatherData)
            } else {
                // Handle null result or error case
            }
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
}