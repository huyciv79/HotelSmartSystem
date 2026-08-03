package com.example.hotelsmartbooking.core.network.api

import com.example.hotelsmartbooking.domain.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AiApiService {

    @GET("health")
    suspend fun healthCheck(): Response<Map<String, String>>

    @POST("chat")
    suspend fun chat(@Body request: AiChatRequest): Response<AiChatResponse>

    @POST("start-booking")
    suspend fun startBooking(@Body request: AiStartBookingRequest): Response<AiChatResponse>

    @POST("cancel-booking")
    suspend fun cancelBooking(@Body request: AiCancelBookingRequest): Response<AiChatResponse>
}
