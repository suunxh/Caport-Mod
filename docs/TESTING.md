# Validation and in-game checklist

## Original 1.0.0 validation

Validated in the cloud environment on 2026-10-09 with Temurin Java 8u472,
Gradle 2.14.1, pinned ForgeGradle 2.1, and Forge 1.8.9-11.15.1.2318:

- `./gradlew setupDecompWorkspace build --max-workers=2`: **passed** after filling
  the legacy asset cache via verified HTTPS downloads.
- Full test suite: **66 passed, 0 failed, 0 errors, 0 skipped**.
  Target/safety: 18; pressure plate search: 22; commands/keybinds: 24;
  persistent Forge configuration: 2.
- Initial core-only suite: **60 passed** before adding the four command-delivery
  tests; the complete build above exercised the current 64 core tests too.
- `./gradlew runClient`: started on an Xorg dummy display with Mesa software
  rendering, reached the Minecraft 1.8.9 main menu, and Forge reported
  **4 mods loaded and active**, including Housing Teleport Helper (the original 1.0.0 display name, now caport).
- The game generated `run/config/housingteleporthelper.cfg` with manual mode,
  authorization false, and an empty private-server allowlist.
- Both translated bindings appeared in Controls with G/H defaults. In a local
  creative flat world, G aimed at a stone block `(0,5,5)` and opened chat with
  `/tp 0.5 6 5.5`; H found a stone plate `(0,4,8)` and opened chat with
  `/tp 0.5 4 8.5`. Neither action sent a command automatically. Escape cancelled
  G; pressing Enter after H sent one command and the integrated server confirmed
  the teleport to `(0.5,4,8.5)`.
- The reobfuscated installable JAR was produced in `build/libs/`; generated
  outputs are ignored and are not intended for commit.

These are real development-client startup and representative singleplayer
functional checks, not proof of behavior on a multiplayer server. The cloud
machine has no audio device; Minecraft fell back to silent mode. No multiplayer
server permissions or Hypixel authorization have been verified. A first smoke
attempt encountered a class-loading failure after setup replaced the JAR while
the client was running; restarting with the completed artifact resolved it and
both features passed. Stop the client before rebuilding its runtime JAR.
The remaining checks below are not claimed as completed.

## Interactive checks (not yet completed)

- [ ] Install the built JAR in a normal Forge 1.8.9 client; see it in Mods.
- [x] Both actions appear in Controls with G/H defaults and the translated names (original branding).
- [ ] Rebind both keys, including a mouse button; conflicting assignments give
  feedback and perform neither action.
- [ ] Hold G/H: exactly one action. Open chat, type G/H, then close while holding
  the key: no action until a new press. Try pause/inventory screens as well.
- [ ] Aim at a full block, slab, and stair; verify top-center coordinates and
  decimal Y. Test negative X/Z, exactly the configured range, and beyond it.
- [ ] Put a ceiling/wall above the target or target a nonsolid block; no unsafe
  command is prepared. Test the world ceiling and unloaded chunk boundaries.
- [ ] Place each of the four vanilla plate types on ordinary supporting blocks.
  Verify H uses the support surface at plate-block Y, not Y+1.
- [ ] Place plates ahead/behind/outside the cone, above/below, and at/beyond the
  64-block 3D boundary. Test north/south/east/west facings.
- [ ] Use several plates: the nearest safe 3D candidate wins; equal distances use
  angle, then X/Y/Z. An obstructed nearer plate is skipped.
- [ ] Search a dense loaded build; verify responsiveness. Move, open a screen,
  leave the world, and respawn during a pending search; no old command executes.
- [x] Manual mode only opens prefilled chat. Escape cancels; Enter sends one
  ordinary command (verified locally). A server without permission can still
  reject that command.
- [ ] Test integer mode on full blocks/plates and decimal mode on fractional
  surfaces. Low precision/integer rounding must never embed the player or
  silently choose another block; unsupported rounded positions are rejected.
- [ ] With express permission on singleplayer/private testing, enable direct
  mode AND authorization (plus the exact private hostname for multiplayer).
  One press sends one ordinary command. Repeated presses within cooldown do
  not send; no retries occur after command denial.
- [ ] Disable authorization or remove the private host: direct mode falls back
  to manual chat. Recognized Hypixel domains stay blocked until the separate risk opt-in;
  this setting does not provide approval or a ban-free testing guarantee. Test
  the mechanics locally and seek staff confirmation before Hypixel use.
- [ ] Edit the configuration, restart, and verify persistence and invalid-value
  correction. Set feedback false to verify messages are suppressed.

## Reproduce automated checks

```sh
# After the initial Java 8 / Forge setup:
./gradlew build --max-workers=2
./gradlew -b core-tests.gradle test
```

JUnit results: `build/test-results/TEST-*.xml` (full suite) and
`build/core-tests/test-results/TEST-*.xml` (core suite). HTML reports are under
the corresponding `reports/tests/` directories. Use results from the current
command, and do not treat a skipped or zero-test run as a passing suite.

## caport 1.0.1 checks

The displayed mod name, Controls category, message prefix, JAR, and release title
now use `caport`. The existing configuration filename and keybinding identifiers
are retained to preserve user settings. `/caport direct`, `/caport manual`, and
`/caport status` provide client-local persistent mode controls. Private multiplayer
activation requires the `authorized` argument. Hypixel activation requires the
separate explicit `hypixel-risk` argument and warning; it remains blocked by default.

Automated tests additionally cover persisted mode changes, exact host opt-ins,
missing consent, invalid hosts, default Hypixel blocking, the separate Hypixel
risk flag, and revocation through manual mode. No Hypixel account or live-server
automation test has been performed, and no claim of approval or ban-free use is made.

- Full build: **71 passed, 0 failed, 0 errors, 0 skipped** (18 target/safety,
  22 pressure-plate search, 25 commands/keybinds, 6 persistent configuration).
- [x] Forge client started; caport resources and `[caport]` messages verified.
  `/caport status`, `/caport direct`, and `/caport manual` worked in singleplayer.
- [x] After `/caport direct`, G sent exactly one command to `(0.5,6,5.5)` and H
  sent exactly one command to `(0.5,4,8.5)`, with no chat screen or Enter press.
  The integrated server confirmed both teleports. `/caport manual` saved manual
  mode and H opened prefilled chat again; Escape cancelled it.
- [x] Mode persistence, default Hypixel blocking, separate risk opt-in, exact
  allowlisting, missing consent, and manual revocation passed automated checks.
- [ ] Restart a normal installed client to check mode persistence interactively.
- [ ] Test on an expressly permitted private multiplayer server. No live Hypixel
  test was performed; check current rules and seek staff confirmation first.

## caport 1.0.2 checks

Validated on 2026-10-10 with the same Java 8 / Forge toolchain:

- Full build: **74 passed, 0 failed, 0 errors, 0 skipped** (18 target/safety,
  22 pressure-plate search, 26 commands/keybinds, 8 persistent configuration).
- In the local Forge client, the first G action opened `/tp 0.5 6 5.5` in
  manual chat and showed the singleplayer `/caport direct` tip. Escape cancelled
  it. A second G action prepared the same command without repeating the tip.
  Neither preparation sent a teleport command.
- The generated configuration saved `manualModeHintShown=true` and retained
  `MANUAL_CONFIRMATION`. Automated reload checks verified that the hint state
  survives restarts and mode changes, and adding the setting to an older config
  preserves its direct mode, host opt-ins, custom command, and target range.
- Context tests verified the suggested singleplayer, private-server, and Hypixel
  commands, including hostname normalization and a Hypixel-looking unrelated host.
- Minecraft's Controls code saves key changes in `options.txt`; caport keeps its
  existing keybinding identifiers. No live Hypixel testing was performed.
