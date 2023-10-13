package ru.descend.emekapplication

import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun String.toBase64() : String {
    return kotlin.io.encoding.Base64.encode(this.toByteArray())
}

@OptIn(ExperimentalEncodingApi::class)
fun String.fromBase64() : String {
    return kotlin.io.encoding.Base64.decode(this).toString(Charsets.UTF_8)
}