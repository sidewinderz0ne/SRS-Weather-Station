package com.srs.weather

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import android.view.View
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.srs.weather.DBHelper.Companion.dbTabStationList
import com.srs.weather.DBHelper.Companion.db_id
import com.srs.weather.DBHelper.Companion.db_loc
import org.json.JSONException
import org.json.JSONObject

object AppUtils {

    const val mainServer = "https://srs-ssms.com/"

    const val TAG_SUCCESS = "success"
    const val TAG_MESSAGE = "message"
    const val TAG_LIST = "listData"

    const val LOG_STATION = "stationLog"
    const val ACTION_UPDATE = "com.srs.weather.ACTION_UPDATE"

    fun checkConnectionDevice(context: Context): Boolean {
        val connectivityManager: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val con = try {
            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state
        } catch (e: Exception) {
            NetworkInfo.State.DISCONNECTED
        }

        return con === NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state === NetworkInfo.State.CONNECTED
    }

    @SuppressLint("Recycle", "Range")
    fun getFirstRowData(context: Context): List<Any> {
        val selectQuery = "SELECT * FROM $dbTabStationList ORDER BY $db_id ASC"
        val db = DBHelper(context).readableDatabase
        val cursor: Cursor?
        var firstRowData = listOf<Any>()

        try {
            cursor = db.rawQuery(selectQuery, null)
            if (cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndex(db_id))
                val location = cursor.getString(cursor.getColumnIndex(db_loc))
                firstRowData = listOf(id, location)
            }
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
        }

        return firstRowData
    }

    fun getCountDataStationWs(context: Context): Int {
        val db = DBHelper(context).readableDatabase
        val query = "SELECT COUNT(*) FROM $dbTabStationList"
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

    fun checkDataStationWs(context: Context, arg: String? = "", view: View? = null) {
        val dbHandler = DBHelper(context)
        val prefManager = PrefManager(context)

        val strReq: StringRequest =
            @SuppressLint("SetTextI18n")
            object : StringRequest(
                Method.POST,
                mainServer + "aws_misol/getListStation1.php",
                Response.Listener { response ->
                    try {
                        val jObj = JSONObject(response)
                        val success = jObj.getInt(TAG_SUCCESS)

                        if (success == 1) {
                            Log.d(LOG_STATION, jObj.getString(TAG_MESSAGE))
                        } else if (success == 2) {
                            Log.d(LOG_STATION, jObj.getString(TAG_MESSAGE))

                            dbHandler.deleteDb()
                            val dataListWs = jObj.getJSONArray(TAG_LIST)
                            var statusQuery = 1L
                            for (i in 0 until dataListWs.length()) {
                                val jsonObject = dataListWs.getJSONObject(i)
                                val idFromJson = jsonObject.getInt("id")
                                val locFromJson = jsonObject.getString("loc")

                                val status = dbHandler.addWeatherStationList(
                                    idws = idFromJson,
                                    loc = locFromJson
                                )

                                if (status == 0L) {
                                    statusQuery = 0L
                                }

                                if (arg!!.isNotEmpty()) {
                                    if (i == 0) {
                                        prefManager.idStation = idFromJson
                                        prefManager.locStation = locFromJson

                                        prefManager.idStation1 = idFromJson
                                        prefManager.locStation1 = locFromJson
                                        prefManager.idStation2 = idFromJson
                                        prefManager.locStation2 = locFromJson
                                        prefManager.idStation3 = idFromJson
                                        prefManager.locStation3 = locFromJson
                                        prefManager.idStation4 = idFromJson
                                        prefManager.locStation4 = locFromJson
                                    }
                                }
                            }

                            if (statusQuery > -1) {
                                Log.d(LOG_STATION, "Sukses insert!")
                                prefManager.hexStation = jObj.getString("md5")

                            } else {
                                Log.d(LOG_STATION, "Terjadi kesalahan, hubungi pengembang")
                                dbHandler.deleteDb()

                                checkDataStationWs(context)
                            }
                        }

                        if (view != null) {
                            view.visibility = View.GONE
                            Toast.makeText(context, jObj.getString(TAG_MESSAGE), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: JSONException) {
                        Log.d(LOG_STATION, "Data error, hubungi pengembang: $e")
                        e.printStackTrace()

                        checkDataStationWs(context)

                        if (view != null) {
                            view.visibility = View.GONE
                            Toast.makeText(context, "Data error, hubungi pengembang: $e", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                Response.ErrorListener { error ->
                    Log.d(LOG_STATION, "Terjadi kesalahan koneksi: $error")

                    checkDataStationWs(context)

                    if (view != null) {
                        view.visibility = View.GONE
                        Toast.makeText(context, "Terjadi kesalahan koneksi: $error", Toast.LENGTH_LONG).show()
                    }
                }) {

                override fun getParams(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    params["hexApp"] = try {
                        prefManager.hexStation.toString()
                    } catch (e: Exception) {
                        ""
                    }
                    return params
                }
            }

        strReq.retryPolicy = DefaultRetryPolicy(
            90000,  // Socket timeout in milliseconds (30 seconds)
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(context).add(strReq)
    }
}