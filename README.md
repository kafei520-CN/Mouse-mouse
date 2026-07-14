<div align="center">
  <img src="https://raw.githubusercontent.com/kafei520-CN/Mouse-mouse/main/common/src/main/resources/mouse_logo.png" width="144" alt="Mouse-mouse Logo">

  <h1>Mouse-mouse</h1>

  <p><strong>Multi-instance mouse &amp; keyboard input splitter for Minecraft</strong></p>
  <p>Let two players share one PC, each controlling their own Minecraft window with their own devices</p>

  <p>
    <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.21--1.21.x-62B47A?style=for-the-badge">
    <img alt="NeoForge" src="https://img.shields.io/badge/NeoForge-21.1.x-EF6F3E?style=for-the-badge">
    <img alt="Fabric" src="https://img.shields.io/badge/Fabric-0.16.x-B9C4D4?style=for-the-badge">
    <img alt="Platform" src="https://img.shields.io/badge/Platform-Windows%20Only-0078D6?style=for-the-badge">
    <img alt="License" src="https://img.shields.io/badge/License-GPL--3.0-2E3440?style=for-the-badge">
  </p>
</div>

---

## What does this mod do?

Mouse-mouse solves one specific problem: **two people playing Minecraft simultaneously on the same PC**.

When two Minecraft windows are open on the same machine, they compete for the same mouse and keyboard — only the focused window receives input normally. This mod captures raw input at the system level via Windows Raw Input and routes each device's events to its assigned Minecraft instance, so both windows run independently without interfering with each other.

## Features

- **Per-instance device selection** — each window picks its own mouse and keyboard, no conflicts
- **Virtual cursor** — menus use a software cursor for clicks, drags, and scrolling, isolated from the physical mouse
- **Input isolation** — events from unassigned devices are blocked so they don't affect the game
- **Safety chord `Alt + F8`** — opens the device selection screen at any time and temporarily releases mouse capture
- **No config files** — device selection is stored only in memory; nothing is written to disk
- **Multi-instance cooperation** — both instances share one splitter process; duplicate launches are detected and skipped automatically

## Installation

> **Windows only.** This mod relies on the Windows Raw Input API and does not work on Linux or macOS.

### Step 1 — Download splitter.exe

Download `splitter.exe` from [GitHub Releases](https://github.com/kafei520-CN/Mouse-mouse/releases/tag/exe) and place it in your Minecraft instance's `mods/` folder:

```
.minecraft/mods/
├── Mouse-mouse-NeoForge-x.x.x+mcx.xx.x.jar   ← mod jar
└── splitter.exe                                 ← required, download separately
```

> `splitter.exe` is the Raw Input capture process. The mod launches it automatically on startup. If it is missing from `mods/`, the mod will not function.

### Step 2 — Install the mod

1. Install **NeoForge** or **Fabric** for your Minecraft version
2. Drop the mod `.jar` into your `mods/` folder
3. **(Fabric only)** Also install [Fabric API](https://modrinth.com/mod/fabric-api)
4. Confirm `splitter.exe` is present in `mods/`
5. Launch the game, then press `Alt + F8` in-game or on a screen to open the device selection UI

## Usage

### Two players, one PC

1. Start the first Minecraft instance
2. Start the second Minecraft instance
3. In the first window, press `Alt + F8`, select the first set of devices, click **Save**
4. In the second window, press `Alt + F8`, select the second set of devices, click **Save**
5. Both windows now respond independently to their assigned devices

### Safety chord `Alt + F8`

| Situation | Effect |
| --- | --- |
| Want to change devices | Opens the device selection screen |
| Mouse is captured and can't leave the window | Temporarily releases mouse capture |
| Wrong device selected | Re-enter the screen to reassign or deselect |

## Compatibility

| | Requirement |
| --- | --- |
| Minecraft | 1.21 – 1.21.x |
| NeoForge | 21.1.x |
| Fabric Loader | 0.16.x or later |
| Fabric API | Required |
| Java | 21 |
| OS | **Windows only** |
| Local port | `127.0.0.1:19091` (localhost, not exposed externally) |

## Known Limitations

- Windows clients only — Linux and macOS are not supported
- Device selection is not persisted; you must re-select after every launch
- If port `19091` is already in use by another process, the mod will fail to start
- Some virtual HID devices or touchpads may appear in the device list
- This is a client-side mod; the server does not need it installed

## How It Works

```
Physical devices A / B
        │
        ▼
  splitter.exe  (Windows Raw Input)
        │ IPC: 127.0.0.1:19091
        ▼
  Minecraft instance A ←── input from device A
  Minecraft instance B ←── input from device B
```

On startup, the mod launches `splitter.exe`, which enumerates physical input devices via Windows Raw Input and opens an IPC channel. Each Minecraft instance claims the devices it wants; the Java side then receives raw events from those devices and injects them into Minecraft's input pipeline via Mixin. Menu interaction is handled through a per-instance virtual cursor, so the physical mouse position does not directly affect GUI state.

## License

GPL-3.0-only — see [LICENSE](LICENSE) for the full terms.
