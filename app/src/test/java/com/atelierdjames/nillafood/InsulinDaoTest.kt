package com.atelierdjames.nillafood

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InsulinDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: InsulinInjectionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.insulinDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun linearDecaySummedAcrossInjections() = runBlocking {
        val injections = listOf(
            InsulinInjectionEntity("i1", 0L, "rapid", 1f),
            InsulinInjectionEntity("i2", 300000L, "rapid", 1f)
        )
        dao.insertAll(injections)

        val expected = injections.map { it.toInjection() }
            .toIobSeries(activityWindowMs = 600000L)

        val result = dao.streamIOB(600000L).first()

        assertEquals(expected.size, result.size)
        for (i in expected.indices) {
            assertEquals(expected[i].ts, result[i].ts)
            assertEquals(expected[i].iob, result[i].iob, 0.0001f)
        }
    }
}
