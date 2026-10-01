package com.shinnie.optimize

import android.content.Context
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Key format:  FF-XXXX-XXXX-SSSSSSSS
 *  - FF-XXXX-XXXX = payload (anything you like, e.g. a customer id)
 *  - SSSSSSSS     = first 8 hex chars of HMAC-SHA256(secret, payload)
 *
 * NOTE: an offline check like this can be bypassed by someone who decompiles the APK.
 * For real protection, validate the key on your own server (see README notes).
 */
class LicenseManager(context: Context) {

    private val prefs = context.getSharedPreferences("license_prefs", Context.MODE_PRIVATE)

    fun isActivated(): Boolean =
        prefs.getString(KEY_SAVED, null)?.let { validate(it) } ?: false

    fun activate(input: String): Boolean {
        val key = input.trim().uppercase()
        if (!validate(key)) return false
        prefs.edit().putString(KEY_SAVED, key).apply()
        return true
    }

    fun deactivate() = prefs.edit().remove(KEY_SAVED).apply()

    fun validate(key: String): Boolean {
        if (key.trim().equals(MASTER_KEY, ignoreCase = true)) return true
        val parts = key.split("-")
        if (parts.size != 4 || parts[0] != "FF") return false
        val payload = "${parts[0]}-${parts[1]}-${parts[2]}"
        val expected = sign(payload).toByteArray()
        return MessageDigest.isEqual(expected, parts[3].toByteArray()) // constant-time compare
    }

    companion object {
        private const val KEY_SAVED = "activation_key"
        private const val MASTER_KEY = "ShinnieFree"
        // CHANGE THIS and keep it private. Same value must be used in KeyGen.kt
        private const val SECRET = "CHANGE_ME_TO_A_LONG_RANDOM_SECRET"

        fun sign(payload: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(SECRET.toByteArray(), "HmacSHA256"))
            return mac.doFinal(payload.toByteArray())
                .joinToString("") { "%02X".format(it) }
                .take(8)
        }
    }
}
