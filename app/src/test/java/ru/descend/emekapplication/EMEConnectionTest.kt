package ru.descend.emekapplication

import kotlinx.coroutines.test.runTest
import org.jetbrains.annotations.TestOnly
import org.junit.Test

@TestOnly
class EMEConnectionTest {

    val emeObj = EMEConnection.init("10.10.10.236", "5400", "terminals.html")

    @Test
    fun test_simple_connection() = runTest {
        emeObj.doRequest("key" to "Hello world").onResult {
            println(it)
            println("getSuccess: " + it.getSuccess())
            println("getError: " + it.getError())
        }
    }

    @Test
    fun test_simple_method() = runTest {
        emeObj.doRequestMethod("DoThisTimer", 0).onResult {
            println(it)
            println("getSuccess: " + it.getSuccess())
            println("getError: " + it.getError())
        }
    }

    @Test
    fun test_hard_method() = runTest {
        emeObj.doRequestMethod("CheckSSCC", 2, "key1" to "123", "param" to "Sample param").onResult {
            println(it)
            println("getSuccess: " + it.getSuccess())
            println("getError: " + it.getError())
        }
    }

    @Test
    fun test_fields() = runTest {
        emeObj.doRequestField("Orders", 79, "Salutation").onResult {
            println(it)
            println("getSuccess: " + it.getSuccess())
            println("getError: " + it.getError())
        }
    }

    @Test
    fun test_base64() {
        val param = "Русская строка 666 and england"
        val encodeParam = param.toBase64()
        println(encodeParam)
        val decodeParam = encodeParam.fromBase64()
        println(decodeParam)
    }
}