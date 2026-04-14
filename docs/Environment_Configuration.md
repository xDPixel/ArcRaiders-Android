# macOS Development Environment Configuration & Checklist

This document details the current state of the macOS development environment required for building the Ark Companion Android application, identifies missing components, and provides an actionable checklist for setup.

## 1. Current Environment Status

The system was audited to verify the presence of essential development tools. Below are the results:

| Tool / Dependency | Status | Version / Details | Notes |
| :--- | :--- | :--- | :--- |
| **Xcode Command Line Tools** | ✅ Installed | `/Applications/Xcode.app/Contents/Developer` | Required for standard compilation (C/C++, Git dependencies). |
| **Homebrew** | ✅ Installed | `5.0.9` | macOS package manager, essential for installing other tools. |
| **Java Development Kit (JDK)** | ✅ Installed | `OpenJDK 25 (Homebrew build)` | Java is required for Gradle and Android compilation. Note: Android development typically prefers JDK 17. JDK 25 may cause Gradle sync issues depending on the Gradle version. |
| **Node.js** | ✅ Installed | `v23.5.0` | Useful for auxiliary scripting, mock servers, or future web integrations. |
| **npm** | ✅ Installed | `10.9.2` | Node package manager. |
| **Git** | ✅ Installed | `2.43.0` | Source control management. |
| **Android SDK / ANDROID_HOME** | ❌ **Missing/Not Configured** | `None` | The `$ANDROID_HOME` environment variable is empty. The Android SDK is strictly required to compile Android apps. |
| **Gradle CLI** | ❌ **Missing globally** | `None` | A global installation is missing, though we generated a Gradle Wrapper (`gradlew`) in the project directory which is the preferred method for building. |

---

## 2. Identified Issues & Missing Components

1. **Android SDK is not configured in the path:** 
   The environment variable `$ANDROID_HOME` is not set. Without the Android SDK (and the `cmdline-tools`, `build-tools`, and `platform-tools`), the project cannot be compiled.
2. **JDK Version Compatibility:**
   The installed JDK is version 25. Android Gradle Plugin (AGP) 8.2+ officially supports JDK 17. While JDK 25 might work with the latest Gradle versions, it often leads to compatibility errors during the build phase. It is highly recommended to install and use JDK 17 specifically for Android.

---

## 3. Configuration & Installation Checklist

Please execute the following steps to finalize your macOS development environment:

### Step 1: Install Android Studio & SDK
Android Studio is the easiest way to install the Android SDK and manage emulators.
- [ ] Download and install [Android Studio](https://developer.android.com/studio).
- [ ] Open Android Studio, proceed through the setup wizard, and ensure the **Android SDK**, **Android SDK Command-line Tools**, and **Android SDK Build-Tools** are checked and installed.

### Step 2: Configure Environment Variables
You need to expose the Android SDK to your terminal.
- [ ] Open your `~/.zshrc` (or `~/.bash_profile`) file.
- [ ] Add the following lines to the bottom of the file:
  ```bash
  export ANDROID_HOME=$HOME/Library/Android/sdk
  export PATH=$PATH:$ANDROID_HOME/emulator
  export PATH=$PATH:$ANDROID_HOME/platform-tools
  export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
  ```
- [ ] Apply the changes by running: `source ~/.zshrc`

### Step 3: Install/Configure JDK 17 (Recommended)
Since Android development strongly prefers JDK 17, install it via Homebrew to prevent Gradle sync issues.
- [ ] Run the following command:
  ```bash
  brew install openjdk@17
  ```
- [ ] Symlink it to the system Java wrappers:
  ```bash
  sudo ln -sfn /usr/local/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
  ```
- [ ] Set your `JAVA_HOME` in `~/.zshrc`:
  ```bash
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
  ```
- [ ] Apply the changes: `source ~/.zshrc`

### Step 4: Verify Project Gradle Wrapper
The project relies on the Gradle Wrapper to ensure everyone uses the same Gradle version.
- [ ] Navigate to the project root: `cd "ArcRaiders Android"`
- [ ] Make the wrapper executable: `chmod +x gradlew`
- [ ] Verify the build environment: `./gradlew tasks`

Once these steps are completed, your macOS environment will be fully equipped to compile, test, and run the Ark Companion Android application.