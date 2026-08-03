package com.example.hotelsmartbooking.core.network

import com.example.hotelsmartbooking.data.local.datastore.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val userPreferences: UserPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip adding Authorization header for public auth endpoints
        if (path.contains("/auth/login") ||
            path.contains("/auth/register") ||
            path.contains("/auth/verify-otp") ||
            path.contains("/auth/forgot-password") ||
            path.contains("/auth/verify-forgot-otp") ||
            path.contains("/auth/reset-password")
        ) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking { userPreferences.getAccessTokenSync() }
        val requestBuilder = originalRequest.newBuilder()

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
