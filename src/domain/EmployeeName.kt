package domain

import com.github.michaelbull.result.*

class EmployeeName private constructor(val value: String) {
    companion object {
        fun from(value: String?): Result<EmployeeName, EmployeeNameError> {
            if (value == null) return Err(EmployeeNameError.Null)
            if (value.isEmpty()) return Err(EmployeeNameError.Empty(value))
            return Ok(EmployeeName(value))
        }
    }
}

sealed class EmployeeNameError {
    data object Null : EmployeeNameError()
    data class Empty(val value: String) : EmployeeNameError()
}
