# MahjongCraft 0.6.0: implementation plan (handoff)

A self-contained brief for whoever builds 0.6.0. The decisions below were settled with the
project owner; don't re-open them, ask only if something here turns out to be impossible.

## Project facts

- Fork of doublemoon1119/MahjongCraft at https://github.com/Aischii/MahjongCraft. Work on branch
  `feature/gameplay-ui`, merge into `main` for the release.
- Fabric mod for **Minecraft 26.3**, Kotlin, Fabric Loom 1.19.4, Gradle 9.9.1. Minecraft 26.3 is
  **unobfuscated**: use the official Mojang class and method names (`Level`, `BlockEntity`,
  `InteractionResult`, `ServerLevel`…). There is no Yarn mapping.
- Gradle runs on JDK 21+; the build downloads a Java 25 toolchain itself.
  - `./gradlew build` builds the jar (`build/libs/mahjongcraft-mc26.3-<version>.jar`) and runs the
    unit tests (`src/test/kotlin`, 21 tests today, all must keep passing).
  - `./gradlew runClient` starts a dev client (uses `run/`, git-ignored).
- Version lives in `gradle.properties` (`modVersion`). Release notes come from `CHANGELOG.md`.
- Translations: `src/main/resources/assets/mahjongcraft/lang/{en_us,ja_jp,zh_cn,zh_tw}.json`.
  Every new key goes into all four files. Keep the files' existing line endings.
- Do not launch, close or edit the owner's own Minecraft install (Modrinth). Only the dev client.

## 1. Version

Set `modVersion = 0.6.0` in `gradle.properties`, and add a `## 0.6.0` section at the top of
`CHANGELOG.md` (same style as 0.5.1: short player-facing bullets under `### Playing` /
`### Decoration` / `### Fixes`).

## 2. Disconnect hardening

**Bug:** a 0.5.0 client on a 0.5.1 server was kicked with "Network protocol error" whenever it
was in riichi. The server's `DiscardRequest` (in
`game/mahjong/riichi/model/ActionOptions.kt`) gained a field `auto`; the old client's strict
`Json.decodeFromString` threw `JsonDecodingException: Encountered an unknown key 'auto'` inside
the payload handler, and an exception in a payload handler disconnects the player.

Do all three:

1. **Lenient JSON.** Add one shared instance, e.g. in `network/`:
   `val PayloadJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }`, and use it
   for every encode/decode in the mahjong payloads and listeners:
   `network/mahjong_game/*`, `network/mahjong_table/*`, `network/mahjong_tile_code/*`, plus
   the places that build those payloads' `extraData` (e.g. `MahjongPlayer.kt`,
   `ClientMahjongState.kt`, `ActionOptions.kt`). `network/mahjong_stats` already uses its own
   lenient instance; switch it to the shared one.
2. **No disconnect on a bad packet.** In `network/CustomPayloadListener.kt`, wrap the calls to
   `onClientReceive` and `onServerReceive` in `try/catch (e: Exception)` that logs
   `"Ignored a bad <id> packet"` with the exception (use the mod's logger) and returns.
3. **Version mismatch message.** When a player joins a table
   (`MahjongTablePayloadListener`, `MahjongTableBehavior.JOIN`), the client includes its mod
   version (read from `FabricLoader.getInstance().getModContainer("mahjongcraft")`). If it
   differs from the server's, send that player a chat message such as
   `"This server has MahjongCraft 0.6.0, you have 0.5.1. Please update."` (new lang key
   `mahjongcraft.message.version_mismatch` with two `%s`). Joining still works. The JOIN
   payload must stay readable by older servers/clients (an extra field, decoded leniently).

Tests: a unit test that decodes a `DiscardRequest` JSON with an extra unknown key using the
shared instance, and one that decodes an old-format JSON without `auto` (defaults to false).

## 3. Tile block: mahjong tiles as decorations, like candles

Today, sneak + right-click on a block with a Mahjong Tile item spawns a loose
`MahjongTileEntity` (`item/MahjongTile.kt`, `useOn`). Plain right-click cycles the tile's face:
the face is an int `code` 0..37 in the item's `DataComponents.CUSTOM_DATA`.
`MahjongTileEntityRenderer` and `CodeItemModelProperty` already draw a tile by its code.

Build:

- **Block** `mahjongcraft:mahjong_tile_block` (register in `registry/BlockRegistry.kt`, block
  entity type in `registry/BlockEntityTypeRegistry.kt`). It has no item of its own and is not
  in the creative tab; the Mahjong Tile item places it.
- **Block entity** stores up to **4 tiles** in placement order (each its `code`), a `pose`
  (`STANDING` or `FLAT`), and the facing of the first placement. Save/load with the usual
  `saveAdditional`/`loadAdditional` pattern used by `MahjongTableBlockEntity`, and sync to
  clients (update packet + `getUpdateTag`) so the renderer sees changes.
- **Placing:** sneak + right-click with a tile on a block face places a new tile block in front
  of that face (or adds to the tile block you clicked, up to 4). The tile keeps its face. This
  **replaces** the old loose-entity placement in `MahjongTile.useOn`; existing
  `MahjongTileEntity`s in worlds stay untouched. Plain right-click with a tile still cycles its
  face. Consume one tile unless the player is in creative.
- **Pose:** STANDING = tiles upright side by side in a row, like a hand, facing whoever placed
  the first tile; FLAT = face up in a 2×2. Right-click the block **with an empty hand** to flip
  the whole block between the two.
- **Taking back:** sneak + right-click with an empty hand removes the **last** tile placed and
  gives it to the player (or drops it if the inventory is full); removing the last one removes
  the block. Breaking the block drops every tile, each with its face (`CUSTOM_DATA` `code`).
- **Block feel:** waterloggable (like candles), no light, **no collision**, a small outline
  shape that fits the tiles so it can still be clicked and broken. Instantly breakable, wood/
  bamboo sounds like the table.
- **Rendering:** a `BlockEntityRenderer` that draws each tile with the existing tile item model
  for its code (reuse what `MahjongTileEntityRenderer` does), positioned per pose and count.
- **Text:** block name `block.mahjongcraft.mahjong_tile_block` ("Mahjong Tiles") in all four
  languages. README "Crafting" section: update the decoration paragraph (place up to 4 per
  block, empty hand flips standing/flat, sneak + empty hand takes one back). Mahjong Guide
  Crafting tab text if it mentions placing tiles.
- Needed assets: blockstate JSON and a (blank/particle) block model so the game loads cleanly;
  the visible tiles come from the renderer.

## 4. Verify

1. `./gradlew build`: compiles, all tests pass.
2. In the dev client (`./gradlew runClient`), check: place 1–4 tiles, mixed faces; flip pose;
   take back one at a time; break with tiles inside (drops keep faces); place in water;
   reload the world (tiles, pose and facing persist); a mahjong game still runs.
3. Check `run/logs/latest.log` for errors.

## 5. Release 0.6.0

GitHub Actions is blocked on this account (billing lock), so release from the local build:

1. Commit on `feature/gameplay-ui`, merge into `main` (`--no-ff`), push both.
2. Tag `v0.6.0` on `main` and push it (the Release workflow will start and fail on billing;
   ignore it).
3. Write release notes the same way `.github/workflows/release.yml` does (CHANGELOG section,
   Download line, Requirements table from `gradle.properties`, README link, credit to
   doublemoon1119), then:
   `gh release create v0.6.0 build/libs/mahjongcraft-mc26.3-0.6.0.jar --title "MahjongCraft v0.6.0 for Minecraft 26.3" --notes-file notes.md --generate-notes --notes-start-tag v0.5.1 --latest --verify-tag`
4. Download the asset back and confirm its sha256 matches the local jar.

Remind the owner: the VPS server and every player must update to 0.6.0 together.
