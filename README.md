# Disc Jockey

**[中文说明](README_zh.md)**

Play [Note Block Studio](https://opennbs.org/) songs on real note blocks in Minecraft — the mod tunes nearby note blocks and "clicks" them in sync with the song.

> A maintained fork of [SemmieDev/Disc-Jockey](https://github.com/SemmieDev/Disc-Jockey), updated for the latest Minecraft versions with many new features.

## Features

### Playback core
- Play `.nbs` songs on note blocks (works in survival / adventure)
- Import MIDI files (`.mid` / `.midi`) — experimental, enable it in the config
- **Preview** songs locally without touching note blocks
- Adjustable playback speed (`/discjockey speed`)
- Play modes: single loop, list loop, random, stop after current

### Song library
- Multi-level folders with navigation (open folder / `..` back)
- Search (applies to the current folder)
- Favorites — star a song, favorites are listed first
- Drag & drop song files into the window to import
- Auto-scroll to the last selected song, optionally remember it across restarts

### Smart playback
- **Auto-switch note blocks** — walks around a big note block setup? The mod re-scans nearby blocks and switches to the ones you can still reach (configurable)
- **Live tuning check** — if a block's pitch changed during playback (e.g. another player re-tuned it), playback pauses and re-tunes automatically
- Distance detection based on the vanilla reach (`blockInteractionRange`), so reach-extending mods work too
- On-screen progress bar overlay while a song plays (configurable, hidden while chat is open)
- Omnidirectional note block sounds (clientside, makes big setups sound better)

### User interface
- Song selection GUI (default key is `J`, or use `/discjockey`) — the keybind lives in its own "Disc Jockey" category in the controls settings
- Keyboard navigation in the list:
  | Key | Action |
  |-----|--------|
  | `↑` / `↓` | Select previous / next entry |
  | `Enter` | Play selected song / open folder / go up |
  | `Shift+Enter` | Preview selected song |
  | `←` | Move focus out of the list (to buttons) |
- After opening a folder, the `..` entry is focused automatically so you can keep navigating with the keyboard
- "Blocks" screen shows every note block the song needs with counts
- Instrument remapping (map an instrument to be played by a different instrument's note blocks)

## Commands (`/discjockey`)

| Command | Description |
|---------|-------------|
| `/discjockey` | Open the song selection screen |
| `/discjockey reload` | Reload all songs from the songs folder |
| `/discjockey play <song>` | Play a song by name (tab-completed) |
| `/discjockey random` | Play a random song from the current folder |
| `/discjockey stop` | Stop playback |
| `/discjockey speed <speed>` | Change playback speed |
| `/discjockey info` | Show current playback status |
| `/discjockey loop <yes\|no>` | Loop the current song |
| `/discjockey remapInstruments map\|unmap\|show\|clear` | Manage instrument mappings |

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version
2. Drop the mod jar, [Fabric API](https://modrinth.com/mod/fabric-api) and [Cloth Config](https://modrinth.com/mod/cloth-config) into your `mods` folder
3. Put your songs into `<minecraft config folder>/disc_jockey/songs/` (`.nbs`, `.mid`/`.midi`)
4. In game, stand near note blocks matching your song's instruments and press `J`

> The config file lives at `config/disc_jockey/config.json`.

## Configuration

| Option | Default | Description |
|--------|---------|-------------|
| Hide Warning | off | Skip the anti-cheat warning message |
| Disable Async Playback | off | Play notes synchronously on client ticks (fixes playback not starting, may cost FPS) |
| Omnidirectional Note Block Sounds | on | Clientside only, see above |
| Expected Server Version | All | How reachable note blocks are determined (`≤1.20.4`, `≥1.20.5`, or `All`) |
| Tuning Speed | Spigot | How fast blocks get tuned: Snail (10/s), Safe (20/s), Spigot, Flash — use lower speeds on anti-cheat servers |
| Playback Packet Ratelimit | 500/s | Cap on interaction packets per second while playing |
| Delay Playback Start By | 0s | Extra wait after tuning finishes before playback starts |
| Instrument Detection Workaround | on | Fixes noteblocks not detecting their instrument; may break custom-block plugins (Nexo etc.) |
| Enable Experimental MIDI | off | Load `.mid`/`.midi` files (takes effect after reload) |
| Auto Scroll to Last Selected | on | Scroll to and open the folder of the last selected song when opening the GUI |
| Remember Last Selected on Restart | off | Keep the last selected song after restarting the game |
| Show Progress Bar Overlay | on | HUD progress bar while playing (hidden when a screen/chat is open) |
| Auto Switch Note Blocks | on | When you move too far during playback, switch to other reachable note blocks instead of stopping |
| Retune Replaced Note Blocks | on | Verify the target block's pitch during playback; pause and re-tune automatically if it was replaced or re-tuned |

## Supported versions

| Maintainer | Minecraft versions |
|------------|--------------------|
| [SemmieDev (upstream)](https://github.com/SemmieDev/Disc-Jockey) | 1.18.x, 1.19.x, 1.20–1.20.2, 1.20.4–1.20.6, 1.21 |
| [This fork](https://github.com/xjjakm/Disc-Jockey) | 26.1, 26.2, 26.3 |

## Known behaviors

- Search and random play only apply to the currently opened folder
- The progress bar is hidden while the chat is open
- The song must run in survival mode; creative mode is rejected
- All songs in a folder must have their required instruments available within reach

## Troubleshooting

- **"You went too far away"** — you left the reach of a needed note block. Move back, or let Auto Switch Note Blocks find another block
- **"The Note Blocks near you are not in the correct configuration"** — build the missing note blocks listed in the chat
- **Tuning fails / kicks on servers** — lower Tuning Speed (Safe or Snail) and/or the packet ratelimit
- **MIDI files don't show up** — enable *Enable Experimental MIDI* and run `/discjockey reload`
