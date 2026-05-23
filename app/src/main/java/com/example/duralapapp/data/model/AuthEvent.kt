package com.example.duralapapp.data.model

sealed class AuthEvent {
    data class Logout(val reason: String) : AuthEvent()
}
