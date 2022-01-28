package com.rishav.symblai

import com.google.gson.annotations.SerializedName

data class SymblResult(
    @SerializedName("message") val message: Message? = null
)

data class Message(
    @SerializedName("punctuated") val payload: Punctuated? = null
)

data class Punctuated(
    @SerializedName("transcript") val transcript: String? = null
)
