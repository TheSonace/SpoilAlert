package com.example.spoilalert

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.spoilalert.ui.theme.SpoilAlertTheme
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        fun doDatabaseThings(driver: SqlDriver){
            val database = Database(driver)
            val itemQueries = database.itemQueries
            Log.d("TAG", itemQueries.selectAll().executeAsList().toString())
            itemQueries.insert(
                "0001",
                "2000-01-01",
                "1900-01-01",
                "2025-01-01")
            Log.d("TAG", itemQueries.selectAll().executeAsList().toString())
        }


        doDatabaseThings(AndroidSqliteDriver(Database.Schema, this, "launch.db"))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpoilAlertTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SpoilAlertTheme {
        Greeting("Android")
    }
}

