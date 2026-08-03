package com.example.hotelsmartbooking.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.hotelsmartbooking.data.local.db.dao.BookingDao
import com.example.hotelsmartbooking.data.local.db.dao.RoomTypeDao
import com.example.hotelsmartbooking.data.local.db.entity.BookingEntity
import com.example.hotelsmartbooking.data.local.db.entity.RoomTypeEntity

@Database(
    entities = [RoomTypeEntity::class, BookingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HotelDatabase : RoomDatabase() {
    abstract fun roomTypeDao(): RoomTypeDao
    abstract fun bookingDao(): BookingDao
}
