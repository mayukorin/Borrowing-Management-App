package domain

import com.github.michaelbull.result.*
import java.time.LocalDate

class Period private constructor(
    val from: LocalDate,
    val to: LocalDate
) {
    companion object {
        fun from(from: LocalDate?, to: LocalDate?, today: LocalDate): Result<Period, PeriodError> {
            if (from == null) return Err(PeriodError.FromIsNull)
            if (to == null) return Err(PeriodError.ToIsNull)
            if (from >= to) return Err(PeriodError.InvalidRange(from, to))
            if (from < today) return Err(PeriodError.PastDate(from, today))
            return Ok(Period(from, to))
        }
    }

    override fun toString(): String = "Period(from=$from, to=$to)"
}

sealed class PeriodError {
    data object FromIsNull : PeriodError()
    data object ToIsNull : PeriodError()
    data class InvalidRange(val from: LocalDate, val to: LocalDate) : PeriodError()
    data class PastDate(val from: LocalDate, val today: LocalDate) : PeriodError()
}
