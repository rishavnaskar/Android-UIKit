package com.rishav.banuba

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.agora.agorauikit_android.AgoraConnectionData
import io.agora.agorauikit_android.AgoraVideoViewer
import io.agora.agorauikit_android.requestPermissions

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.enterCall)

        button.setOnClickListener {
            val callIntent = Intent(this, CallActivity::class.java)
            startActivity(callIntent)
        }
    }
}