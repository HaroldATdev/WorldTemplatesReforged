# World Templates Reforged

**Create your modpack's world from a ready-made template — in one click.**

World Templates Reforged lets modpack authors ship a **pre-built world** (spawn
builds, structures, NPCs, datapacks, everything) inside the mod itself, and gives
players a dedicated **"Create World"** button on the world selection screen. One
click, and the mod installs the template and clones it into a **fresh world with
a new random seed** — server-style templates, but for singleplayer.

## ✨ Features

- 🌍 **Embedded world templates** — your finished world ships as a zip inside the
  mod. It is installed on first use (with zip-slip protection and `level.dat`
  validation) and cloned on demand.
- ️ **Custom world button** — adds a configurable button to the world selection
  screen. The vanilla *"Create New World"* button can be hidden or kept.
- 🗂️ **Template selection screen** — players pick between registered templates;
  each entry shows its name, icon, folder and source.
-  **Game mode selection** — Survival (default), Creative, Adventure, Spectator
  and **Hardcore**, written straight into `level.dat`.
- ️ **World icons that stick** — the created world keeps the template's icon,
  auto-resized to the 64×64 vanilla requires, and vanilla never overwrites it.
- 🔀 **Fresh seeds every time** — each world gets a brand-new `SecureRandom` seed
  (never 0, never repeated), written into `level.dat`.
- 🧹 **Clean cloning** — playerdata, advancements and stats are wiped; the world
  is built in a staging folder and only published when everything succeeds.
  Failed attempts never leave junk worlds behind, and broken leftovers are
  cleaned automatically.
- 📑 **Sortable template list** — `A-Z` (default, locale-aware), `Z-A`, `Newest`,
  `Oldest` or `Original`, switchable live from the selector.
- 🛟 **Error handling** — a dedicated error screen explains what went wrong and
  offers Retry, World list and Main menu actions.
- ⚙️ **Full in-game config** — open from the Mods list → **Config**. Everything is
  customizable without touching a single file.

## ️ For modpack creators

Open the config from **Mods → World Templates Reforged → Config**.

### Button tab

| Option | What it does |
|---|---|
| Text | Button label on the world selection screen |
| Tooltip | Hover text (empty = no tooltip) |
| Pos X | Button X position (`-1` = centered) |
| Pos Y | Button Y position |
| Width / Height | Button size (with live preview) |

### Templates tab

| Option | What it does |
|---|---|
| Default template | Preselected template; cycles through the registered ones |
| Choose | **YES**: opens the template selector · **NO**: uses the first sorted template directly |
| Main template name | Override the template's displayed name |
| Icon | Override the icon texture (empty = the template's own `icon.png`) |
| World base name | New worlds are named `Trilcera`, `Trilcera 2`, ... |
| Show descriptions | Folder/Source info lines in the template list |
| Show 'Create New World' | Keep or hide the vanilla button |
| Sort | Template list order: A-Z, Z-A, Newest, Oldest, Original |
| Mode | Default game mode: Survival, Creative, Adventure, Spectator, Hardcore |

Every label in the config screen shows a **description tooltip** on hover.

All settings persist in `config/worldtemplates-client.toml`.

### Shipping your own world

Replace the embedded `/trilcera-template.zip` inside the mod jar with your own
world zip. Both layouts are supported: the world at the zip root, or nested one
folder deep. The folder will contain `level.dat` when installed.

## ❓ FAQ

**Does it work on servers?**
It is client-side only — designed for singleplayer world creation. On dedicated
servers it does nothing.

**Where is the template stored?**
In `<game dir>/trilcera-template/`. Created worlds go to `saves/` like any normal
world.

**Can players still create normal worlds?**
Yes — set *"Show 'Create New World'"* to **YES** in the config.

**Does my world get uploaded anywhere?**
No. Everything happens locally on the player's computer.

## 🧱 Requirements

- Minecraft **1.20.1**
- **Forge 47.x** (client side)
- No dependencies

## 🙏 Credits

- Architecture inspired by **World Templates** by *BrandonItaly* (NeoForge)
- World-cloning flow based on the **LonKraft Template** API
- Built by **Trilcera** for the Trilcera modpack

## 📜 License

MIT — free to include in your modpack.