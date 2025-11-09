package domain

interface IEquipmentRepository {
    fun nextId(): EquipmentId
    fun save(equipment: Equipment)
    fun findById(id: EquipmentId): Equipment?
}
