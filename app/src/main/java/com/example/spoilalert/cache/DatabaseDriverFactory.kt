package com.example.spoilalert.cache

import app.cash.sqldelight.db.SqlDriver
import com.example.spoilalert.Database
import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(Database.Schema, context, "launch.db")
    }
}

