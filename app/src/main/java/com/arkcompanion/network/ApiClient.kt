package com.arkcompanion.network

import com.arkcompanion.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import java.io.IOException

// Interceptor for graceful degradation & retry logic
class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        var response: Response? = null
        var responseOK = false
        var tryCount = 0

        while (!responseOK && tryCount < maxRetries) {
            try {
                response = chain.proceed(request)
                responseOK = response.isSuccessful
            } catch (e: Exception) {
                if (tryCount == maxRetries - 1) {
                    throw e
                }
            } finally {
                if (!responseOK && tryCount < maxRetries - 1) {
                    response?.close()
                    // Exponential backoff
                    try {
                        Thread.sleep((1000 * Math.pow(2.0, tryCount.toDouble())).toLong())
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }
            tryCount++
        }
        return response ?: throw IOException("Failed after $maxRetries retries")
    }
}

object ApiClient {
    private const val JSON_MIME_TYPE = "application/json"
    private const val ACCEPT_HEADER = "Accept"
    private const val USER_AGENT_HEADER = "User-Agent"

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header(ACCEPT_HEADER, JSON_MIME_TYPE)
                .header(USER_AGENT_HEADER, BuildConfig.API_USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(RetryInterceptor(3))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        })
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MIME_TYPE.toMediaType()))
        .build()
}
