package domain

import com.github.michaelbull.result.*

class EquipmentName private constructor(val value: String) {
    companion object {
        fun from(value: String?): Result<EquipmentName, EquipmentNameError> {
            if (value == null) return Err(EquipmentNameError.Null)
            if (value.isEmpty()) return Err(EquipmentNameError.Empty(value))
            return Ok(EquipmentName(value))
        }
    }
}

sealed class EquipmentNameError {
    data object Null : EquipmentNameError()
    data class Empty(val value: String) : EquipmentNameError()
}
