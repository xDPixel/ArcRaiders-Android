# Ark Companion Android - Component-Based Development Plan & Tracker

This document organizes the deliverables for the Ark Companion Android application by functional components/modules, shifting away from chronological timelines. It serves as a visual dashboard and tracking framework to easily assess the project's overall progress at the component level.

---

## 1. Tracking Framework Definitions

All components are tracked using the following three distinct status categories:

- 🟢 **Completed**: The component is fully implemented, thoroughly tested, code reviewed, and documented. It meets all PRD acceptance criteria.
- 🟡 **Needs Enhancement**: The component is functional and integrated but requires optimization, bug fixes, architectural refinement, or additional features to meet the strict PRD standards.
- 🔴 **To Be Developed**: Work on this component has not yet started, or it is in an incomplete, non-functional state.

### Review Process
- **Weekly Component Review**: Stakeholders and developers review the status of each component.
- **Criteria for "Completed"**:
  - 100% of defined deliverables implemented.
  - Test coverage report shows > 80% coverage for the specific module.
  - Passed peer code review.
  - KDoc / inline documentation updated.
  - Verified adherence to zero-data-collection and stateless constraints.

---

## 2. Visual Dashboard

| Component / Module | Status | Completion % | Critical Path |
| :--- | :---: | :---: | :---: |
| **Core Architecture & Setup** | 🟡 | 80% | Yes |
| **Network & Data Layer** | 🟡 | 90% | Yes |
| **Design System (One UI 8.5)** | 🟡 | 60% | Yes |
| **Hideout Progression Simulator** | � | 100% | No |
| **ARC Intelligence Module** | � | 100% | No |
| **Item Database & Analytics** | � | 100% | Yes |
| **Trader & Quest Ecosystem** | 🔴 | 0% | No |
| **Interactive Map Engine** | 🔴 | 0% | No |

---

## 3. Detailed Component Deliverables

### 3.1 Core Architecture & Setup
**Status**: 🟡 Needs Enhancement  
**Description**: Base project scaffolding, dependency management, and environment configurations.
- [x] Scaffold Android project (Gradle, Manifest).
- [x] Configure dependency injection (Manual or Koin/Hilt).
- [x] Implement Kotlin Coroutines setup.
- [ ] **Needs Enhancement**: Resolve JDK 25 vs JDK 17 conflict on build machine to unblock CLI compilation.
- **Deliverables**: `build.gradle.kts` configuration, `App.kt` setup, environment audit documentation.

### 3.2 Network & Data Layer
**Status**: 🟡 Needs Enhancement  
**Description**: Stateless REST API communication, error handling, and in-memory caching.
- [x] Implement `ApiClient` with OkHttp and Retrofit.
- [x] Implement `RetryInterceptor` for exponential backoff.
- [x] Implement `DataRepository` using `StateFlow` and `Mutex` for in-memory caching.
- [x] Implement anonymous concurrent user tracking ping.
- [ ] **Needs Enhancement**: Write unit tests for `RetryInterceptor` and `DataRepository` concurrency locks.
- **Deliverables**: Network client configuration, Repository interfaces, Test coverage reports.

### 3.3 Design System (One UI 8.5)
**Status**: 🟡 Needs Enhancement  
**Description**: UI toolkit adhering to Samsung One UI 8.5 constraints (Dark mode, large typography, specific spacing).
- [x] Define `Color.kt` (Deep blacks, accent blues).
- [x] Define `Theme.kt` (Forced Dark Mode).
- [ ] **To Be Developed**: Define `Typography.kt` (16sp baseline, specific fonts).
- [ ] **To Be Developed**: Build reusable atomic components (`ArkCard`, `SkeletonLoader`, etc.).
- **Deliverables**: Compose Theme files, UI Component Library documentation.

### 3.4 Hideout Progression Simulator
**Status**: � Completed  
**Description**: Simulator for Hideout station upgrades, tracking requirements and unlocks.
- [x] Implement `HideoutStationCard` Compose UI (Interactive level increment/decrement).
- [x] Implement `HideoutScreen` with List of tables (Workbench, Gunsmith, Scrappy, Stash).
- [x] Implement `HideoutViewModel` to fetch dynamic requirements based on level from Room DB.
- [x] Map JSON DTOs to Hideout domain models (`HideoutTableEntity`).
- **Deliverables**: Jetpack Compose screens, ViewModel logic, Domain models.

### 3.5 ARC Intelligence Module
**Status**: � Completed  
**Description**: Tactical profiles on ARC enemies, detailing spawn zones and loot tables.
- [x] Implement `ArcIntelligenceBoard` Compose UI (Grid layout).
- [x] Implement `ArcThreatCard` Compose UI.
- [x] Implement `ArcsScreen` with List view of all arcs.
- [x] Connect UI to Room DB via `ArcViewModel`.
- [x] Implement detailed view sheet for specific loot drops.
- **Deliverables**: UI Screens, API integration, Navigation routing.

### 3.6 Item Database & Analytics
**Status**: � Completed  
**Description**: Search and filtering engine for items by category, rarity, and name.
- [x] Implement `LazyVerticalGrid` (3-columns) for item catalog (`ItemsScreen`).
- [x] Implement `ItemCard` showing name, image, price, rarity, and category.
- [x] Implement `ItemDetailScreen` for deep statistics and redirect from click.
- [x] Implement querying in `ItemsViewModel` (from Room DB).
- [x] Implement search bar and filter chips UI.
- **Deliverables**: Catalog UI, Detail UI, Search logic, API integration.

### 3.7 Trader & Quest Ecosystem
**Status**: 🔴 To Be Developed  
**Description**: Interactive tracking of in-game traders and their rotational inventories.
- [ ] **To Be Developed**: Define Trader and Quest data models.
- [ ] **To Be Developed**: Implement Trader dashboard UI with countdown timers.
- [ ] **To Be Developed**: Implement Quest hierarchy tree UI.
- **Deliverables**: Trader API endpoints, Compose screens, Time-calculation utilities.

### 3.8 Interactive Map Engine
**Status**: 🔴 To Be Developed  
**Description**: Geospatial visualization of key POIs, extraction zones, and ARC spawn locations.
- [ ] **To Be Developed**: Integrate lightweight map rendering engine (e.g., Mapbox or custom canvas).
- [ ] **To Be Developed**: Implement stateless tile fetching logic.
- [ ] **To Be Developed**: Implement POI marker rendering and filtering logic.
- **Deliverables**: Map rendering component, Tile fetcher, Marker data models.

---
**Last Updated**: April 2026  
**Status**: Active Tracking