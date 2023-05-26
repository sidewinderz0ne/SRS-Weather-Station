package com.srs.weather

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.srs.weather.R.layout.activity_list_station
import kotlinx.android.synthetic.main.activity_list_station.*
import kotlin.system.exitProcess

class StationList : AppCompatActivity() {
    private var idStationArray = ArrayList<Int>()
    private var locStationArray = ArrayList<String>()

    var locStation = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_list_station)
        getListStation()

        val prefManager = PrefManager(this)

        spListStation.setItems(locStationArray)
        spListStation.setOnItemSelectedListener { view, position, id, item ->
            locStation = locStationArray[position]
            prefManager.idStation = idStationArray[position]
        }

        bt_save_station.setOnClickListener {
            Toast.makeText(applicationContext, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
            finishAffinity()
            exitProcess(0)
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
}