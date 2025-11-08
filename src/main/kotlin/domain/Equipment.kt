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

        internal fun createForTest(
            id: EquipmentId,
            name: EquipmentName,
            status: EquipmentStatus,
            borrowings: List<Borrowing>
        ): Equipment {
            return Equipment(id, name, status, borrowings)
        }
    }

    fun borrow(borrowing: Borrowing, today: LocalDate): Result<Equipment, EquipmentError> {
        // 廃棄済み備品を借りることはできない
        if (status == EquipmentStatus.DISPOSED) {
            return Err(EquipmentError.AlreadyDisposed)
        }

        // 既存の borrowings と期間が重複する貸出は追加できない
        val overlappingBorrowing = borrowings.find { existing ->
            existing.overlaps(borrowing)
        }
        if (overlappingBorrowing != null) {
            return Err(EquipmentError.PeriodOverlap(overlappingBorrowing, borrowing))
        }

        // 新しい貸出を追加
        val newBorrowings = borrowings + borrowing

        // 新しい貸出の期間が today を含む場合は status を BORROWED に更新
        val newStatus = if (borrowing.contains(today)) {
            EquipmentStatus.BORROWED
        } else {
            status
        }

        return Ok(
            Equipment(
                id = id,
                name = name,
                status = newStatus,
                borrowings = newBorrowings
            )
        )
    }

    fun returnBorrowing(borrowingId: BorrowingId, today: LocalDate): Result<Equipment, EquipmentError> {
        // 廃棄済み備品に対する返却は禁止
        if (status == EquipmentStatus.DISPOSED) {
            return Err(EquipmentError.AlreadyDisposed)
        }

        // borrowings から該当する borrowingId を探す
        if (borrowings.none { it.id == borrowingId }) {
            return Err(EquipmentError.BorrowingNotFound(borrowingId))
        }

        // 該当する borrowing を borrowings から削除
        val newBorrowings = borrowings.filter { it.id != borrowingId }

        // borrowings に today を含む期間の貸出がなくなった場合は status を AVAILABLE に戻す
        val hasCurrentBorrowing = newBorrowings.any { borrowing ->
            borrowing.contains(today)
        }
        val newStatus = if (hasCurrentBorrowing) {
            EquipmentStatus.BORROWED
        } else {
            EquipmentStatus.AVAILABLE
        }

        return Ok(
            Equipment(
                id = id,
                name = name,
                status = newStatus,
                borrowings = newBorrowings
            )
        )
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
