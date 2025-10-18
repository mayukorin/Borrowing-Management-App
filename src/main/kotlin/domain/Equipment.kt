package domain

import com.github.michaelbull.result.*
import java.time.LocalDate

class Equipment private constructor(
    val id: EquipmentId,
    val name: EquipmentName,
    val status: EquipmentStatus,
    val borrowings: List<Borrowing>
) {
    companion object {
        fun create(id: EquipmentId, name: EquipmentName): Equipment {
            return Equipment(
                id = id,
                name = name,
                status = EquipmentStatus.AVAILABLE,
                borrowings = emptyList()
            )
        }
    }

    fun dispose(today: LocalDate): Result<Equipment, EquipmentError> {
        // 既に廃棄済みの場合はエラー
        if (status == EquipmentStatus.DISPOSED) {
            return Err(EquipmentError.AlreadyDisposed)
        }
        // 現在貸出中または未来の予約がある場合は廃棄できない
        val hasActiveOrFutureBorrowing = borrowings.any { borrowing ->
            borrowing.isActiveOrFuture(today)
        }
        if (hasActiveOrFutureBorrowing) {
            return Err(EquipmentError.CannotDisposeWhileBorrowed)
        }

        return Ok(
            Equipment(
                id = id,
                name = name,
                status = EquipmentStatus.DISPOSED,
                borrowings = borrowings
            )
        )
    }
}

enum class EquipmentStatus {
    AVAILABLE,
    BORROWED,
    DISPOSED
}

sealed class EquipmentError {
    data object AlreadyDisposed : EquipmentError()
    data object AlreadyBorrowed : EquipmentError()
    data class PeriodOverlap(val existingBorrowing: Borrowing, val newBorrowing: Borrowing) : EquipmentError()
    data class BorrowingNotFound(val borrowingId: BorrowingId) : EquipmentError()
    data object CannotDisposeWhileBorrowed : EquipmentError()
}
