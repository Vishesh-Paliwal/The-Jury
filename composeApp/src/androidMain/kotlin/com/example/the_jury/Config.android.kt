package com.example.the_jury

actual object Config {
    actual fun getApiKey(): String {
        return try {
            // Use BuildConfig which is generated from local.properties
            com.example.the_jury.BuildConfig.API_KEY
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}