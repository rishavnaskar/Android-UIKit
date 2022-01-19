package com.rishav.symblai

import ai.symbl.android.extension.ExtensionManager
import ai.symbl.android.extension.SymblAIFilterManager
import ai.symbl.android.extension.model.request.*
import ai.symbl.android.extension.model.response.SymblResponse
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import io.agora.agorauikit_android.AgoraConnectionData
import io.agora.agorauikit_android.AgoraVideoViewer
import io.agora.agorauikit_android.requestPermissions
import io.agora.rtc2.Constants
import io.agora.rtc2.IMediaExtensionObserver
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.UserInfo
import io.agora.rtc2.video.VideoEncoderConfiguration
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity(), IMediaExtensionObserver {
    private var agView: AgoraVideoViewer? = null
    private val meetingId = "test"
    private val symblUserId = "user@mail.com"
    private val TAG = "Agora_SymblTag java :"
    private lateinit var infoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        infoTextView = findViewById(R.id.infoTextView)

        this.agView = AgoraVideoViewer(
            this,
            AgoraConnectionData(
                appId = appID,
                extensionName = listOf(ExtensionManager.EXTENSION_NAME),
                iMediaExtensionObserver = this
            ),
        )

        val frameLayout: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ).also { it.bottomMargin = 250 }
        this.addContentView(agView, frameLayout)

        if (AgoraVideoViewer.requestPermissions(this)) {
            initAgoraEngine()
        } else {
            val joinButton = Button(this)
            val joinString = "Allow Camera and Microphone, then click here"
            joinButton.text = joinString
            joinButton.setOnClickListener {
                if (AgoraVideoViewer.requestPermissions(this)) {
                    (joinButton.parent as ViewGroup).removeView(joinButton)
                    initAgoraEngine()
                }
            }
            joinButton.setBackgroundColor(Color.GREEN)
            joinButton.setTextColor(Color.RED)
            this.addContentView(
                joinButton,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 300)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disableEffect()
        this.agView?.agkit?.leaveChannel()
    }

    @SuppressLint("SetTextI18n")
    private fun initAgoraEngine() {
        try {
            this.agView?.agkit?.enableExtension(
                ExtensionManager.EXTENSION_VENDOR_NAME,
                ExtensionManager.EXTENSION_FILTER_NAME,
                true
            )

            this.agView?.join(meetingId, role = Constants.CLIENT_ROLE_BROADCASTER)

            val pluginParams = JSONObject()
            setSymblPluginConfigs(pluginParams)
            agView?.agkit?.setExtensionProperty(
                ExtensionManager.EXTENSION_VENDOR_NAME,
                ExtensionManager.EXTENSION_FILTER_NAME,
                "init",
                pluginParams.toString()
            )

            val button: Button = findViewById(R.id.togglebtn)
            var toggle = false

            button.setOnClickListener {
                if (!toggle) {
                    enableEffect(JSONObject())
                    Toast.makeText(this@MainActivity, "Effect enabled", Toast.LENGTH_LONG).show()
                    toggle = !toggle
                    button.text = "On"
                } else {
                    disableEffect()
                    Toast.makeText(this@MainActivity, "Effect disabled", Toast.LENGTH_LONG).show()
                    toggle = !toggle
                    button.text = "Off"
                }
            }
        } catch (e: Exception) {
            println("---- " + e.message + " ----")
        }
    }

    @Throws(JSONException::class)
    private fun setSymblPluginConfigs(pluginParams: JSONObject) {
        try {
            pluginParams.put("secret", symblApiSecret)
            pluginParams.put("appKey", symblAppId)
            pluginParams.put("meetingId", UUID.randomUUID().toString())
            pluginParams.put("userId", symblUserId)
            pluginParams.put("name", "Test Name")
            pluginParams.put("languageCode", "en-US")

            // Symbl main extension config objects
            val symblParams = SymblPluginConfig()

            // Set the Symbl API Configuration
            val apiConfig = ApiConfig()
            apiConfig.appId = symblAppId
            apiConfig.appSecret = symblApiSecret
            apiConfig.tokenApi = "https://api.symbl.ai/oauth2/token:generate"
            apiConfig.symblPlatformUrl = "api-agora-1.symbl.ai"
            symblParams.apiConfig = apiConfig

            // Set the Symbl Confidence Level and Language Code
            val realtimeFlowInitRequest = RealtimeStartRequest()
            val realtimeAPIConfig = RealtimeAPIConfig()
            realtimeAPIConfig.confidenceThreshold = 0.5
            realtimeAPIConfig.languageCode = "en-US"

            // Set the Speaker information
            val speaker = Speaker()
            speaker.name = "Test Name"
            speaker.userId = symblUserId
            realtimeFlowInitRequest.speaker = speaker

            // Set the meeting encoding and speaker sample rate hertz
            val speechRecognition = SpeechRecognition()
            speechRecognition.encoding = "LINEAR16"
            speechRecognition.sampleRateHertz = 44100.0
            realtimeAPIConfig.speechRecognition = speechRecognition

            // Set the redaction content values
            val redaction = Redaction()
            redaction.identifyContent = true
            redaction.redactContent = true
            redaction.redactionString = "*****"
            realtimeAPIConfig.redaction = redaction

            // Set the Tracker (custom business intent) information
            realtimeFlowInitRequest.config = realtimeAPIConfig
            val tracker1 = Tracker()
            tracker1.name = "Budget"
            val vocabulary: MutableList<String> = ArrayList()
            vocabulary.add("budgeted")
            vocabulary.add("budgeted decision")
            tracker1.vocabulary = vocabulary
            val trackerList: MutableList<Tracker> = ArrayList()
            trackerList.add(tracker1)

            // Set the Symbl conversation parameters
            realtimeFlowInitRequest.trackers = trackerList
            realtimeFlowInitRequest.type = "start_request"
            realtimeFlowInitRequest.id = meetingId
            realtimeFlowInitRequest.sentiments = true
            realtimeFlowInitRequest.insightTypes =
                listOf("action_item", "question", "follow_up")
            symblParams.realtimeStartRequest = realtimeFlowInitRequest
            val gson = Gson()
            pluginParams.put("inputRequest", gson.toJson(symblParams))

        } catch (ex: Exception) {
            Log.e(TAG, "ERROR while setting Symbl extension configuration")
        }
    }

    private fun enableEffect(pluginParams: JSONObject) {
        try {
            setSymblPluginConfigs(pluginParams)

            this.agView?.agkit?.setExtensionProperty(
                ExtensionManager.EXTENSION_VENDOR_NAME,
                ExtensionManager.EXTENSION_FILTER_NAME,
                "start",
                pluginParams.toString()
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun disableEffect() {
        val o = JSONObject()
        this.agView?.agkit?.setExtensionProperty(
            ExtensionManager.EXTENSION_VENDOR_NAME,
            ExtensionManager.EXTENSION_FILTER_NAME,
            "stop",
            o.toString()
        )
    }

    override fun onEvent(vendor: String, extension: String, key: String, value: String) {
        println("---- onEvent ----")

        Log.i(
            TAG,
            "Symbl conversation Event \n \n  $vendor  extension: $extension  key: $key  value: $value"
        )
        val sb = StringBuilder()
        sb.append(value)
        if ("result" == key) {
            try {
                val json = Gson()
                val symblResponse = json.fromJson(value, SymblResponse::class.java)
                if (symblResponse!!.getEvent() != null && symblResponse.getEvent().isNotEmpty()) {
                    when (symblResponse.getEvent()) {
                        SymblAIFilterManager.SYMBL_START_PLUGIN_REQUEST -> {}
                        SymblAIFilterManager.SYMBL_ON_MESSAGE -> try {
                            println("---- onMessage ----")
                            if (symblResponse.getErrorMessage() != null && symblResponse.getErrorMessage()
                                    .isNotEmpty()
                            ) {
                                Log.e(TAG, symblResponse.errorMessage)
                            }
                        } catch (ex: Exception) {
                            Log.e(
                                TAG,
                                "ERROR on Symbl message on message transform error " + ex.message
                            )
                        }
                        SymblAIFilterManager.SYMBL_CONNECTION_ERROR -> Log.i(
                            TAG,
                            "SYMBL_CONNECTION_ERROR error code %s , error message " + symblResponse.getErrorCode()
                        )
                        SymblAIFilterManager.SYMBL_WEBSOCKETS_CLOSED -> Log.i(
                            TAG,
                            "SYMBL_CONNECTION_ERROR " + symblResponse.getErrorCode()
                        )
                        SymblAIFilterManager.SYMBL_TOKEN_EXPIRED -> {}
                        SymblAIFilterManager.SYMBL_STOP_REQUEST -> {}
                        SymblAIFilterManager.SYMBL_ON_CLOSE -> {}
                        SymblAIFilterManager.SYMBL_SEND_REQUEST -> {}
                        SymblAIFilterManager.SYMBL_ON_ERROR -> {}
                    }
                } else { // all error cases handle here
                    sb.append(
                        """
Symbl event :${symblResponse.getEvent()}"""
                    )
                    sb.append(
                        """
Error Message :${symblResponse.getErrorMessage()}"""
                    )
                    sb.append(
                        """
Error code :${symblResponse.getErrorCode()}"""
                    )
                }
            } catch (exception: Exception) {
                println("result parse error ")
            }
        }

        this.runOnUiThread {
            Log.i(TAG, "--- onThread ---")
            Log.i(TAG, "---- $sb ----")
            infoTextView.text = sb.toString()
        }
    }
}