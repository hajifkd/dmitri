package tokyo.theta.dmitri.util

import java.io.File
import java.lang.StringBuilder
import java.security.DigestInputStream
import java.security.MessageDigest

fun sha1Hash(file: File): String {
    val digest = MessageDigest.getInstance("SHA-1")
    val digestInputStream = DigestInputStream(file.inputStream(), digest)
    var buf = ByteArray(4096)
    while (digestInputStream.read(buf) != -1);
    return digest.digest().map { String.format("%02x", it) }
        .fold(StringBuilder()) { acc, x -> acc.append(x) }
        .toString()
}