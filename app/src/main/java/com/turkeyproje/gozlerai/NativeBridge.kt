package com.turkeyproje.gozlerai

object NativeBridge {
    init {
        System.loadLibrary("gozlerai_jni")
    }

    external fun nativeLoadModel(path: String): Boolean
    external fun nativeGenerate(prompt: String, maxTokens: Int): String
    external fun nativeUnload()
}
