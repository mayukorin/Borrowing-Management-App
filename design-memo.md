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

import java.time.LocalDate

class Borrowing private constructor(
    val id: BorrowingId,
    val employeeId: EmployeeId,
    val equipmentId: EquipmentId,
    val period: Period
) {
    companion object {
        fun create(
            id: BorrowingId,
            employeeId: EmployeeId,
            equipmentId: EquipmentId,
            period: Period
        ): Borrowing
    }

    fun isActiveOrFuture(today: LocalDate): Boolean
}
```

**設計方針:**
- Borrowing 自体は返却状態を持たない（isReturned フィールドなし）
- 返却状態は Equipment が管理する（borrowings リストへの返却日時の記録など）
- 単一責任の原則: Borrowing は「誰が・何を・いつからいつまで借りるか」の情報のみを表現
- `isActiveOrFuture`: 貸出が現在進行中または未来の予約かを判定（内部で period.isOngoingOrFuture を呼び出す）

### Equipment

```kotlin
// Equipment.kt
package domain

import com.github.michaelbull.result.*
import java.time.LocalDate

class Equipment private constructor(
    val id: EquipmentId,
    val name: EquipmentName,
    val status: EquipmentStatus,
    val borrowings: List<Borrowing>
) {
    companion object {
        fun create(id: EquipmentId, name: EquipmentName): Equipment
    }

    fun borrow(borrowing: Borrowing, today: LocalDate): Result<Equipment, EquipmentError>

    fun returnBorrowing(borrowingId: BorrowingId, today: LocalDate): Result<Equipment, EquipmentError>

    fun dispose(today: LocalDate): Result<Equipment, EquipmentError>
}

enum class EquipmentStatus {
    AVAILABLE,
    BORROWED,
    DISPOSED
}

sealed class EquipmentError {
    data object AlreadyDisposed : EquipmentError()
    data class PeriodOverlap(val existingBorrowing: Borrowing, val newBorrowing: Borrowing) : EquipmentError()
    data class BorrowingNotFound(val borrowingId: BorrowingId) : EquipmentError()
    data object CannotDisposeWhileBorrowed : EquipmentError()
}
```

**設計方針:**
- `borrowings`: すべての貸出情報（過去・現在・未来の予約）を一元管理
- 現在貸出中かどうかは `borrowings` と `today` から判定可能
- 期間重複チェック時に `borrowings` 全体をチェックすることで、未来の予約との重複も検出できる
- 返却済みかどうかの判定は、Period と `today` の比較で実現（`borrowing.period.to < today` なら返却済み）
- 状態の一貫性: `(status == BORROWED) ⇔ (borrowings に today を含む期間の貸出が存在する)`

#### Equipment のライフイベント

**1. Equipment が新規作成される (`create`)**
- 初期状態は AVAILABLE
- borrowings は空リスト

**2. Equipment に新たに貸出が追加される (`borrow`)**
- 廃棄済み備品を借りることはできない（status == DISPOSED）
- 既存の borrowings と期間が重複する貸出は追加できない（PeriodOverlap エラー）
- 成功時は borrowings に新しい貸出を追加し、必要に応じて status を更新
  - 新しい貸出の期間が today を含む場合は status を BORROWED に更新
  - 未来の予約の場合は status は AVAILABLE のまま

**3. Equipment から貸出が返却される (`returnBorrowing`)**
- borrowings から該当する borrowingId を探す
- 見つからない場合はエラー（BorrowingNotFound）
- 成功時は該当する borrowing を borrowings から削除
- borrowings に today を含む期間の貸出がなくなった場合は status を AVAILABLE に戻す

**4. Equipment が廃棄される (`dispose`)**
- 現在貸出中の場合（borrowings に today を含む期間の貸出が存在する場合）は廃棄できない（CannotDisposeWhileBorrowed）
- 未来の予約が残っている場合も廃棄できない（CannotDisposeWhileBorrowed）
- 既に廃棄済みの場合はエラー（AlreadyDisposed）
- 成功時は status を DISPOSED に変更
- 廃棄後は AVAILABLE に戻せない（状態の不可逆性）

## チェック責務

- NotNull/NotEmpty のバリデーションを `from` メソッドで実施
- ID のフォーマットチェック（EmployeeId: "emp-" で始まる、EquipmentId: "eq-" で始まる、BorrowingId: "brw-" で始まる）
- Period のチェック（from < to、過去日でないこと）
- 期間重複チェックは Equipment が責務を持つ（borrowings 全体との重複確認）
- 返却チェックは Equipment が責務を持つ（borrowings からの該当 borrowing の検索）
- コンストラクタは private にして、不正な状態のオブジェクト生成を防ぐ
- Result パターンでエラーハンドリング
- Period と Equipment の各メソッドは `today` を引数で受け取ることで、テスタビリティを確保
- Equipment は不変性を保つため、各操作メソッドで新しいインスタンスを返す

## カプセル化の方針

### Period の操作メソッド

Period のフィールドへの直接アクセスは極力避け、以下のようなメソッドでカプセル化する方針：

```kotlin
// 期間が進行中または未来のものかを判定（Equipment の dispose メソッドで使用）
fun isOngoingOrFuture(today: LocalDate): Boolean {
    return to >= today
}

// 期間の重複判定（Equipment の borrow メソッドで使用予定）
fun overlaps(other: Period): Boolean {
    return this.from < other.to && this.to > other.from
}

// 特定日の含有判定
fun contains(date: LocalDate): Boolean {
    return date >= from && date < to  // または date <= to（要件による）
}
```

**注意**:
- ビジネスルールのチェック（「貸出期間が重なる予約はできない」等）は Equipment 起点で実装する
- これらのメソッドは Equipment 実装時に必要に応じて追加する
