package com.example.the_jury

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.example.the_jury.di.appModule
import com.example.the_jury.di.getPlatformModule
import com.example.the_jury.ui.screens.HomeScreen
import com.example.the_jury.ui.theme.AppTheme
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = {
        modules(appModule, getPlatformModule())
    }) {
        AppTheme {
            Navigator(HomeScreen) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}