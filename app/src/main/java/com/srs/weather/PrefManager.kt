package com.srs.weather

import android.content.Context
import android.content.SharedPreferences

class PrefManager(_context: Context) {
    var pref: SharedPreferences
    var editor: SharedPreferences.Editor
    var context: Context? = null

    // shared pref mode
    var privateMode = 0

    // shared pref mode
    var PRIVATE_MODE = 0

    var isFirstTimeLaunch: Boolean
        get() = pref.getBoolean(IS_FIRST_TIME_LAUNCH, true)
        set(isFirstTime) {
            editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime)
            editor.commit()
        }

    var login: Int
        get() = pref.getInt(LOGIN, 0)
        set(isLogged) {
            editor.putInt(LOGIN, isLogged)
            editor.commit()
        }

    var session: Boolean
        get() = pref.getBoolean(SESSION, false)
        set(sessionActive) {
            editor.putBoolean(SESSION, sessionActive)
            editor.commit()
        }

    var versionSt: Int
        get() = pref.getInt(version_tag, 0)
        set(versionStCount) {
            editor.putInt(version_tag, versionStCount)
            editor.commit()
        }

    var idStation: Int
        get() = pref.getInt(id_station, 15)
        set(idStCount) {
            editor.putInt(id_station, idStCount)
            editor.commit()
        }

    var idStation1: Int
        get() = pref.getInt(id_station1, 15)
        set(idStCount1) {
            editor.putInt(id_station1, idStCount1)
            editor.commit()
        }

    var idStation2: Int
        get() = pref.getInt(id_station2, 15)
        set(idStCount2) {
            editor.putInt(id_station2, idStCount2)
            editor.commit()
        }

    var idStation3: Int
        get() = pref.getInt(id_station3, 15)
        set(idStCount3) {
            editor.putInt(id_station3, idStCount3)
            editor.commit()
        }

    var idStation4: Int
        get() = pref.getInt(id_station4, 15)
        set(idStCount4) {
            editor.putInt(id_station4, idStCount4)
            editor.commit()
        }

    var locStation: String?
        get() = pref.getString(loc_station, "SRS")
        set(locStCount) {
            editor.putString(loc_station, locStCount)
            editor.commit()
        }

    var locStation1: String?
        get() = pref.getString(loc_station1, "SRS")
        set(locStCount1) {
            editor.putString(loc_station1, locStCount1)
            editor.commit()
        }

    var locStation2: String?
        get() = pref.getString(loc_station2, "SRS")
        set(locStCount2) {
            editor.putString(loc_station2, locStCount2)
            editor.commit()
        }

    var locStation3: String?
        get() = pref.getString(loc_station3, "SRS")
        set(locStCount3) {
            editor.putString(loc_station3, locStCount3)
            editor.commit()
        }

    var locStation4: String?
        get() = pref.getString(loc_station4, "SRS")
        set(locStCount4) {
            editor.putString(loc_station4, locStCount4)
            editor.commit()
        }

    var userid: String?
        get() = pref.getString(USERID, null)
        set(userId) {
            editor.putString(USERID, userId)
            editor.commit()
        }

    var name: String?
        get() = pref.getString(NAME, null)
        set(sureName) {
            editor.putString(NAME, sureName)
            editor.commit()
        }

    var email: String?
        get() = pref.getString(EMAIL, null)
        set(mail) {
            editor.putString(EMAIL, mail)
            editor.commit()
        }

    var password: String?
        get() = pref.getString(PASSWORD, null)
        set(pass) {
            editor.putString(PASSWORD, pass)
            editor.commit()
        }

    var prevActivity: Int
        get() = pref.getInt(prevAct, 0)
        set(prev) {
            editor.putInt(prevAct, prev)
            editor.commit()
        }

    var mainDataArray: List<String>?
        get() {
            val dataArrayString = pref.getString(dataArrayMain, null)
            return dataArrayString?.split(",")?.map { it.trim() }
        }
        set(dataMain) {
            val dataArrayString = dataMain?.joinToString(",")
            editor.putString(dataArrayMain, dataArrayString)
            editor.commit()
        }

    var secondDataArray1: List<String>?
        get() {
            val dataArrayStringSc1 = pref.getString(dataArraySecond1, null)
            return dataArrayStringSc1?.split(",")?.map { it.trim() }
        }
        set(dataSc1) {
            val dataArrayStringSc1 = dataSc1?.joinToString(",")
            editor.putString(dataArraySecond1, dataArrayStringSc1)
            editor.commit()
        }

    var secondDataArray2: List<String>?
        get() {
            val dataArrayStringSc2 = pref.getString(dataArraySecond2, null)
            return dataArrayStringSc2?.split(",")?.map { it.trim() }
        }
        set(dataSc2) {
            val dataArrayStringSc2 = dataSc2?.joinToString(",")
            editor.putString(dataArraySecond2, dataArrayStringSc2)
            editor.commit()
        }

    var secondDataArray3: List<String>?
        get() {
            val dataArrayStringSc3 = pref.getString(dataArraySecond3, null)
            return dataArrayStringSc3?.split(",")?.map { it.trim() }
        }
        set(dataSc3) {
            val dataArrayStringSc3 = dataSc3?.joinToString(",")
            editor.putString(dataArraySecond3, dataArrayStringSc3)
            editor.commit()
        }

    var secondDataArray4: List<String>?
        get() {
            val dataArrayStringSc4 = pref.getString(dataArraySecond4, null)
            return dataArrayStringSc4?.split(",")?.map { it.trim() }
        }
        set(dataSc4) {
            val dataArrayStringSc4 = dataSc4?.joinToString(",")
            editor.putString(dataArraySecond4, dataArrayStringSc4)
            editor.commit()
        }

    companion object {
        // Shared preferences file name
        private const val PREF_NAME = "sulungresearch"
        private const val IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"
        private const val LOGIN = "Login"
        private const val SESSION = "Session"

        const val version_tag = "versionSt"
        const val id_station = "idStation"
        const val loc_station = "locStation"

        const val id_station1 = "idStation1"
        const val id_station2 = "idStation2"
        const val id_station3 = "idStation3"
        const val id_station4 = "idStation4"
        const val loc_station1 = "locStation1"
        const val loc_station2 = "locStation2"
        const val loc_station3 = "locStation3"
        const val loc_station4 = "locStation4"
        const val USERID = "user_id"
        const val NAME = "name"
        const val EMAIL = "email"
        const val PASSWORD = "password"
        const val prevAct = "prevAct"

        // Data Widget
        const val dataArrayMain = "dataMain"
        const val dataArraySecond1 = "dataSecond1"
        const val dataArraySecond2 = "dataSecond2"
        const val dataArraySecond3 = "dataSecond3"
        const val dataArraySecond4 = "dataSecond4"
    }

    init {
        pref = _context.getSharedPreferences(PREF_NAME, privateMode)
        editor = pref.edit()
    }

    fun prefManag(context: Context) {
        this.context = context
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref.edit()
    }

    fun timeLaunch(): Boolean {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true)
    }
}