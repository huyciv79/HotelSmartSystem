package com.example.hotelsmartbooking.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.hotelsmartbooking.presentation.components.ElysianBottomNavigation
import com.example.hotelsmartbooking.presentation.feature.ai.AiChatScreen
import com.example.hotelsmartbooking.presentation.feature.ai.AiChatViewModel
import com.example.hotelsmartbooking.presentation.feature.auth.*
import com.example.hotelsmartbooking.presentation.feature.booking.*
import com.example.hotelsmartbooking.presentation.feature.ekyc.EkycVerificationScreen
import com.example.hotelsmartbooking.presentation.feature.ekyc.EkycViewModel
import com.example.hotelsmartbooking.presentation.feature.home.*
import com.example.hotelsmartbooking.presentation.feature.hotel.RoomTypeDetailScreen
import com.example.hotelsmartbooking.presentation.feature.hotel.RoomTypeDetailViewModel
import com.example.hotelsmartbooking.presentation.feature.profile.ProfileScreen
import com.example.hotelsmartbooking.presentation.feature.profile.ProfileViewModel
import com.example.hotelsmartbooking.presentation.feature.qr.QrCheckInScreen

object NavRoutes {
    const val AUTH_GRAPH = "auth_graph"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val OTP = "otp/{email}"
    const val FORGOT_PASSWORD = "forgot_password"

    const val MAIN_GRAPH = "main_graph"
    const val HOME = "home"
    const val SEARCH = "search"
    const val AI_CHAT = "ai_chat"
    const val BOOKINGS = "bookings"
    const val PROFILE = "profile"

    const val ROOM_DETAIL = "room_detail/{roomTypeId}"
    const val BOOKING_FLOW = "booking_flow/{roomTypeId}"
    const val BOOKING_DETAIL = "booking_detail/{bookingId}"
    const val QR_CHECKIN = "qr_checkin/{bookingId}"
    const val EKYC = "ekyc"
}

@Composable
fun RootNavHost(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)
    val startDestination = if (isLoggedIn) NavRoutes.MAIN_GRAPH else NavRoutes.AUTH_GRAPH

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Authentication Graph
        navigation(
            startDestination = NavRoutes.LOGIN,
            route = NavRoutes.AUTH_GRAPH
        ) {
            composable(NavRoutes.LOGIN) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(NavRoutes.MAIN_GRAPH) {
                            popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) },
                    onNavigateToForgotPassword = { navController.navigate(NavRoutes.FORGOT_PASSWORD) }
                )
            }

            composable(NavRoutes.REGISTER) {
                RegisterScreen(
                    viewModel = authViewModel,
                    onNavigateToOtp = { email ->
                        navController.navigate("otp/$email")
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable(
                route = NavRoutes.OTP,
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                OtpVerificationScreen(
                    email = email,
                    viewModel = authViewModel,
                    onVerifySuccess = {
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
        }

        // Main App Container Graph
        composable(NavRoutes.MAIN_GRAPH) {
            MainAppContainer(rootNavController = navController)
        }

        // Secondary Detail Screens
        composable(
            route = NavRoutes.ROOM_DETAIL,
            arguments = listOf(navArgument("roomTypeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val roomTypeId = backStackEntry.arguments?.getLong("roomTypeId") ?: 0L
            val viewModel: RoomTypeDetailViewModel = hiltViewModel()
            RoomTypeDetailScreen(
                roomTypeId = roomTypeId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCheckout = { id -> navController.navigate("booking_flow/$id") }
            )
        }

        composable(
            route = NavRoutes.BOOKING_FLOW,
            arguments = listOf(navArgument("roomTypeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val roomTypeId = backStackEntry.arguments?.getLong("roomTypeId") ?: 0L
            val viewModel: BookingViewModel = hiltViewModel()
            BookingFlowScreen(
                roomTypeId = roomTypeId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onBookingSuccess = { bookingId ->
                    navController.navigate("booking_detail/$bookingId") {
                        popUpTo(NavRoutes.MAIN_GRAPH)
                    }
                }
            )
        }

        composable(
            route = NavRoutes.BOOKING_DETAIL,
            arguments = listOf(navArgument("bookingId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getLong("bookingId") ?: 0L
            val viewModel: BookingViewModel = hiltViewModel()
            BookingDetailScreen(
                bookingId = bookingId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToQrModal = { id -> navController.navigate("qr_checkin/$id") }
            )
        }

        composable(
            route = NavRoutes.QR_CHECKIN,
            arguments = listOf(navArgument("bookingId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getLong("bookingId") ?: 0L
            val viewModel: BookingViewModel = hiltViewModel()
            QrCheckInScreen(
                bookingId = bookingId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.EKYC) {
            val viewModel: EkycViewModel = hiltViewModel()
            EkycVerificationScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainAppContainer(rootNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            ElysianBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = { item ->
                    bottomNavController.navigate(item.route) {
                        popUpTo(bottomNavController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.HOME) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToRoomDetail = { id -> rootNavController.navigate("room_detail/$id") },
                    onNavigateToSearch = { bottomNavController.navigate(NavRoutes.SEARCH) },
                    onNavigateToAiChat = { bottomNavController.navigate(NavRoutes.AI_CHAT) }
                )
            }

            composable(NavRoutes.SEARCH) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HotelSearchScreen(
                    viewModel = homeViewModel,
                    onNavigateToRoomDetail = { id -> rootNavController.navigate("room_detail/$id") }
                )
            }

            composable(NavRoutes.AI_CHAT) {
                val aiViewModel: AiChatViewModel = hiltViewModel()
                AiChatScreen(viewModel = aiViewModel)
            }

            composable(NavRoutes.BOOKINGS) {
                val bookingViewModel: BookingViewModel = hiltViewModel()
                BookingHistoryScreen(
                    viewModel = bookingViewModel,
                    onNavigateToDetail = { id -> rootNavController.navigate("booking_detail/$id") }
                )
            }

            composable(NavRoutes.PROFILE) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateToEkyc = { rootNavController.navigate(NavRoutes.EKYC) },
                    onLogoutSuccess = {
                        rootNavController.navigate(NavRoutes.AUTH_GRAPH) {
                            popUpTo(NavRoutes.MAIN_GRAPH) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
