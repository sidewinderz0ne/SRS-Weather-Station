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

    companion object {
        // Shared preferences file name
        private const val PREF_NAME = "sulungresearch"
        private const val IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"
        private const val LOGIN = "Login"
        private const val SESSION = "Session"

        const val version_tag = "versionSt"
        const val id_station = "idSt"
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