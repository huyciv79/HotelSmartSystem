package com.example.hotelsmartbooking.domain.model

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String = "INFO",
    @SerializedName("read") val read: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null
)
