package com.example.duralapapp.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.local.OfflineUiBus
import com.example.duralapapp.data.network.OfflineModeException
import com.example.duralapapp.data.network.SessionExpiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel() {
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    protected fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                when (e) {
                    is OfflineModeException -> OfflineUiBus.show()
                    else -> _userMessage.value = mapToUserMessage(e)
                }
            }
        }
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    protected open fun mapToUserMessage(error: Exception): String {
        return when (error) {
            is OfflineModeException -> "You are offline. Check your internet and try again."
            is SessionExpiredException -> "Your session expired. Please sign in again."
            else -> "Something went wrong. Please try again."
        }
    }
}
