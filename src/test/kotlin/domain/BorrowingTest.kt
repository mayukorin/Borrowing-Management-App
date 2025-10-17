package domain

import com.github.michaelbull.result.*
import java.time.LocalDate
import kotlin.test.*

class BorrowingTest {

    // テスト用のヘルパー関数
    private fun createValidBorrowingId() = BorrowingId.from("brw-001").unwrap()
    private fun createValidEmployeeId() = EmployeeId.from("emp-001").unwrap()
    private fun createValidEquipmentId() = EquipmentId.from("eq-001").unwrap()
    private fun createValidPeriod(today: LocalDate = LocalDate.now()): Period {
        val from = today.plusDays(1)
        val to = today.plusDays(3)
        return Period.from(from, to, today).unwrap()
    }

    @Test
    fun `create - 正常に貸出情報を作成できる`() {
        // Arrange
        val id = createValidBorrowingId()
        val employeeId = createValidEmployeeId()
        val equipmentId = createValidEquipmentId()
        val period = createValidPeriod()

        // Act
        val borrowing = Borrowing.create(
            id = id,
            employeeId = employeeId,
            equipmentId = equipmentId,
            period = period
        )

        // Assert
        assertEquals(id, borrowing.id)
        assertEquals(employeeId, borrowing.employeeId)
        assertEquals(equipmentId, borrowing.equipmentId)
        assertEquals(period, borrowing.period)
    }
}
