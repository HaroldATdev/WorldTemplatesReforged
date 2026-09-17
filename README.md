# World Templates Reforged

Create your modpack's world from a ready-made template — in one click.
Forge **1.20.1**, client-side.

A pre-built world (spawn builds, structures, NPCs, datapacks, everything) ships
inside the mod, and the world selection screen gets a configurable
**"Create World"** button. Pressing it installs the template and clones it into a
fresh world with a new random seed.

- 🌍 Embedded templates with zip-slip protection and `level.dat` validation
- ️ Configurable button (text, tooltip, position, size) that can replace the
  vanilla *"Create New World"* button
- 🗂️ Template selection screen with icons and sorting (A-Z, Z-A, Newest, Oldest,
  Original)
- 🎮 Game mode choice before creation: Survival, Creative, Adventure, Spectator,
  Hardcore
- 🔀 Fresh `SecureRandom` seed per world, written into `level.dat`
- 🧹 Staged cloning — failed attempts leave no junk worlds behind
- 🛟 Error screen with reason + Retry / World list / Main menu
- ⚙️ Everything configurable in-game, saved to `config/worldtemplates-client.toml`

## Documentation

- [DESCRIPTION.md](DESCRIPTION.md) — full feature list and configuration reference
- [CHANGELOG.md](CHANGELOG.md) — version history

## Building

```bash
# Requires a Java 17 JDK
export JAVA_HOME=/path/to/jdk-17
./gradlew build
```

The mod jar is produced in `build/libs/`.

## Requirements

- Minecraft 1.20.1
- Forge 47.x (client side)
- No dependencies

## Credits

- Architecture inspired by **World Templates** by *BrandonItaly* (NeoForge)
- World-cloning flow based on the **LonKraft Template** API
- Built by **Trilcera**

## License

MIT