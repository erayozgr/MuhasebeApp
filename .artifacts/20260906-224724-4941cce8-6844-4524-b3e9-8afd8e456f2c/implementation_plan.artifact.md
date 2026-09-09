# Fix Transaction Sorting and Missing Types in Reports

The "All Transactions" section in the Reporting screen has sorting issues:
1. Transactions from the same day are sorted incorrectly; the newest must be at the top regardless of category.
2. Collections (tahsilat) and payments (ödeme) must be confirmed as visible in this list.

## Proposed Changes

### UI Logic & Sorting

#### [RaporlamaScreen.kt](file:///D:/Projeler/MuhasebeApp/shared/src/commonMain/kotlin/com/eray/muhasebeapp/ui/screens/RaporlamaScreen.kt)

- **Enhanced Sorting Logic:** I will update the sorting of `hafifList` to use a more precise comparison. Instead of just `parseTarihMillis`, I will sort by a combination of timestamp (descending) and transaction ID (descending) to act as a tie-breaker for transactions with identical timestamps.
- **Unified List Display:** Ensure that within each date group, all transaction types (Sales, Purchases, Expenses, Collections, Payments, Stock) are interleaved and sorted strictly by time (newest first).
- **Excel Filter Fix:** Fix the logical error in the Excel export wizard where payments were being incorrectly filtered in the "Genel Rapor".

### Data Models (Robustness)

#### [CommonModels.kt](file:///D:/Projeler/MuhasebeApp/shared/src/commonMain/kotlin/com/eray/muhasebeapp/data/model/CommonModels.kt)

- Make `tarih`, `musteriAdi`, and `tedarikciAdi` nullable in `Tahsilat` and `TedarikciOdemesi` models to prevent serialization failures if the API response is incomplete.
- Add `id` to `HafifIslem` sealed class to facilitate sorting by ID.

```kotlin
sealed class HafifIslem(val tarih: String, val id: Long) {
    class S(val satis: Satis) : HafifIslem(satis.tarih ?: "", satis.id ?: 0L)
    // ... other types
}
```

## Verification Plan

### Manual Verification
- Use `analyze_file` to ensure no syntax errors.
- Verify the sorting logic by reviewing the `sortedWith` / `compareByDescending` implementation.
- Check `IslemKart` to ensure it correctly displays all 6 transaction types.
