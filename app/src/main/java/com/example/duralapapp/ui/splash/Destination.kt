package com.example.duralapapp.ui.splash

sealed class Destination(val route: String) {
    data object Splash : Destination("splash")
    data object Login : Destination("login")
    data object Home : Destination("home")
    data object Profile : Destination("profile")
}
