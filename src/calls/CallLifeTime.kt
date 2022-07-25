package org.synthesis.calls

import java.time.LocalDateTime

sealed class CallLifeTime {
    abstract fun isLive(): Boolean

    abstract val startDate: LocalDateTime
    abstract val endDate: LocalDateTime?

    data class Limited(
        override val startDate: LocalDateTime,
        override val endDate: LocalDateTime
    ) : CallLifeTime() {
        override fun isLive() = startDate <= LocalDateTime.now() && endDate >= LocalDateTime.now()
    }

    data class Unlimited(
        override val startDate: LocalDateTime,
        override val endDate: LocalDateTime? = null
    ) : CallLifeTime() {
        override fun isLive() = startDate <= LocalDateTime.now()
    }

    companion object {
        fun create(from: LocalDateTime, to: LocalDateTime? = null): CallLifeTime {
            if (to != null) {
                val endDate = to.withHour(23).withMinute(59)

                return Limited(from, endDate)
            }

            return Unlimited(from)
        }
    }
}
