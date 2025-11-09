package app

import com.github.michaelbull.result.*
import domain.*

class EquipmentAppService(
    private val equipmentRepository: IEquipmentRepository
) {
    fun registerEquipment(command: RegisterEquipmentCommand): Result<EquipmentDto, RegisterEquipmentError> {
        return binding {
            val equipmentName = EquipmentName.from(command.name)
                .mapError { RegisterEquipmentError.InvalidName(it) }
                .bind()

            val equipmentId = equipmentRepository.nextId()
            val equipment = Equipment.create(equipmentId, equipmentName)
            equipmentRepository.save(equipment)

            EquipmentDto(
                id = equipment.id.value,
                name = equipment.name.value,
                status = equipment.status.name
            )
        }
    }
}

data class RegisterEquipmentCommand(
    val name: String?
)

data class EquipmentDto(
    val id: String,
    val name: String,
    val status: String
)

sealed class RegisterEquipmentError {
    data class InvalidName(val error: EquipmentNameError) : RegisterEquipmentError()
}
