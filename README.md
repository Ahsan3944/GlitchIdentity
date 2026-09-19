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

## Commands

- /glitch add <player>
- /glitch remove <player>
- /glitch list
- /glitch reload
- /glitch help

All commands are OP-only.

## Architecture

- Shared core: player store, frame generation, messages and message utilities.
- Fabric: common/server death interception plus client-side ChatHud animation.
- Paper: Paper/Adventure death-message replacement with DamageSource-based attribution.
- One Gradle project attempts to package the Fabric and Paper components as a universal artifact.

## Minecraft target

Primary and tested target: Minecraft Java 1.21.11.

Nearby 1.21.x versions are not promised until they have their own compatibility/build coverage.

## Rendering

Fabric animation is rendered inside one persistent chat entry. New frames do not create new chat messages.

The renderer uses independently randomized characters and colors and continues while the chat entry remains renderable.

Paper and vanilla-client fallback messages are static by design; they never reveal the configured player's real identity.
