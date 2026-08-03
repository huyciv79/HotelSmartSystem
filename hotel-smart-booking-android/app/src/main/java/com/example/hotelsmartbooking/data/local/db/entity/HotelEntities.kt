package com.example.hotelsmartbooking.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_room_types")
data class RoomTypeEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val basePrice: Double,
    val dynamicPrice: Double?,
    val adultCapacity: Int,
    val childCapacity: Int,
    val totalCapacity: Int,
    val description: String?,
    val thumbnail: String?,
    val averageRating: Double,
    val reviewCount: Int,
    val isFavorite: Boolean = true
)

@Entity(tableName = "cached_bookings")
data class BookingEntity(
    @PrimaryKey val id: Long,
    val bookingReference: String,
    val roomTypeName: String,
    val checkInDate: String,
    val checkOutDate: String,
    val status: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val thumbnail: String?
)
