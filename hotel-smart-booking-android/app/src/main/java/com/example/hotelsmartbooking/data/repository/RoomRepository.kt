package com.example.hotelsmartbooking.data.repository

import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.core.network.api.HotelApiService
import com.example.hotelsmartbooking.data.local.db.dao.RoomTypeDao
import com.example.hotelsmartbooking.data.local.db.entity.RoomTypeEntity
import com.example.hotelsmartbooking.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface RoomRepository {
    suspend fun getRoomTypes(): Flow<Resource<List<RoomTypeSummary>>>
    suspend fun getRoomTypeDetail(id: Long): Flow<Resource<RoomTypeDetail>>
    suspend fun filterRoomTypes(criteria: RoomFilterCriteria): Flow<Resource<List<RoomTypeSummary>>>
    suspend fun getFeedbacks(roomTypeId: Long? = null): Flow<Resource<List<FeedbackItem>>>
    suspend fun submitFeedback(request: FeedbackRequest): Flow<Resource<FeedbackItem>>
    fun getFavorites(): Flow<List<RoomTypeEntity>>
    suspend fun isFavorite(id: Long): Boolean
    suspend fun toggleFavorite(roomType: RoomTypeSummary)
}

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val apiService: HotelApiService,
    private val roomTypeDao: RoomTypeDao
) : RoomRepository {

    override suspend fun getRoomTypes(): Flow<Resource<List<RoomTypeSummary>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getRoomTypes()
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Không thể tải danh sách loại phòng"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối máy chủ", e))
        }
    }

    override suspend fun getRoomTypeDetail(id: Long): Flow<Resource<RoomTypeDetail>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getRoomTypeDetail(id)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Không tìm thấy thông tin phòng"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun filterRoomTypes(criteria: RoomFilterCriteria): Flow<Resource<List<RoomTypeSummary>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.filterRoomTypes(criteria)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Lỗi lọc danh sách phòng"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun getFeedbacks(roomTypeId: Long?): Flow<Resource<List<FeedbackItem>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getFeedbacks(roomTypeId)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Success(emptyList()))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun submitFeedback(request: FeedbackRequest): Flow<Resource<FeedbackItem>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.submitFeedback(request)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Không thể gửi đánh giá"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override fun getFavorites(): Flow<List<RoomTypeEntity>> = roomTypeDao.getAllFavoriteRoomTypes()

    override suspend fun isFavorite(id: Long): Boolean = roomTypeDao.isFavorite(id)

    override suspend fun toggleFavorite(roomType: RoomTypeSummary) {
        val exists = roomTypeDao.isFavorite(roomType.id)
        if (exists) {
            roomTypeDao.deleteFavorite(roomType.id)
        } else {
            roomTypeDao.insertFavorite(
                RoomTypeEntity(
                    id = roomType.id,
                    name = roomType.name,
                    basePrice = roomType.basePrice,
                    dynamicPrice = roomType.dynamicPrice,
                    adultCapacity = roomType.adultCapacity,
                    childCapacity = roomType.childCapacity,
                    totalCapacity = roomType.totalCapacity,
                    description = roomType.description,
                    thumbnail = roomType.thumbnail,
                    averageRating = roomType.averageRating,
                    reviewCount = roomType.reviewCount,
                    isFavorite = true
                )
            )
        }
    }
}
