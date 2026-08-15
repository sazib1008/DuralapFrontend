package com.example.duralapapp.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var previousIsSpeakerphoneOn: Boolean = false
    private var previousIsMicrophoneMute: Boolean = false
    private var audioFocusRequest: AudioFocusRequest? = null

    fun start(isSpeakerphoneDefault: Boolean = false) {
        previousAudioMode = audioManager.mode
        previousIsSpeakerphoneOn = audioManager.isSpeakerphoneOn
        previousIsMicrophoneMute = audioManager.isMicrophoneMute

        requestAudioFocus()

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isMicrophoneMute = false
        setSpeakerphone(isSpeakerphoneDefault)
    }

    fun setSpeakerphone(on: Boolean) {
        audioManager.isSpeakerphoneOn = on
    }

    fun isSpeakerphoneOn(): Boolean {
        return audioManager.isSpeakerphoneOn
    }

    fun setMicrophoneMute(mute: Boolean) {
        audioManager.isMicrophoneMute = mute
    }

    fun isMicrophoneMute(): Boolean {
        return audioManager.isMicrophoneMute
    }

    fun stop() {
        abandonAudioFocus()
        audioManager.isSpeakerphoneOn = previousIsSpeakerphoneOn
        audioManager.isMicrophoneMute = previousIsMicrophoneMute
        audioManager.mode = previousAudioMode
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { /* handle focus change if needed */ }
                .build()

            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            audioManager.abandonAudioFocus(null)
        }
    }
}
