# GlitchIdentity

Universal animated glitch-identity system for Minecraft **1.21.x**, targeting **Fabric** and **Paper**.

## Core behavior
Selected players have their real killer identity replaced in death notifications by a continuously animated, randomized, multi-colored glitch string. The animation occupies one fixed visual slot and must not spam repeated chat messages.

## Commands
- `/glitch add <player>`
- `/glitch remove <player>`
- `/glitch list`
- `/glitch reload`
- `/glitch help`

All commands require operator permission.

## Architecture
A shared core owns state, animation generation, configuration, and platform-neutral contracts. Fabric and Paper adapters provide platform-specific event interception and rendering.

## Compatibility
Primary target: Minecraft 1.21.11. The project should maximize compatibility across nearby 1.21.x releases (initially 1.21.9–1.21.11 where APIs permit) and avoid unnecessary version-specific internals.

## Important rendering requirement
The intended UX is one death notification whose killer-name region remains visually animated in place. A normal immutable server chat packet cannot be continuously rewritten after rendering; therefore the adapters must use the strongest platform-supported mechanism available, with client-side rendering/overlay support for Fabric and a compatible Paper-side component strategy where possible. Never leak the actual configured player's name in the displayed glitch segment.
