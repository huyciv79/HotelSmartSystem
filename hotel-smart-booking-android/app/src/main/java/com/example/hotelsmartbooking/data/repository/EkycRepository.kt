package com.example.hotelsmartbooking.data.repository

import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.core.network.api.HotelApiService
import com.example.hotelsmartbooking.domain.model.EkycStatusResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface EkycRepository {
    suspend fun getEkycStatus(): Flow<Resource<EkycStatusResponse>>
    suspend fun submitEkyc(
        frontFile: File,
        backFile: File,
        selfieFile: File,
        leftFile: File,
        rightFile: File,
        upFile: File,
        downFile: File
    ): Flow<Resource<EkycStatusResponse>>
}

@Singleton
class EkycRepositoryImpl @Inject constructor(
    private val apiService: HotelApiService
) : EkycRepository {

    override suspend fun getEkycStatus(): Flow<Resource<EkycStatusResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getEkycStatus()
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Success(EkycStatusResponse(verified = false)))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun submitEkyc(
        frontFile: File,
        backFile: File,
        selfieFile: File,
        leftFile: File,
        rightFile: File,
        upFile: File,
        downFile: File
    ): Flow<Resource<EkycStatusResponse>> = flow {
        emit(Resource.Loading)
        try {
            fun createFilePart(name: String, file: File): MultipartBody.Part {
                val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                return MultipartBody.Part.createFormData(name, file.name, reqFile)
            }

            val frontPart = createFilePart("frontImage", frontFile)
            val backPart = createFilePart("backImage", backFile)
            val selfiePart = createFilePart("selfieImage", selfieFile)
            val leftPart = createFilePart("leftImage", leftFile)
            val rightPart = createFilePart("rightImage", rightFile)
            val upPart = createFilePart("upImage", upFile)
            val downPart = createFilePart("downImage", downFile)

            val response = apiService.submitEkyc(frontPart, backPart, selfiePart, leftPart, rightPart, upPart, downPart)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Xác thực eKYC không thành công. Ảnh CCCD hoặc khuôn mặt không rõ nét."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi xác thực eKYC", e))
        }
    }
}
