package com.tourverse.data.session

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedSessionTokenStore(context: Context) : SessionTokenStore {
    private val preferences = context.getSharedPreferences("tourverse_secure_session", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override fun readRefreshToken(): String? = runCatching {
        val encoded = preferences.getString(TOKEN, null) ?: return null
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val ivSize = bytes.first().toInt()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 1, ivSize))
        String(cipher.doFinal(bytes.copyOfRange(1 + ivSize, bytes.size)), Charsets.UTF_8)
    }.getOrElse {
        clear()
        null
    }

    override fun writeRefreshToken(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val value = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted
        preferences.edit().putString(TOKEN, Base64.encodeToString(value, Base64.NO_WRAP)).apply()
    }

    override fun clear() {
        preferences.edit().remove(TOKEN).apply()
    }

    private fun key(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(
                android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val TOKEN = "refresh_token"
        const val KEY_ALIAS = "tourverse_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
