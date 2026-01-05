# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Quick Commands is an IntelliJ plugin that enables quick terminal command execution within JetBrains IDEs. It provides a dropdown menu in the Terminal toolbar for single-click access to predefined commands, with support for both global (all projects) and project-specific commands.

## Build Commands

```bash
# Build distributable plugin ZIP
./gradlew buildPlugin
# Output: build/distributions/quick-commands-plugin-1.0.0.zip

# Start test IDE with plugin loaded
./gradlew runIde

# Clean build artifacts
./gradlew clean
```

## Architecture

### Layer Structure

```
src/main/kotlin/com/quickcommands/
├── settings/     # Persistent settings (global + project level)
├── actions/      # UI actions (dropdown, shortcuts, execution)
└── util/         # Terminal execution utility
```

### Key Components

**Settings Layer:**
- `CommandEntry` - Data model with name, command, UUID id
- `GlobalCommandSettings` - Application-level settings (stored in `terminalCommanderGlobal.xml`)
- `ProjectCommandSettings` - Project-level settings (stored in `terminalCommander.xml`)
- `QuickCommandsConfigurable` - Two-tab settings UI (Global/Project commands)

**Actions Layer:**
- `QuickCommandsDropdownAction` - Main dropdown menu in Terminal toolbar
- `ShowQuickCommandsAction` - Keyboard shortcut popup (Ctrl+Alt+T)
- `RunCommandAction` - Individual command executor
- `OpenSettingsAction` - Settings link action

**Utility Layer:**
- `TerminalExecutor` - Creates new terminal tab and executes commands via `TerminalToolWindowManager`

### Service Pattern

Both settings classes implement `PersistentStateComponent` for XML serialization:
- Global settings: `ApplicationManager.getApplication().getService()`
- Project settings: `project.getService()`

### Plugin Configuration

- Plugin descriptor: `src/main/resources/META-INF/plugin.xml`
- Actions registered under `TerminalToolwindowActionGroup`
- Depends on: `com.intellij.modules.platform`, `org.jetbrains.plugins.terminal`

## Technical Stack

| Component | Version |
|-----------|---------|
| Kotlin | 1.9.25 |
| Gradle | 8.10 |
| IntelliJ Platform SDK | 2024.2 |
| Java Toolchain | 21 |
| Supported IDE Range | 2024.2 - 2025.3.* |

## Key Files Reference

| File | Purpose |
|------|---------|
| `plugin.xml` | Plugin descriptor, actions, services, extensions |
| `build.gradle.kts` | Build configuration, dependencies |
| `QuickCommandsConfigurable.kt` | Settings UI (162 lines) |
| `TerminalExecutor.kt` | Terminal integration |

## Sürüm Yönetimi (Semantic Versioning)

Her değişiklikten sonra `gradle.properties` dosyasındaki `pluginVersion` değerini güncelle:

| Değişiklik Türü | Sürüm | Örnek |
|-----------------|-------|-------|
| **MAJOR** (breaking change, büyük mimari değişiklik) | X.0.0 | 1.0.0 → 2.0.0 |
| **MINOR** (yeni özellik, geriye uyumlu) | X.Y.0 | 1.0.0 → 1.1.0 |
| **PATCH** (bug fix, küçük düzeltme) | X.Y.Z | 1.0.0 → 1.0.1 |

**Değişiklik sonrası yapılacaklar:**
1. `gradle.properties` → `pluginVersion` güncelle
2. `CHANGELOG.md` → Yeni sürüm notları ekle (en üste)

## Development Notes

- All source files include Claude attribution comments (70%+)
- Comments are in Turkish per project convention
- No external dependencies beyond IntelliJ platform
- No test framework configured
