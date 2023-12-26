package com.srs.weather

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.srs.weather.R.layout.activity_list_station
import kotlinx.android.synthetic.main.activity_list_station.*
import kotlinx.android.synthetic.main.activity_login.logo_ssms
import kotlinx.android.synthetic.main.activity_login.lottie
import kotlinx.android.synthetic.main.spinner_list.view.*
import kotlin.system.exitProcess

@Suppress("DEPRECATION")
class StationList : AppCompatActivity() {
    private var idStationArray = ArrayList<Int>()
    private var locStationArray = ArrayList<String>()

    var idStation = 0
    var idStation1 = 0
    var idStation2 = 0
    var idStation3 = 0
    var idStation4 = 0
    var locStation = ""
    var locStation1 = ""
    var locStation2 = ""
    var locStation3 = ""
    var locStation4 = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefManager = PrefManager(this)
        if (!prefManager.session) {
            prefManager.prevActivity = 1
            // Session is false, redirect to the login activity
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish() // Optional: Call finish() to close the current activity
            return // Optional: Add a return statement to prevent further execution of the code in onCreate
        }

        setContentView(activity_list_station)
        getListStation()

        Glide.with(this)//GLIDE LOGO FOR LOADING LAYOUT
            .load(R.mipmap.ic_launcher)
            .into(logo_ssms_station)
        lottieStation.setAnimation(R.raw.loading_circle)//ANIMATION WITH LOTTIE FOR LOADING LAYOUT
        lottieStation.loop(true)
        lottieStation.playAnimation()

        stationMain.spListStation.setItems(locStationArray)
        stationMain.spListStation.setOnItemSelectedListener { view, position, id, item ->
            locStation = locStationArray[position]
            idStation = idStationArray[position]
        }
        if (prefManager.locStation.toString().isNotEmpty()) {
            stationMain.spListStation.text = prefManager.locStation
            locStation = prefManager.locStation.toString()
            idStation = prefManager.idStation
        }

        station1.tvListStation.text = "Station 1"
        station1.spListStation.setItems(locStationArray)
        station1.spListStation.setOnItemSelectedListener { view, position, id, item ->
            locStation1 = locStationArray[position]
            idStation1 = idStationArray[position]
        }
        if (prefManager.locStation1.toString().isNotEmpty()) {
            station1.spListStation.text = prefManager.locStation1.toString()
            locStation1 = prefManager.locStation1.toString()
            idStation1 = prefManager.idStation1
        }

        station2.tvListStation.text = "Station 2"
        station2.spListStation.setItems(locStationArray)
        station2.spListStation.setOnItemSelectedListener { view, position, id, item ->
            locStation2 = locStationArray[position]
            idStation2 = idStationArray[position]
        }
        if (!prefManager.locStation2.toString().isNullOrEmpty()) {
            station2.spListStation.text = prefManager.locStation2
            locStation2 = prefManager.locStation2.toString()
            idStation2 = prefManager.idStation2
        }

        station3.tvListStation.text = "Station 3"
        station3.spListStation.setItems(locStationArray)
        station3.spListStation.setOnItemSelectedListener { view, position, id, item ->
            locStation3 = locStationArray[position]
            idStation3 = idStationArray[position]
        }
        if (!prefManager.locStation3.toString().isNullOrEmpty()) {
            station3.spListStation.text = prefManager.locStation3
            locStation3 = prefManager.locStation3.toString()
            idStation3 = prefManager.idStation3
        }

        station4.tvListStation.text = "Station 4"
        station4.spListStation.setItems(locStationArray)
        station4.spListStation.setOnItemSelectedListener { view, position, id, item ->
            locStation4 = locStationArray[position]
            idStation4 = idStationArray[position]
        }
        if (prefManager.locStation4.toString().isNotEmpty()) {
            station4.spListStation.text = prefManager.locStation4
            locStation4 = prefManager.locStation4.toString()
            idStation4 = prefManager.idStation4
        }

        ivRefreshList.setOnClickListener {
            tv_hint_loading_station.text = "Sedang memperbarui data.."
            progressBarStation.visibility = View.VISIBLE
            AppUtils.checkDataStationWs(this, "", progressBarStation)
            getListStation()
        }

        bt_save_station.setOnClickListener {
            prefManager.idStation = idStation
            prefManager.idStation1 = idStation1
            prefManager.idStation2 = idStation2
            prefManager.idStation3 = idStation3
            prefManager.idStation4 = idStation4
            prefManager.locStation = locStation
            prefManager.locStation1 = locStation1
            prefManager.locStation2 = locStation2
            prefManager.locStation3 = locStation3
            prefManager.locStation4 = locStation4
            Toast.makeText(applicationContext, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()

            // Trigger widget update for the first widget
            val updateIntent1 = Intent(this, WeatherWidgetProvider::class.java)
            updateIntent1.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            updateIntent1.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(
                ComponentName(applicationContext, WeatherWidgetProvider::class.java)
            ))
            sendBroadcast(updateIntent1)

            // Trigger widget update for the second widget
            val updateIntent2 = Intent(this, WidgetProviderSecond::class.java)
            updateIntent2.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            updateIntent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(
                ComponentName(applicationContext, WidgetProviderSecond::class.java)
            ))
            sendBroadcast(updateIntent2)

            // Navigate back to the home screen
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
        }
    }

    @SuppressLint("Range")
    private fun getListStation() {
        idStationArray.clear()
        locStationArray.clear()
        val selectQuery =
            "SELECT  * FROM ${DBHelper.dbTabStationList} ORDER BY ${DBHelper.db_id} ASC"
        val db = DBHelper(this).readableDatabase
        var i: Cursor?
        try {
            i = db.rawQuery(selectQuery, null)
            if (i.moveToFirst()) {
                do {
                    locStationArray.add(
                        try {
                            i.getString(i.getColumnIndex(DBHelper.db_loc))
                        } catch (e: Exception) {
                            ""
                        }
                    )
                    idStationArray.add(
                        try {
                            i.getInt(i.getColumnIndex(DBHelper.db_id))
                        } catch (e: Exception) {
                            0
                        }
                    )
                } while (i!!.moveToNext())
            }
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
        }
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
    }
}