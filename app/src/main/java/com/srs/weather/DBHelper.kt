package com.srs.weather

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "weather"
        const val DATABASE_VERSION = 1
        const val dbTabStationList = "weather_station_list"

        const val db_id = "id"
        const val db_loc = "loc"

    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableWeatherList = "CREATE TABLE $dbTabStationList ($db_id INTEGER, $db_loc VARCHAR)"
        db.execSQL(createTableWeatherList)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrade if needed
        db.execSQL("DROP TABLE IF EXISTS $dbTabStationList")
        onCreate(db)
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