package com.example.the_jury.di

import android.content.Context
import com.example.the_jury.database.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(get<Context>()) }
}

actual fun getPlatformModule(): Module = androidModule