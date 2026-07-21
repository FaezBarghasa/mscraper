package com.example.db

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SurrealDatabase(private val context: Context) {
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

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
