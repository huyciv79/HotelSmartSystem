package com.example.hotelsmartbooking.di

import android.content.Context
import androidx.room.Room
import com.example.hotelsmartbooking.data.local.db.HotelDatabase
import com.example.hotelsmartbooking.data.local.db.dao.BookingDao
import com.example.hotelsmartbooking.data.local.db.dao.RoomTypeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHotelDatabase(
        @ApplicationContext context: Context
    ): HotelDatabase = Room.databaseBuilder(
        context,
        HotelDatabase::class.java,
        "elysian_hotel.db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideRoomTypeDao(database: HotelDatabase): RoomTypeDao = database.roomTypeDao()

    @Provides
    fun provideBookingDao(database: HotelDatabase): BookingDao = database.bookingDao()
}
