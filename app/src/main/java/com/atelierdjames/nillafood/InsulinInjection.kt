package com.atelierdjames.nillafood

/** Model representing a single insulin injection recorded by the user. */

data class InsulinInjection(
    /** Unique identifier used when syncing with Nightscout. */
    val id: String,
    /** Time the injection occurred, in epoch milliseconds. */
    val time: Long,
    /** Name of the insulin that was injected. */
    val insulin: String,
    /** Number of units delivered. */
    val units: Float,
)
