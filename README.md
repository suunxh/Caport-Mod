# caport (Caport-Mod)

A **client-only Minecraft Java 1.8.9 / Forge** mod that prepares a teleport command
from the block you aim at or the nearest pressure plate in front of you. It uses
Java 8, Forge 11.15.1.2318, ForgeGradle 2.1, and Gradle 2.14.1. No mixins,
movement packets, server mod, or runtime third-party libraries are needed.

## Use the mod

- **G — Teleport to Target Block:** casts a dedicated ray up to 100 blocks,
  finds the first targeted block, and prepares a command for its top surface.
- **H — Teleport to Pressure Plate:** searches up to 64 blocks in a horizontal
  ±30° cone, then prepares a command for the nearest safe vanilla pressure plate.
- The default opens chat with `/tp {x} {y} {z}` filled in. **Press Enter yourself**
  to send it, or Escape to cancel. Nothing is sent automatically in this mode.
- Change either key in **Options → Controls → caport**. Mouse
  button binds are supported. If both actions share one key, neither runs; a
  message asks you to choose different keys. Other Minecraft key conflicts remain
  visible in Controls and should also be resolved there.
- Keys activate once per physical press, ignore chat/other screens, and require
  a focused game with a player and world. A key held while closing a GUI does not
  activate until released and pressed again. Very brief taps between client ticks
  can be missed; hold the key through at least one tick.

Stand still while an H search completes. Opening a screen, moving more than half
a block, changing worlds/players, or taking more than 200 ticks cancels the search.
Another feature keypress replaces the pending search. Facing is captured when H
is pressed; turning afterwards does not alter that search's cone.

The server must support the configured command and grant teleport permission.
The mod cannot create permission or force a teleport; the server can reject or
adjust destinations. Searching uses only client-loaded chunk data and does not
load new chunks or alter normal attack/interaction reach.

## Hypixel and server rules

**This mod is not Hypixel-approved.** Neither execution mode has official approval.
Housing teleport permission does not authorize a macro. Hypixel restricts gameplay
automation/macros, and even client-side targeting assistance is not guaranteed to
comply with its rules. Check current server rules before using the mod.

Direct command mode is disabled by default. To enable one-key commands without
editing a file:

- In singleplayer, type `/caport direct`.
- On an expressly authorized private server, type `/caport direct authorized`.
  This adds only that exact current hostname/IP to the allowlist.
- On a recognized Hypixel host, ordinary direct activation stays blocked.
  The separate `/caport direct hypixel-risk` command explicitly opts into the
  requested Hypixel behavior and displays a ban-risk warning. **This is not
  staff approval or a safe-testing guarantee.** Seek current Hypixel staff
  confirmation before using automated commands there.
- `/caport manual` restores chat confirmation and clears the Hypixel risk opt-in.
- `/caport status` shows the configured mode and Hypixel risk opt-in.

Mode changes take effect immediately and persist across restarts. G/H send one
ordinary command when direct mode is enabled and permitted by the local settings;
no Enter press is required. The existing cooldown, geometry checks, and server
permissions still apply. Blocked hosts fall back to manual chat; unsafe destinations are rejected.

Hypixel direct mode requires the general direct-command opt-in, an exact host in
`directAllowedServers`, and `hypixelDirectRiskAcknowledged=true`. These settings
are set together only by the explicit risk command on the current recognized
Hypixel host. Hostnames are normalized for case, ports, trailing dots, and IDN;
no wildcards are accepted. Recognition cannot detect every alias/proxy/IP, and
an allowlist entry or risk acknowledgment does not establish server permission.

**There is no guaranteed ban-free test on Hypixel.** Use a singleplayer creative
world with cheats enabled to verify targeting, plate height, and one-command
execution. On Hypixel, first seek staff clarification of current rules for the
exact behavior. Manual mode is not claimed to be approved either.

There are no automated retries, teleport chains, permission bypasses, position
spoofing, anti-cheat bypasses, or X-ray rendering. `/caport` is a client-local
settings command and is never sent to the server.

## Build a JAR

Install a **Java 8 JDK** (not just a JRE), point `JAVA_HOME` to it, and check
`java -version` and `javac -version`. Java 17/21 cannot run this legacy build.
The wrapper downloads the pinned Gradle runtime with SHA-256 verification.

From the repository directory on Linux/macOS:

```sh
./gradlew setupDecompWorkspace getAssetIndex -x getAssets --max-workers=2
python3 tools/fetch-assets.py
./gradlew setupDecompWorkspace build --max-workers=2
```

On Windows, use `gradlew.bat` and `py -3 tools/fetch-assets.py` instead. The Python
helper works around this ForgeGradle release's obsolete HTTP asset URL: it downloads
via HTTPS and verifies the asset index and every asset against Mojang's published
SHA-1 and size. It is repeatable and reuses verified cached files. It downloads no
assets into source control. Python 3 is a build/setup helper, not a mod dependency.

The first setup downloads/decompiles Minecraft and Forge and can take several
minutes. Later builds reuse the Gradle cache:

```sh
./gradlew build --max-workers=2
```

The installable output is **`build/libs/caport-1.0.1.jar`**.
Use the normal JAR, not a sources or development JAR. `build/`, `run/`, and
dependency caches are ignored and must not be committed. No Minecraft or Forge
binaries are included in this repository. The small Gradle wrapper JAR is the
build launcher; its provenance is documented in `gradle/wrapper/README.md`.

An IDE can import the Gradle project after setup. `./gradlew runClient` starts a
development client on a machine with an X11/desktop display and OpenGL support.
The mod needs no account credentials to build or run unit tests.

## Install

1. Download `caport-*.jar` from the Assets section of the
   [latest GitHub release](https://github.com/suunxh/Caport-Mod/releases/latest).
2. Install Minecraft 1.8.9 and the **Forge 1.8.9 11.15.1.2318** client profile,
   then copy the downloaded JAR into your Minecraft instance's `mods` folder.
   When updating, remove the previous copy of this mod first; keep only one caport
   installation. Your existing configuration/keybind settings are retained.
3. Launch the Forge profile and confirm **caport** appears in Mods.
4. Enter a world/server where you have permission, then try G or H and check the
   prepared command before pressing Enter. Do not install this mod on the server.

On Windows with the default launcher directory, press Win+R, enter
`%appdata%\.minecraft`, and create/open `mods` there. Keep the downloaded file as
`.jar`; it goes inside that folder. You do not need Gradle to install a release.

## Automatic releases

Every push to `main` builds the mod, runs the full tests, and publishes a GitHub
release with the installable JAR and SHA-256 checksum. A failed build publishes
nothing. Releases are named `build-<run number>-<attempt>` so each successful run
has its own download. Pushing a `v*` tag also publishes a release under that tag.
The workflow can be started manually from **Actions → Build and release mod JAR →
Run workflow**. It uses GitHub's built-in token; no personal token is required.

The workflow is in `.github/workflows/release.yml`; asset publication is handled
by `tools/publish-release.sh`. Build artifacts remain outside Git source control.

## Configuration

The configuration filename remains unchanged so existing settings are preserved.
After the first launch, edit **`config/housingteleporthelper.cfg`** in that Minecraft
instance while the game is closed, then restart. For `runClient`, this file is
under `run/config/`. Keybinds are saved by Minecraft Controls, not this file.
Forge includes explanatory comments in the generated configuration.

| Category | Setting | Default | Accepted values |
| --- | --- | --- | --- |
| general | targetBlockRange | 100 | 1–256 blocks |
| general | pressurePlateRange | 64 | 1–64 blocks (3D) |
| general | pressurePlateConeHalfAngle | 30 | 1–89 degrees |
| general | commandTemplate | `/tp {x} {y} {z}` | One slash command, exactly one of each placeholder |
| general | coordinatePrecision | 5 | Integer 0–8 decimal places |
| general | integerCoordinates | false | true / false |
| general | feedbackMessages | true | true / false |
| general | directCommandCooldownTicks | 20 | Integer 0–1200; 20 ticks = 1 second |
| execution | mode | MANUAL_CONFIRMATION | MANUAL_CONFIRMATION / DIRECT_COMMAND |
| execution | privateTestingAuthorized | false | General explicit direct-command opt-in (legacy setting name) |
| execution | hypixelDirectRiskAcknowledged | false | Separate Hypixel opt-in; not staff approval |
| execution | directAllowedServers | empty list | Exact explicitly opted-in hostnames/IPs |

The command template accepts literal alphanumeric arguments and exactly one
standalone `{x}`, `{y}`, `{z}` token each; for example `/teleport {x} {y} {z}`.
Newlines, duplicate/missing placeholders, unknown placeholders, and commands
longer than Minecraft 1.8.9's 100-character chat limit are rejected. Invalid
ranges, nonfinite/fractional integer settings, templates, and modes are restored
to safe defaults and saved. No server commands are inferred or probed.

Decimal output is locale-independent, uses no scientific notation, and omits
trailing zeros. Integer fallback **floors** each coordinate, including negative
ones. After formatting, the resulting coordinates are checked again; the helper
never silently substitutes another block. Integer mode or low precision can make
a fractional surface unsafe, in which case no command is prepared. Prefer decimals.

## Geometry and performance

Full blocks use their top center: block `(100,64,200)` gives `(100.5,65,200.5)`.
Slabs/stairs use their actual multipart collision boxes; the center's highest
supporting surface is selected only if the player's full body fits.

In **vanilla 1.8.9**, all four pressure plate types have **no collision box**.
Their 1/16 or 1/32 visual height is not a standing collision surface. A plate at
`(100,64,200)` on an ordinary block uses `(100.5,64,200.5)`, the support block's top,
so the player intersects the plate's activation volume. It does not use Y=65 or
the visible top. Unsupported plates and plates above tall fence surfaces are
rejected. Integer-only teleportation may not land exactly on other fractional
surfaces or as the server expects; it remains subject to clearance validation.

Checks include world height, loaded geometry/neighbors, actual support, player
bounding-box clearance, fluids, fire, and current destination geometry. The server
remains authoritative. Conservative checks near unloaded chunk edges can reject
otherwise safe locations; let nearby chunks load and try again.

H uses a snapshot of feet position and yaw. Candidates must be in the horizontal
cone, strictly ahead, and within the 3D radius of their actual standing position.
Zero-length horizontal vectors are excluded. Ranking is distance, angular
alignment, then lexicographic X/Y/Z order with a 1e-9 floating-point tolerance.
Stone, wooden, light weighted, and heavy weighted vanilla plates are supported.

Worst-case complexity is O(r³) with O(1) search state. Horizontal distance/cone
and loading checks prune columns before block lookups; null/empty chunk sections
skip 16-block spans. Collision checks are reserved for plate candidates. Each
client tick processes at most **8192 scan steps**, with a **2 ms time budget**
checked every 32 steps (a soft time bound including individual geometry checks).
No search runs while idle. Dense builds may take several seconds; searches time
out after 200 ticks and report cancellation instead of acting on an old result.

## Tests and further validation

```sh
./gradlew test
# Core-only tests, without downloading Forge/Minecraft:
./gradlew -b core-tests.gradle test
```

Tests cover ray distance/negative/unloaded/obstructed targets, full blocks/slabs/
stairs/plate support, directional/3D search filtering and ties, all cardinal yaw
directions, work budgets, formatting, private-server restrictions, manual/direct
delivery, cooldowns, physical key edges/conflicts/customization, and persistent
Forge configuration defaults/corrections. See [the testing checklist](docs/TESTING.md)
for the current results and in-game checks still requiring a player.

## Source layout

- `HousingTeleportHelperMod`, `ModConfiguration`, `ChatFeedback`, `CaportCommand`: Forge lifecycle,
  persistent configuration, client-local mode commands, and `[caport]` messages.
- `KeybindHandler`: Controls registration, physical input, bounded tick work,
  and cancellation.
- `MinecraftWorldView`: loaded client chunk access and 1.8.9 collision/ray APIs.
- `CommandExecutionService`: revalidation and connection to Minecraft chat.
- `core/`: testable targeting, incremental search, geometry, formatting, execution
  policy/delivery, and input gate. No Minecraft dependencies in this package.
- `src/test/java/`: unit/configuration tests; `src/main/resources/`: metadata and
  Controls translations; `tools/fetch-assets.py`: verified HTTPS asset preparation.

Cloud setup uses the existing checkout; each task is already isolated, so there
is no need to create a Git worktree. Retained Java/Gradle caches survive snapshots;
display/client processes must be started again when needed.
