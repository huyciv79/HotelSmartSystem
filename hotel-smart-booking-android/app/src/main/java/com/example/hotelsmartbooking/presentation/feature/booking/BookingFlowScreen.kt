package com.example.hotelsmartbooking.presentation.feature.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hotelsmartbooking.presentation.components.ElysianTopAppBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BookingFlowScreen(
    roomTypeId: Long,
    viewModel: BookingViewModel,
    onNavigateBack: () -> Unit,
    onBookingSuccess: (Long) -> Unit
) {
    val today = remember { LocalDate.now() }
    val tomorrow = remember { today.plusDays(1) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    var checkInDate by remember { mutableStateOf(today.format(dateFormatter)) }
    var checkOutDate by remember { mutableStateOf(tomorrow.format(dateFormatter)) }
    var adults by remember { mutableIntStateOf(1) }
    var children by remember { mutableIntStateOf(0) }
    var selectedMethod by remember { mutableStateOf("Manual") }
    var specialRequests by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is BookingUiState.CreatedSuccess) {
            val createdId = (uiState as BookingUiState.CreatedSuccess).booking.id
            viewModel.resetState()
            onBookingSuccess(createdId)
        }
    }

    Scaffold(
        topBar = {
            ElysianTopAppBar(
                title = "Xác nhận đặt phòng",
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
                        Text(
                            text = "Thời gian lưu trú",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = checkInDate,
                                onValueChange = { checkInDate = it },
                                label = { Text("Ngày check-in") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = checkOutDate,
                                onValueChange = { checkOutDate = it },
                                label = { Text("Ngày check-out") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Người lớn", style = MaterialTheme.typography.bodyLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedIconButton(onClick = { if (adults > 1) adults-- }) { Text("-") }
                                Text(text = "$adults", modifier = Modifier.padding(horizontal = 12.dp))
                                OutlinedIconButton(onClick = { adults++ }) { Text("+") }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Trẻ em", style = MaterialTheme.typography.bodyLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedIconButton(onClick = { if (children > 0) children-- }) { Text("-") }
                                Text(text = "$children", modifier = Modifier.padding(horizontal = 12.dp))
                                OutlinedIconButton(onClick = { children++ }) { Text("+") }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Hình thức Check-in",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedMethod == "Manual",
                                onClick = { selectedMethod = "Manual" }
                            )
                            Text("Check-in trực tiếp tại Lễ tân")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedMethod == "QR",
                                onClick = { selectedMethod = "QR" }
                            )
                            Text("Tự check-in bằng QR Code / FaceID (Yêu cầu eKYC)")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = specialRequests,
                    onValueChange = { specialRequests = it },
                    label = { Text("Yêu cầu đặc biệt (Không bắt buộc)") },
                    placeholder = { Text("Ví dụ: Phòng tầng cao, 1 giường lớn...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState is BookingUiState.Error) {
                    Text(
                        text = (uiState as BookingUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        viewModel.createBooking(
                            roomTypeId = roomTypeId,
                            checkInDate = checkInDate,
                            checkOutDate = checkOutDate,
                            adults = adults,
                            children = children,
                            checkInMethod = selectedMethod,
                            specialRequests = specialRequests
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState !is BookingUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState is BookingUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("XÁC NHẬN ĐẶT PHÒNG", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
