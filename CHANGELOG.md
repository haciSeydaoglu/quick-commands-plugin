# Changelog

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
