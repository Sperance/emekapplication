package ru.descend.emekapplication

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class EMEResponse {

    companion object {
        const val EMECONNECTION_VERSION = "0.0.1"
    }

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

    fun isSuccessfully() = errorString == null

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

        val tError = if (errorString.isNullOrBlank()) "" else "ERR:$errorString,"
        val tParams = if (params.isNullOrBlank()) "" else "PARAMS:$params"

        return "EMEResponse={$tError$tParams}"
    }

    fun onResult(function: (obj: EMEResponse) -> Unit) {
        function.invoke(this)
    }
}