package com.example.duralapapp.ui.splash

import androidx.lifecycle.ViewModel
import com.example.duralapapp.data.local.TokenValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenValidator: TokenValidator
) : ViewModel() {

    suspend fun resolveStartDestination(): Destination {
        if (!tokenValidator.isLoggedIn()) return Destination.Login
        if (!tokenValidator.isTokenExpired()) return Destination.Home
        return if (tokenValidator.hasRefreshToken()) Destination.Home else Destination.Login
    }
}
