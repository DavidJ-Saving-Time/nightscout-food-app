package com.atelierdjames.nillafood

import android.content.Context

object InsulinInjectionStorage {
    private fun db(context: Context) = DatabaseProvider.db(context)

    suspend fun getAll(context: Context): List<InsulinInjection> {
        return db(context).insulinDao().getAll().map { it.toInjection() }
    }

    suspend fun addAll(context: Context, injections: List<InsulinInjection>) {
        val entities = injections.map { InsulinInjectionEntity.from(it) }
        if (entities.isNotEmpty()) {
            db(context).insulinDao().insertAll(entities)
        }
    }

    suspend fun getLatestTimestamp(context: Context): String? {
        return db(context).insulinDao().getLatestTimestamp()
    }
}
