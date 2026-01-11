package com.example.the_jury.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), ".jury/jury.db")
        databasePath.parentFile?.mkdirs()
        
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
        
        // Create schema - this is safe now because we use IF NOT EXISTS in SQL files
        JuryDatabase.Schema.create(driver)
        
        return driver
    }
}