package com.example.hotelsmartbooking.data.local.db.dao

import androidx.room.*
import com.example.hotelsmartbooking.data.local.db.entity.BookingEntity
import com.example.hotelsmartbooking.data.local.db.entity.RoomTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomTypeDao {
    @Query("SELECT * FROM saved_room_types ORDER BY id DESC")
    fun getAllFavoriteRoomTypes(): Flow<List<RoomTypeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_room_types WHERE id = :roomTypeId)")
    suspend fun isFavorite(roomTypeId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(roomType: RoomTypeEntity)

    @Query("DELETE FROM saved_room_types WHERE id = :roomTypeId")
    suspend fun deleteFavorite(roomTypeId: Long)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM cached_bookings ORDER BY id DESC")
    fun getCachedBookings(): Flow<List<BookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<BookingEntity>)

    @Query("DELETE FROM cached_bookings")
    suspend fun clearBookings()
}
