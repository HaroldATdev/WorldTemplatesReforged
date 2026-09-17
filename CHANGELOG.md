# Changelog

All notable changes to **World Templates Reforged** are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-09-11

### Added
- **Game mode selection** before creating the world (Survival by default, plus
  Creative, Adventure, Spectator and **Hardcore**). The chosen mode is written
  into `level.dat`: `GameType`, `hardcore` flag, forced `Difficulty: Hard` for
  Hardcore and `allowCommands` for Creative/Spectator.
- **`defaultGameMode`** config option (`survival` / `creative` / `adventure` /
  `spectator` / `hardcore`). Invalid values fall back to `survival` with a
  warning in the log.
- **Template sorting** with 5 modes: `A-Z` (default, locale-aware so Spanish
  accents and Ñ sort correctly), `Z-A`, `Newest`, `Oldest` and `Original`
  (registration order).
- **`templateSorting`** config option, plus a live **"Sort: X"** button in the
  template selector that cycles the modes and saves the choice.
- **Error screen** (`TrilceraErrorScreen`) that shows a specific reason and
  offers **Retry**, **World list** and **Main menu** actions, covering:
  - no registered template found,
  - installed template missing/corrupt `level.dat`,
  - missing embedded template zip,
  - IO failure while cloning (disk full, permissions, ...),
  - corrupt `level.dat` (NBT read failure),
  - world created but failing to open.
- Pre-flight validation: template problems are reported **immediately when the
  button is pressed** instead of leaving a broken create screen.
- Diagnostic log line when the button is pressed:
  `[Trilcera Templates] Button pressed: chooseTemplate=X, sort=Y, templates=N, using='...'`.

### Fixed
- **Cancel** in the template selector (and **Back** in the create screen) now
  always returns to the world list. Both build a fresh `SelectWorldScreen`
  parented to `TitleScreen`, so stale screen references (e.g. overlays added by
  other mods) can no longer swallow the click.
- **`chooseTemplate = No`** is now deterministic: it never falls back to opening
  the selector. It always uses the **first template of the sorted list**, and if
  the registry is empty it builds the embedded `trilcera-template` on the fly.
- World `icon.png` is now resized to the **64x64** size vanilla requires, so the
  created world shows the template icon in the world list instead of logging
  `IllegalArgumentException: Icon must be 64x64`. Failures to resize are
  non-fatal and only produce a warning.
- Icon lookup resolves the **real template root** (handles worlds nested one
  folder deep inside the installed template), so the template icon is found.

### Changed
- Mod renamed to **World Templates Reforged**.
- Config screen: new **Sort** and **Game mode** rows, and every label now shows
  a **description tooltip** on hover.

## [1.0.0] - 2026-09-11

### Added
- Initial release.
- **"Create World" button** on the world selection screen (configurable text,
  tooltip, position and size) that replaces or complements the vanilla
  *"Create New World"* button.
- **Embedded world templates**: the mod ships a ready-made world as a zip inside
  the jar, installed on first use into `<game dir>/trilcera-template/` with
  zip-slip protection and `level.dat` validation.
- **Template selection screen** listing name, icon, folder and source of every
  registered template.
- **World cloning** based on the LonKraft Template flow: overworld copy, fresh
  `SecureRandom` seed (never 0 and never repeated), playerdata/advancements/stats
  wipe and `level.dat` rewrite.
- **Clean cloning**: worlds are built in a staging folder and only published when
  every step succeeds, so failed attempts never leave junk worlds behind. Broken
  leftovers are cleaned automatically.
- **In-game config screen** (Mod List -> Config) with Button and Templates tabs,
  saved to `config/worldtemplates-client.toml`.

[1.0.1]: https://github.com/HaroldATdev/WorldTemplatesReforged/releases/tag/v1.0.1
[1.0.0]: https://github.com/HaroldATdev/WorldTemplatesReforged/releases/tag/v1.0.0