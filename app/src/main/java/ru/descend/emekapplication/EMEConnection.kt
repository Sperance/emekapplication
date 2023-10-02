package ru.descend.emekapplication

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

class EMEResponse {

    private val data = ArrayList<Pair<String, String>>()
    lateinit var URL: String
    var errorString: String? = null
    var timeBeginRequest: Long = 0
    var timeEndRequest: Long = 0
    val UUIDRequest = UUID.randomUUID().toString()
    lateinit var requestString: String

    fun create(inParams: String) {

        if (errorString != null) {
            Log.e("EMECommection", errorString?:"")
            return
        }

        parseInnerString(inParams)
    }

    /**
     * Получение всего массива данных с сервера по запросу
     */
    fun getData() = data

    /**
     * Вывод времени работы запроса на сервер
     * #
     * SimpleDateFormat("HH:mm:ss.SSS").format(obj.getTimeRequest())
     * @return Объект даты представляющий продолжительность запроса
     */
    fun getTimeRequest() = Date(timeEndRequest - timeBeginRequest)

    /**
     * Получение строкового представления даты выполнения запроса
     */
    fun getTimeRequestAsString() = SimpleDateFormat("HH:mm:ss.SSS").format(getTimeRequest())

    /**
     * Получить кол-во объектов ключ=значение от сервера
     */
    fun getCountItems() = data.size

    /**
     * Найти значение по ключу
     * @return содержимое ключа, либо Null если таковой не найден
     */
    fun getValue(key: String): String? {
        return data.find { it.first == key }?.second
    }

    /**
     * Проверка ключа в ответе от сервера
     */
    fun isExistsKey(key: String): Boolean {
        return data.find { it.first == key } != null
    }

    private fun parseInnerString(inParams: String) {
        val lines = inParams.split("&")
        lines.forEach {
            val line = it.split("=")
            if (line.size == 2) data.add(Pair(line[0], line[1]))
        }
    }

    override fun toString(): String {
        var params = ""
        data.forEach { params += it.first + "=" + it.second + ";" }
        return "EMEResponse={$params}"
    }
}

object EMEConnection {

    private val SERVER_URL = "http://10.10.10.236:5400/terminals.html"

    private val client = OkHttpClient.Builder()
//        .callTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
//        .readTimeout(60, TimeUnit.SECONDS)
//        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun doRequest(vararg params: Pair<String, String>) : EMEResponse {

        val responseObj = EMEResponse()

        val deferredResult = CompletableDeferred<EMEResponse>()
        val formBody = FormBody.Builder()
        var reqString = ""
        params.forEach {
            formBody.add(it.first, it.second)
            reqString += it.first + "=" + it.second + ";"
        }

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(formBody.build())
            .build()

        responseObj.requestString = reqString
        responseObj.URL = SERVER_URL
        responseObj.timeBeginRequest = System.currentTimeMillis()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                responseObj.errorString = "Error onFailure: ${e.message}"
                responseObj.timeEndRequest = System.currentTimeMillis()
                deferredResult.complete(responseObj)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        responseObj.errorString = "Запрос к серверу не был успешен: ${response.code} ${response.message}"
                        responseObj.timeEndRequest = System.currentTimeMillis()
                        deferredResult.complete(responseObj)
                        return
                    }
                    val objResult = response.body!!.string().split("<BR>")
                    var result = ""
                    objResult.forEachIndexed { index, s ->
                        if (index != 0 && index != objResult.size - 1) result += "$s&"
                    }
                    responseObj.timeEndRequest = System.currentTimeMillis()
                    responseObj.create(result)
                    deferredResult.complete(responseObj)
                }
            }
        })
        return deferredResult.await()
    }
}