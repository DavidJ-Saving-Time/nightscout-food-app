package com.atelierdjames.nillafood

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MealDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var mealDao: MealDao
    private lateinit var treatmentDao: TreatmentDao
    private lateinit var glucoseDao: GlucoseDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mealDao = db.mealDao()
        treatmentDao = db.treatmentDao()
        glucoseDao = db.glucoseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun simpleMealImpact() = runBlocking {
        val meal = TreatmentEntity("m1", 20f, 0f, 0f, "", 1000L)
        treatmentDao.insertAll(listOf(meal))
        val baseline = GlucoseEntry("g1", 100f, null, null, 900L, null)
        val peak = GlucoseEntry("g2", 150f, null, null, 5400L, null)
        glucoseDao.insertAll(listOf(baseline, peak))

        val impacts = mealDao.getRecentMealImpacts()
        assertEquals(1, impacts.size)
        val impact = impacts[0]
        assertEquals(20f, impact.carbs, 0.001f)
        assertEquals(150f, impact.peak!!, 0.001f)
        assertEquals(50f, impact.delta!!, 0.001f)
    }

    @Test
    fun noDataAfterMeal() = runBlocking {
        val meal = TreatmentEntity("m2", 30f, 0f, 0f, "", 10000L)
        treatmentDao.insertAll(listOf(meal))
        val baseline = GlucoseEntry("g3", 110f, null, null, 9900L, null)
        glucoseDao.insertAll(listOf(baseline))

        val impacts = mealDao.getRecentMealImpacts()
        assertEquals(1, impacts.size)
        val impact = impacts[0]
        assertNull(impact.peak)
        assertNull(impact.delta)
    }
}

