package com.example.the_jury

import java.util.Properties

actual object Config {
    actual fun getApiKey(): String {
        return try {
            val properties = Properties()
            val inputStream = this::class.java.classLoader.getResourceAsStream("local.properties")
            if (inputStream != null) {
                properties.load(inputStream)
                properties.getProperty("API_KEY", "")
            } else {
                // Fallback: try to read from system property or environment
                System.getProperty("API_KEY") ?: System.getenv("API_KEY") ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}