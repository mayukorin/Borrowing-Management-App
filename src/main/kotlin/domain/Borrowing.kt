package domain

import com.github.michaelbull.result.*

class Borrowing private constructor(
    val id: BorrowingId,
    val employeeId: EmployeeId,
    val equipmentId: EquipmentId,
    val period: Period,
    val isReturned: Boolean
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
                period = period,
                isReturned = false
            )
        }
    }

    fun markAsReturned(): Result<Borrowing, BorrowingError> {
        if (isReturned) {
            return Err(BorrowingError.AlreadyReturned)
        }
        return Ok(
            Borrowing(
                id = id,
                employeeId = employeeId,
                equipmentId = equipmentId,
                period = period,
                isReturned = true
            )
        )
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
        return "Borrowing(id=$id, employeeId=$employeeId, equipmentId=$equipmentId, period=$period, isReturned=$isReturned)"
    }
}

sealed class BorrowingError {
    data object AlreadyReturned : BorrowingError()
}
