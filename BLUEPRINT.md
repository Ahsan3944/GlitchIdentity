# GlitchIdentity — Implementation Blueprint

## 1. Goal
Build one project producing a universal distributable artifact where possible, with both Fabric and Paper entrypoints and a shared GlitchCore. Primary target Minecraft 1.21.11, with nearby 1.21.x compatibility prioritized.

## 2. Functional scope
- Maintain a persistent set of glitch-enabled player UUIDs/names.
- Intercept player-kill/death notifications.
- If the killer is configured for glitch identity, replace only the killer-name region with an animated 5–7 character corruption string.
- Each animation frame independently randomizes letters, digits, and symbols.
- Each character receives an independent bright/readable color.
- Frame updates occur rapidly in place; implementation must not flood chat with new death-message lines.
- The real killer name must never be exposed by the glitch rendering.
- Non-glitch kills remain normal.
- Configuration survives restart and reload.
- Commands are OP-only.

## 3. Command contract
`/glitch add <player>`
Adds a player to the glitch identity set.

`/glitch remove <player>`
Removes a player from the set.

`/glitch list`
Lists currently configured players.

`/glitch reload`
Reloads persistent configuration and applies it safely.

`/glitch help`
Shows only the supported commands.

No additional public commands are planned for v1.

## 4. Animation model
Default visible length: 6 characters, configurable internally within 5–7.
Character pool: A–Z, a–z, 0–9 plus a controlled symbol set such as `# @ $ % & * + = ? ! _`.
Each frame:
1. Generate a fresh character for every slot.
2. Generate a fresh color for every slot from a constrained high-contrast palette.
3. Render to exactly the same character slot.
4. Never render the configured real name.
Suggested frame interval: 50–100 ms, with 75 ms as the initial default.
Animation duration should be bounded to the notification lifetime unless the platform rendering surface supports persistent chat animation cleanly.

## 5. Visual contract
Example:
`UltraOP was slain by 7@F2X9`
Next frame:
`UltraOP was slain by #Q1A$Z`
Then:
`UltraOP was slain by X9!3@K`

The surrounding message stays stable. Only the glitch region changes.

## 6. Persistence
Use a small config file, e.g. `config/glitchidentity.json` or platform-appropriate equivalent.
Store UUIDs where possible; resolve names for command feedback/list display.
Atomic/safe writes are preferred.

## 7. Permissions
Every command requires OP. Platform adapters should reject non-players appropriately and avoid introducing a separate permission dependency in v1.

## 8. Shared module
GlitchCore should contain:
- GlitchIdentityService
- GlitchPlayerStore
- GlitchAnimation
- GlitchFrameGenerator
- ColorPalette
- DeathMessageModel
- GlitchConfig
- platform-neutral command/help model
No Fabric/Paper classes in core.

## 9. Fabric adapter
Use Fabric's server/client capabilities as required. Because live mutation of an already-rendered vanilla chat line is not inherently a server-only operation, prefer a controlled custom client rendering path when exact in-place animation is required. Keep networking/state messages minimal and deterministic.

## 10. Paper adapter
Use Paper/Adventure APIs to intercept/replace death messages without leaking the killer. For exact persistent in-place animated rendering, document the client capability boundary and use the best supported route rather than pretending a static chat component can animate after delivery.

## 11. Universal JAR
Attempt a single artifact containing both entrypoints and shared core. The build must ensure Fabric loader metadata and Paper metadata coexist without breaking either loader. If a truly single artifact is technically impossible for a release configuration, keep the shared source/build project and produce platform artifacts as a fallback rather than compromising runtime correctness.

## 12. Compatibility strategy
- Target 1.21.11 first.
- Test 1.21.10 and 1.21.9.
- Avoid NMS and unstable internals where possible.
- Isolate unavoidable version-specific code behind tiny adapter interfaces.
- CI matrix should compile/test each supported platform/version independently.

## 13. Error handling
- Unknown player: friendly command error.
- Duplicate add: no duplicate state; report already enabled.
- Remove missing player: report not configured.
- Malformed config: preserve backup and start from safe defaults, with clear console warning.
- Animation/render failures: fallback to a non-leaking static corrupted string rather than exposing the real killer name.

## 14. Testing
Unit:
- random frame length always 5–7
- characters always from approved pool
- colors always from approved palette
- no generated frame equals the real name by design
- store add/remove/idempotency
Integration:
- normal kill unchanged
- glitch killer intercepted
- non-op commands rejected
- reload preserves state
- target-version loading on Fabric and Paper

## 15. Release
Repository name: `GlitchIdentity`
Display name: `Glitch Identity`
Primary version: `1.0.0`
Primary Minecraft target: `1.21.11`
Compatibility goal: `1.21.9–1.21.11` where platform APIs permit.
