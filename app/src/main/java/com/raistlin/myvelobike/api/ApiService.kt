package com.raistlin.myvelobike.api

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.raistlin.myvelobike.dao.RideDao
import com.raistlin.myvelobike.dto.Authorization
import com.raistlin.myvelobike.dto.Events
import com.raistlin.myvelobike.dto.LoginData
import com.raistlin.myvelobike.dto.MobileTokenStorage
import com.raistlin.myvelobike.dto.Profile
import com.raistlin.myvelobike.dto.ProfileResponse
import com.raistlin.myvelobike.dto.Ride
import com.raistlin.myvelobike.dto.Station
import com.raistlin.myvelobike.dto.TokenStorage
import com.raistlin.myvelobike.entity.toEntity
import com.raistlin.myvelobike.store.getAuthData
import com.raistlin.myvelobike.store.getLoginData
import com.raistlin.myvelobike.store.getQrator
import com.raistlin.myvelobike.store.saveAuthData
import com.raistlin.myvelobike.store.saveBalance
import com.raistlin.myvelobike.store.saveLastSync
import com.raistlin.myvelobike.store.saveQrator
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.charsets.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


class ApiService(private val context: Context, private val dao: RideDao) : Closeable {

    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val pwaClient = veloClient(true)
    private val mobileClient = veloClient(false)

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun login(login: String, pin: String): Authorization {
        checkQrator()
        val response = pwaClient.post(
            urlString = "https://pwa.velobike.ru/api/api-auth/authenticate"
        ) {
            veloCookies()
            contentType(ContentType.Application.Json)
            setBody(LoginData(login, pin))
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException("Received ${response.status.value}: ${response.status.description}")
        }
        val storage = response.body<TokenStorage>()
        val mobile = json.decodeFromString<MobileTokenStorage>(Base64.decode(storage.token.split(".")[1]).toString(Charset.defaultCharset()))
        return Authorization(storage.token, mobile.token)
    }

    private suspend fun profile(): Profile {
        val response = mobileClient.get(
            urlString = "https://apivelobike.velobike.ru/v2/profile"
        ) {
            parameter("rnd", System.currentTimeMillis().toString())
            parameter("timestamp", System.currentTimeMillis().toString())
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException("Received ${response.status.value}: ${response.status.description}")
        }
        return response.body<ProfileResponse>().profile
    }

    fun syncStats() = flow {
        context.saveBalance(profile().balance)
        var offset = 0
        do {
            val events = stats(STAT_LIMIT, offset)
            val rides = events.items.filterIsInstance<Ride>()
            val completed = dao.hasRide(rides.last().id)
            dao.insertRides(rides.toEntity())
            offset += STAT_LIMIT
            emit(offset)
        } while (events.hasMore && !completed)
    }

    private suspend fun stats(limit: Int, offset: Int): Events {
        val response = mobileClient.get(
            urlString = "https://apivelobike.velobike.ru/ride/history"
        ) {
            parameter("limit", limit)
            parameter("offset", offset)
        }
        if (!response.status.isSuccess()) throw RuntimeException("${response.status.value}: ${response.status.description}")
        return response.body() as Events
    }

    suspend fun syncStations() {
        checkQrator()
        val response = pwaClient.get(
            urlString = "https://pwa.velobike.ru/api/supabase/rest/v1/stations?select=*&type=in.%281%2C0%2C2%29&latitude=gt.55.05827365451158&latitude=lt.56.339365366745504&longitude=gt.37.13358349632464&longitude=lt.38.265175293199626"
        ) {
            veloCookies()
        }
        if (!response.status.isSuccess()) throw RuntimeException("${response.status.value}: ${response.status.description}")
        val stations = response.body<List<Station>>()

        withContext(Dispatchers.IO) {
            dao.clearStations()
            dao.insertStations(stations.filter { it.id > 0 }.toEntity())
            context.saveLastSync(System.currentTimeMillis())
        }
    }

    override fun close() {
        pwaClient.close()
        mobileClient.close()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun passQrator() {
        val qratorId = withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                val webView = WebView(context)
                webView.clearCache(true)
                webView.settings.apply { javaScriptEnabled = true }
                webView.loadUrl("https://pwa.velobike.ru/")
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        if (view.progress != 100) return

                        val cookies = CookieManager.getInstance().getCookie(url)
                        cookies.split(";").map { it.trim() }.find { it.contains("qrator_jsid") }?.split("=")?.get(1)?.let {
                            continuation.resume(it)
                        }
                    }
                }
            }
        }
        context.saveQrator(qratorId, System.currentTimeMillis())
    }

    private suspend fun checkQrator() {
        val (id, time) = context.getQrator().value
        if (id.isEmpty() || System.currentTimeMillis() - time > 86_400_000) {
            runCatching { passQrator() }
        }
    }

    private suspend fun HttpRequestBuilder.veloCookies() {
        header("ApiKey", API_KEY)
        cookie("qrator_jsid", context.getQrator().value.first)
    }

    private fun veloClient(pwa: Boolean) = HttpClient {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val auth = context.getAuthData().value
                    BearerTokens(if (pwa) auth.pwaToken else auth.mobileToken, "")
                }
                refreshTokens {
                    val loginData = context.getLoginData()
                    val auth = login(loginData.value.login, loginData.value.pin)
                    context.saveAuthData(auth)
                    BearerTokens(auth.pwaToken, "")
                }
                sendWithoutRequest { request ->
                    request.url.pathSegments.last() != "token" && request.url.pathSegments.last() != "authenticate"
                }
            }
        }
    }

    @Suppress("RegExpRedundantEscape")
    companion object {
        const val STAT_LIMIT = 15

        const val API_KEY =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3OTg3MjM3Iiwicm9sZXMiOlsiQ0xJRU5UIl0sIm1vYmlsZV90b2tlbiI6IlJiWkdwbHZ5a25HbGV1dG4yUVFmYmdwNjVLT2MwNUxRdGF5b2NJeXZxYyIsImV4dGVybmFsX2lkIjoiNTMzMjAxIiwidXNlcl90eXBlIjoiY2xpZW50Iiwibm1jX2FjY2Vzc190b2tlbiI6IlJiWkdwbHZ5a25HbGV1dG4yUVFmYmdwNjVLT2MwNUxRdGF5b2NJeXZxYyIsIm5tY19yZWZyZXNoX3Rva2VuIjoiZGdSd1gxWm9KZVJxVDNVcUJSc0VIVjg1aTg5eGhaQkRYWjNOTDU0bVI2bTR3OFRxIiwibm1jX3Rva2VuX3R5cGUiOiJCZWFyZXIiLCJubWNfdXNlcl9pZCI6IjUzMzIwMSIsIm5tY19leHBpcmVzX2luIjoiODY0MDAwIiwibm1jX2ZpcmViYXNlX3Rva2VuIjoiZXlKaGJHY2lPaUFpVWxNeU5UWWlMQ0FpZEhsd0lqb2dJa3BYVkNJc0lDSnJhV1FpT2lBaU9UZ3lNbVV6WlRobFpUSXdOV00wT1RCbE5UVTRZakV6WVdKak9HSmlZVGN3WmpVeVpqaGxNQ0o5LmV5SnBjM01pT2lBaVptbHlaV0poYzJVdFlXUnRhVzV6WkdzdE1HUm9aV1ZBZG1Wc2IySnBhMlV0WWprM05tWXVhV0Z0TG1kelpYSjJhV05sWVdOamIzVnVkQzVqYjIwaUxDQWljM1ZpSWpvZ0ltWnBjbVZpWVhObExXRmtiV2x1YzJSckxUQmthR1ZsUUhabGJHOWlhV3RsTFdJNU56Wm1MbWxoYlM1bmMyVnlkbWxqWldGalkyOTFiblF1WTI5dElpd2dJbUYxWkNJNklDSm9kSFJ3Y3pvdkwybGtaVzUwYVhSNWRHOXZiR3RwZEM1bmIyOW5iR1ZoY0dsekxtTnZiUzluYjI5bmJHVXVhV1JsYm5ScGRIa3VhV1JsYm5ScGRIbDBiMjlzYTJsMExuWXhMa2xrWlc1MGFYUjVWRzl2Ykd0cGRDSXNJQ0oxYVdRaU9pQWlOVE16TWpBeElpd2dJbWxoZENJNklERTNNVE00TmpZek9UWXNJQ0psZUhBaU9pQXhOekV6T0RZNU9UazJmUS5keHpWdVd5SjAwMl9vcW10Z010WlZrdllPQkQzdFY2OE14bFlSbDBsb095OVlkOTYxQWZ0akdYcXU3SjlQU0RwTFBFb3hFZi1sdF9waGZXZzJzYTJmeVRQa2t0NXBMRVRwWTdOMU1IM3c0SHZHRExwYW5NNGhSdXowNVR1REticTlzWUVNbmg1WUp3dTR1MnZZWEdpcjdhQVZFcXRqSkRHUElSci1HajZDR0FUM1dfeWM4X1JzTDdKOWQ5Y3VYNjdOR3h5V3M4dXFWMGNTSE1YdlNGTGxSOE1GNFVGZTBfQ2FWaDR6MWhWZllpb0FDdFEtdTVZLXBUMjk3cHE2a19jemtZZ3hPbmJKVzNGdmZrZEwzdk5mRnhuM21mTnIwRFM0T2MwTWd5Tlh6TmdWdEFWSWxTclRiZ2ZNRE1kWjMwaHMyT0dfbEhucy1LV0JzWVJ0THVzMXciLCJpYXQiOjE3MTM4NjYzOTcsImV4cCI6MTcxMzk1Mjc5N30.gJ6TVAB8eSx22hLTCu842ABiXtliLv7rc9whz-eVRQU"
    }
}