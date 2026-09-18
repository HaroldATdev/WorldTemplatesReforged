# Changelog

All notable changes to **World Templates Reforged** are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.8] - 2026-09-18
### Fixed
- **Selector header no longer overlaps itself.** The `Orden` button was drawn at
  `y = 42`, right on top of the selected template name drawn at `y = 40`, so the
  button covered the name. The header now has an explicit vertical rhythm —
  title (`15`) → selected template name (`32`) → `Orden` button (`50..68`) →
  list (`74`) — and the geometry is resolved once in `computeLayout()`, so the
  drawn text and the widgets always agree at any GUI scale and after a resize.
### Added
- **Mod logo.** `logoFile` now points at the bundled `worldtemplates_logo.png`,
  so the mod shows an icon in the in-game mod list instead of a placeholder.

## [1.0.7] - 2026-09-18
### Fixed
- **Dead buttons after Back from the create screen**: Back returned to the
  *same* template selector instance it came from. That instance was already
  removed and kept `closing = true`, so both `onCreate` and `onClose` returned
  immediately (nothing reacted any more) and its widgets were duplicated by the
  second `init()`. Back now opens a **fresh** selector.
- **Unresponsive selector when a click was rejected as a replay**: `onCreate`
  set `closing = true` *before* consulting the replay guard, so a single
  suppressed click left the screen permanently inert.
- `closing` is now reset in `init()` on both of our screens, which also covers
  window resizes (previously a resize could leave the screen unresponsive).
- The Retry action of the error screen no longer runs on an already replaced
  create-screen instance; it opens a fresh one (or reopens the world that failed
  to load).
### Changed
- Create screen layout: the game mode selector sits **directly above** the
  create button and the status line is drawn above it (26 px steps), so the text
  no longer overlaps the mode button.

## [1.0.6] - 2026-09-18
### Fixed
- **Our whole flow was skipped when `saves/` had no worlds.** Vanilla's
  `WorldSelectionList.loadLevels()` calls
  `CreateWorldScreen.openFresh(minecraft, null)` by itself when no world exists,
  and the previous "ignore internal opens" rule let that vanilla screen through
  (its parent is a `GenericDirtMessageScreen`, so it looked internal). The player
  landed on the vanilla world creation screen instead of the configured flow.
  That auto-open is now recognized (empty world list) and routed to our own flow.
- **`chooseTemplate = NO` opened the selector anyway** on that same vanilla
  auto-open, because the redirect always built a `WorldTemplateScreen`. The
  world-list button and the intercepted open now share a single entry point that
  respects the setting and goes straight to the create screen with the first
  template after the configured sorting.
- **Data-loss risk removed from the leftover cleanup.** It used to delete any
  folder named `<base name>` or `<base name> ...` that lacked our marker, which
  could delete a player's own world sharing that name (for example a world copied
  from a server). It now removes only our own dot-prefixed staging folders
  (`.<name>.creating`), which are never worlds.
### Changed
- Cancel/Back returns to the **world list when worlds exist** and to the **main
  menu when the list is empty** (going back to an empty list would bounce straight
  into our flow again).
- Clearer diagnostics: `[WTR] CreateWorldScreen interceptado (padre=..., listaVacia=...)`,
  `[WTR] flujo propio: ...` and `[WTR] CreateWorldScreen ignorado (flujo ajeno: padre=...)`.

## [1.0.5] - 2026-09-18
### Fixed
- Template icon rendering in the selector: the full icon region is now scaled
  into the 32x32 slot instead of showing only its top-left quarter.
- `icon.png` is resolved from the template's real root (zips that wrap the world
  in a folder are handled) and the world's icon is resized to the 64x64 vanilla
  requires, so the world list shows it and never regenerates its own.
- No more orphaned worlds after a failed creation: worlds are built in a
  `.<name>.creating` staging folder and published only on success.
### Added
- Game mode selection before creating the world (Survival default, Creative,
  Adventure, Spectator, Hardcore) written into `level.dat`, plus the
  `defaultGameMode` config option.
- Template sorting (`A-Z` default, `Z-A`, `NEWEST`, `OLDEST`, `ORIGINAL`) with a
  live "Orden" button in the selector and the `templateSorting` config option.
- Error screen with the specific reason and Retry / World list / Main menu actions.

## [1.0.4] - 2026-09-18
### Fixed
- **Crash** (`Icon already closed`) when creating a world while another world
  existed: reusing a removed `SelectWorldScreen` instance (direct field swap)
  left it with closed favicons; `setScreen()` later removed it a second time.
  Removed instances are never reused now - a fresh world list is always
  created.
- **Template selector kept appearing even with `chooseTemplate = NO`**: vanilla
  opens `CreateWorldScreen` internally (parent = `GenericDirtMessageScreen`)
  while the world list processes existing worlds, and the redirect hijacked
  that internal open. The redirect now only applies to user-facing parents
  (world list or our own screens).
- **Replayed clicks after the datapack reload**: the suppression window started
  *before* the screen switch, but `SelectWorldScreen.init()` blocks for seconds
  (managedBlock reload), so the window had expired by the time replays fired.
  Suppression now starts *after* the switch completes.
### Added
- `[WTR] CreateWorldScreen interno ignorado` log line for internal vanilla opens.

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