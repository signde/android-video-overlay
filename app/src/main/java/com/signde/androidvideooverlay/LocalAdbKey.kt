package com.signde.androidvideooverlay

import android.util.Base64
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbCrypto
import java.io.File

/** Setup and playback must identify as the same ADB client. Never import a computer's key. */
object LocalAdbKey {
    @Synchronized
    fun loadOrCreate(directory: File): AdbCrypto {
        val privateKey = File(directory, "private.key")
        val publicKey = File(directory, "public.key")
        val encoder = AdbBase64 { Base64.encodeToString(it, Base64.NO_WRAP) }
        if (privateKey.exists() && publicKey.exists()) {
            runCatching { AdbCrypto.loadAdbKeyPair(encoder, privateKey, publicKey) }
                .getOrNull()?.let { return it }
        }
        return AdbCrypto.generateAdbKeyPair(encoder).also {
            it.saveAdbKeyPair(privateKey, publicKey)
        }
    }
}
