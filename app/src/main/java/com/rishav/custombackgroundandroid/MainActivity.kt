package com.rishav.custombackgroundandroid

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import io.agora.agorauikit_android.AgoraButton
import io.agora.agorauikit_android.AgoraConnectionData
import io.agora.agorauikit_android.AgoraVideoViewer
import io.agora.agorauikit_android.requestPermissions
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.video.VirtualBackgroundSource

import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.dhaval2404.imagepicker.ImagePicker

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {
    private var agView: AgoraVideoViewer? = null
    private val tag = "VirtualBackground"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            this.agView =
                AgoraVideoViewer(this, AgoraConnectionData(appId = appID))

            val frameLayout: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )

            this.addContentView(agView, frameLayout)

            if (AgoraVideoViewer.requestPermissions(this)) {
                joinCall()
            } else {
                val joinButton = Button(this)
                val joinString = "Allow Camera and Microphone, then click here"
                joinButton.text = joinString
                joinButton.setOnClickListener {
                    if (AgoraVideoViewer.requestPermissions(this)) {
                        (joinButton.parent as ViewGroup).removeView(joinButton)
                        joinCall()
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
            Log.i(tag, "---- ERROR ----")
            e.message?.let { Log.i(tag, it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        this.agView?.leaveChannel()
    }

    private fun joinCall() {
        val backgroundButton = addBackgroundButton()
        this.agView?.join("test", role = Constants.CLIENT_ROLE_BROADCASTER)

        var toggle = false
        backgroundButton.setOnClickListener {
            if (toggle) {
                if (isLegacyExternalStoragePermissionRequired()) {
                    requestLegacyWriteExternalStoragePermission();
                } else {
                    toggle = !toggle

                    // For virtual background IMAGE
                    pickImageFromGallery()

                    // For virtual background COLOR
//                    val backgroundSource = virtualBackgroundCOLOR(0xFFB6C2)
//                    enableVirtualBackground(backgroundSource)

                    // For virtual background BLUR
//                    val backgroundSource =
//                        virtualBackgroundBLUR(VirtualBackgroundSource.BLUR_DEGREE_MEDIUM)
//                    backgroundSource?.let { it1 -> enableVirtualBackground(it1) }
                }
            } else {
                toggle = !toggle
                disableCustomBackground()
            }
        }
    }

    private fun addBackgroundButton(): AgoraButton {
        val backgroundButton = AgoraButton(this)
        backgroundButton.setBackgroundResource(R.drawable.ic_baseline_image_24)
        this.agView?.agoraSettings?.extraButtons?.add(backgroundButton)

        return backgroundButton
    }

    private val pickImageFromGalleryResult = registerForActivityResult(
        StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val data: Intent = it.data!!
            val backgroundSource = virtualBackgroundIMG(data.data!!.path)
            enableVirtualBackground(backgroundSource)
            return@registerForActivityResult
        } else {
            Log.e(tag, "--- ERROR AFTER IMAGE ---")
        }
    }

    private fun pickImageFromGallery() {
        ImagePicker.with(this).crop().compress(1024).maxResultSize(1080, 1080).createIntent {
            pickImageFromGalleryResult.launch(it)
        }
    }

    private fun virtualBackgroundIMG(imgSrc: String? = null): VirtualBackgroundSource {
        val backgroundSource = VirtualBackgroundSource()
        backgroundSource.backgroundSourceType = VirtualBackgroundSource.BACKGROUND_IMG
        backgroundSource.source = imgSrc
        return backgroundSource
    }

    @Suppress("SameParameterValue")
    private fun virtualBackgroundCOLOR(color: Int): VirtualBackgroundSource {
        val backgroundSource = VirtualBackgroundSource()
        backgroundSource.backgroundSourceType = VirtualBackgroundSource.BACKGROUND_COLOR
        backgroundSource.color = color
        return backgroundSource
    }


    private fun virtualBackgroundBLUR(blurDegree: Int): VirtualBackgroundSource? {
        if (blurDegree > 3 || blurDegree < 0) {
            Log.i(tag, "Invalid Blur Degree")
            return null
        }
        val backgroundSource = VirtualBackgroundSource()
        backgroundSource.backgroundSourceType = VirtualBackgroundSource.BACKGROUND_BLUR
        backgroundSource.blur_degree = blurDegree
        return backgroundSource
    }

    private fun enableVirtualBackground(backgroundSource: VirtualBackgroundSource) {
        this.agView?.agkit?.enableVirtualBackground(true, backgroundSource)
        this.agView?.agkit?.addHandler(object : IRtcEngineEventHandler() {
            override fun onVirtualBackgroundSourceEnabled(enabled: Boolean, reason: Int) {
                super.onVirtualBackgroundSourceEnabled(enabled, reason)
                Log.i(tag, "Virtual Background - ${backgroundSource.backgroundSourceType}")
                println(enabled)
                println(reason)
            }
        })
    }

    private fun disableCustomBackground() {
        this.agView?.agkit?.enableVirtualBackground(false, VirtualBackgroundSource())
    }

    private fun isLegacyExternalStoragePermissionRequired(): Boolean {
        val permissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return Build.VERSION.SDK_INT < 29 && !permissionGranted
    }

    private fun requestLegacyWriteExternalStoragePermission() {
        val permission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val code = 456
        ActivityCompat.requestPermissions(this, permission, code)
    }
}