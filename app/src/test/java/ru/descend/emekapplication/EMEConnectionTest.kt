package ru.descend.emekapplication

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime

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

    private val repeatCount = 10

    /**
     * Синхронные запросы
     */
    @Test
    fun test_speed_request_main() = runTest(timeout = Duration.INFINITE) {
        var index = 0
        val sa = measureTime {
            launch {
                repeat(repeatCount) {
                    index++
                    emeObj.doRequest("key" to "Hello world $index").onResult {
                        println(it)
                    }
                }
            }.join()
        }
        println("Ms: ${sa.inWholeMilliseconds}")
    }

    /**
     * Асинхронные запросы
     */
    @Test
    fun test_speed_request() = runTest(timeout = Duration.INFINITE) {
        var index = 0
        val sa = measureTime {
            launch {
                repeat(repeatCount) {
                    launch {
                        index++
                        emeObj.doRequest("key" to "Hello world $index").onResult {
                            println(it)
                        }
                    }
                }
            }.join()
        }
        println("Ms: ${sa.inWholeMilliseconds}")
    }

    @Test
    fun test_many_args_connection() = runTest {
        emeObj.doRequest(
            "SSCC" to "1234812348912",
            "IncomeNo" to "VB0001230/24",
            "Stamp" to "90529057235972350993"
        ).onResult {
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
    fun test_error_method() = runTest {
        emeObj.doRequestMethod("FatalMethod", 1, "Order" to "8584759").onResult {
            println(it)
            println("getSuccess: " + it.getSuccess())
            println("getError: " + it.getError())
        }
    }

    @Test
    fun test_hard_method() = runTest {
        emeObj.doRequestMethod("CheckSSCC", 2, "key1" to "123", "param" to "Sample param")
            .onResult {
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
}