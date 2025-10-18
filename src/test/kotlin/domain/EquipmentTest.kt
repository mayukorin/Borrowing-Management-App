package domain

import com.github.michaelbull.result.*
import java.time.LocalDate
import kotlin.test.*

class EquipmentTest {

    // テスト用のヘルパー関数
    private fun createValidEquipmentId() = EquipmentId.from("eq-001").unwrap()
    private fun createValidEquipmentName() = EquipmentName.from("プロジェクター").unwrap()
    private fun createValidEmployeeId() = EmployeeId.from("emp-001").unwrap()
    private fun createValidBorrowingId() = BorrowingId.from("brw-001").unwrap()

    private fun createValidPeriod(
        from: LocalDate,
        to: LocalDate,
        today: LocalDate = LocalDate.now()
    ): Period {
        return Period.from(from, to, today).unwrap()
    }

    private fun createBorrowing(
        id: BorrowingId,
        employeeId: EmployeeId,
        equipmentId: EquipmentId,
        period: Period
    ): Borrowing {
        return Borrowing.create(
            id = id,
            employeeId = employeeId,
            equipmentId = equipmentId,
            period = period
        )
    }

    @Test
    fun `create - 正常に備品を作成できる`() {
        // Arrange
        val id = createValidEquipmentId()
        val name = createValidEquipmentName()

        // Act
        val equipment = Equipment.create(id, name)

        // Assert
        assertEquals(id, equipment.id)
        assertEquals(name, equipment.name)
        assertEquals(EquipmentStatus.AVAILABLE, equipment.status)
        assertTrue(equipment.borrowings.isEmpty())
    }

    @Test
    fun `dispose - 過去の貸出のみ存在する場合、正常に廃棄できる`() {
        // Arrange
        val today = LocalDate.of(2025, 10, 20)
        val id = createValidEquipmentId()
        val name = createValidEquipmentName()

        // 過去の貸出を作成（2025-10-10 から 2025-10-15、today より前に終了）
        val pastPeriod = createValidPeriod(
            from = LocalDate.of(2025, 10, 10),
            to = LocalDate.of(2025, 10, 15),
            today = LocalDate.of(2025, 10, 9)
        )
        val pastBorrowing = createBorrowing(
            id = createValidBorrowingId(),
            employeeId = createValidEmployeeId(),
            equipmentId = id,
            period = pastPeriod
        )

        // 過去の貸出を含む備品を作成（内部的にborrowingsリストに追加）
        val equipment = Equipment.create(id, name)
        // Equipment の内部状態を直接操作できないため、リフレクションまたは
        // Equipment クラスに borrowings を含むコンストラクタを使用
        // ここでは簡易的に、Equipment の private コンストラクタを利用
        val equipmentWithPastBorrowing = Equipment::class.java
            .getDeclaredConstructor(
                EquipmentId::class.java,
                EquipmentName::class.java,
                EquipmentStatus::class.java,
                List::class.java
            )
            .apply { isAccessible = true }
            .newInstance(id, name, EquipmentStatus.AVAILABLE, listOf(pastBorrowing))

        // Act
        val result = equipmentWithPastBorrowing.dispose(today)

        // Assert
        assertTrue(result is Ok)
        val disposedEquipment = result.unwrap()
        assertEquals(EquipmentStatus.DISPOSED, disposedEquipment.status)
        assertEquals(id, disposedEquipment.id)
        assertEquals(name, disposedEquipment.name)
        assertEquals(listOf(pastBorrowing), disposedEquipment.borrowings)
    }

    @Test
    fun `dispose - 既に廃棄済みの場合、エラーを返す`() {
        // Arrange
        val today = LocalDate.of(2025, 10, 20)
        val id = createValidEquipmentId()
        val name = createValidEquipmentName()

        // 既に廃棄済みの備品を作成
        val disposedEquipment = Equipment::class.java
            .getDeclaredConstructor(
                EquipmentId::class.java,
                EquipmentName::class.java,
                EquipmentStatus::class.java,
                List::class.java
            )
            .apply { isAccessible = true }
            .newInstance(id, name, EquipmentStatus.DISPOSED, emptyList<Borrowing>())

        // Act
        val result = disposedEquipment.dispose(today)

        // Assert
        assertTrue(result is Err)
        assertEquals(EquipmentError.AlreadyDisposed, result.unwrapError())
    }

    @Test
    fun `dispose - 現在貸出中の場合、エラーを返す`() {
        // Arrange
        val today = LocalDate.of(2025, 10, 20)
        val id = createValidEquipmentId()
        val name = createValidEquipmentName()

        // 現在貸出中の貸出を作成（2025-10-18 から 2025-10-25、today を含む）
        val currentPeriod = createValidPeriod(
            from = LocalDate.of(2025, 10, 18),
            to = LocalDate.of(2025, 10, 25),
            today = LocalDate.of(2025, 10, 17)
        )
        val currentBorrowing = createBorrowing(
            id = createValidBorrowingId(),
            employeeId = createValidEmployeeId(),
            equipmentId = id,
            period = currentPeriod
        )

        val equipmentWithCurrentBorrowing = Equipment::class.java
            .getDeclaredConstructor(
                EquipmentId::class.java,
                EquipmentName::class.java,
                EquipmentStatus::class.java,
                List::class.java
            )
            .apply { isAccessible = true }
            .newInstance(id, name, EquipmentStatus.BORROWED, listOf(currentBorrowing))

        // Act
        val result = equipmentWithCurrentBorrowing.dispose(today)

        // Assert
        assertTrue(result is Err)
        assertEquals(EquipmentError.CannotDisposeWhileBorrowed, result.unwrapError())
    }

    @Test
    fun `dispose - 未来の予約がある場合、エラーを返す`() {
        // Arrange
        val today = LocalDate.of(2025, 10, 20)
        val id = createValidEquipmentId()
        val name = createValidEquipmentName()

        // 未来の予約を作成（2025-10-25 から 2025-10-30、today より後）
        val futurePeriod = createValidPeriod(
            from = LocalDate.of(2025, 10, 25),
            to = LocalDate.of(2025, 10, 30),
            today = LocalDate.of(2025, 10, 20)
        )
        val futureBorrowing = createBorrowing(
            id = createValidBorrowingId(),
            employeeId = createValidEmployeeId(),
            equipmentId = id,
            period = futurePeriod
        )

        val equipmentWithFutureBorrowing = Equipment::class.java
            .getDeclaredConstructor(
                EquipmentId::class.java,
                EquipmentName::class.java,
                EquipmentStatus::class.java,
                List::class.java
            )
            .apply { isAccessible = true }
            .newInstance(id, name, EquipmentStatus.AVAILABLE, listOf(futureBorrowing))

        // Act
        val result = equipmentWithFutureBorrowing.dispose(today)

        // Assert
        assertTrue(result is Err)
        assertEquals(EquipmentError.CannotDisposeWhileBorrowed, result.unwrapError())
    }

    @Test
    fun `borrow - 廃棄済み備品を借りることはできない`() {
        // Arrange
        val today = LocalDate.of(2025, 10, 20)
        val id = createValidEquipmentId()
        val name = createValidEquipmentName()

        // 廃棄済み備品を作成
        val disposedEquipment = Equipment::class.java
            .getDeclaredConstructor(
                EquipmentId::class.java,
                EquipmentName::class.java,
                EquipmentStatus::class.java,
                List::class.java
            )
            .apply { isAccessible = true }
            .newInstance(id, name, EquipmentStatus.DISPOSED, emptyList<Borrowing>())

        // 新しい貸出を作成
        val period = createValidPeriod(
            from = LocalDate.of(2025, 10, 20),
            to = LocalDate.of(2025, 10, 25),
            today = today
        )
        val borrowing = createBorrowing(
            id = createValidBorrowingId(),
            employeeId = createValidEmployeeId(),
            equipmentId = id,
            period = period
        )

        // Act
        val result = disposedEquipment.borrow(borrowing, today)

        // Assert
        assertTrue(result is Err)
        assertEquals(EquipmentError.AlreadyDisposed, result.unwrapError())
    }

    @Test
    fun `borrow - 既存の貸出と期間が重複する貸出は追加できない`() {
        // Arrange
        val today = LocalDate.of(2025, 10, 20)
        val id = createValidEquipmentId()
        val name = createValidEquipmentName()

        // 既存の貸出を作成（2025-10-20 から 2025-10-25）
        val existingPeriod = createValidPeriod(
            from = LocalDate.of(2025, 10, 20),
            to = LocalDate.of(2025, 10, 25),
            today = today
        )
        val existingBorrowing = createBorrowing(
            id = BorrowingId.from("brw-001").unwrap(),
            employeeId = createValidEmployeeId(),
            equipmentId = id,
            period = existingPeriod
        )

        val equipment = Equipment::class.java
            .getDeclaredConstructor(
                EquipmentId::class.java,
                EquipmentName::class.java,
                EquipmentStatus::class.java,
                List::class.java
            )
            .apply { isAccessible = true }
            .newInstance(id, name, EquipmentStatus.BORROWED, listOf(existingBorrowing))

        // 重複する期間の新しい貸出を作成（2025-10-23 から 2025-10-28、既存と重複）
        val overlappingPeriod = createValidPeriod(
            from = LocalDate.of(2025, 10, 23),
            to = LocalDate.of(2025, 10, 28),
            today = today
        )
        val newBorrowing = createBorrowing(
            id = BorrowingId.from("brw-002").unwrap(),
            employeeId = createValidEmployeeId(),
            equipmentId = id,
            period = overlappingPeriod
        )

        // Act
        val result = equipment.borrow(newBorrowing, today)

        // Assert
        assertTrue(result is Err)
        val error = result.unwrapError()
        assertTrue(error is EquipmentError.PeriodOverlap)
        assertEquals(existingBorrowing, error.existingBorrowing)
        assertEquals(newBorrowing, error.newBorrowing)
    }

    @Test
    fun `borrow - 本日を含む貸出をした場合は、statusが「貸出中」になること`() {
        // Arrange
        val today = LocalDate.of(2025, 10, 20)
        val id = createValidEquipmentId()
        val name = createValidEquipmentName()

        val equipment = Equipment.create(id, name)

        // 本日を含む貸出を作成（2025-10-20 から 2025-10-25）
        val period = createValidPeriod(
            from = LocalDate.of(2025, 10, 20),
            to = LocalDate.of(2025, 10, 25),
            today = today
        )
        val borrowing = createBorrowing(
            id = createValidBorrowingId(),
            employeeId = createValidEmployeeId(),
            equipmentId = id,
            period = period
        )

        // Act
        val result = equipment.borrow(borrowing, today)

        // Assert
        assertTrue(result is Ok)
        val borrowedEquipment = result.unwrap()
        assertEquals(EquipmentStatus.BORROWED, borrowedEquipment.status)
        assertTrue(borrowedEquipment.borrowings.contains(borrowing))
        assertEquals(id, borrowedEquipment.id)
        assertEquals(name, borrowedEquipment.name)
    }

    @Test
    fun `borrow - 未来の貸出をした場合は、statusは「AVAILABLE」のままである`() {
        // Arrange
        val today = LocalDate.of(2025, 10, 20)
        val id = createValidEquipmentId()
        val name = createValidEquipmentName()

        val equipment = Equipment.create(id, name)

        // 未来の貸出を作成（2025-10-25 から 2025-10-30、today より後に開始）
        val futurePeriod = createValidPeriod(
            from = LocalDate.of(2025, 10, 25),
            to = LocalDate.of(2025, 10, 30),
            today = today
        )
        val futureBorrowing = createBorrowing(
            id = createValidBorrowingId(),
            employeeId = createValidEmployeeId(),
            equipmentId = id,
            period = futurePeriod
        )

        // Act
        val result = equipment.borrow(futureBorrowing, today)

        // Assert
        assertTrue(result is Ok)
        val equipmentWithReservation = result.unwrap()
        assertEquals(EquipmentStatus.AVAILABLE, equipmentWithReservation.status)
        assertTrue(equipmentWithReservation.borrowings.contains(futureBorrowing))
        assertEquals(id, equipmentWithReservation.id)
        assertEquals(name, equipmentWithReservation.name)
    }

    @Test
    fun `borrow - 貸出開始日当日（from == today）は、statusが「貸出中」になること`() {
        // Arrange
        val today = LocalDate.of(2025, 10, 20)
        val id = createValidEquipmentId()
        val name = createValidEquipmentName()

        val equipment = Equipment.create(id, name)

        // 貸出開始日が today と同じ貸出を作成（2025-10-20 から 2025-10-25）
        val period = createValidPeriod(
            from = today,
            to = LocalDate.of(2025, 10, 25),
            today = today
        )
        val borrowing = createBorrowing(
            id = createValidBorrowingId(),
            employeeId = createValidEmployeeId(),
            equipmentId = id,
            period = period
        )

        // Act
        val result = equipment.borrow(borrowing, today)

        // Assert
        assertTrue(result is Ok)
        val borrowedEquipment = result.unwrap()
        assertEquals(EquipmentStatus.BORROWED, borrowedEquipment.status)
        assertTrue(borrowedEquipment.borrowings.contains(borrowing))
        assertEquals(id, borrowedEquipment.id)
        assertEquals(name, borrowedEquipment.name)
    }

    @Test
    fun `borrow - 貸出終了日当日（to == today）は、statusは「貸出中」である`() {
        // Arrange
        val today = LocalDate.of(2025, 10, 25)
        val id = createValidEquipmentId()
        val name = createValidEquipmentName()

        val equipment = Equipment.create(id, name)

        // 貸出終了日が today と同じ貸出を作成（2025-10-20 から 2025-10-25）
        val period = createValidPeriod(
            from = LocalDate.of(2025, 10, 20),
            to = today,
            today = LocalDate.of(2025, 10, 19)
        )
        val borrowing = createBorrowing(
            id = createValidBorrowingId(),
            employeeId = createValidEmployeeId(),
            equipmentId = id,
            period = period
        )

        // Act
        val result = equipment.borrow(borrowing, today)

        // Assert
        assertTrue(result is Ok)
        val borrowedEquipment = result.unwrap()
        assertEquals(EquipmentStatus.BORROWED, borrowedEquipment.status)
        assertTrue(borrowedEquipment.borrowings.contains(borrowing))
        assertEquals(id, borrowedEquipment.id)
        assertEquals(name, borrowedEquipment.name)
    }
}
