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
