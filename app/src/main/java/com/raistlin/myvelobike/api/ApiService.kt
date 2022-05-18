package com.raistlin.myvelobike.api

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.raistlin.myvelobike.dao.RideDao
import com.raistlin.myvelobike.dto.Authorization
import com.raistlin.myvelobike.dto.Events
import com.raistlin.myvelobike.dto.Ride
import com.raistlin.myvelobike.dto.Station
import com.raistlin.myvelobike.entity.toEntity
import com.raistlin.myvelobike.store.getAuthData
import com.raistlin.myvelobike.store.getLoginData
import com.raistlin.myvelobike.store.saveAuthData
import com.raistlin.myvelobike.util.JSONUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ApiService(private val context: Context, private val dao: RideDao) : Closeable {

    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val client = HttpClient {
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
                    BearerTokens(auth.token, auth.refreshToken)
                }
                refreshTokens {
                    val loginData = context.getLoginData()
                    val auth = login(loginData.value.login, loginData.value.pin)
                    context.saveAuthData(auth)
                    BearerTokens(auth.token, auth.refreshToken)
                }
                sendWithoutRequest { request ->
                    request.url.pathSegments.last() != "token"
                }
            }
        }
    }

    suspend fun login(login: String, pin: String): Authorization {
        val response = client.submitForm(
            url = "https://apivelobike.velobike.ru/v3/oauth/token",
            formParameters = Parameters.build {
                append("grant_type", "password")
                append("username", login)
                append("password", pin)
                append("client_id", "L3bCri!NzYY3e!raFgw@LhC8zHvGXbY48j@QDe7gkFxahvkg")
                append("client_secret", "Kd!ygCM9CJMMCyNqRfY@U932KfzrNrFrBk-bHPTcxo@*BzL8XcGWbPJ!jb-!9oNGGogP7TVWazN*cp@KmmseBnkKW6aHL4rKPrX983DBdVsrZuD43uAF-FDm")
            }) {
            parameter("source", "mobile_app")
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException("Received ${response.status.value}: ${response.status.description}")
        }
        return response.body()
    }

    fun syncStats() = flow {
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
        val response = client.get(
            urlString = "https://apivelobike.velobike.ru/ride/history"
        ) {
            parameter("limit", limit)
            parameter("offset", offset)
        }
        if (!response.status.isSuccess()) throw RuntimeException("${response.status.value}: ${response.status.description}")
        return response.body() as Events
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun syncStations() {
        val stations = withContext(Dispatchers.Main) {
            suspendCoroutine<List<Station>> { continuation ->
                val webView = WebView(context)
                webView.clearCache(true)
                webView.settings.apply { javaScriptEnabled = true }
                webView.loadUrl("https://velobike.ru/ajax/parkings/")
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript("(function() { return (document.getElementsByTagName('html')[0].innerHTML); })();") { html ->
                            val items = STATIONS_REGEX.find(JSONUtils.unescape(html))?.groupValues?.get(1) ?: return@evaluateJavascript // throw RuntimeException("Unable to extract data from $html")
                            continuation.resume(json.decodeFromString(items))
                        }
                    }
                }
            }
        }
        withContext(Dispatchers.IO) {
            dao.insertStations(stations.toEntity())
        }
    }

    override fun close() {
        client.close()
    }

    @Suppress("RegExpRedundantEscape")
    companion object {
        const val STAT_LIMIT = 15
        val STATIONS_REGEX = "\\{\"Items\": (\\[.*\\])\\}".toRegex()
    }
}