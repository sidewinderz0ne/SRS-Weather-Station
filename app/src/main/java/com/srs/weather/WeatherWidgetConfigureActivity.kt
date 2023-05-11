package com.srs.weather

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WeatherWidgetConfigureActivity : AppCompatActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_widget_configure)

        // Set the result to canceled by default
        setResult(Activity.RESULT_CANCELED)

        // Get the widget ID passed from the widget provider
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        // If the widget ID is invalid, finish the activity
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Save the widget ID and finish the activity when the "Save" button is clicked
        val saveButton: Button = findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            saveWidgetConfiguration(this@WeatherWidgetConfigureActivity, appWidgetId)

            // Set the result to OK and pass the widget ID back to the widget provider
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)

            finish()
        }
    }

    private fun saveWidgetConfiguration(context: Context, appWidgetId: Int) {
        // Save the widget configuration settings using SharedPreferences or any other storage mechanism
        // You can retrieve the configuration settings from input fields or any other user input in the activity
    }
}
