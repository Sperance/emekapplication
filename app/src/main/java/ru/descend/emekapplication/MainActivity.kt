package ru.descend.emekapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var eConn: EMEConnection

    private fun asyncMethod() {
        lifecycleScope.launch {
            eConn.doRequestMethod("CheckSSCC", 2, "param1" to "333313123", "param2" to "Sample text").onResult {
                if (it.isSuccessfully()){
                    println("[${it.UUIDRequest}] $it")
                } else {
                    println("[${it.UUIDRequest}] ${it.errorString}")
                }
            }
        }
    }

    private fun asyncMethod2(){
        lifecycleScope.launch {
            eConn.doRequestMethod("DoThisTimer", 0).onResult {
                if (it.isSuccessfully()){
                    println("[${it.UUIDRequest}] $it")
                } else {
                    println("[${it.UUIDRequest}] ${it.errorString}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        eConn = EMEConnection
            .setTimeouts(15)
            .init("10.10.10.236", "5400", "terminals.html")

        findViewById<Button>(R.id.button_check_SSCC).setOnClickListener {
            asyncMethod()
        }

        findViewById<Button>(R.id.button_do_method).setOnClickListener {
            asyncMethod2()
        }

        findViewById<Button>(R.id.button_connection).setOnClickListener {

            lifecycleScope.launch {

                val txetCode = findViewById<EditText>(R.id.text_order_id).text.toString()

                eConn.doRequest("Command" to "Jurassic Park2", "NewParam" to "123", "OrderNo" to txetCode).onResult {
                    if (it.isSuccessfully()){
                        println("[${it.UUIDRequest}] $it")
                    } else {
                        println("[${it.UUIDRequest}] ${it.errorString}")
                    }
                }

//                val obj = EMEConnection.doRequest("Command" to "Jurassic Park2", "NewParam" to "123")
//                println("Obj: $obj, ${obj.getTimeRequestAsString()}, ${obj.UUIDRequest}, req: ${obj.requestString}")
            }
        }
    }
}