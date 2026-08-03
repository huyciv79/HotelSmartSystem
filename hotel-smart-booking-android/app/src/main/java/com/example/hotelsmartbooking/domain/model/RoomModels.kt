package com.example.hotelsmartbooking.domain.model

import com.google.gson.annotations.SerializedName

data class RoomTypeSummary(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("basePrice") val basePrice: Double,
    @SerializedName("dynamicPrice") val dynamicPrice: Double? = null,
    @SerializedName("adultCapacity") val adultCapacity: Int,
    @SerializedName("childCapacity") val childCapacity: Int,
    @SerializedName("totalCapacity") val totalCapacity: Int = adultCapacity + childCapacity,
    @SerializedName("description") val description: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("averageRating") val averageRating: Double = 5.0,
    @SerializedName("reviewCount") val reviewCount: Int = 0,
    @SerializedName("amenities") val amenities: List<String> = emptyList()
)

data class RoomTypeDetail(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("basePrice") val basePrice: Double,
    @SerializedName("dynamicPrice") val dynamicPrice: Double? = null,
    @SerializedName("adultCapacity") val adultCapacity: Int,
    @SerializedName("childCapacity") val childCapacity: Int,
    @SerializedName("totalCapacity") val totalCapacity: Int,
    @SerializedName("description") val description: String? = null,
    @SerializedName("status") val status: String = "Active",
    @SerializedName("images") val images: List<String> = emptyList(),
    @SerializedName("amenities") val amenities: List<String> = emptyList(),
    @SerializedName("reviews") val reviews: List<FeedbackItem> = emptyList()
)

data class FeedbackItem(
    @SerializedName("id") val id: Long,
    @SerializedName("userFullName") val userFullName: String,
    @SerializedName("userAvatar") val userAvatar: String? = null,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class FeedbackRequest(
    @SerializedName("bookingId") val bookingId: Long,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String
)

data class RoomFilterCriteria(
    @SerializedName("checkInDate") val checkInDate: String? = null,
    @SerializedName("checkOutDate") val checkOutDate: String? = null,
    @SerializedName("minPrice") val minPrice: Double? = null,
    @SerializedName("maxPrice") val maxPrice: Double? = null,
    @SerializedName("adults") val adults: Int? = null,
    @SerializedName("children") val children: Int? = null,
    @SerializedName("amenities") val amenities: List<String>? = null
)
