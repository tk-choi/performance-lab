package com.perflab.sqllab.domain.model

import java.time.LocalDate

data class DateRange(
    val from: LocalDate,
    val to: LocalDate
) {
    init {
        require(from <= to) { "from($from) must be before or equal to to($to)" }
    }
}
