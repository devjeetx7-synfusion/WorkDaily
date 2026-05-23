# Firestore Architecture Refactor Guide

This document outlines the best practices and architectural changes implemented during the Firestore nested structure refactor.

## 🏗️ New Nested Structure

The application has moved from a flat structure to a strictly isolated, role-based nested hierarchy under each user:

- `users/{userId}`
  - `contractor/data/`
    - `workers/{workerId}`
      - `attendance/{date}`
    - `workTypes/{typeId}`
    - `summaries/{YYYY-MM}`
  - `personal/data/`
    - `attendance/{date}`
    - `summaries/{YYYY-MM}`

## ✅ Key Benefits

- **Strict Data Isolation:** Users only have access to their own sub-collections.
- **Improved Performance:** No `where` filters (like `contractorId`) are required for ownership checks.
- **Scalability:** Large datasets are naturally partitioned by user.
- **Summary Optimization:** Aggregated stats are pre-calculated in the `summaries` collection, reducing read costs and improving UI speed.

## 🛠️ Implementation Details

### 1. Centralized Repository (`FirestoreRepository.kt`)
Always use `FirestoreRepository` for Firestore interactions. It provides:
- Correct document paths for nested collections.
- Automatic cascade deletes for workers.
- Automatic monthly summary updates during attendance operations.

### 2. ViewModel Refactor
ViewModels now receive `FirestoreRepository` via `ViewModelFactory`. They should:
- Use path-based document access.
- Leverage `summaries` for stats display when possible.
- Implement optimistic UI updates for better UX.

### 3. Data Models
Redundant fields like `contractorId`, `user_id`, and `worker_id` have been removed from data classes. Document paths now define ownership and relationships.

## ⚠️ Edge Case Handling

### 1. Partial Migration
The `DataMigrationManager` handles moving legacy data. If migration fails midway:
- The app can be re-run to retry (migration is idempotent as it uses `set`).
- Legacy collections should only be cleaned up after full verification of the new structure.

### 2. Missing Data
- Use `snapshot.exists()` checks in listeners.
- Default values (e.g., default wages) are used when specific fields are missing.

## 🚀 Compose UI Integration Best Practices

- Use `StateFlow` from ViewModels to observe data changes.
- Wrap UI updates in `LaunchedEffect` or `collectAsStateWithLifecycle` for efficient lifecycle management.
- Provide clear loading and empty states for nested collections.
- Use `isRefreshing` flags to handle manual pull-to-refresh.

---
*Maintained by Jules - Software Engineer*
