package domain

import com.github.michaelbull.result.*

class EmployeeId private constructor(val value: String) {
    companion object {
        fun from(value: String?): Result<EmployeeId, EmployeeIdError> {
            if (value == null) return Err(EmployeeIdError.Null)
            if (!value.startsWith("emp-")) return Err(EmployeeIdError.InvalidFormat(value))
            return Ok(EmployeeId(value))
        }
    }
}

sealed class EmployeeIdError {
    data object Null : EmployeeIdError()
    data class InvalidFormat(val value: String) : EmployeeIdError()
}
