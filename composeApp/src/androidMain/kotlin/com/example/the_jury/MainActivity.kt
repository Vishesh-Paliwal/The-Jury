package com.example.the_jury

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplication
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.example.the_jury.di.appModule
import com.example.the_jury.di.getPlatformModule
import com.example.the_jury.ui.screens.HomeScreen
import com.example.the_jury.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AndroidApp()
        }
    }
}

@Composable
fun AndroidApp() {
    val context = LocalContext.current
    KoinApplication(application = {
        androidContext(context)
        modules(appModule, getPlatformModule())
    }) {
        AppTheme {
            Navigator(HomeScreen) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    AndroidApp()
}