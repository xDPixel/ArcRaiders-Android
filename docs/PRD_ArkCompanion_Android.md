# Ark Companion Android - Product Requirements Document

## 1. Product Overview

Ark Companion Android is a lightweight, privacy-respecting, and open-source mobile application designed to enhance the gaming experience for players of *Arc Raiders*. Built entirely natively using Kotlin and Jetpack Compose, the application provides players with a robust, real-time knowledge base encompassing items, hideout progressions, ARC enemy intelligence, map data, and trader inventories.

The application operates under strict constraints: it is completely free, open-source, collects zero user data, and functions purely statelessly without any persistent local storage or user authentication.

## 2. Core Specifications & Constraints

### 2.1 Monetization & Distribution Model
- **Completely Free:** No advertisements, no subscription tiers, and no in-app purchases.
- **Distribution:** Distributed as a standalone APK or via open-source repositories (e.g., GitHub, F-Droid). It will **not** be distributed through the Google Play Store or any commercial app marketplace.
- **Licensing:** The codebase must be 100% open-source with appropriate permissive licensing (e.g., MIT or GPLv3).

### 2.2 Privacy & Data Collection
- **Zero-Data-Collection Architecture:** No personal information, usage analytics, or behavioral data is stored or transmitted.
- **Concurrent User Tracking:** The only exception is a real-time user count mechanism tracking active concurrent users. This is implemented via a privacy-compliant, anonymous, aggregate-only method that adheres strictly to GDPR and other data protection regulations. This is clearly disclosed in the privacy policy.
- **No Third-Party Trackers:** No Firebase Analytics, Crashlytics, or external tracking SDKs.

### 2.3 Language & Localization
- **English Exclusive:** The application interface is built exclusively in English for this initial version.
- **No Localization Framework:** All UI text, error messages, and documentation are hardcoded or defined in standard English resources without the implementation of any multi-language localization framework.

### 2.4 Authentication & Data Architecture
- **No User Accounts:** Operates without any user authentication system or account creation.
- **No Persistent Storage:** Zero persistent local data storage (no Room database, SQLite, SharedPreferences, or DataStore for user data).
- **Stateless API Connections:** Functions entirely through stateless REST API connections to the backend MetaForge services.
- **In-Memory Caching Only:** Caches only essential temporary data in memory during active sessions. Complete memory cleanup occurs upon app termination.

### 2.5 Technical Requirements
- **Lightweight & Performant:** Functions independently without requiring server-side user management.
- **API Integration:** Implements proper API integration patterns with strict timeout handling, retry logic (exponential backoff), and graceful degradation.
- **Error Handling:** Comprehensive user-facing error messaging for API connectivity issues, network interruptions, and offline scenarios while maintaining the no-data-storage constraint.

## 3. Core Feature Set
- **Item Database & Analytics:** A fast search and filtering engine allowing users to parse items by category, rarity, and name via stateless API queries.
- **Hideout Progression Simulator:** A progression tracker that maps requirements and unlocks for Hideout stations, fetching data dynamically.
- **ARC Intelligence:** Tactical profiles on ARC enemies, detailing spawn zones and precise loot drop tables.
- **Trader & Quest Ecosystem:** Interactive tracking of in-game traders and their rotational inventories.
- **Interactive Map:** Geospatial visualization of key POIs, extraction zones, and ARC spawn locations rendered via stateless API tile/data fetching.

## 4. UI Layouts & User Experience (One UI 8.5 Compatibility)

The application is heavily optimized for Android Dark Mode, aligning with Samsung One UI 8.5 design principles:
- **Visual Language:** Deep blacks, elevated gray cards, and high-contrast typography to mimic the aesthetic of the *Arc Raiders* universe.
- **Typography:** Samsung One UI prefers larger, comfortable text. Set `bodyLarge` baseline to 16sp with generous line height.
- **Spacing:** Consistent 4/8/12/16dp grid system. Large hit targets (minimum 48dp) for all interactive elements.
- **Components:** Emphasize bottom sheets, edge-to-edge layouts with translucent status/navigation bars. Haptics for primary actions.
- **State Management:** Skeleton screens during stateless API fetches. Friendly, informative error states when the network is unavailable.

## 5. Technical Architecture

### 5.1 Frontend Architecture
- **Framework:** Native Android with Kotlin.
- **UI Toolkit:** Jetpack Compose.
- **Architecture Pattern:** MVVM (Model-View-ViewModel) with Unidirectional Data Flow.
- **Dependency Injection:** Manual DI or lightweight framework (e.g., Koin) to avoid annotation processing overhead if desired, though Hilt is acceptable if it remains lightweight.
- **Concurrency:** Kotlin Coroutines and `StateFlow` for reactive state management.

### 5.2 Network & Data Layer
- **Networking:** Retrofit with OkHttp.
- **Caching:** In-memory caching only (e.g., using `LruCache` or simple state holders in Kotlin). Images are cached in memory via Coil (configured to disable disk caching).
- **Resilience:** OkHttp Interceptors for timeouts, retries, and the anonymous concurrent user ping.

## 6. Success Criteria
- The application compiles successfully and runs seamlessly on Android 8.0+ devices.
- Zero local files (databases, shared preferences) are created for user data.
- API handles timeouts and displays clear error messages to the user.
- UI adheres to One UI 8.5 guidelines and operates perfectly in Dark Mode.
- Codebase is fully open-source and free of proprietary analytics SDKs.

---

**Document Version**: 2.0  
**Last Updated**: April 2026  
**Status**: Approved for Development