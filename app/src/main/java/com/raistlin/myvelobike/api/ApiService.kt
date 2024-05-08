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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.Closeable
import kotlin.coroutines.resume
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
            if (response.status.value == 403) {
                passQrator()
                return login(login, pin)
            } else {
                throw RuntimeException("Received ${response.status.value}: ${response.status.description}")
            }
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
            suspendCancellableCoroutine { continuation ->
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
                            if (continuation.isActive) {
                                continuation.resume(it)
                            }
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
        header("ApiKey", context.getAuthData().value.pwaToken)
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
                    runCatching {
                        val loginData = context.getLoginData()
                        val auth = login(loginData.value.login, loginData.value.pin)
                        context.saveAuthData(auth)
                        BearerTokens(auth.pwaToken, "")
                    }.getOrNull()
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
    }
}