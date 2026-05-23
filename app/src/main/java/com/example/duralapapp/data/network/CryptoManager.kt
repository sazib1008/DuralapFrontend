package com.example.duralapapp.data.network

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor() {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    private fun getKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            createKey()
        }
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): CryptoResult {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())

            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val output = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, output, 0, iv.size)
            System.arraycopy(encrypted, 0, output, iv.size, encrypted.size)

            CryptoResult.Success(Base64.encodeToString(output, Base64.NO_WRAP))
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            CryptoResult.Error("ENCRYPTION_FAILED")
        }
    }

    fun decrypt(encryptedText: String): CryptoResult {
        return try {
            val decoded = Base64.decode(encryptedText, Base64.NO_WRAP)

            if (decoded.size <= IV_LENGTH) {
                return CryptoResult.Error("INVALID_DATA")
            }

            val iv = decoded.copyOfRange(0, IV_LENGTH)
            val cipherBytes = decoded.copyOfRange(IV_LENGTH, decoded.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(TAG_LENGTH, iv))

            val result = cipher.doFinal(cipherBytes)
            CryptoResult.Success(String(result, Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            CryptoResult.Error("DECRYPTION_FAILED")
        }
    }

    sealed class CryptoResult {
        data class Success(val data: String) : CryptoResult()
        data class Error(val reason: String) : CryptoResult()
    }

    companion object {
        private const val KEY_ALIAS = "secure_token_key_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 128
        private const val TAG = "CryptoManager"
    }
}