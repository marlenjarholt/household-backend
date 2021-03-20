package no.householdBackend.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream

fun hash(s: String): String {
    val hash = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
    return Base64.getEncoder().encodeToString(hash)
}

fun secureRandom(bits: Int = 256): String {
    val allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()
    val random = SecureRandom()

    return IntStream.range(0, bits / 8)
        .mapToObj { allowedChars[random.nextInt(allowedChars.size)].toString() }
        .collect(Collectors.joining())
}