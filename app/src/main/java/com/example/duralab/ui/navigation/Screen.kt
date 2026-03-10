package com.example.duralab.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object Profile : Screen("profile")
    object Call : Screen("call/{userId}") {
        fun createRoute(userId: String) = "call/$userId"
    }
    object Splash : Screen("splash")
}
