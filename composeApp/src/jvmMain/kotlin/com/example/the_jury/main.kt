package com.example.the_jury

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "the_jury",
    ) {
        App()
    }
}