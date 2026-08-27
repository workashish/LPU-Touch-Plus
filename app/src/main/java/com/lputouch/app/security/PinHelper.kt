package com.lputouch.app.security

import java.security.MessageDigest

object PinHelper {
    fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(("lputouch-plus:$pin").toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun verify(pin: String, storedHash: String): Boolean =
        hash(pin) == storedHash
}
