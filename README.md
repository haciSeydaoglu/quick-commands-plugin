# Quick Commands

A quick terminal command execution plugin for JetBrains IDEs (IntelliJ IDEA, PhpStorm, WebStorm, etc.).

## Features

- **Dropdown Menu**: Single-click accessible command list in Terminal toolbar
- **Global Commands**: Define commands visible in all projects
- **Project-Specific Commands**: Custom commands visible only in the related project
- **Easy Configuration**: User-friendly settings interface

## Installation

### Build from Source

```bash
# Build the plugin
./gradlew buildPlugin

# Output file:
# build/distributions/quick-commands-plugin-1.0.0.zip
```

Installation in IDE:
1. **Settings** → **Plugins** → ⚙️ (settings icon)
2. Click **Install Plugin from Disk...**
3. Select `build/distributions/quick-commands-plugin-1.0.0.zip` file
4. Restart the IDE

### Development Mode

```bash
# Start test IDE with plugin loaded
./gradlew runIde
```

## Usage

1. Open the **Terminal** panel (Alt+F12 or View → Tool Windows → Terminal)
2. Select a command from the **Quick Commands** dropdown menu in the toolbar
3. The command automatically runs in a new terminal tab

### Settings

**Settings** → **Tools** → **Quick Commands**

- **Global Commands** tab: Define commands visible in all projects
- **Project Commands** tab: Define commands visible only in this project

## Requirements

- IntelliJ IDEA / PhpStorm / WebStorm / other JetBrains IDEs **2024.1** or higher
- Java **17** or higher

## Technical Details

| Component | Version |
|-----------|---------|
| Kotlin | 1.9.25 |
| Gradle | 8.10 |
| IntelliJ Platform SDK | 2024.1.4 |
| Supported IDE Range | 2024.1 - 2025.1.* |

## License

This project is licensed under the [MIT License](LICENSE).
