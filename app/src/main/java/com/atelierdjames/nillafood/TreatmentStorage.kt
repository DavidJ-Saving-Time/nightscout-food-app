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

    suspend fun replaceAll(context: Context, treatments: List<Treatment>) {
        val dao = db(context).treatmentDao()
        dao.deleteAll()
        val entities = treatments.mapNotNull { TreatmentEntity.from(it) }
        if (entities.isNotEmpty()) {
            dao.insertAll(entities)
        }
    }

    suspend fun delete(context: Context, id: String) {
        db(context).treatmentDao().deleteById(id)
    }

    suspend fun getLatestTimestamp(context: Context): Long? {
        return db(context).treatmentDao().getLatestTimestamp()
    }

    suspend fun getSince(context: Context, start: Long): List<Treatment> {
        return db(context).treatmentDao().getSince(start).map { it.toTreatment() }
    }
}
