# caport

Teleport to a targeted block or the nearest suitable pressure plate ahead.
Choose the release for your Minecraft version; the two JARs are not interchangeable.

## Before installing

| Minecraft Java version | Mod loader | Java runtime | Additional mod | JAR to download |
| --- | --- | --- | --- | --- |
| **1.8.9** | **Forge 11.15.1.2318** | **Java 8** | None | `caport-forge-1.8.9-1.1.0.jar` |
| **26.3** | **Fabric Loader 0.19.5 or newer compatible with 26.3** | **Java 25** | **Fabric API 0.162.0+26.3** (or a compatible newer build for 26.3) | `caport-fabric-26.3-1.1.0.jar` |

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

- **G:** teleport to the top of the block you aim at, up to 100 blocks away.
  Closed/open trapdoors, fences, walls, panes, and other collision surfaces are
  supported where the player has actual support and enough room.
- **H:** teleport to the nearest suitable pressure plate ahead, within 64 blocks
  and a ±30° cone. A local plate cache speeds up repeated searches; initial cache
  loading and very dense builds can still take longer. Plates are checked again
  before a command is sent.
- Manual mode is the default: **Enter** sends the prepared command; **Escape** cancels.
- The first manual preparation shows a one-time tip for enabling direct mode.
- Direct mode sends one command per keypress without Enter. The default cooldown
  is one second; holding a key does not repeat teleports.
- Change the keys in **Options → Controls → caport**.

The server must allow you to use `/tp`. Unsupported or obstructed destinations
are rejected; the mod cannot force the server to accept a teleport.

Keybinds are saved by Minecraft in `options.txt`. Mode, server opt-ins, and command
settings persist across logins and restarts in the same installation:

- Forge 1.8.9: `config/housingteleporthelper.cfg` (existing settings are retained).
- Fabric 26.3: `config/caport.properties`.

Edit custom command/coordinate settings with Minecraft closed, then restart.
A different server hostname needs its own direct-mode opt-in.

## Commands

| Command | What it does |
| --- | --- |
| `/caport status` | Show the current mode and Hypixel direct opt-in. |
| `/caport manual` | Restore manual confirmation and turn off Hypixel direct mode. |
| `/caport direct` | Enable direct mode in singleplayer. |
| `/caport direct authorized` | Enable direct mode for the current private server. |
| `/caport direct hypixel-risk` | Enable direct mode for the current Hypixel host, including Housing. |

**Use at your own risk.** Server rules apply; faster local lookup does not establish
ban-free use. H can find loaded plates behind walls because it does not require
line of sight.
