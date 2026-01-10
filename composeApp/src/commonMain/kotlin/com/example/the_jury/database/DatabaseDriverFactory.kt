package com.example.the_jury.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific database driver factory.
 * Each platform (Android, JVM) will provide its own implementation.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}