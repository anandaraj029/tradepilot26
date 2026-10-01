package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedBrokerCredentials(
    val brokerCode: String,
    val apiKey: String,
    val apiSecret: String,
    val accessToken: String,
    val refreshToken: String,
    val encryptedAt: Long,
    val algorithm: String = "AES-256-GCM"
)

/**
 * Secure encrypted local storage provider for broker credentials and OAuth2 tokens.
 *
 * Employs AES-256-GCM authenticated encryption with a unique 12-byte initialization vector (IV)
 * per encryption cycle and SHA-256 key derivation. Ensures sensitive broker API secrets and
 * OAuth2 tokens never sit in plaintext in local databases or shared preferences.
 */
class SecureBrokerStorage(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    // Deterministic app-scoped master key seed
    private val masterKeyBytes: ByteArray by lazy {
        val seed = "AI_TRADEPILOT_BROKER_VAULT_${context.packageName}_SECURE_OAUTH2_KEYSTORE_2026"
        val md = MessageDigest.getInstance("SHA-256")
        md.digest(seed.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Encrypts and securely saves broker OAuth2 credentials.
     */
    fun saveCredentials(
        brokerCode: String,
        apiKey: String,
        apiSecret: String,
        accessToken: String,
        refreshToken: String = ""
    ): Boolean {
        return try {
            val payload = "$apiKey||$apiSecret||$accessToken||$refreshToken||${System.currentTimeMillis()}"
            val encryptedPayload = encryptString(payload)

            prefs.edit()
                .putString("${KEY_PREFIX_CREDS}$brokerCode", encryptedPayload)
                .putLong("${KEY_PREFIX_TIME}$brokerCode", System.currentTimeMillis())
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Decrypts and retrieves stored credentials for the given broker.
     */
    fun getCredentials(brokerCode: String): EncryptedBrokerCredentials? {
        val encryptedData = prefs.getString("${KEY_PREFIX_CREDS}$brokerCode", null) ?: return null
        val time = prefs.getLong("${KEY_PREFIX_TIME}$brokerCode", System.currentTimeMillis())
        return try {
            val decrypted = decryptString(encryptedData)
            val parts = decrypted.split("||")
            if (parts.size >= 4) {
                EncryptedBrokerCredentials(
                    brokerCode = brokerCode,
                    apiKey = parts[0],
                    apiSecret = parts[1],
                    accessToken = parts[2],
                    refreshToken = parts[3],
                    encryptedAt = time
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Wipes credentials for a specific broker.
     */
    fun deleteCredentials(brokerCode: String) {
        prefs.edit()
            .remove("${KEY_PREFIX_CREDS}$brokerCode")
            .remove("${KEY_PREFIX_TIME}$brokerCode")
            .apply()
    }

    /**
     * Wipes all broker secrets from the encrypted vault.
     */
    fun clearAllVault() {
        prefs.edit().clear().apply()
    }

    /**
     * Verifies if credentials exist and are encrypted for a broker.
     */
    fun hasCredentials(brokerCode: String): Boolean {
        return prefs.contains("${KEY_PREFIX_CREDS}$brokerCode")
    }

    // --- Cryptographic AES-256-GCM Implementation ---

    private fun encryptString(plaintext: String): String {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val keySpec = SecretKeySpec(masterKeyBytes, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

        // Combine IV + Ciphertext
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptString(encodedCiphertext: String): String {
        val combined = Base64.decode(encodedCiphertext, Base64.NO_WRAP)
        if (combined.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Ciphertext is too short for valid GCM payload")
        }

        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)

        val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH)
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)

        val keySpec = SecretKeySpec(masterKeyBytes, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    companion object {
        private const val PREFS_NAME = "secure_broker_vault"
        private const val KEY_PREFIX_CREDS = "enc_broker_creds_"
        private const val KEY_PREFIX_TIME = "enc_broker_time_"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12 // 96 bits for GCM
        private const val GCM_TAG_LENGTH = 128 // 128 bits auth tag
    }
}
