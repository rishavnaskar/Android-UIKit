package com.rishav.banuba

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import com.banuba.agora.plugin.BanubaEffectsLoader
import com.banuba.agora.plugin.BanubaExtensionManager
import com.banuba.agora.plugin.BanubaResourceManager
import com.banuba.agora.plugin.model.ArEffect
import com.rishav.banuba.widget.carousel.EffectsCarouselView
import io.agora.agorauikit_android.AgoraConnectionData
import io.agora.agorauikit_android.AgoraVideoViewer
import io.agora.agorauikit_android.requestPermissions
import io.agora.rtc2.Constants
import io.agora.rtc2.video.VideoEncoderConfiguration

@ExperimentalUnsignedTypes
class CallActivity : AppCompatActivity() {
    private var agView: AgoraVideoViewer? = null

    private val banubaResourceManager by lazy(LazyThreadSafetyMode.NONE) {
        BanubaResourceManager(this)
    }

    private val videoEncoderConfiguration = VideoEncoderConfiguration(
        VideoEncoderConfiguration.VD_840x480,
        VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
        VideoEncoderConfiguration.STANDARD_BITRATE,
        VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
    )

    private val onEffectPrepared = object : BanubaResourceManager.EffectPreparedCallback {
        override fun onPrepared(effectName: String) {
            sendEffectToFilter(effectName)
        }
    }
    private val effectsCarouselCallback = object : EffectsCarouselView.ActionCallback {
        override fun onEffectsSelected(effect: ArEffect) {
            banubaResourceManager.prepareEffect(effect.name, onEffectPrepared)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        prepareBanuba()
        System.loadLibrary("banuba");

        try {
            // Initializing Android UI Kit
            this.agView = AgoraVideoViewer(
                this,
                AgoraConnectionData(
                    appId = appID,
                    extensionName = listOf(BanubaExtensionManager.EXTENSION_NAME)
                )
            )

            val frameLayout: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ).also { it.setMargins(0, 0, 0, 250) }

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
        } catch (e: Exception) {
            println("--- Error Running AgView ----")
        }
    }

    private fun initAgoraEngine() {
        agView!!.agkit.enableExtension(
            BanubaExtensionManager.VENDOR_NAME,
            BanubaExtensionManager.VIDEO_FILTER_NAME,
            true
        )
        agView!!.agkit.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        agView!!.agkit.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        agView!!.agkit.setVideoEncoderConfiguration(videoEncoderConfiguration)
        agView!!.agkit.enableVideo()
        agView!!.agkit.enableAudio()
        agView!!.agkit.setLocalRenderMode(
            Constants.RENDER_MODE_HIDDEN,
            Constants.VIDEO_MIRROR_MODE_DISABLED
        )
        agView!!.join("test", role = Constants.CLIENT_ROLE_BROADCASTER)
        initBanuba()
    }

    private fun prepareBanuba() {
        banubaResourceManager.prepare()
        val effectsCarouselView: EffectsCarouselView = findViewById(R.id.effectsCarouselView)
        effectsCarouselView.actionCallback = effectsCarouselCallback
        val effects = BanubaEffectsLoader(this).loadEffects()
        effectsCarouselView.setEffectsList(listOf(ArEffect.EMPTY) + effects, 0)
    }

    private fun initBanuba() {
        agView!!.agkit.setExtensionProperty(
            BanubaExtensionManager.VENDOR_NAME,
            BanubaExtensionManager.VIDEO_FILTER_NAME,
            BanubaExtensionManager.KEY_SET_RESOURCES_PATH,
            banubaResourceManager.resourcesPath
        )
        agView!!.agkit.setExtensionProperty(
            BanubaExtensionManager.VENDOR_NAME,
            BanubaExtensionManager.VIDEO_FILTER_NAME,
            BanubaExtensionManager.KEY_SET_EFFECTS_PATH,
            banubaResourceManager.effectsPath
        )
        agView!!.agkit.setExtensionProperty(
            BanubaExtensionManager.VENDOR_NAME,
            BanubaExtensionManager.VIDEO_FILTER_NAME,
            BanubaExtensionManager.KEY_SET_TOKEN,
            banubaClientToken
        )
    }

    private fun sendEffectToFilter(effect: String) {
        agView!!.agkit.setExtensionProperty(
            BanubaExtensionManager.VENDOR_NAME,
            BanubaExtensionManager.VIDEO_FILTER_NAME,
            BanubaExtensionManager.KEY_LOAD_EFFECT,
            effect
        )
    }
}