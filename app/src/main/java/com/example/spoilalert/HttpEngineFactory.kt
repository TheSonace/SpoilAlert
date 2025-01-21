package com.example.spoilalert

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

class HttpEngineFactory {

    fun create(): HttpClientEngine {
        return Android.create()
    }
}