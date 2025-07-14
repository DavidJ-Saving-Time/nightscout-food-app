package com.atelierdjames.nillafood

/**
 * Amount of insulin on board for a given timestamp.
 */
data class IOBPoint(
    val ts: Long,
    val iob: Float
)

/**
 * Remaining active insulin from this injection at [timestamp].
 */
fun InsulinInjection.iobAt(timestamp: Long, activityWindowMs: Long): Float {
    if (timestamp < time) return 0f
    if (timestamp > time + activityWindowMs) return 0f
    val elapsed = timestamp - time
    val remainingFrac = 1f - (elapsed.toFloat() / activityWindowMs)
    return units * remainingFrac
}

/**
 * Generate decay points for this injection using a fixed step.
 */
fun InsulinInjection.decayPoints(
    activityWindowMs: Long,
    stepMs: Long = 300000L
): List<IOBPoint> {
    val result = mutableListOf<IOBPoint>()
    var ts = time
    val end = time + activityWindowMs
    while (ts <= end) {
        result.add(IOBPoint(ts, iobAt(ts, activityWindowMs)))
        ts += stepMs
    }
    return result
}

/**
 * Sum decay of all injections into a combined series.
 */
fun List<InsulinInjection>.toIobSeries(
    activityWindowMs: Long,
    stepMs: Long = 300000L
): List<IOBPoint> {
    if (isEmpty()) return emptyList()
    val start = this.minOf { it.time }
    val end = this.maxOf { it.time } + activityWindowMs
    val result = mutableListOf<IOBPoint>()
    var ts = start
    while (ts <= end) {
        var total = 0f
        for (inj in this) {
            total += inj.iobAt(ts, activityWindowMs)
        }
        result.add(IOBPoint(ts, total))
        ts += stepMs
    }
    return result
}
