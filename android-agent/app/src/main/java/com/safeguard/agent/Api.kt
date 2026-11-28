package com.safeguard.agent

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class LoginRequest(val email: String, val password: String)
data class PairRequest(val email: String, val password: String, val name: String, val platform: String = "Android", val clientKey: String)
data class RegisterAgentRequest(val deviceId: String, val platform: String = "Android", val osVersion: String, val fcmToken: String, val enrollmentMode: String)
data class LocationRequest(val latitude: Double, val longitude: Double, val accuracy: Float)
data class AckRequest(val commandId: String)
data class LoginResponse(val user: UserDto? = null, val error: String? = null)
data class UserDto(val id: String? = null, val fullName: String? = null, val email: String? = null, val role: String? = null)
data class PairResponse(val deviceToken: String? = null, val device: DeviceDto? = null, val error: String? = null)
data class DeviceDto(val id: String? = null, val name: String? = null)
data class RegisterResponse(val registered: Boolean = false, val deviceId: String? = null, val agentToken: String? = null, val error: String? = null)
data class CommandsResponse(val commands: List<DeviceCommand> = emptyList())
data class DeviceCommand(val id: String, val action: String, val enabled: Boolean = true)
data class ApiError(val error: String? = null)

interface SafeguardApi {
    @POST("api/auth/login") suspend fun login(@Body request: LoginRequest): retrofit2.Response<LoginResponse>
    @GET("api/phone/state") suspend fun state(): retrofit2.Response<Map<String, Any>>
    @POST("api/devices/pair-account") suspend fun pair(@Body request: PairRequest): retrofit2.Response<PairResponse>
    @POST("api/devices/register-agent") suspend fun registerAgent(@Body request: RegisterAgentRequest): retrofit2.Response<RegisterResponse>
    @GET("api/phone/commands") suspend fun commands(): retrofit2.Response<CommandsResponse>
    @POST("api/phone/commands/ack") suspend fun acknowledge(@Body request: AckRequest): retrofit2.Response<Map<String, Any>>
    @POST("api/phone/location") suspend fun location(@Body request: LocationRequest): retrofit2.Response<Map<String, Any>>
    @POST("api/phone/fcm-token") suspend fun updateFcmToken(@Body body: Map<String, String>): retrofit2.Response<Map<String, Any>>
    @POST("api/phone/unpair") suspend fun unpair(): retrofit2.Response<Map<String, Any>>
    @POST("api/devices/{id}/sim-alert") suspend fun reportSimAlert(@Path("id") id: String, @Body body: Map<String, String>): retrofit2.Response<Map<String, Any>>
}

object Api {
    private val gson = Gson()
    private val cookieRecordType = object : TypeToken<List<CookieRecord>>() {}.type
    private lateinit var context: Context
    lateinit var service: SafeguardApi
        private set

    fun initialize(appContext: Context) {
        if (::service.isInitialized) return
        context = appContext.applicationContext
        SecureStore.initialize(context)
        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val records = readCookies().filterNot { old -> cookies.any { it.name == old.name && it.domain == old.domain && it.path == old.path } }
                    .toMutableList()
                records += cookies.map { CookieRecord.from(it) }
                SecureStore.put("session_cookies", gson.toJson(records))
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val records = readCookies().filter { it.expiresAt > System.currentTimeMillis() }
                if (records.size != readCookies().size) SecureStore.put("session_cookies", gson.toJson(records))
                return records.mapNotNull { it.toCookie(url) }
            }

            private fun readCookies(): List<CookieRecord> = try {
                gson.fromJson<List<CookieRecord>>(SecureStore.get("session_cookies", "[]"), cookieRecordType) ?: emptyList()
            } catch (_: Exception) { emptyList() }
        }
        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                SecureStore.agentToken().takeIf(String::isNotBlank)?.let { request.header("Authorization", "Bearer $it") }
                chain.proceed(request.build())
            }
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).writeTimeout(20, TimeUnit.SECONDS)
            .build()
        service = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(client).addConverterFactory(GsonConverterFactory.create(gson)).build().create(SafeguardApi::class.java)
    }

    fun error(response: retrofit2.Response<*>): String {
        val raw = response.errorBody()?.string()
        return try { gson.fromJson(raw, ApiError::class.java)?.error ?: "Request failed (${response.code()})." }
        catch (_: Exception) { "Request failed (${response.code()})." }
    }
}

private data class CookieRecord(
    val name: String, val value: String, val domain: String, val path: String,
    val expiresAt: Long, val secure: Boolean, val httpOnly: Boolean,
    val hostOnly: Boolean, val persistent: Boolean
) {
    fun toCookie(url: HttpUrl): Cookie? = try {
        val builder = Cookie.Builder().name(name).value(value).path(path)
        if (hostOnly) builder.hostOnlyDomain(domain) else builder.domain(domain)
        if (persistent) builder.expiresAt(expiresAt)
        if (secure) builder.secure()
        if (httpOnly) builder.httpOnly()
        builder.build().takeIf { it.matches(url) }
    } catch (_: Exception) { null }

    companion object {
        fun from(cookie: Cookie) = CookieRecord(cookie.name, cookie.value, cookie.domain, cookie.path, cookie.expiresAt, cookie.secure, cookie.httpOnly, cookie.hostOnly, cookie.persistent)
    }
}
