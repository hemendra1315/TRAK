package app.trak.core.model

/**
 * A 1–5 self-report rating. Each scale defines its anchors so a higher number never means
 * different things without a label (e.g. energy 5 = very high, soreness 5 = severe).
 * A missing rating is `null`, never 0.
 */
@JvmInline
value class Rating5 private constructor(val value: Int) {
    companion object {
        const val MIN = 1
        const val MAX = 5
        fun of(value: Int): Rating5? = if (value in MIN..MAX) Rating5(value) else null
    }
}

/** Session effort on the 1–10 RPE scale. A missing value is `null`, never 0. */
@JvmInline
value class Rpe10 private constructor(val value: Int) {
    companion object {
        const val MIN = 1
        const val MAX = 10
        fun of(value: Int): Rpe10? = if (value in MIN..MAX) Rpe10(value) else null
    }
}
