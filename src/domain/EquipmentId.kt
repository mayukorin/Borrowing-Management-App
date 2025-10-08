package domain

import com.github.michaelbull.result.*

class EquipmentId private constructor(val value: String) {
    companion object {
        fun from(value: String?): Result<EquipmentId, EquipmentIdError> {
            if (value == null) return Err(EquipmentIdError.Null)
            if (!value.startsWith("eq-")) return Err(EquipmentIdError.InvalidFormat(value))
            return Ok(EquipmentId(value))
        }
    }
}

sealed class EquipmentIdError {
    data object Null : EquipmentIdError()
    data class InvalidFormat(val value: String) : EquipmentIdError()
}
