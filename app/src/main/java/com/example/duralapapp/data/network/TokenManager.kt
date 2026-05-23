package com.example.duralapapp.data.network

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_preferences"
)

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val TOKEN_TYPE_KEY = stringPreferencesKey("token_type")
        private val EXPIRES_IN_KEY = longPreferencesKey("expires_in")
        private val ACCESS_TOKEN_EXPIRES_AT_KEY = longPreferencesKey("access_token_expires_at")
        private val TOKEN_ISSUED_AT_KEY = longPreferencesKey("token_issued_at")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        const val EXPIRY_GRACE_PERIOD_MS = 60_000L
        private const val LEGACY_CIPHER_PREFIX = "enc:"
        private const val LEGACY_KEY_ALIAS = "duralap_token_key"
    }

    val accessToken: Flow<String?> = context.authDataStore.data
        .map { prefs -> prefs[ACCESS_TOKEN_KEY]?.let(::decryptOrNull) }

    val refreshToken: Flow<String?> = context.authDataStore.data
        .map { prefs -> prefs[REFRESH_TOKEN_KEY]?.let(::decryptOrNull) }

    val tokenType: Flow<String?> = context.authDataStore.data
        .map { prefs -> prefs[TOKEN_TYPE_KEY] }

    val expiresIn: Flow<Long?> = context.authDataStore.data
        .map { prefs -> prefs[EXPIRES_IN_KEY] }

    val accessTokenExpiresAt: Flow<Long?> = context.authDataStore.data
        .map { prefs -> prefs[ACCESS_TOKEN_EXPIRES_AT_KEY] }

    val tokenIssuedAt: Flow<Long?> = context.authDataStore.data
        .map { prefs -> prefs[TOKEN_ISSUED_AT_KEY] }

    val userId: Flow<String?> = context.authDataStore.data
        .map { prefs -> prefs[USER_ID_KEY] }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        tokenType: String = "Bearer",
        expiresIn: Long,
        userId: String? = null
    ) {
        val issuedAt = System.currentTimeMillis()
        val expiresAt = issuedAt + expiresIn.toMillisDuration()

        context.authDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = requireEncrypt(accessToken)
            prefs[REFRESH_TOKEN_KEY] = requireEncrypt(refreshToken)
            prefs[TOKEN_TYPE_KEY] = tokenType
            prefs[EXPIRES_IN_KEY] = expiresIn
            prefs[ACCESS_TOKEN_EXPIRES_AT_KEY] = expiresAt
            prefs[TOKEN_ISSUED_AT_KEY] = issuedAt
            userId?.let { prefs[USER_ID_KEY] = it }
        }
    }

    suspend fun clearSession() {
        context.authDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun hasTokens(): Boolean {
        return context.authDataStore.data.map { prefs ->
            prefs[ACCESS_TOKEN_KEY] != null && prefs[REFRESH_TOKEN_KEY] != null
        }.firstOrNull() ?: false
    }

    /**
     * True when the access token is missing expiry metadata or within [gracePeriodMillis] of expiring.
     */
    suspend fun isAccessTokenExpired(
        currentTimeMillis: Long = System.currentTimeMillis(),
        gracePeriodMillis: Long = EXPIRY_GRACE_PERIOD_MS
    ): Boolean {
        val expiresAt = accessTokenExpiresAt.firstOrNull()
        return expiresAt == null || currentTimeMillis >= (expiresAt - gracePeriodMillis)
    }

    private fun requireEncrypt(plainText: String): String {
        return when (val result = cryptoManager.encrypt(plainText)) {
            is CryptoManager.CryptoResult.Success -> result.data
            is CryptoManager.CryptoResult.Error ->
                throw IllegalStateException("Token encryption failed: ${result.reason}")
        }
    }

    private fun decryptOrNull(cipherText: String): String? {
        if (cipherText.startsWith(LEGACY_CIPHER_PREFIX)) {
            return decryptLegacyOrNull(cipherText)
        }
        return when (val result = cryptoManager.decrypt(cipherText)) {
            is CryptoManager.CryptoResult.Success -> result.data
            is CryptoManager.CryptoResult.Error -> null
        }
    }

    /** Supports tokens encrypted by the previous TokenCipher implementation. */
    private fun decryptLegacyOrNull(cipherText: String): String? {
        return runCatching {
            val payload = cipherText.removePrefix(LEGACY_CIPHER_PREFIX)
            val combined = Base64.decode(payload, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key = keyStore.getKey(LEGACY_KEY_ALIAS, null) as? SecretKey ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(128, iv)
            )
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun Long.toMillisDuration(): Long {
        return if (this >= 1_000_000L) this else TimeUnit.SECONDS.toMillis(this)
    }
}
