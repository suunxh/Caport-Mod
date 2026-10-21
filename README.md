# caport

Teleport to a targeted block or the nearest suitable pressure plate ahead.
Choose the release for your Minecraft version; the two JARs are not interchangeable.

## Before installing

| Minecraft Java version | Mod loader | Java runtime | Additional mod | JAR to download |
| --- | --- | --- | --- | --- |
| **1.8.9** | **Forge 11.15.1.2318** | **Java 8** | None | `caport-forge-1.8.9-1.2.2.jar` |
| **26.3** | **Fabric Loader 0.19.5 or newer compatible with 26.3** | **Java 25** | **Fabric API 0.162.0+26.3** (or a compatible newer build for 26.3) | `caport-fabric-26.3-1.2.2.jar` |

1. Install [Forge for 1.8.9](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html)
   or [Fabric Loader for 26.3](https://fabricmc.net/use/installer/), then launch that profile once.
2. Download the matching caport JAR from the
   [Forge releases](https://github.com/suunxh/Caport-Mod/releases?q=forge-1.8.9) or
   [Fabric releases](https://github.com/suunxh/Caport-Mod/releases?q=fabric-26.3).
3. Copy caport into that profile's `mods` folder. For Fabric, also copy
   [Fabric API for 26.3](https://github.com/FabricMC/fabric/releases/tag/0.162.0%2B26.3)
   into the same folder.
4. Remove the previous caport JAR when updating; keep one caport copy per profile.
   Launch the matching loader profile, using the Java runtime listed above.

For the default Windows launcher folder, press **Win+R**, enter
`%appdata%\.minecraft\mods`, and create the folder if needed. A launcher with a
custom game directory has its own `mods` folder. Use separate game directories
for different Minecraft versions. The official launcher normally supplies Java;
third-party launchers may need you to select Java 8 or Java 25 explicitly.
Install caport on the client only.

## Usage

- **H:** teleport to the block you aim at, up to 100 blocks away.
  Lily pads on water, closed/open attached trapdoors, fences, walls, panes, and
  other collision surfaces use their actual standing height. Ladders use their
  top edge where there is room to stand; aiming at either half of a door selects
  the top of the door. Grass/flowers, all rail types (including activator rails),
  torches, and signs target the ground beneath them when possible, otherwise the ray
  continues behind them. Entities do not obstruct targeting or the local landing
  check. Vines place you in the climbable space beside their supporting block.
  Cobwebs place you inside the aimed web. Normal climbing, slowing, and falling
  physics apply. If a full solid block has no clear
  standing position above it (such as the middle of a wall), caport requests a
  position **inside that aimed block**, rather than targeting through the wall.
- **Z:** teleport to the nearest suitable pressure plate ahead, within 64 blocks
  and a ±30° cone. A local plate cache speeds up repeated searches; initial cache
  loading and very dense builds can still take longer. Plates are checked again
  before a command is sent.
- Manual mode is the default: **Enter** sends the prepared command; **Escape** cancels.
- The first manual preparation shows a one-time tip for enabling direct mode.
- Direct mode sends one command per keypress without Enter. The default cooldown
  is one second; holding a key does not repeat teleports.
- Change the keys in **Options → Controls → caport**.
- A caport loaded message appears in chat when joining a world/server, unless
  chat feedback is disabled in the config.

The server must allow you to use `/tp`; the mod cannot force it to accept a
teleport. Embedded wall targets intentionally skip the normal standing/collision
check and can cause suffocation or falling, or be rejected/adjusted by the server.
Pressure plates still require a clear, supported landing position.

**Updating retains existing keybinds.** To use the new H/Z defaults on an existing
installation, reset the two caport controls or rebind them yourself.

Keybinds are saved by Minecraft in `options.txt`. Mode, server opt-ins, and command
settings persist across logins and restarts in the same installation:

- Forge 1.8.9: `config/housingteleporthelper.cfg` (existing settings are retained).
- Fabric 26.3: `config/caport.properties`.

Edit custom command/coordinate settings with Minecraft closed, then restart.
A different server hostname needs its own direct-mode opt-in. Fractional standing
heights retain extra Y precision when needed (up to eight decimal places), even
if the configured precision is lower. Integer-only mode still rejects a rounded
position that would no longer be a valid landing.

Target calculation uses loaded client world data. It sends no scan requests;
only submitting a teleport command sends its coordinates to the server. Caport
does not add custom movement/rotation packets or automatically hold climbing keys.

## Commands

| Command | What it does |
| --- | --- |
| `/caport status` | Show the current mode and Hypixel direct opt-in. |
| `/caport manual` | Restore manual confirmation and turn off Hypixel direct mode. |
| `/caport direct` | Enable direct mode in singleplayer. |
| `/caport direct authorized` | Enable direct mode for the current private server. |
| `/caport direct hypixel-risk` | Enable direct mode for the current Hypixel host, including Housing. |

**Use at your own risk.** Server rules apply; faster local lookup does not establish
ban-free use. Z can find loaded plates behind walls because it does not require
line of sight.
