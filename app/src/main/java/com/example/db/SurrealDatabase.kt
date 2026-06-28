package com.example.db

import android.content.Context
import com.surrealdb.driver.Surreal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SurrealDatabase(private val context: Context) {
    private val driver = Surreal()
    private val prefs = context.getSharedPreferences("surreal_db_prefs", Context.MODE_PRIVATE)

    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val endpoint = prefs.getString("endpoint", "file://" + context.filesDir.absolutePath + "/music.db") ?: ""
            driver.connect(endpoint)
            
            val ns = prefs.getString("namespace", "crysta") ?: "crysta"
            val db = prefs.getString("database", "music") ?: "music"
            driver.use(ns, db)

            // For SurrealDB v3+, embedded file mode doesn't strictly require signin if it's local only,
            // but we'll provide the option for remote/auth-enabled setups.
            if (endpoint.startsWith("ws") || endpoint.startsWith("http")) {
                val user = prefs.getString("username", "root") ?: "root"
                val pass = prefs.getString("password", "root") ?: "root"
                driver.signin(user, pass)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDriver(): Surreal = driver

    companion object {
        @Volatile
        private var INSTANCE: SurrealDatabase? = null

        fun getInstance(context: Context): SurrealDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = SurrealDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
