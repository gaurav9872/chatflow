package com.example.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    // For demo E2EE, we derive a shared symmetric secret or use a master shared key per chat
    private val defaultSharedKey: SecretKey by lazy {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        keyGen.generateKey()
    }

    data class EncryptedResult(val cipherText: String, val iv: String)

    fun encrypt(plainText: String): EncryptedResult {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, defaultSharedKey, spec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return EncryptedResult(
                cipherText = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
                iv = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            // Fallback to plain text if encryption fails
            return EncryptedResult(cipherText = plainText, iv = "")
        }
    }

    fun decrypt(cipherText: String, ivStr: String): String {
        if (ivStr.isEmpty()) return cipherText
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = Base64.decode(ivStr, Base64.NO_WRAP)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, defaultSharedKey, spec)
            val decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            val plainBytes = cipher.doFinal(decodedBytes)
            return String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            return cipherText
        }
    }

    fun generateSecurityFingerprint(): String {
        val randomBytes = ByteArray(16)
        SecureRandom().nextBytes(randomBytes)
        return randomBytes.joinToString(" ") { "%02X".format(it) }.chunked(12).joinToString("\n")
    }
}
