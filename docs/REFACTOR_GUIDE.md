# Firestore Architecture Refactor Guide

This document outlines the architectural patterns and best practices implemented during the Firestore nested structure refactor for the **WorkDaily** ecosystem.

## 🏗️ New Nested Structure

The application has transitioned from a flat collection structure to a strictly isolated, role-based nested hierarchy under each user document. This ensures data privacy and simplifies security rules.

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

- **Strict Data Isolation:** Users have exclusive access to their own sub-collections, preventing unauthorized data exposure.
- **Improved Performance:** Eliminates the need for complex `where` filters (e.g., `contractorId`) for basic ownership checks.
- **Scalability:** Data is naturally partitioned by user, supporting large-scale usage without performance degradation.
- **Summary Optimization:** Aggregated statistics are pre-calculated in the `summaries` collection, significantly reducing read costs and enhancing UI responsiveness.

## 🛠️ Implementation Details

### 1. Centralized Repository (`FirestoreRepository.kt`)
All Firestore interactions must be routed through the `FirestoreRepository`. Key responsibilities include:
- Resolving correct document paths for nested collections.
- Managing automatic cascade deletions for worker-related records.
- Handling monthly summary updates atomically during attendance operations.

### 2. ViewModel Architecture
ViewModels receive the `FirestoreRepository` via a `ViewModelFactory`. Standard practices include:
- Utilizing path-based document access for precision.
- Prioritizing `summaries` for high-level dashboard statistics.
- Implementing optimistic UI updates to provide a responsive user experience.

### 3. Data Models
Redundant identification fields (e.g., `contractorId`, `user_id`) have been removed from data classes. Document paths now serve as the primary source of truth for ownership and relational context.

## ⚠️ Edge Case Handling

### 1. Partial Migration
The `DataMigrationManager` handles the transition of legacy data. If a migration is interrupted:
- The process is idempotent (using `set` operations), allowing for safe re-runs.
- Legacy collections are retained until full verification of the new structure is confirmed.

### 2. Data Integrity
- Implement `snapshot.exists()` checks in all listeners.
- Utilize default values (e.g., standard wage rates) when specific fields are absent in the database.

## 🚀 Compose UI Best Practices

- Observe data changes using `StateFlow` from ViewModels.
- Manage UI updates within `LaunchedEffect` or `collectAsStateWithLifecycle` for lifecycle-aware execution.
- Provide comprehensive loading, empty, and error states for all nested data operations.
- Utilize `isRefreshing` flags to provide standard pull-to-refresh functionality.
