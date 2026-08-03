package com.example.hotelsmartbooking.domain.model

import com.google.gson.annotations.SerializedName

data class BookingStateSchema(
    @SerializedName("step") val step: String = "idle",
    @SerializedName("hotel") val hotel: Map<String, Any>? = null,
    @SerializedName("room_type_id") val roomTypeId: Long? = null,
    @SerializedName("room_type_name") val roomTypeName: String = "",
    @SerializedName("check_in") val checkIn: String? = null,
    @SerializedName("check_out") val checkOut: String? = null,
    @SerializedName("adults") val adults: Int = 1,
    @SerializedName("children") val children: Int = 0,
    @SerializedName("check_in_method") val checkInMethod: String = "Manual",
    @SerializedName("special_requests") val specialRequests: String = "",
    @SerializedName("access_token") val accessToken: String = "",
    @SerializedName("booking_result") val bookingResult: Map<String, Any>? = null,
    @SerializedName("error") val error: String = "",
    @SerializedName("chat_history") val chatHistory: List<Map<String, String>> = emptyList()
)

data class AiChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("state") val state: BookingStateSchema
)

data class AiChatResponse(
    @SerializedName("response") val response: String,
    @SerializedName("state") val state: BookingStateSchema,
    @SerializedName("search_results") val searchResults: Map<String, Any>? = null
)

data class AiStartBookingRequest(
    @SerializedName("hotel") val hotel: Map<String, Any>,
    @SerializedName("state") val state: BookingStateSchema
)

data class AiCancelBookingRequest(
    @SerializedName("state") val state: BookingStateSchema
)
