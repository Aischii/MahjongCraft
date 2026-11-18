# Plan: fix issue #21 and port MahjongCraft to Minecraft 26.3 (Fabric)

Fork: `Aischii/MahjongCraft`, base `main` (1.20.6, mod 0.3.13). Upstream `refactor/v2` is not used.

## The bug (upstream issue #21)

`MahjongGame.startRound` and `MahjongGame` wait-for-start run in `CoroutineScope(Dispatchers.IO)`
(`MahjongGame.kt:257`, `:1428`), so entity spawn/remove and table sync run off the server thread.
With C2ME (via Architectury's entity hook) this throws
`ConcurrentModificationException: Async entity unload` at `MahjongGame.kt:277` (removing dice).
The handler only logs "Something happened", the round loop dies, bots stop, tile clicks do nothing.

## Decisions

| Topic | Decision |
|---|---|
| Base | `main` branch |
| Fix | Coroutine dispatcher that runs on the Minecraft server thread (`server.execute`); all game coroutines use it. `delay()` keeps working. |
| Client coroutines | Audit; change only those that touch client state off the render thread (most already use `ClientScheduler`). |
| Errors in a round | Log the full stack trace, tell players in chat, reset the table to its waiting state. |
| Targets | 26.3 only |
| Order | Branch 1 fix on 1.20.6, then branch 2 port on top of it |
| Mappings | Loom `migrateMappings` to Mojang names on 1.20.6 first, build green, commit; then bump to 26.3 |
| Toolchain | Upgrade Gradle wrapper + Loom to run on JDK 26; Gradle toolchains provision JDK 21 (1.20.6) and 25 (26.3) |
| Version | 0.4.0, jar `mahjongcraft-mc26.3-0.4.0.jar` |
| Delivery | Branches on the fork only; no upstream PR for now |
| Verification | Gradle build + unit test that game coroutines run on the server-thread dispatcher; Claude runs `runClient` and drives a bot game via computer use |

## Target dependency versions (checked 2026-11-18)

- Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.166.2+26.3
- Fabric Language Kotlin 1.14.2+kotlin.2.4.21 (Kotlin 2.4.21)
- Cloth Config 26.3.159+fabric, Mod Menu 21.0.1
- LibGui 18.0.1 (built against 26.3-rc-2; confirm it loads on 26.3 final)
- Mahjong4j 0.3.2 (plain Java, unchanged)

## Steps

### Branch `fix/server-thread-dispatcher` (on 1.20.6)
1. Upgrade the Gradle wrapper and Loom so the build runs on JDK 26 with a JDK 21 toolchain; build green.
2. Add a server-thread `CoroutineDispatcher` and move `jobRound` / `jobWaitForStart` (and the
   `withContext(Dispatchers.Default)` in `MahjongBoard.kt:355` if it touches the world) onto it.
3. Replace the "Something happened" handler: log, notify players, reset the table.
4. Audit client coroutines; fix any that touch client state off-thread.
5. Unit test for the dispatcher; build green; commit.

### Branch `port/26.3` (from branch 1)
6. `migrateMappings` to Mojang names on 1.20.6; fix leftovers; build green; commit.
7. Bump Minecraft, Loader, Fabric API, FLK/Kotlin, Cloth, Mod Menu, LibGui, Java 25; mod 0.4.0.
8. Fix the real API breaks (networking payloads, rendering, entities, items/data components, GUI input).
9. Build green; `runClient`; play a 4-bot round through at least one hand; commit and push both branches.
