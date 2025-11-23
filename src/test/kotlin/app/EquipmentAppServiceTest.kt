package app

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import domain.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EquipmentAppServiceTest {
    private lateinit var equipmentRepository: IEquipmentRepository
    private lateinit var equipmentAppService: EquipmentAppService

    companion object {
        private const val VALID_EQUIPMENT_ID = "eq-001"
        private const val VALID_EQUIPMENT_NAME = "プロジェクター"
    }

    @BeforeEach
    fun setup() {
        equipmentRepository = mockk()
        equipmentAppService = EquipmentAppService(equipmentRepository)
    }

    @Test
    fun `有効な備品名で備品を登録できる`() {
        // Given
        val expectedId = EquipmentId.from(VALID_EQUIPMENT_ID).unwrap()
        every { equipmentRepository.nextId() } returns expectedId
        every { equipmentRepository.save(any()) } just Runs

        // When
        val result = equipmentAppService.registerEquipment(
            RegisterEquipmentCommand(name = VALID_EQUIPMENT_NAME)
        )

        // Then
        assertTrue(result is Ok)
        val dto = result.value
        assertEquals(VALID_EQUIPMENT_ID, dto.id)
        assertEquals(VALID_EQUIPMENT_NAME, dto.name)
        assertEquals("AVAILABLE", dto.status)

        verify(exactly = 1) { equipmentRepository.nextId() }
        verify(exactly = 1) { equipmentRepository.save(any()) }
    }

    @Test
    fun `備品名がnullの場合はInvalidNameエラーを返す`() {
        // When
        val result = equipmentAppService.registerEquipment(
            RegisterEquipmentCommand(name = null)
        )

        // Then
        assertTrue(result is Err)
        val error = result.error
        assertTrue(error is RegisterEquipmentError.InvalidName)
        assertTrue(error.error is EquipmentNameError.Null)

        verify(exactly = 0) { equipmentRepository.save(any()) }
    }

    @Test
    fun `備品名が空文字列の場合はInvalidNameエラーを返す`() {
        // When
        val result = equipmentAppService.registerEquipment(
            RegisterEquipmentCommand(name = "")
        )

        // Then
        assertTrue(result is Err)
        val error = result.error
        assertTrue(error is RegisterEquipmentError.InvalidName)
        assertTrue(error.error is EquipmentNameError.Empty)
        assertEquals("", (error.error as EquipmentNameError.Empty).value)

        verify(exactly = 0) { equipmentRepository.save(any()) }
    }
}