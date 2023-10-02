package ru.descend.emekapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_connection).setOnClickListener {
            lifecycleScope.launch {
                val obj = EMEConnection.doRequest("Command" to "Jurassic Park2", "NewParam" to "123")
                println("Obj: $obj, ${obj.getTimeRequestAsString()}, ${obj.UUIDRequest}, req: ${obj.requestString}")
            }
        }
    }
}