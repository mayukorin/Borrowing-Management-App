package domain

import com.github.michaelbull.result.*

class BorrowingId private constructor(val value: String) {
    companion object {
        fun from(value: String?): Result<BorrowingId, BorrowingIdError> {
            if (value == null) return Err(BorrowingIdError.Null)
            if (!value.startsWith("brw-")) return Err(BorrowingIdError.InvalidFormat(value))
            return Ok(BorrowingId(value))
        }
    }

    override fun toString(): String = "BorrowingId($value)"
}

sealed class BorrowingIdError {
    data object Null : BorrowingIdError()
    data class InvalidFormat(val value: String) : BorrowingIdError()
}
