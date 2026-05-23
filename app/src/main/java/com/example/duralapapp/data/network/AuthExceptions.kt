package com.example.duralapapp.data.network

import java.io.IOException

class OfflineModeException : IOException("No network connection available")

class SessionExpiredException : IOException("Session expired; please sign in again")
