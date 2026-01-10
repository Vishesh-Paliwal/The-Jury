package com.example.the_jury.di

import com.example.the_jury.database.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

val jvmModule = module {
    single { DatabaseDriverFactory() }
}

actual fun getPlatformModule(): Module = jvmModule