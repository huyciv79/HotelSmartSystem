package com.example.hotelsmartbooking.data.repository

import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.core.network.api.HotelApiService
import com.example.hotelsmartbooking.domain.model.NotificationResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationRepository {
    suspend fun getNotifications(): Flow<Resource<List<NotificationResponse>>>
    suspend fun getUnreadCount(): Flow<Resource<Int>>
    suspend fun markAsRead(id: Long): Flow<Resource<String>>
    suspend fun markAllAsRead(): Flow<Resource<String>>
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val apiService: HotelApiService
) : NotificationRepository {

    override suspend fun getNotifications(): Flow<Resource<List<NotificationResponse>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getNotifications()
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Success(emptyList()))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun getUnreadCount(): Flow<Resource<Int>> = flow {
        try {
            val response = apiService.getUnreadCount()
            if (response.isSuccessful && response.body()?.data != null) {
                val count = response.body()!!.data!!["unreadCount"] ?: 0
                emit(Resource.Success(count))
            } else {
                emit(Resource.Success(0))
            }
        } catch (e: Exception) {
            emit(Resource.Success(0))
        }
    }

    override suspend fun markAsRead(id: Long): Flow<Resource<String>> = flow {
        try {
            val response = apiService.markNotificationRead(id)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun markAllAsRead(): Flow<Resource<String>> = flow {
        try {
            val response = apiService.markAllNotificationsRead()
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }
}
