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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                // Handle the click action here
                Log.d("WeatherWidgetProvider", "Widget clicked!")
                FetchWeatherDataTask(context, appWidgetId).execute()
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.weather_widget_layout2)
        val updateIntent = createUpdateIntent(context, appWidgetId)
        views.setOnClickPendingIntent(R.id.weather_widget_layout_id, updateIntent)

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

        override fun doInBackground(vararg params: Unit?): WeatherData {
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

            return WeatherData("", "", "", "", "", "", "", "")
        }

        override fun onPostExecute(result: WeatherData) {
            super.onPostExecute(result)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val remoteViews =
                RemoteViews(context.packageName, R.layout.weather_widget_layout2)
            remoteViews.setTextViewText(R.id.weatherTemperature, result.temperature)
            remoteViews.setTextViewText(R.id.rainfallRate, result.rainRate)
            remoteViews.setTextViewText(R.id.humidity, result.humidity)
            remoteViews.setTextViewText(R.id.windSpeed, result.windspeed)
            remoteViews.setTextViewText(R.id.date, result.date)
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
}