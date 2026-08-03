package com.example.hotelsmartbooking.data.repository

import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.core.network.api.HotelApiService
import com.example.hotelsmartbooking.data.local.db.dao.BookingDao
import com.example.hotelsmartbooking.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface BookingRepository {
    suspend fun createBooking(request: CreateBookingRequest): Flow<Resource<BookingResponse>>
    suspend fun getBookingDetail(id: Long): Flow<Resource<BookingResponse>>
    suspend fun getBookingHistory(): Flow<Resource<List<BookingHistoryResponse>>>
    suspend fun cancelBooking(id: Long, reason: String = "Khách hàng hủy trên app"): Flow<Resource<String>>
    suspend fun getInvoice(id: Long): Flow<Resource<InvoiceResponse>>
    suspend fun generateQrToken(id: Long): Flow<Resource<QrTokenResponse>>
    suspend fun checkInWithQr(token: String): Flow<Resource<String>>
    suspend fun requestExtension(request: ExtensionRequest): Flow<Resource<String>>
    suspend fun requestRoomChange(request: RoomChangeRequest): Flow<Resource<String>>
    suspend fun requestEarlyCheckout(request: EarlyCheckoutRequest): Flow<Resource<String>>
    suspend fun requestRefund(bookingId: Long, reason: String): Flow<Resource<String>>
    suspend fun createPaypalOrder(bookingId: Long): Flow<Resource<PaymentPaypalOrderResponse>>
    suspend fun capturePaypalOrder(orderId: String, bookingId: Long): Flow<Resource<String>>
    suspend fun createVnPayUrl(bookingId: Long, bankCode: String? = null): Flow<Resource<PaymentVnPayResponse>>
}

@Singleton
class BookingRepositoryImpl @Inject constructor(
    private val apiService: HotelApiService,
    private val bookingDao: BookingDao
) : BookingRepository {

    override suspend fun createBooking(request: CreateBookingRequest): Flow<Resource<BookingResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createBooking(request)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Đặt phòng thất bại. Vui lòng kiểm tra lại ngày và loại phòng."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun getBookingDetail(id: Long): Flow<Resource<BookingResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getBookingDetail(id)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Không tìm thấy thông tin đơn đặt"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun getBookingHistory(): Flow<Resource<List<BookingHistoryResponse>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getBookingHistory()
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Không thể lấy lịch sử đặt phòng"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun cancelBooking(id: Long, reason: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.cancelBooking(id, mapOf("cancellationReason" to reason))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Hủy đặt phòng không thành công"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun getInvoice(id: Long): Flow<Resource<InvoiceResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getInvoice(id)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Không thể xem hóa đơn"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun generateQrToken(id: Long): Flow<Resource<QrTokenResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.generateQrToken(id)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Tài khoản chưa hoàn tất eKYC hoặc đơn hàng chưa hợp lệ"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi tạo QR Token", e))
        }
    }

    override suspend fun checkInWithQr(token: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.checkInWithQr(QrCheckInRequest(token))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Check-in QR thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun requestExtension(request: ExtensionRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.requestExtension(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Không thể gửi yêu cầu gia hạn"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun requestRoomChange(request: RoomChangeRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.requestRoomChange(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Không thể gửi yêu cầu đổi phòng"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun requestEarlyCheckout(request: EarlyCheckoutRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.requestEarlyCheckout(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Không thể gửi yêu cầu trả phòng sớm"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun requestRefund(bookingId: Long, reason: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.requestRefund(bookingId, RefundRequest(bookingId, reason))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Không thể tạo yêu cầu hoàn tiền"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun createPaypalOrder(bookingId: Long): Flow<Resource<PaymentPaypalOrderResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createPaypalOrder(PaymentPaypalCreateOrderRequest(bookingId))
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Không thể khởi tạo thanh toán PayPal"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi thanh toán", e))
        }
    }

    override suspend fun capturePaypalOrder(orderId: String, bookingId: Long): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.capturePaypalOrder(PaymentPaypalCaptureRequest(orderId, bookingId))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Xác nhận thanh toán PayPal không thành công"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi thanh toán", e))
        }
    }

    override suspend fun createVnPayUrl(bookingId: Long, bankCode: String?): Flow<Resource<PaymentVnPayResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createVnPayUrl(PaymentVnPayRequest(bookingId, bankCode))
            if (response.isSuccessful && response.body()?.data != null) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error("Không thể khởi tạo cổng thanh toán VNPay"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi thanh toán", e))
        }
    }
}
