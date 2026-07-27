package com.example.duralapapp

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.duralapapp.data.local.AuthEventBus
import com.example.duralapapp.data.local.OfflineUiBus
import com.example.duralapapp.data.model.AuthEvent
import com.example.duralapapp.ui.common.OfflineScreen
import com.example.duralapapp.ui.login.LoginScreen
import com.example.duralapapp.ui.main.HomeScreen
import com.example.duralapapp.ui.main.ProfileScreen
import com.example.duralapapp.ui.splash.Destination
import com.example.duralapapp.ui.splash.SplashScreen
import com.example.duralapapp.ui.theme.DuralapAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DuralapAppTheme {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    AuthEventBus.events.collect { event ->
                        if (event is AuthEvent.Logout) {
                            OfflineUiBus.dismiss()
                            navController.navigate(Destination.Login.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                val showOffline by OfflineUiBus.visible.collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Destination.Splash.route
                    ) {
                        composable(Destination.Splash.route) {
                            SplashScreen(navController = navController)
                        }
                        composable(Destination.Login.route) {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate(Destination.Home.route) {
                                        popUpTo(Destination.Login.route) { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate(Destination.Register.route)
                                }
                            )
                        }
                        composable(Destination.Register.route) {
                            com.example.duralapapp.ui.login.RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate(Destination.Home.route) {
                                        popUpTo(Destination.Login.route) { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(Destination.Home.route) {
                            HomeScreen(
                                onOpenProfile = {
                                    navController.navigate(Destination.Profile.route)
                                },
                                onOpenSearch = {
                                    navController.navigate(Destination.Search.route)
                                },
                                onOpenRequests = {
                                    navController.navigate(Destination.Requests.route)
                                },
                                onOpenChat = { conversationId, recipientName ->
                                    navController.navigate(
                                        Destination.ChatDetail.createRoute(conversationId, recipientName)
                                    )
                                }
                            )
                        }
                        composable(Destination.Profile.route) {
                            ProfileScreen(
                                onBackToHome = { navController.popBackStack() }
                            )
                        }
                        composable(Destination.Search.route) {
                            com.example.duralapapp.ui.search.SearchUserScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable(Destination.Requests.route) {
                            com.example.duralapapp.ui.requests.ConversationRequestsScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable(Destination.ChatDetail.route) {
                            com.example.duralapapp.ui.chat.ChatDetailScreen(
                                onBackClick = { navController.popBackStack() },
                                onStartCall = { targetUserId, targetUserName, conversationId, callType ->
                                    navController.navigate(
                                        Destination.Call.createRoute(targetUserId, targetUserName, conversationId, callType)
                                    )
                                }
                            )
                        }
                        composable(Destination.Call.route) {
                            com.example.duralapapp.ui.call.CallScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }

                    if (showOffline) {
                        OfflineScreen(
                            onBack = { OfflineUiBus.dismiss() },
                            onRetry = { OfflineUiBus.dismiss() },
                            onOpenSettings = {
                                startActivity(
                                    Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
