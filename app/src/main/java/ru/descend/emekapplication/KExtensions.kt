package ru.descend.emekapplication

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.io.encoding.ExperimentalEncodingApi

fun getTimeFromMillis(millisFrom: Long, millisTo: Long) =
    SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT).format(Date(millisTo - millisFrom))

@OptIn(ExperimentalEncodingApi::class)
fun String.toBase64(): String {
    return kotlin.io.encoding.Base64.encode(this.toByteArray())
}

@OptIn(ExperimentalEncodingApi::class)
fun String.fromBase64(): String {
    return kotlin.io.encoding.Base64.decode(this).toString(Charsets.UTF_8)
}