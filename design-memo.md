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
    fun overlaps(other: Borrowing): Boolean
    fun contains(date: LocalDate): Boolean
}
```

**設計方針:**
- Borrowing 自体は返却状態を持たない（isReturned フィールドなし）
- 返却状態は Equipment が管理する（borrowings リストへの返却日時の記録など）
- 単一責任の原則: Borrowing は「誰が・何を・いつからいつまで借りるか」の情報のみを表現
- `isActiveOrFuture`: 貸出が現在進行中または未来の予約かを判定（内部で period.isOngoingOrFuture を呼び出す）
- `overlaps`: この貸出が他の貸出と期間が重複するかを判定（内部で period.overlaps を呼び出す）
- `contains`: この貸出が指定日を含むかを判定（内部で period.contains を呼び出す）

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
- 廃棄済み備品に対する返却はできない（status == DISPOSED の場合はエラー）
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

Period のフィールドへの直接アクセスは極力避け、以下のようなメソッドでカプセル化する：

```kotlin
// 期間が進行中または未来のものかを判定
fun isOngoingOrFuture(today: LocalDate): Boolean {
    return to >= today
}

// 期間の重複判定（連続している場合も重複と見なす）
fun overlaps(other: Period): Boolean {
    return this.from <= other.to && this.to >= other.from
}

// 特定日の含有判定（開始日と終了日を含む）
fun contains(date: LocalDate): Boolean {
    return date >= from && date <= to
}
```

### Borrowing の操作メソッド

Borrowing の period フィールドへの直接アクセスは極力避け、Borrowing のメソッドを通じてアクセスする：

```kotlin
// 貸出が現在進行中または未来の予約かを判定（内部で period.isOngoingOrFuture を呼び出す）
fun isActiveOrFuture(today: LocalDate): Boolean {
    return period.isOngoingOrFuture(today)
}

// 貸出が他の貸出と期間が重複するかを判定（内部で period.overlaps を呼び出す）
fun overlaps(other: Borrowing): Boolean {
    return period.overlaps(other.period)
}

// 貸出が指定日を含むかを判定（内部で period.contains を呼び出す）
fun contains(date: LocalDate): Boolean {
    return period.contains(date)
}
```

**カプセル化の階層**:
- Equipment は Borrowing のメソッドを呼び出す（Borrowing.period に直接アクセスしない）
- Borrowing は Period のメソッドを呼び出す（Period のフィールドに直接アクセスしない）
- これにより、Tell, Don't Ask の原則と Law of Demeter に従った設計を実現

**境界値の仕様**:
- `contains(date)`: 期間の開始日（from）と終了日（to）の両方を含む
  - `date >= from && date <= to`
  - 例: 10/20～10/25 の期間は、10/20 と 10/25 の両方を含む
- `overlaps(other)`: 連続している期間も重複と見なす
  - `this.from <= other.to && this.to >= other.from`
  - 例: 10/20～10/25 と 10/25～10/30 は 10/25 で重複（連続）していると見なす
  - 例: 10/20～10/25 と 10/26～10/30 は重複していない（断続的）

## アプリケーションサービス層の設計方針

### Command オブジェクトの使用

アプリケーションサービスのメソッドは、プリミティブ型の引数を直接受け取るのではなく、Command オブジェクトを受け取る設計とする。

**Command オブジェクトを使う理由:**
- **拡張性**: 引数が増えた場合でも、Command オブジェクトに新しいフィールドを追加するだけでメソッドシグネチャが変わらない
- **可読性**: メソッド呼び出し時に何を渡しているか明確になる（例: `RegisterEquipmentCommand(name = "プロジェクター")`）
- **バリデーションの集約**: Command オブジェクトにバリデーションロジックをまとめることができる
- **意図の明確化**: Command の名前（例: `RegisterEquipmentCommand`）が何をするかを明示する

**例:**
```kotlin
// 悪い例: プリミティブ型を直接受け取る
fun registerEquipment(name: String?): EquipmentDto

// 良い例: Command オブジェクトを受け取る
data class RegisterEquipmentCommand(val name: String?)
fun registerEquipment(command: RegisterEquipmentCommand): EquipmentDto
```

### DTO（Data Transfer Object）と Result 型の返却

アプリケーションサービスのメソッドは、ドメインエンティティを直接返すのではなく、DTO を Result 型でラップして返す設計とする。

**DTO を返す理由:**
- **レイヤー分離**: ドメイン層とプレゼンテーション層を分離し、ドメインモデルの変更がプレゼンテーション層に影響しないようにする
- **情報隠蔽**: エンティティの内部構造（例: すべてのフィールドや関係）を外部に公開せず、必要な情報だけを公開できる
- **柔軟性**: API レスポンスの形式を自由に設計できる（エンティティの構造に縛られない）
- **セキュリティ**: 機密情報や内部実装の詳細を隠蔽できる

**Result 型でラップする理由:**
- **ドメインエラーの型安全な処理**: ビジネスルール違反を型として表現し、コンパイラにエラーハンドリングを強制させる
- **エラーの明確な区別**: ドメインエラー（Result）とインフラエラー（Exception）を明確に分離
- **エラーの網羅性**: sealed class により、すべてのエラーケースの処理を保証
- **拡張性**: 新しいドメインエラーを追加しやすい

**例:**
```kotlin
// 悪い例: エンティティを直接返す
fun registerEquipment(command: RegisterEquipmentCommand): Equipment

// 良い例: Result<DTO, エラー型> を返す
data class EquipmentDto(
    val id: String,
    val name: String,
    val status: String
)

sealed class RegisterEquipmentError {
    data class InvalidName(val error: EquipmentNameError) : RegisterEquipmentError()
}

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
```

**エラーハンドリングの責務分担:**
- **ドメインエラー**: Result 型で返す（業務ルール違反、バリデーションエラーなど）
- **インフラエラー**: Exception をスローする（DB接続エラー、トランザクションエラーなど）

### リポジトリのエラーハンドリング

リポジトリ層でのエラー（例: DB接続エラー、トランザクションエラーなど）は、アプリケーション層で回復不可能なインフラストラクチャの問題であることが多い。そのため、`Result` 型で返すのではなく、Exception を throw する設計とする。

**理由:**
- インフラエラーはアプリケーション層で処理すべきではない（フレームワーク層で処理）
- ビジネスロジックのエラー（ドメインエラー）とインフラエラーを明確に区別できる
- コードがシンプルになる（Result のネストが不要）

**例:**
```kotlin
interface IEquipmentRepository {
    fun nextId(): EquipmentId
    fun save(equipment: Equipment)  // 失敗時は Exception を throw
    fun findById(id: EquipmentId): Equipment?  // 見つからない場合は null（正常なケース）
}
```

### ID生成の責務

エンティティの ID 生成は、ドメイン層の関心事ではなくインフラストラクチャ層の関心事として扱う。そのため、リポジトリが `nextId()` メソッドを提供し、ID 生成の実装詳細（UUID、シーケンス、カウンターなど）を隠蔽する。

**理由:**
- ID 生成方法（UUID、DB シーケンス、カウンターなど）は永続化の実装詳細
- ドメイン層は ID 生成の具体的な方法を知る必要がない
- テスト時に ID 生成をモック化しやすい

**例:**
```kotlin
val equipmentId = equipmentRepository.nextId()  // リポジトリが採番
val equipment = Equipment.create(equipmentId, name)
```
