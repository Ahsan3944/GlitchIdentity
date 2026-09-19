# GlitchIdentity — Implementation Blueprint

## 1. Product goal

GlitchIdentity is a cross-platform Minecraft 1.21.11 system that hides configured players' real identities in death notifications and replaces them with a continuously animated, randomized glitch identity.

Each configured player has an explicit glitch color mode:

- **COLORFUL** — the existing multi-colored glitch behavior.
- **WHITE** — the same glitch behavior with all glitch characters rendered in white (#FFFFFF).

### Core user experience

- A configured player remains configured until explicitly removed.
- If the configured player is the killer, only the killer-name region becomes glitch.
- If the configured player is the victim, only the victim-name region becomes glitch.
- If both are configured, both regions glitch independently.
- An ordinary player remains normal unless configured.
- The death line behaves like a real Minecraft chat line and remains animated while visible in chat history.
- Animation does not create repeated chat spam.
- A configured player's real name must not be exposed in displayed death notifications.
- The selected COLORFUL/WHITE mode is preserved for that player.

## 2. Runtime architecture

### Fabric 1.21.11

- Primary animated implementation.
- Uses the vanilla DamageTracker death message as the source message.
- Server replaces configured identities with private markers before delivery.
- Fabric clients render markers as continuously changing glitch segments.
- Non-Fabric clients receive a static randomized glitch replacement instead of the real configured name.
- The victim's native DeathMessageS2CPacket is sanitized separately, preserving the normal death-screen flow.
- Uses Fabric's common main initializer so integrated/singleplayer servers are supported.
- The selected glitch mode is passed through the Fabric death-message pipeline.
- COLORFUL markers render with the existing randomized character colors.
- WHITE markers render every glitch character in white.

### Paper 1.21.11

- Uses Paper/Adventure's native PlayerDeathEvent pipeline.
- Killer attribution checks DamageSource causing entity, direct entity, then Bukkit killer fallback.
- Paper intentionally provides a static non-leaking glitch fallback.
- The event's normal death-message pipeline is preserved by replacing event.deathMessage rather than manually sending a second chat message.
- The selected COLORFUL/WHITE mode is used when generating the static glitch replacement.
- Continuous in-place animation is a Fabric client capability, not claimed as a Paper-only feature.

## 3. Commands

All commands are OP-only.

### Add

The color mode is mandatory:

`text
/glitch add <player> colorful
/glitch add <player> white
/glitch add @ colorful
/glitch add @ white
`

Rules:

- /glitch add <player> must not execute.
- The color argument must be exactly colorful or white.
- Invalid values such as red must not execute.
- Adding an already configured player with a different mode changes the stored mode.
- @ applies the selected mode to all currently online players.

### Other commands

`text
/glitch remove <player>
/glitch remove @
/glitch list
/glitch reload
/glitch version
/glitch help
`

### List

Configured players are displayed with their active mode:

`text
UltraOP [COLORFUL]
Ahsan [WHITE]
`

### Tab completion

- /glitch add suggests online players who are not configured and @.
- After the player argument, completion suggests exactly:
  - colorful
  - white
- /glitch remove suggests online configured players and @.

## 4. Glitch identity generation

- Visible length: 5–7 characters, normally 6.
- Every character is independently randomized.
- Every character receives an independent color in COLORFUL mode.
- WHITE mode forces every character color to 0xFFFFFF.
- Pool includes letters, digits, symbols, currency, mathematical symbols, arrows and geometric/glitch glyphs.
- Consecutive frames are rejected when identical.
- Current Fabric frame interval is approximately 30 ms.
- No artificial six-second stop.
- The existing COLORFUL generation behavior is preserved.

## 5. Rendering contract

Example:

`text
kokoro was slain by 7@F2X9
kokoro was slain by #Q1A$Z
kokoro was slain by X9!3@K
`

Only configured identity regions change; the surrounding death message remains the vanilla message structure.

### Mode rendering

COLORFUL:

`text
7@F2X9
#Q1A$Z
X9!3@K
`

with independently randomized character colors.

WHITE:

`text
7@F2X9
#Q1A$Z
X9!3@K
`

with every character rendered as white.

The character-generation logic remains the same between modes; the selected mode controls the glitch character colors.

## 6. Message safety

- Real configured names are not placed in client-facing glitch markers.
- Fabric safe messages are derived from DamageTracker.getDeathMessage().
- Fabric mode-specific markers carry the selected COLORFUL/WHITE mode.
- Non-Fabric clients receive static corrupted replacements.
- The victim-specific native death packet is sanitized independently.
- Failure paths must prefer a non-leaking glitch fallback over exposing the configured name.
- White mode must never fall back to displaying the configured player's real name.

## 7. Persistence

Configured player modes are stored by UUID.

### Current format

`text
UUID|COLORFUL
UUID|WHITE
`

### Backward compatibility

Legacy UUID-only entries are interpreted as:

`text
UUID|COLORFUL
`

This preserves existing installations that were configured before color modes were added.

Persistence requirements:

- Store UUIDs.
- Store the selected glitch mode with each UUID.
- Resolve names only for administrative feedback/listing.
- Persist across restart.
- Reload replaces in-memory state from disk.
- Malformed UUID entries are ignored with warnings.
- Invalid mode entries are ignored with warnings.
- Adding an already configured player with a different mode updates the stored mode.

## 8. Architecture

### Shared core

- GlitchStore
- GlitchColorMode
- GlitchFrameGenerator
- GlitchMessages
- GlitchMessageKey
- AnimatedGlitchText
- GlitchTextSanitizer
- Future platform-neutral services/models

### Fabric

- Common/server initializer
- Client initializer
- Server death interception mixin
- Client ChatHud interception
- ChatHudLine dynamic rendering
- Fabric payload capability detection
- Fabric sanitizer/network adapter
- Mode-aware Fabric payload markers

### Paper

- Paper/Adventure adapter
- PlayerDeathEvent interception
- DamageSource attribution
- Mode-aware static non-leaking fallback

## 9. Universal artifact

Keep Fabric and Paper source in one Gradle project and publish a universal artifact where runtime metadata permits it.

The artifact must not load client-only classes on dedicated servers or Paper-only classes on Fabric paths.

Current artifact:

`text
GlitchIdentity-1.0.0-universal.jar
`

## 10. Compatibility policy

- Primary tested target: Minecraft 1.21.11.
- Fabric Loader target: 0.19.3.
- Nearby 1.21.x compatibility is a future goal and is not promised until independently tested.
- Version-specific internals remain isolated in adapters/mixins.

## 11. Error and fallback policy

- Unknown player: clear command feedback.
- Duplicate add/remove: idempotent state and clear feedback.
- Missing add color argument: command must be rejected.
- Invalid add color argument: command must be rejected.
- Malformed configuration: safe load with warning.
- Rendering failure: static corrupted identity.
- Never fall back to displaying a configured player's real name.
- White mode must retain non-leaking behavior on all fallback paths.

## 12. Testing matrix

### Unit tests

- 5–7 character frame length
- Approved character pool
- Color count and format
- Frame variation
- Message fingerprint stability
- Store idempotency
- COLORFUL mode storage
- WHITE mode storage
- WHITE frame colors are all 0xFFFFFF
- Legacy/default COLORFUL behavior remains intact

### Fabric

- Ordinary kill
- Configured killer / ordinary victim
- Configured player with a custom display name/team prefix
- Configured player referenced by username and display name
- Ordinary killer / configured victim
- Both configured
- Victim death packet
- Chat history animation
- COLORFUL animated rendering
- WHITE animated rendering
- Vanilla client static fallback
- Integrated/singleplayer command registration
- /glitch add requires a valid color mode
- Tab completion exposes exactly colorful and white
- /glitch list displays the stored mode

### Paper

- Direct player kill
- Projectile/indirect damage
- Configured victim
- Configured killer
- Ordinary death
- Death-message gamerule disabled
- Native Paper death-message pipeline
- COLORFUL static rendering
- WHITE static rendering
- Mode persistence and reload
- Required color argument validation
- Tab completion for colorful and white
- /glitch list displays the stored mode

### Build validation

GitHub Actions runs:

`text
gradle clean test build --stacktrace
`

The workflow also uploads the generated universal JAR as a build artifact.

## 13. Release

Repository: GlitchIdentity
Display name: Glitch Identity
Current project version: 1.0.0
Primary Minecraft target: 1.21.11
Fabric Loader: 0.19.3
Fabric: animated client experience
Paper: static non-leaking fallback
Color modes: COLORFUL and WHITE
Persistence: UUID|MODE with legacy UUID-only compatibility
Universal artifact: GlitchIdentity-1.0.0-universal.jar

### Current command contract

`text
/glitch add <player> colorful
/glitch add <player> white
/glitch add @ colorful
/glitch add @ white
/glitch remove <player>
/glitch remove @
/glitch list
/glitch reload
/glitch version
/glitch help
`
