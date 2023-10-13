package ru.descend.emekapplication

import android.os.Build
import kotlinx.coroutines.CompletableDeferred
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.headersContentLength
import java.io.IOException
import java.net.ProtocolException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit


object EMEConnection {

    private val MEDIA_TYPE: MediaType = "application/json; charset=utf-8".toMediaType()
    private const val TAG = "EMEACon"

    private var SERVER_IP: String = ""
    private var SERVER_PORT: String = ""
    private var SERVER_FILENAME: String = ""
    private var SERVER_URL: String = ""
    private lateinit var CLIENT: OkHttpClient

    private var p_Timeouts: Long = 10

    private var formBody = FormBody.Builder(Charsets.UTF_8)
    private var formBodyString = ""

    fun setTimeouts(seconds: Long) : EMEConnection {
        p_Timeouts = seconds
        if (p_Timeouts < 0) throw IllegalArgumentException("Timeout secs does not be low than 0")

        return this
    }

    suspend fun doRequestField(entry: String, lineId: Int, field: String) : EMEResponse {
        if (entry.isNullOrBlank()) throw IllegalArgumentException("Object name is empty $entry")
        if (lineId <= -1) throw IllegalArgumentException("Entry($entry) line is incorrect $lineId")
        if (field.isNullOrBlank()) throw IllegalArgumentException("Field for entry $entry is not initialized $field")

        addParam("EMEACon_ENTRY", entry)
        addParam("EMEACon_ENTRY_LINE", lineId.toString())
        addParam("EMEACon_ENTRY_FIELD", field)

        return doRequest()
    }

    suspend fun doRequestMethod(method: String, paramsCount: Int, vararg params: Pair<String, String>) : EMEResponse {
        if (method.isNullOrBlank()) throw IllegalArgumentException("Method name does not be Empty $method")
        if (paramsCount < 0) throw IllegalArgumentException("Method params $paramsCount does not be low than 0")
        if (paramsCount != params.size) throw IllegalArgumentException("The method($method) should contain $paramsCount parameters, but contains ${params.size} parameters")

        addParam("EMEACon_METHOD", method)
        addParam("EMEACon_METHOD_PARAMS", paramsCount.toString())

        return doRequest(*params)
    }

    fun init(ip: String, port: String, filename: String): EMEConnection {
        SERVER_IP = ip
        SERVER_PORT = port
        SERVER_FILENAME = filename
        SERVER_URL = "http://$SERVER_IP:$SERVER_PORT/$SERVER_FILENAME"

        initializeClient()
        return this
    }

    private fun initializeClient() {
        CLIENT = OkHttpClient.Builder()
            .callTimeout(p_Timeouts, TimeUnit.SECONDS)
            .connectTimeout(p_Timeouts, TimeUnit.SECONDS)
            .readTimeout(p_Timeouts, TimeUnit.SECONDS)
            .writeTimeout(p_Timeouts, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun addSystemFields(responseObj: EMEResponse) {
        formBody.add("EMEACon", EMEResponse.EMECONNECTION_VERSION)  //версия модуля
        addSysParam("EMEACon_DATESTAMP", SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.ROOT).format(System.currentTimeMillis())) //дата\время устройства
        addSysParam("EMEACon_DEVICE", "SDK: ${Build.VERSION.SDK_INT} PHONE: ${Build.BRAND} ${Build.MODEL}") //информация об устройстве
        addSysParam("EMEACon_REQ_UUID", responseObj.UUIDRequest)
        addSysParam("EMEACon_REQ_URL", responseObj.URL)
        addSysParam("EMEACon_REQ_STRING", responseObj.requestString)
    }

    private fun addParam(key: String, value: String) {
        formBody.add(key.toBase64(), value.toBase64())
        formBodyString += "$key=$value;"
    }

    private fun addSysParam(key: String, value: String) {
        formBody.add(key.toBase64(), value.toBase64())
    }

    private fun clearTrash() {
        formBodyString = ""
        formBody = FormBody.Builder(Charsets.UTF_8)
    }

    suspend fun doRequest(vararg params: Pair<String, String>) : EMEResponse {

        val responseObj = EMEResponse()
        val deferredResult = CompletableDeferred<EMEResponse>()

        params.forEach {
            formBody.add(it.first.toBase64(), it.second.toBase64())
            formBodyString += it.first + "=" + it.second + ";"
        }

        responseObj.URL = SERVER_URL
        responseObj.timeBeginRequest = System.currentTimeMillis()
        responseObj.requestString = formBodyString

        addSystemFields(responseObj)

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(formBody.build())
            .build()

        clearTrash()

        CLIENT.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                responseObj.errorString = "on Failure: ${e.message}"
                responseObj.timeEndRequest = System.currentTimeMillis()
                deferredResult.complete(responseObj)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        responseObj.errorString = "on response Error: ${response.code} ${response.message}"
                        responseObj.timeEndRequest = System.currentTimeMillis()
                        deferredResult.complete(responseObj)
                        return
                    }

                    val objResult: ArrayList<String>
                    try {
                        val mainBody = response.peekBody(response.headersContentLength()).string()
                        objResult = mainBody.split("<BR>") as ArrayList<String>
                    } catch (e: ProtocolException) {
                        responseObj.errorString = e.message
                        responseObj.timeEndRequest = System.currentTimeMillis()
                        deferredResult.complete(responseObj)
                        return
                    }

                    var result = ""
                    objResult.forEachIndexed { index, s ->
                        if (index != 0 && index != objResult.size - 1) result += "$s&"
                    }
                    responseObj.timeEndRequest = System.currentTimeMillis()

                    responseObj.create(result)
                    if (responseObj.isExistsKey("Error")) {
                        responseObj.errorString = responseObj.getValue("Error")
                        responseObj.removeForKey("Error")
                    }

                    deferredResult.complete(responseObj)
                }
            }
        })
        return deferredResult.await()
    }
}