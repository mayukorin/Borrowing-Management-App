# 設計メモ

## Value Object のメソッドシグネチャ

### EmployeeName

```kotlin
// EmployeeName.kt
package domain

import com.github.michaelbull.result.*

class EmployeeName private constructor(val value: String) {
    companion object {
        fun from(value: String?): Result<EmployeeName, EmployeeNameError>
    }
}

sealed class EmployeeNameError {
    data object Null : EmployeeNameError()
    data class Empty(val value: String) : EmployeeNameError()
}
```

### EquipmentName

```kotlin
// EquipmentName.kt
package domain

import com.github.michaelbull.result.*

class EquipmentName private constructor(val value: String) {
    companion object {
        fun from(value: String?): Result<EquipmentName, EquipmentNameError>
    }
}

sealed class EquipmentNameError {
    data object Null : EquipmentNameError()
    data class Empty(val value: String) : EquipmentNameError()
}
```

### EmployeeId

```kotlin
// EmployeeId.kt
package domain

import com.github.michaelbull.result.*

class EmployeeId private constructor(val value: String) {
    companion object {
        fun from(value: String?): Result<EmployeeId, EmployeeIdError>
    }
}

sealed class EmployeeIdError {
    data object Null : EmployeeIdError()
    data class InvalidFormat(val value: String) : EmployeeIdError()
}
```

### EquipmentId

```kotlin
// EquipmentId.kt
package domain

import com.github.michaelbull.result.*

class EquipmentId private constructor(val value: String) {
    companion object {
        fun from(value: String?): Result<EquipmentId, EquipmentIdError>
    }
}

sealed class EquipmentIdError {
    data object Null : EquipmentIdError()
    data class InvalidFormat(val value: String) : EquipmentIdError()
}
```

### Period

```kotlin
// Period.kt
package domain

import com.github.michaelbull.result.*
import java.time.LocalDate

class Period private constructor(
    val from: LocalDate,
    val to: LocalDate
) {
    companion object {
        fun from(from: LocalDate?, to: LocalDate?, today: LocalDate): Result<Period, PeriodError>
    }
}

sealed class PeriodError {
    data object FromIsNull : PeriodError()
    data object ToIsNull : PeriodError()
    data class InvalidRange(val from: LocalDate, val to: LocalDate) : PeriodError()
    data class PastDate(val from: LocalDate, val today: LocalDate) : PeriodError()
}
```

### BorrowingId

```kotlin
// BorrowingId.kt
package domain

import com.github.michaelbull.result.*

class BorrowingId private constructor(val value: String) {
    companion object {
        fun from(value: String?): Result<BorrowingId, BorrowingIdError>
    }
}

sealed class BorrowingIdError {
    data object Null : BorrowingIdError()
    data class InvalidFormat(val value: String) : BorrowingIdError()
}
```

## Entity のメソッドシグネチャ

### Borrowing

```kotlin
// Borrowing.kt
package domain

import com.github.michaelbull.result.*

class Borrowing private constructor(
    val id: BorrowingId,
    val employeeId: EmployeeId,
    val equipmentId: EquipmentId,
    val period: Period,
    val isReturned: Boolean
) {
    companion object {
        fun create(
            id: BorrowingId,
            employeeId: EmployeeId,
            equipmentId: EquipmentId,
            period: Period
        ): Borrowing
    }

    fun return(): Result<Borrowing, BorrowingError>
}

sealed class BorrowingError {
    data object AlreadyReturned : BorrowingError()
}
```

### Equipment

```kotlin
// Equipment.kt
package domain

import com.github.michaelbull.result.*

class Equipment private constructor(
    val id: EquipmentId,
    val name: EquipmentName,
    val status: EquipmentStatus,
    val borrowings: List<Borrowing>
) {
    companion object {
        fun create(id: EquipmentId, name: EquipmentName): Equipment
    }

    fun borrow(borrowing: Borrowing): Result<Equipment, EquipmentError>

    fun returnBorrowing(borrowingId: BorrowingId): Result<Equipment, EquipmentError>

    fun dispose(): Result<Equipment, EquipmentError>
}

enum class EquipmentStatus {
    AVAILABLE,
    BORROWED,
    DISPOSED
}

sealed class EquipmentError {
    data object AlreadyDisposed : EquipmentError()
    data object AlreadyBorrowed : EquipmentError()
    data class PeriodOverlap(val existingBorrowing: Borrowing, val newBorrowing: Borrowing) : EquipmentError()
    data class BorrowingNotFound(val borrowingId: BorrowingId) : EquipmentError()
    data object CannotDisposeWhileBorrowed : EquipmentError()
}
```

#### Equipment のライフイベント

**1. Equipment が新規作成される (`create`)**
- 初期状態は AVAILABLE
- borrowings は空リスト

**2. Equipment に新たに貸出が追加される (`borrow`)**
- 廃棄済み備品を借りることはできない（status == DISPOSED）
- 返却されていない備品を再度貸し出すことはできない（borrowings に isReturned = false のものがないか）
- 貸出期間が重なる予約はできない（borrowings リストで期間重複チェック）
- 成功時は status を BORROWED に更新

**3. Equipment から貸出が返却される (`returnBorrowing`)**
- 該当する borrowingId の貸出を borrowings リストから探す
- 見つかった borrowing に対して `borrowing.return()` を実行
- 全ての貸出が返却済みなら status を AVAILABLE に戻す

**4. Equipment が廃棄される (`dispose`)**
- 貸出中の備品は廃棄できない（borrowings に isReturned = false のものがないか）
- 既に廃棄済みの場合はエラー
- 成功時は status を DISPOSED に変更
- 廃棄後は AVAILABLE に戻せない（状態の不可逆性）

## チェック責務

- NotNull/NotEmpty のバリデーションを `from` メソッドで実施
- ID のフォーマットチェック（EmployeeId: "emp-" で始まる、EquipmentId: "eq-" で始まる、BorrowingId: "brw-" で始まる）
- Period のチェック（from < to、過去日でないこと）
- Borrowing の返却チェック（既に返却済みの貸出を再度返却できない）
- コンストラクタは private にして、不正な状態のオブジェクト生成を防ぐ
- Result パターンでエラーハンドリング
- Period は `today` を引数で受け取ることで、テスタビリティを確保
- Borrowing は不変性を保つため、`return()` で新しいインスタンスを返す
