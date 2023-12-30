package com.srs.weather.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "weather"
        const val DATABASE_VERSION = 2
        const val dbTabStationList = "weather_station_list"
        const val dbTabDataWidget = "widget_data"

        const val db_id = "id"
        const val db_loc = "loc"
        const val db_response_data = "response_data"
        const val db_widget = "widget"

    }

    private val createTableWeatherList = "CREATE TABLE $dbTabStationList ($db_id INTEGER, $db_loc VARCHAR)"
    private val createTableWidgetData = "CREATE TABLE $dbTabDataWidget ($db_id INTEGER, $db_response_data TEXT, $db_widget INTEGER)"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(createTableWeatherList)
        db.execSQL(createTableWidgetData)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(createTableWidgetData)
        }
    }

    fun addWeatherStationList(
        idws: Int,
        loc: String
    ): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(db_id, idws)
        contentValues.put(db_loc, loc)
        val success = db.insert(dbTabStationList, null, contentValues)
        db.close()
        return success
    }

    fun deleteDb() {
        val db = this.writableDatabase
        db.delete(dbTabStationList, null, null)
        db.close()
    }
}