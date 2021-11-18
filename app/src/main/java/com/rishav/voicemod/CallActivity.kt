package com.rishav.voicemod

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import io.agora.agorauikit_android.AgoraConnectionData
import io.agora.agorauikit_android.AgoraVideoViewer
import io.agora.agorauikit_android.requestPermissions
import io.agora.rtc2.Constants
import net.voicemod.agoraplugin.ExtensionManager

@ExperimentalUnsignedTypes
class CallActivity : AppCompatActivity() {
    private var agView: AgoraVideoViewer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        try {
            this.agView = AgoraVideoViewer(this, AgoraConnectionData("--- APP-ID ----"))

            this.addContentView(
                agView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            if (AgoraVideoViewer.requestPermissions(this)) {
                initVoiceMod().apply {
                    agView!!.join("test", role = Constants.CLIENT_ROLE_BROADCASTER).apply {
                        runVoiceMod()
                    }
                }
            } else {
                val joinButton = Button(this)
                val joinString = "Allow Camera and Microphone, then click here"
                joinButton.text = joinString
                joinButton.setOnClickListener {
                    if (AgoraVideoViewer.requestPermissions(this)) {
                        (joinButton.parent as ViewGroup).removeView(joinButton)
                        initVoiceMod().apply {
                            agView!!.join("test", role = Constants.CLIENT_ROLE_BROADCASTER).apply {
                                runVoiceMod()
                            }
                        }

                    }
                }
                joinButton.setBackgroundColor(Color.GREEN)
                joinButton.setTextColor(Color.RED)
                this.addContentView(
                    joinButton,
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 300)
                )
            }
        } catch (e: Exception) {
            println("--- Error Running AgView ----")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agView?.agkit?.enableExtension(
            ExtensionManager.EXTENSION_VENDOR_NAME,
            ExtensionManager.EXTENSION_AUDIO_FILTER_NAME,
            false
        )
    }

    private fun initVoiceMod() {
        try {
            agView!!.agkit.enableExtension(
                ExtensionManager.EXTENSION_VENDOR_NAME,
                ExtensionManager.EXTENSION_AUDIO_FILTER_NAME,
                true
            )

            val userData = "{" +
                    "\"apiKey\": \"---- VoiceMod-API-Key ----\"," +
                    "\"apiSecret\": \"---- VoiceMod-API-Secret ----\"" +
                    "}"
            agView!!.agkit.setExtensionProperty(
                ExtensionManager.EXTENSION_VENDOR_NAME,
                ExtensionManager.EXTENSION_AUDIO_FILTER_NAME,
                "vcmd_user_data",
                userData
            )

            println("---- SUCCESS ----")
        } catch (e: Exception) {
            println("---- FAILED VOICEMOD ----")
        }
    }

    private fun runVoiceMod() {
        try {
            val presets = arrayOf(
                "\"magic-chords\"",
                "\"baby\"",
                "\"cave\"",
                "\"titan\"",
                "\"robot\"",
                "\"lost-soul\""
            )

            agView!!.agkit.setExtensionProperty(
                ExtensionManager.EXTENSION_VENDOR_NAME,
                ExtensionManager.EXTENSION_AUDIO_FILTER_NAME,
                "vcmd_voice",
                presets[3]
            )

            println("---- SUCCESS ----")
        } catch (e: Exception) {
            println("---- FAILED VOICEMOD ----")
        }
    }
}