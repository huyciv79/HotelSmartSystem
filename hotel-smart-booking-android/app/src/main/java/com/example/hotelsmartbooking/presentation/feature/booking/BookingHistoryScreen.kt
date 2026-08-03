package com.example.hotelsmartbooking.presentation.feature.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hotelsmartbooking.domain.model.BookingHistoryResponse
import com.example.hotelsmartbooking.presentation.components.ElysianTopAppBar
import com.example.hotelsmartbooking.presentation.components.EmptyStateView
import com.example.hotelsmartbooking.presentation.components.ErrorStateView
import com.example.hotelsmartbooking.presentation.components.RoomCardShimmer
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BookingHistoryScreen(
    viewModel: BookingViewModel,
    onNavigateToDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    LaunchedEffect(Unit) {
        viewModel.loadBookingHistory()
    }

    Scaffold(
        topBar = {
            ElysianTopAppBar(title = "Đơn đặt phòng của tôi")
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
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        items(3) { RoomCardShimmer() }
                    }
                }
                is BookingUiState.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetry = { viewModel.loadBookingHistory() }
                    )
                }
                is BookingUiState.HistorySuccess -> {
                    if (state.bookings.isEmpty()) {
                        EmptyStateView(
                            title = "Chưa có đơn đặt phòng nào",
                            message = "Khám phá phòng ngay và bắt đầu chuyến du lịch của bạn!"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            items(state.bookings) { booking ->
                                BookingItemCard(
                                    booking = booking,
                                    currencyFormatter = currencyFormatter,
                                    onClick = { onNavigateToDetail(booking.id) }
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun BookingItemCard(
    booking: BookingHistoryResponse,
    currencyFormatter: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mã: ${booking.bookingReference}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (booking.status) {
                        "Confirmed", "Checked-in" -> MaterialTheme.colorScheme.primaryContainer
                        "Completed" -> MaterialTheme.colorScheme.secondaryContainer
                        "Cancelled" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = booking.status,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = booking.roomTypeName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Thời gian: ${booking.checkInDate} ➔ ${booking.checkOutDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currencyFormatter.format(booking.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(onClick = onClick) {
                    Text("Chi tiết đơn")
                }
            }
        }
    }
}
