package com.atelierdjames.nillafood

data class InsulinUsageSummary(
    val day: String,
    val novorapid: Float,
    val tresiba: Float,
    val carbs: Float,
    val protein: Float,
    val fat: Float
)
