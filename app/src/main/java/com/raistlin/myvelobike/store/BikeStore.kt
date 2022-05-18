package com.raistlin.myvelobike.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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

private val AUTH_LOGIN = stringPreferencesKey("auth_login")
private val AUTH_PIN = stringPreferencesKey("auth_pin")
private val AUTH_TOKEN = stringPreferencesKey("auth_token")
private val AUTH_REFRESH_TOKEN = stringPreferencesKey("auth_refresh_token")

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
        preferences[AUTH_TOKEN] = auth.token
        preferences[AUTH_REFRESH_TOKEN] = auth.refreshToken
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
            token = preferences[AUTH_TOKEN] ?: "",
            refreshToken = preferences[AUTH_REFRESH_TOKEN] ?: ""
        )
    }.stateIn(MainScope())