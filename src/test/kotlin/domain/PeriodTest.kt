package domain

import com.github.michaelbull.result.*
import java.time.LocalDate
import kotlin.test.*

class PeriodTest {

    // テスト用のヘルパー関数
    private fun createValidPeriod(
        from: LocalDate,
        to: LocalDate,
        today: LocalDate = LocalDate.now()
    ): Period {
        return Period.from(from, to, today).unwrap()
    }

    @Test
    fun `overlaps - 期間が連続している場合は重複と見なす（終了日と開始日が同じ）`() {
        // Arrange
        val period1 = createValidPeriod(
            from = LocalDate.of(2025, 10, 20),
            to = LocalDate.of(2025, 10, 25),
            today = LocalDate.of(2025, 10, 19)
        )
        val period2 = createValidPeriod(
            from = LocalDate.of(2025, 10, 25),
            to = LocalDate.of(2025, 10, 30),
            today = LocalDate.of(2025, 10, 19)
        )

        // Act
        val result = period1.overlaps(period2)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `overlaps - 期間が断続的な場合は重複と見做さない（期間1の終了日の翌日が期間2の開始日）`() {
        // Arrange
        val period1 = createValidPeriod(
            from = LocalDate.of(2025, 10, 20),
            to = LocalDate.of(2025, 10, 25),
            today = LocalDate.of(2025, 10, 19)
        )
        val period2 = createValidPeriod(
            from = LocalDate.of(2025, 10, 26),
            to = LocalDate.of(2025, 10, 30),
            today = LocalDate.of(2025, 10, 19)
        )

        // Act
        val result = period1.overlaps(period2)

        // Assert
        assertFalse(result)
    }
}
