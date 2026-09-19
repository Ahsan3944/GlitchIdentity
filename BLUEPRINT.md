# GlitchIdentity — Implementation Blueprint

## 1. Product goal
GlitchIdentity is a cross-platform Minecraft 1.21.11 system that hides configured players' real identities in death notifications and replaces them with a continuously animated, randomized, multi-colored glitch identity.

### Core user experience
- A configured player remains configured until explicitly removed.
- If the configured player is the killer, only the killer-name region becomes glitch.
- If the configured player is the victim, only the victim-name region becomes glitch.
- If both are configured, both regions glitch independently.
- An ordinary player remains normal unless configured.
- The death line behaves like a real Minecraft chat line and remains animated while visible in chat history.
- Animation does not create repeated chat spam.
- A configured player's real name must not be exposed in displayed death notifications.

## 2. Runtime architecture

### Fabric 1.21.11
- Primary animated implementation.
- Uses the vanilla DamageTracker death message as the source message.
- Server replaces configured identities with private markers before delivery.
- Fabric clients render markers as continuously changing glitch segments.
- Non-Fabric clients receive a static randomized glitch replacement instead of the real configured name.
- The victim's native DeathMessageS2CPacket is sanitized separately, preserving the normal death-screen flow.
- Uses Fabric's common main initializer so integrated/singleplayer servers are supported.

### Paper 1.21.11
- Uses Paper/Adventure's native PlayerDeathEvent pipeline.
- Killer attribution checks DamageSource causing entity, direct entity, then Bukkit killer fallback.
- Paper intentionally provides a static non-leaking glitch fallback.
- The event's normal death-message pipeline is preserved by replacing event.deathMessage rather than manually sending a second chat message.
- Continuous in-place animation is a Fabric client capability, not claimed as a Paper-only feature.

## 3. Commands
All commands are OP-only:
- /glitch add <player>
- /glitch remove <player>
- Fabric accepts an online player name or UUID; Paper accepts an online/cached player name or UUID.
- /glitch list
- /glitch reload
- /glitch help

No additional public commands are planned for v1.

## 4. Glitch identity generation
- Visible length: 5–7 characters, normally 6.
- Every character is independently randomized.
- Every character receives an independent color.
- Pool includes letters, digits, symbols, currency, mathematical symbols, arrows and geometric/glitch glyphs.
- Consecutive frames are rejected when identical.
- Current Fabric frame interval is approximately 30 ms.
- No artificial six-second stop.

## 5. Rendering contract
Example:
kokoro was slain by 7@F2X9
kokoro was slain by #Q1A$Z
kokoro was slain by X9!3@K

Only configured identity regions change; the surrounding death message remains the vanilla message structure.

## 6. Message safety
- Real configured names are not placed in client-facing glitch markers.
- Fabric safe messages are derived from DamageTracker.getDeathMessage().
- Non-Fabric clients receive static corrupted replacements.
- The victim-specific native death packet is sanitized independently.
- Failure paths must prefer a non-leaking glitch fallback over exposing the configured name.

## 7. Persistence
- Store UUIDs.
- Resolve names only for administrative feedback/listing.
- Persist across restart.
- Reload replaces in-memory state from disk.
- Malformed UUID entries are ignored with warnings.

## 8. Architecture
Shared core:
- GlitchStore
- GlitchFrameGenerator
- GlitchMessages
- GlitchMessageKey
- future platform-neutral services/models

Fabric:
- common/server initializer
- client initializer
- server death interception mixin
- client ChatHud interception
- ChatHudLine dynamic rendering
- Fabric payload capability detection
- Fabric sanitizer/network adapter

Paper:
- Paper/Adventure adapter
- PlayerDeathEvent interception
- DamageSource attribution
- static non-leaking fallback

## 9. Universal artifact
Keep Fabric and Paper source in one Gradle project and publish a universal artifact where runtime metadata permits it.

The artifact must not load client-only classes on dedicated servers or Paper-only classes on Fabric paths.

## 10. Compatibility policy
- Primary tested target: Minecraft 1.21.11.
- Nearby 1.21.x compatibility is a future goal and is not promised until independently tested.
- Version-specific internals remain isolated in adapters/mixins.

## 11. Error and fallback policy
- Unknown player: clear command feedback.
- Duplicate add/remove: idempotent state and clear feedback.
- Malformed configuration: safe load with warning.
- Rendering failure: static corrupted identity.
- Never fall back to displaying a configured player's real name.

## 12. Testing matrix
Unit:
- 5–7 character frame length
- approved character pool
- color count and format
- frame variation
- message fingerprint stability
- store idempotency

Fabric:
- ordinary kill
- configured killer / ordinary victim
- configured player with a custom display name/team prefix
- configured player referenced by username and display name
- ordinary killer / configured victim
- both configured
- victim death packet
- chat history animation
- vanilla client static fallback
- integrated/singleplayer command registration

Paper:
- direct player kill
- projectile/indirect damage
- configured victim
- configured killer
- ordinary death
- death-message gamerule disabled
- native Paper death-message pipeline

## 13. Release
Repository: GlitchIdentity
Display name: Glitch Identity
Current project version: 1.0.0
Primary Minecraft target: 1.21.11
Fabric: animated client experience
Paper: static non-leaking fallback
