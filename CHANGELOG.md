# Changelog

## [1.5.3]
- Simplify Claude skill categories: merge commands and skills into unified groups (Global, Plugin, Project)
- Sort skills alphabetically within each category group
- Add "Claude:" prefix to skill group separator labels for clarity

## [1.5.2]
- Reorder Claude skill groups: global and plugin skills now appear before project-specific skills in the dropdown menu

## [1.5.1]
- Update CLAUDE.md with new architecture documentation (services layer, three-tab UI, updated tech stack)
- Update README.md with Claude Skills feature documentation and corrected settings references
- Update plugin.xml description with auto-detect scripts, Claude skills, and import/export features
- Remove emoji characters from plugin.xml description headings

## [1.5.0]
- Auto-detect scripts from package.json and composer.json files in the project
- Supports monorepo structures: nested package.json files grouped by folder with submenus
- Root scripts shown directly in dropdown, nested scripts in collapsible submenus
- Automatic package manager detection: npm, yarn, pnpm, bun (based on lock files)
- Package manager inheritance: nested packages without lock files inherit root's package manager
- Automatic emoji icons for scripts based on name (build, test, dev, deploy, docker, etc.)
- Composer scripts included with event hooks filtered out
- Auto-detect Claude Code skills and commands (global, project, and plugins)
- Claude skills configurable: enable/disable and optional --dangerously-skip-permissions mode
- Clean settings UI with grouped checkboxes in Project Commands tab
- File watcher invalidates cache when relevant files change

## [1.4.1]
- Fix: Changed all UI strings from Turkish to English for consistency with the rest of the plugin

## [1.4.0]
- Added import/export functionality for Global Commands settings
- Export: Copy to clipboard or save as JSON file
- Import: Paste from clipboard or load from JSON file with preview dialog
- Import uses upsert logic: updates existing commands by name, adds new ones
- Added duplicate command name prevention across both Global and Project tabs
- New commands auto-generate unique names when duplicates exist

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
