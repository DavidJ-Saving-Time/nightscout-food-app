package com.atelierdjames.nillafood

import android.content.Context

object TreatmentStorage {
    private fun db(context: Context) = DatabaseProvider.db(context)

    suspend fun getAll(context: Context): List<Treatment> {
        return db(context).treatmentDao().getAll().map { it.toTreatment() }
    }

    suspend fun addOrUpdate(context: Context, treatments: List<Treatment>) {
        val entities = treatments.mapNotNull { TreatmentEntity.from(it) }
        if (entities.isNotEmpty()) {
            db(context).treatmentDao().insertAll(entities)
        }
    }

    suspend fun getLatestTimestamp(context: Context): Long? {
        return db(context).treatmentDao().getLatestTimestamp()
    }
}
