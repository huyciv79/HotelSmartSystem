package com.example.hotelsmartbooking.presentation.feature.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hotelsmartbooking.presentation.components.ElysianTopAppBar
import com.example.hotelsmartbooking.presentation.components.ErrorStateView
import com.example.hotelsmartbooking.presentation.components.RoomCardShimmer
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BookingDetailScreen(
    bookingId: Long,
    viewModel: BookingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToQrModal: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    LaunchedEffect(bookingId) {
        viewModel.loadBookingDetail(bookingId)
    }

    Scaffold(
        topBar = {
            ElysianTopAppBar(
                title = "Chi tiết đơn đặt phòng",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is BookingUiState.Loading -> {
                    Column(modifier = Modifier.padding(16.dp)) { RoomCardShimmer() }
                }
                is BookingUiState.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetry = { viewModel.loadBookingDetail(bookingId) }
                    )
                }
                is BookingUiState.DetailSuccess -> {
                    val booking = state.booking
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Mã đặt phòng",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = booking.bookingReference,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = booking.roomTypeName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                if (!booking.roomNumber.isNullOrEmpty()) {
                                    Text(
                                        text = "Phòng số: ${booking.roomNumber}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Check-in: ${booking.checkInDate}")
                                Text(text = "Check-out: ${booking.checkOutDate}")
                                Text(text = "Phương thức Check-in: ${booking.checkInMethod}")
                                Text(text = "Trạng thái: ${booking.status}", fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Tổng tiền thanh toán", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = currencyFormatter.format(booking.totalAmount),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // QR Check-in Token Button
                        if (booking.checkInMethod == "QR" || booking.status == "Confirmed") {
                            Button(
                                onClick = { onNavigateToQrModal(booking.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("HIỂN THỊ MÃ QR CHECK-IN", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (booking.status == "Pending" || booking.status == "Confirmed") {
                            OutlinedButton(
                                onClick = { viewModel.cancelBooking(booking.id, "Khách hàng hủy trên app") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("HỦY ĐƠN ĐẶT PHÒNG", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
