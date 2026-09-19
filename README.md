# GlitchIdentity

Universal Glitch Identity system for Minecraft 1.21.11, with a shared core and Fabric/Paper adapters.

## Core behavior

Configured players keep their real identity hidden in death notifications.

- Configured killer -> only the killer region glitches.
- Configured victim -> only the victim region glitches.
- Both configured -> both regions glitch independently.
- Ordinary players remain normal unless configured.
- The configured player stays enabled until explicitly removed.
- Fabric clients receive continuously animated 5–7 character glitch identities in the actual chat line.
- Vanilla/non-GlitchIdentity clients receive a static non-leaking glitch identity.
- Paper provides a static non-leaking fallback.
- No real configured name is intentionally exposed in a displayed death notification.

## Glitch color modes

Each configured player has one glitch color mode:

- **COLORFUL** — the existing animated glitch style with independently randomized character colors.
- **WHITE** — the same animated glitch behavior, but every glitch character is rendered in white (#FFFFFF).

The color mode is required when adding a player.

### Add syntax

`text
/glitch add <player> colorful
/glitch add <player> white
/glitch add @ colorful
/glitch add @ white
`

The old /glitch add <player> syntax is intentionally not accepted, and invalid modes such as red are rejected.

Changing the mode of an already configured player updates that player's stored mode.

## Commands

- /glitch add <player> colorful|white — Enable glitch for one player using the selected mode.
- /glitch add @ colorful|white — Enable glitch for all currently online players using the selected mode.
- /glitch remove <player> — Disable glitch for one configured player.
- /glitch remove @ — Disable glitch for all configured players.
- /glitch list — Show configured glitch players and their modes.
- /glitch reload — Reload configuration.
- /glitch version — Show the installed plugin/mod version.
- /glitch help — Show the formatted command help.

### List output

Configured players are shown with their active mode, for example:

`text
UltraOP [COLORFUL]
Ahsan [WHITE]
`

### Tab completion

Tab completion is context-aware:

- **add** suggests online players who are not configured, plus @.
- After the player argument, **add** suggests exactly colorful and white.
- **remove** suggests online players who are configured, plus @.

All commands are OP/admin permission protected.

## Persistence

Configured modes are stored with UUIDs.

Current format:

`text
UUID|COLORFUL
UUID|WHITE
`

Legacy UUID-only entries from older versions are still supported and are interpreted as **COLORFUL**.

## Architecture

- Shared core: player store, color modes, frame generation, messages and message utilities.
- Fabric: common/server death interception plus client-side ChatHud animation.
- Paper: Paper/Adventure death-message replacement with DamageSource-based attribution.
- One Gradle project packages the Fabric and Paper components as a universal artifact.

## Minecraft target

Primary and tested target: Minecraft Java 1.21.11.

Nearby 1.21.x versions are not promised until they have their own compatibility/build coverage.

## Rendering

Fabric animation is rendered inside one persistent chat entry. New frames do not create new chat messages.

The renderer uses independently randomized characters and:

- **COLORFUL:** independently randomized colors per character.
- **WHITE:** every character uses white (#FFFFFF).

Animation continues while the chat entry remains renderable.

Paper and vanilla-client fallback messages are static by design; they never reveal the configured player's real identity.

## Validation and build

The repository includes automated tests for the core glitch behavior, including both COLORFUL and WHITE modes.

GitHub Actions runs:

`text
gradle clean test build --stacktrace
`

The build produces the universal artifact:

`text
build/libs/GlitchIdentity-1.0.0-universal.jar
`

## Requirements

- Minecraft Java Edition 1.21.11
- Fabric Loader 0.19.3 for Fabric
- Paper API/server support for the 1.21.11 target
- The GlitchIdentity artifact is intended for the corresponding Fabric/Paper runtime paths.

## License

See the repository license configuration and project settings for the current licensing status.
