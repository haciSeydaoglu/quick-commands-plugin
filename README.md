# Quick Commands

A quick terminal command execution plugin for JetBrains IDEs (IntelliJ IDEA, PhpStorm, WebStorm, PyCharm, Android Studio, and more).

## Features

- **Dropdown Menu** - Single-click accessible command list in the Terminal toolbar
- **Global Commands** - Define commands visible across all projects
- **Project Commands** - Custom commands visible only in the related project
- **Auto-detect Scripts** - Automatically discovers scripts from `package.json` and `composer.json` files
- **Monorepo Support** - Nested script files are grouped by folder in collapsible submenus
- **Package Manager Detection** - Detects npm, yarn, pnpm, or bun based on lock files
- **Keyboard Shortcut** - Quick access popup via `Ctrl+Alt+T` (customizable) with speed search
- **Import / Export** - Share your global commands as JSON (clipboard or file)
- **Separators** - Organize commands with visual divider lines
- **Easy Configuration** - User-friendly two-tab settings interface

## Installation

### From JetBrains Marketplace

1. **Settings** > **Plugins** > **Marketplace**
2. Search for **Quick Commands**
3. Click **Install** and restart the IDE

### Build from Source

```bash
./gradlew buildPlugin
# Output: build/distributions/quick-commands-plugin-<version>.zip
```

Then install manually:
1. **Settings** > **Plugins** > gear icon > **Install Plugin from Disk...**
2. Select the generated `.zip` file
3. Restart the IDE

### Development Mode

```bash
./gradlew runIde
```

## Usage

1. Open the **Terminal** panel (`Alt+F12`)
2. Click the **Quick Commands** dropdown in the toolbar
3. Select a command - it runs in a new terminal tab

Or press `Ctrl+Alt+T` to open the Quick Commands popup from anywhere.

### Auto-detected Scripts

Quick Commands automatically discovers scripts from `package.json` and `composer.json` files in your project. They appear as collapsible submenus in the dropdown:

```
── Global ──
  Deploy Production
  Run Tests
── Project: my-app ──
  Start Dev
── Scripts ──
  > npm: root          -> [dev, build, test, lint]
  > yarn: packages/web -> [dev, build, start]
  > composer: root     -> [test, lint, analyse]
──────────
  Settings...
```

- Enabled by default. Disable via **Settings** > **Tools** > **Quick Commands** > **Project Commands** tab
- Lock file detection: `yarn.lock` -> yarn, `pnpm-lock.yaml` -> pnpm, `bun.lockb` -> bun, fallback -> npm
- Composer event hooks (`post-install-cmd`, etc.) are filtered out automatically
- Cache is invalidated when relevant files change

### Settings

**Settings** > **Tools** > **Quick Commands**

| Tab | Description |
|-----|-------------|
| **Global Commands** | Commands visible in all projects. Supports import/export as JSON. |
| **Project Commands** | Commands visible only in this project. Includes the auto-detect scripts toggle. |

**Toolbar actions:** Add, Remove, Move Up/Down, Add Separator (`Ctrl+Shift+S`), Reset to Defaults, Export, Import (global tab only).

## Requirements

- JetBrains IDE **2024.2** or higher (IntelliJ IDEA, PhpStorm, WebStorm, PyCharm, etc.)
- Java **21** or higher

## Technical Details

| Component | Version |
|-----------|---------|
| Kotlin | 2.0.21 |
| Gradle | 9.0 |
| IntelliJ Platform SDK | 2024.2 |
| Supported IDE Range | 2024.2 - 2025.3.* |

## License

This project is licensed under the [MIT License](LICENSE).
