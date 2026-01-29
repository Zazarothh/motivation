# Changelog

All notable changes to the Help Motivation plugin.

## [1.1.0] - 2026-01-29

### Added
- Scheduled messages: Motivational messages now repeat on configurable interval (1m, 5m, 15m, 30m, 1h, 2h, 5h)
- Pure Mode: Fully ignore combat skills (Attack, Strength, Defence, Ranged, Magic, Prayer)

### Changed
- Messages now use scheduler instead of one-time login event
- Pure filter now fully ignores combat skills (not just level 1)

### Removed
- "Show Login Message" config option (replaced by scheduled messages)

## [1.0.1] - 2025-01-19

### Added
- Pure account filter: Ignores level 1 combat skills (Attack, Strength, Defence, Ranged, Magic, Prayer)

### Changed
- Custom messages now replace defaults instead of combining with them
- Blank lines in custom messages are ignored

### Fixed
- Skill detection: Changed `xp > 0` to `xp >= 0` to include skills at level 1 with 0 XP

## [1.0.0] - 2025-01-17

### Added
- Initial release as Help Motivation
- Sidebar panel showing non-99 skills
- Skill levels, XP progress bars, and hiscores ranks
- Motivational messages
- Auto-refresh on skill XP changes
- Column headers in sidebar (Skill, Lvl, Progress, Rank)
- Alternating row colors (odd/even styling)
- Hover effects on skill rows
- RuneScape fonts via FontManager
- Orange color for rank values
- Centered progress column

### Fixed
- Executor blocking: Replaced `Thread.sleep(3000)` with `executor.schedule()`
- Removed `runtimeOnly` dependency for Plugin Hub compatibility
