package domain

import com.github.michaelbull.result.*
import java.time.LocalDate

data class Period private constructor(
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

    /**
     * 指定された日付に対して、この期間が進行中または未来のものかを判定する
     * @param today 基準日
     * @return 期間の終了日が基準日以降の場合 true
     */
    fun isOngoingOrFuture(today: LocalDate): Boolean {
        return to >= today
    }

    /**
     * 期間の重複判定
     * @param other 比較対象の期間
     * @return 期間が重複する場合 true（連続している場合も重複と見なす）
     */
    fun overlaps(other: Period): Boolean {
        return this.from <= other.to && this.to >= other.from
    }

    /**
     * 特定日の含有判定
     * @param date 判定対象の日付
     * @return 期間が指定日を含む場合 true
     */
    fun contains(date: LocalDate): Boolean {
        return date >= from && date <= to
    }
}

sealed class PeriodError {
    data object FromIsNull : PeriodError()
    data object ToIsNull : PeriodError()
    data class InvalidRange(val from: LocalDate, val to: LocalDate) : PeriodError()
    data class PastDate(val from: LocalDate, val today: LocalDate) : PeriodError()
}
