package com.atelierdjames.nillafood

data class TimeInRange(
    val inRange: Float,
    val above: Float,
    val below: Float
)

data class GlucoseStats(
    val avg24h: Float,
    val avg7d: Float,
    val avg14d: Float,
    val tir24h: TimeInRange,
    val tir7d: TimeInRange,
    val tir14d: TimeInRange,
    val hba1c: Float,
    val sd: Float,
    val daysUsed: Int
)
