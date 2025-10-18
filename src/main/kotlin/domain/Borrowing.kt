package domain

import com.github.michaelbull.result.*
import java.time.LocalDate

class Borrowing private constructor(
    val id: BorrowingId,
    val employeeId: EmployeeId,
    val equipmentId: EquipmentId,
    val period: Period
) {
    companion object {
        fun create(
            id: BorrowingId,
            employeeId: EmployeeId,
            equipmentId: EquipmentId,
            period: Period
        ): Borrowing {
            return Borrowing(
                id = id,
                employeeId = employeeId,
                equipmentId = equipmentId,
                period = period
            )
        }
    }

    /**
     * この貸出が現在進行中または未来の予約かを判定する
     * @param today 基準日
     * @return 貸出期間が進行中または未来の場合 true
     */
    fun isActiveOrFuture(today: LocalDate): Boolean {
        return period.isOngoingOrFuture(today)
    }

    /**
     * この貸出が他の貸出と期間が重複するかを判定する
     * @param other 比較対象の貸出
     * @return 期間が重複する場合 true
     */
    fun overlaps(other: Borrowing): Boolean {
        return period.overlaps(other.period)
    }

    /**
     * この貸出が指定日を含むかを判定する
     * @param date 判定対象の日付
     * @return 貸出期間が指定日を含む場合 true
     */
    fun contains(date: LocalDate): Boolean {
        return period.contains(date)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Borrowing) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Borrowing(id=$id, employeeId=$employeeId, equipmentId=$equipmentId, period=$period)"
    }
}
