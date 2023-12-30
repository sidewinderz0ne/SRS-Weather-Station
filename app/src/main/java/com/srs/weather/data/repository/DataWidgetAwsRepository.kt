package com.srs.weather.data.repository

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import com.srs.weather.data.database.DBHelper
import com.srs.weather.data.model.DataWidgetAwsModel

class DataWidgetAwsRepository(context: Context) {

    private val databaseHelper: DBHelper = DBHelper(context)

    fun insertDataWidgetAws(dataWs: DataWidgetAwsModel): Boolean {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DBHelper.db_response_data, dataWs.response_data)
            put(DBHelper.db_widget, dataWs.widget)
        }
        val rowsAffected = db.insert(DBHelper.dbTabDataWidget, null, values)
        db.close()

        return rowsAffected > 0
    }

    fun deleteDataWidgetAws(idWidget: String? = "") {
        val db = databaseHelper.writableDatabase
        val whereClause = if (idWidget.isNullOrEmpty()) null else "${DBHelper.db_widget}=?"
        val whereArgs = if (idWidget.isNullOrEmpty()) null else arrayOf(idWidget)

        db.delete(DBHelper.dbTabDataWidget, whereClause, whereArgs)
        db.close()
    }

    fun getCountDataWidgetAws(): Int {
        val db = databaseHelper.readableDatabase
        val query = "SELECT COUNT(*) FROM ${DBHelper.dbTabDataWidget}"
        val cursor = db.rawQuery(query, null)

        var count = 0
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
        }

        db.close()
        return count
    }

    @SuppressLint("Range")
    fun getAllDataWidgetAws(): List<DataWidgetAwsModel> {
        val dataWidgetAwsList = mutableListOf<DataWidgetAwsModel>()
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DBHelper.dbTabDataWidget}", null)

        cursor.use {
            while (it.moveToNext()) {
                val rdata = it.getString(it.getColumnIndex("response_data"))
                val widget = it.getInt(it.getColumnIndex("widget"))
                val dataWidgetAws = DataWidgetAwsModel(rdata, widget)
                dataWidgetAwsList.add(dataWidgetAws)
            }
        }
        db.close()

        return dataWidgetAwsList
    }
}