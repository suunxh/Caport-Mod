# caport

A teleport helper for Minecraft Java 1.8.9 with Forge 11.15.1.2318.
Download the JAR from the [latest release](https://github.com/suunxh/Caport-Mod/releases/latest)
and place it in your Minecraft `mods` folder. Remove the old copy when updating.

## Usage

- **G:** teleport to the top of the block you aim at, up to 100 blocks away.
- **H:** teleport to the nearest suitable pressure plate ahead, within 64 blocks.
- Manual mode opens chat with the teleport command ready. Press **Enter** to send
  or **Escape** to cancel.
- Direct mode sends one command per keypress without pressing Enter.
- Change the keys in **Options → Controls → caport**.

The server must allow you to use `/tp`. Mode changes are saved across restarts.

## Commands

| Command | What it does |
| --- | --- |
| `/caport status` | Show the current mode and Hypixel direct opt-in. |
| `/caport manual` | Restore manual confirmation and turn off Hypixel direct mode. |
| `/caport direct` | Enable direct mode in singleplayer. |
| `/caport direct authorized` | Enable direct mode for the current private server. |
| `/caport direct hypixel-risk` | Enable direct mode for the current Hypixel host, including Housing. |

**Use at your own risk.**
