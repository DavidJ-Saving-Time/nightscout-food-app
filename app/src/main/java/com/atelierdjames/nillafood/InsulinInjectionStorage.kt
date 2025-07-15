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

    suspend fun replaceAll(context: Context, injections: List<InsulinInjection>) {
        val dao = db(context).insulinDao()
        dao.deleteAll()
        val entities = injections.map { InsulinInjectionEntity.from(it) }
        if (entities.isNotEmpty()) {
            dao.insertAll(entities)
        }
    }

    suspend fun getLatestTimestamp(context: Context): Long? {
        return db(context).insulinDao().getLatestTimestamp()
    }

    suspend fun getLast7DaysSummary(context: Context): List<InsulinUsageSummary> {
        val zone = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zone)
        val startDate = today.minusDays(6)
        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()

        val injections = db(context).insulinDao().getSince(startMillis).map { it.toInjection() }
        val treatments = db(context).treatmentDao().getSince(startMillis).map { it.toTreatment() }

        val map = mutableMapOf<java.time.LocalDate, FloatArray>()
        for (inj in injections) {
            val date = java.time.Instant.ofEpochMilli(inj.time).atZone(zone).toLocalDate()
            if (date.isBefore(startDate) || date.isAfter(today)) continue
            val totals = map.getOrPut(date) { floatArrayOf(0f, 0f, 0f, 0f, 0f) }
            when {
                inj.insulin.equals("Novorapid", ignoreCase = true) -> totals[0] += inj.units
                inj.insulin.equals("Tresiba", ignoreCase = true) -> totals[1] += inj.units
            }
        }

        for (t in treatments) {
            val date = java.time.Instant.ofEpochMilli(t.timestamp).atZone(zone).toLocalDate()
            if (date.isBefore(startDate) || date.isAfter(today)) continue
            val totals = map.getOrPut(date) { floatArrayOf(0f, 0f, 0f, 0f, 0f) }
            totals[2] += t.carbs
            totals[3] += t.protein
            totals[4] += t.fat
        }

        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd")
        val result = mutableListOf<InsulinUsageSummary>()
        for (i in 0..6) {
            val date = startDate.plusDays(i.toLong())
            val totals = map[date] ?: floatArrayOf(0f, 0f, 0f, 0f, 0f)
            result.add(
                InsulinUsageSummary(
                    day = date.format(formatter),
                    novorapid = totals[0],
                    tresiba = totals[1],
                    carbs = totals[2],
                    protein = totals[3],
                    fat = totals[4]
                )
            )
        }
        return result.reversed()
    }
}
