package com.example.hotelsmartbooking.data.repository

import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.core.network.api.AiApiService
import com.example.hotelsmartbooking.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface AiRepository {
    suspend fun chat(message: String, currentState: BookingStateSchema): Flow<Resource<AiChatResponse>>
    suspend fun startBooking(hotel: Map<String, Any>, currentState: BookingStateSchema): Flow<Resource<AiChatResponse>>
    suspend fun cancelBooking(currentState: BookingStateSchema): Flow<Resource<AiChatResponse>>
}

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val aiApiService: AiApiService
) : AiRepository {

    override suspend fun chat(message: String, currentState: BookingStateSchema): Flow<Resource<AiChatResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = aiApiService.chat(AiChatRequest(message, currentState))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Trợ lý AI đang bận, vui lòng thử lại sau."))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Không thể kết nối đến Trợ lý AI (${e.localizedMessage})", e))
        }
    }

    override suspend fun startBooking(hotel: Map<String, Any>, currentState: BookingStateSchema): Flow<Resource<AiChatResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = aiApiService.startBooking(AiStartBookingRequest(hotel, currentState))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Không thể bắt đầu đặt phòng qua AI."))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi kết nối AI (${e.localizedMessage})", e))
        }
    }

    override suspend fun cancelBooking(currentState: BookingStateSchema): Flow<Resource<AiChatResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = aiApiService.cancelBooking(AiCancelBookingRequest(currentState))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Không thể hủy quy trình AI."))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi kết nối AI (${e.localizedMessage})", e))
        }
    }
}
