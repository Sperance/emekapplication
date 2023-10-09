package ru.descend.emekapplication

import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.headersContentLength
import java.io.IOException
import java.net.ProtocolException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object EMEConnection {

    private const val TAG = "EMEACon"

    private var SERVER_IP: String = ""
    private var SERVER_PORT: String = ""
    private var SERVER_FILENAME: String = ""
    private var SERVER_URL: String = ""
    private lateinit var CLIENT: OkHttpClient

    private var p_Timeouts: Long = 15
    private var p_MethodName: String = ""
    private var p_MethodParams: Int = 0

    fun setTimeouts(seconds: Long) : EMEConnection {
        p_Timeouts = seconds
        if (p_Timeouts < 0) throw IllegalArgumentException("Timeout secs does not be low than 0")

        return this
    }

    suspend fun doRequestMethod(method: String, paramsCount: Int, vararg params: Pair<String, String>) : EMEResponse {
        p_MethodName = method
        if (p_MethodName.isBlank()) throw IllegalArgumentException("Method name does not be Empty")

        p_MethodParams = paramsCount
        if (p_MethodParams < 0) throw IllegalArgumentException("Method params does not be low than 0")

        if (p_MethodParams != params.size) throw IllegalArgumentException("The method($p_MethodName) should contain $p_MethodParams parameters, but contains ${params.size} parameters")

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
            .build()
    }

    private fun addSystemFields(formBody: FormBody.Builder, responseObj: EMEResponse) = formBody.apply {
        add("EMEACon", EMEResponse.EMECONNECTION_VERSION)  //версия модуля
        add("EMEACon_DATESTAMP", SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.ROOT).format(System.currentTimeMillis())) //дата\время устройства
        add("EMEACon_DEVICE", "SDK: ${Build.VERSION.SDK_INT} PHONE: ${Build.BRAND} ${Build.MODEL}") //информация об устройстве

        add("EMEACon_REQ_UUID", responseObj.UUIDRequest)
        add("EMEACon_REQ_URL", responseObj.URL)
        add("EMEACon_REQ_STRING", responseObj.requestString)
    }

    suspend fun doRequest(vararg params: Pair<String, String>) : EMEResponse {

        val responseObj = EMEResponse()

        val deferredResult = CompletableDeferred<EMEResponse>()
        val formBody = FormBody.Builder()
        var reqString = ""

        if (p_MethodName.isNotBlank()) {
            reqString += "EMEACon_METHOD=$p_MethodName;"
            formBody.add("EMEACon_METHOD", p_MethodName)
            formBody.add("EMEACon_METHOD_PARAMS", p_MethodParams.toString())
        }

        params.forEach {
            formBody.add(it.first, it.second)
            reqString += it.first + "=" + it.second + ";"
        }

        responseObj.URL = SERVER_URL
        responseObj.timeBeginRequest = System.currentTimeMillis()
        responseObj.requestString = reqString

        addSystemFields(formBody, responseObj)

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(formBody.build())
            .build()

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

                        Log.e(TAG, "---")
                        Log.e(TAG, responseObj.toString())
                        Log.e(TAG, "---")

                        deferredResult.complete(responseObj)
                        return
                    }

                    var result = ""
                    objResult.forEachIndexed { index, s ->
                        if (index != 0 && index != objResult.size - 1) result += "$s&"
                    }
                    responseObj.timeEndRequest = System.currentTimeMillis()

                    responseObj.create(result)
                    if (responseObj.isExistsKey("Error"))
                        responseObj.errorString = responseObj.getValue("Error")

                    Log.e(TAG, responseObj.toString())

                    deferredResult.complete(responseObj)
                }
            }
        })
        return deferredResult.await()
    }
}