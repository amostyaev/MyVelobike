package com.raistlin.myvelobike.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.raistlin.myvelobike.dto.Authorization
import com.raistlin.myvelobike.dto.LoginData
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "authorization"
)
private val Context.prefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "prefs"
)

private val AUTH_LOGIN = stringPreferencesKey("auth_login")
private val AUTH_PIN = stringPreferencesKey("auth_pin")
private val AUTH_PWA_TOKEN = stringPreferencesKey("auth_pwa_token")
private val AUTH_MOBILE_TOKEN = stringPreferencesKey("auth_mobile_token")
private val PREFS_LAST_SYNC = longPreferencesKey("prefs_last_sync")
private val PREFS_BALANCE = intPreferencesKey("prefs_balance")
private val PREFS_QRATOR = stringPreferencesKey("prefs_qrator_id")
private val PREFS_QRATOR_TIME = longPreferencesKey("prefs_qrator_time")

suspend fun Context.saveLoginData(data: LoginData) {
    authDataStore.edit { preferences ->
        preferences[AUTH_LOGIN] = data.login
        preferences[AUTH_PIN] = data.pin
    }
}

suspend fun Context.getLoginData() = authDataStore.data
    .catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preferences ->
        LoginData(
            login = preferences[AUTH_LOGIN] ?: "",
            pin = preferences[AUTH_PIN] ?: ""
        )
    }.stateIn(MainScope())

suspend fun Context.saveAuthData(auth: Authorization) {
    authDataStore.edit { preferences ->
        preferences[AUTH_PWA_TOKEN] = auth.pwaToken
        preferences[AUTH_MOBILE_TOKEN] = auth.mobileToken
    }
}

suspend fun Context.getAuthData() = authDataStore.data
    .catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preferences ->
        Authorization(
            pwaToken = preferences[AUTH_PWA_TOKEN] ?: "",
            mobileToken = preferences[AUTH_MOBILE_TOKEN] ?: ""
        )
    }.stateIn(MainScope())

suspend fun Context.saveLastSync(lastSync: Long) {
    prefsDataStore.edit { preferences ->
        preferences[PREFS_LAST_SYNC] = lastSync
    }
}

suspend fun Context.getLastSync() = prefsDataStore.data
    .catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preferences ->
        preferences[PREFS_LAST_SYNC] ?: 0L
    }.stateIn(MainScope())

suspend fun Context.saveBalance(balance: Int) {
    prefsDataStore.edit { preferences ->
        preferences[PREFS_BALANCE] = balance
    }
}

suspend fun Context.getBalance() = prefsDataStore.data
    .catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preferences ->
        preferences[PREFS_BALANCE] ?: 0
    }.stateIn(MainScope())

suspend fun Context.saveQrator(id: String, time: Long) {
    prefsDataStore.edit { preferences ->
        preferences[PREFS_QRATOR] = id
        preferences[PREFS_QRATOR_TIME] = time
    }
}

suspend fun Context.getQrator() = prefsDataStore.data
    .catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preferences ->
        (preferences[PREFS_QRATOR] ?: "") to (preferences[PREFS_QRATOR_TIME] ?: 0)
    }.stateIn(MainScope())