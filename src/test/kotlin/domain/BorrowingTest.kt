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
        assertFalse(borrowing.isReturned, "新規作成時は未返却であるべき")
    }

    @Test
    fun `markAsReturned - 正常に返却できる`() {
        // Arrange
        val borrowing = Borrowing.create(
            id = createValidBorrowingId(),
            employeeId = createValidEmployeeId(),
            equipmentId = createValidEquipmentId(),
            period = createValidPeriod()
        )

        // Act
        val result = borrowing.markAsReturned()

        // Assert
        assertTrue(result is Ok, "返却は成功するべき")
        val returnedBorrowing = result.unwrap()
        assertTrue(returnedBorrowing.isReturned, "返却後は isReturned が true であるべき")
    }

    @Test
    fun `markAsReturned - 既に返却済みの貸出を再度返却するとエラーになる`() {
        // Arrange
        val borrowing = Borrowing.create(
            id = createValidBorrowingId(),
            employeeId = createValidEmployeeId(),
            equipmentId = createValidEquipmentId(),
            period = createValidPeriod()
        )
        val returnedBorrowing = borrowing.markAsReturned().unwrap()

        // Act
        val result = returnedBorrowing.markAsReturned()

        // Assert
        assertTrue(result is Err, "既に返却済みの場合はエラーであるべき")
        val error = result.unwrapError()
        assertEquals(BorrowingError.AlreadyReturned, error)
    }
}
