# Changelog

## [1.3.1]
- Fix: Prevent empty terminal tab being created when running a command with Terminal window closed
- Use `toolWindow.show()` instead of `activate()` to avoid default tab creation

## [1.3.0]
- Added separator (divider line) support for command lists
- Added "Add Separator" button and Ctrl+Shift+S shortcut in settings UI
- Added "Reset to Defaults" button in settings toolbar
- Expanded default commands: Claude, Codex, ../Claude, ../Codex with super mode variants
- Fixed emoji characters being lost during XML serialization (EmojiSafeConverter)

## [1.2.0]
- Migrated to IntelliJ Platform Gradle Plugin 2.13.1 (from legacy 1.17.4)
- Upgraded Gradle to 9.0
- Upgraded Kotlin to 2.0.21
- Replaced deprecated Terminal API with modern TerminalWidget.sendCommandToExecute
- Extended platform support to 2026.1 (PhpStorm 2026.1 compatibility)

## [1.1.4]
- Revert: Removed manual focus management that caused issues
- Fix: Let ToolWindow handle focus naturally (JetBrains recommendation)
- Fix: Terminal tab switching now works correctly

## [1.1.3]
- Fix: Shift+Enter/Option+Enter not working in second and subsequent terminal tabs
- Added ContentManager integration for proper terminal tab selection
- Implemented multi-stage focus transfer mechanism as JetBrains bug workaround
- Added SwingUtilities.invokeLater for delayed focus confirmation

## [1.1.2]
- Fix: Improved terminal focus handling using doWhenFocusSettlesDown API
- Fix: Terminal keyboard input (Shift+Enter/Option+Enter) now works reliably after keyboard shortcut selection

## [1.1.1]
- Fix: Terminal keyboard input (Shift+Enter/Option+Enter) not working after selecting command via keyboard shortcut
- Improved focus management: Force focus transfer to terminal after popup closes using IdeFocusManager

## [1.1.0]
- Updated platform support to 2024.2 - 2025.3
- Added Java 21 support
- Added automatic version and changelog management

## [1.0.0]
- Initial release
- Global and project-specific command support
- Terminal toolbar integration
