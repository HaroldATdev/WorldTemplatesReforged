# Changelog

All notable changes to **World Templates Reforged** are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.3] - 2026-09-18
### Fixed
- Cancel no longer loops back into the template selector. Root cause: input
  replay mods (Ixeris) re-dispatch queued clicks after the screen switch
  finishes; the replayed click landed on the injected "Crear" button (which
  occupies the old Cancel coordinates) and, with `chooseTemplate = true`,
  re-opened the selector. Clicks arriving right after a screen switch are
  now swallowed.
- Cancel is now instant: the parent world list is swapped back in without
  re-running `SelectWorldScreen.init()`, which triggers a full datapack
  reload ("Preparing world generation..." for seconds on large modpacks)
  while the queued input replays pile up.
- Config toggles ("Escoger", "Descrip", "Mostrar", Orden, Modo, Plantilla)
  no longer double-toggle from replayed clicks - this silently reverted
  `chooseTemplate` back to true, which made the selector appear even when
  set to NO.
### Added
- `[WTR]` diagnostic log lines for intercepted vanilla screens and swallowed
  replayed clicks.

## [1.0.2] - 2026-09-17
### Fixed
- Cancel in the template selector now reliably returns to the world list:
  the screen switch is direct (no deferred `mc.execute`), removing the
  re-entry window that input-replay mods (Ixeris/FancyMenu) exploited.
- "Back" on the create-world screen returns to where you came from
  (template selector or world list) instead of the main menu.
- Error screen: re-added the "World list" button (buttons were
  off-center) and Esc/back now returns to its parent screen.
- Error screen "Retry" no longer gets stuck: the busy flag is reset on
  failure and, if the world was already created, retry re-opens it
  instead of cloning another copy.
### Changed
- All template screens now keep a real parent reference instead of
  relying on stale captured screens.

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