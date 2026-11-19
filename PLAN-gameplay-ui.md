# Plan: gameplay, UI and survival improvements (MahjongCraft 26.3)

Branch: `feature/gameplay-ui`, from `port/26.3`, pushed to `Aischii/MahjongCraft`.
Style: modern but Minecraft. Vanilla GUI sprites, 9-slice panels and font; translucent dark HUD panels
like vanilla chat/subtitles; small slide/fade animations (with a reduced-motion option); sound cues.
All new text in en_us, ja_jp, zh_cn and zh_tw.

## Why players get stuck today

There are no play keybinds. Discarding means right-clicking a tile in your own hand on the table;
call/riichi/win choices pop up a blocking screen; nothing explains this. Recipes exist but nothing
unlocks or explains them.

## Phase 1: controls (first)

- **Hand HUD**: your hand as tile icons along the bottom while seated; selected tile raised.
- **Default keys** (all in Controls → MahjongCraft, rebindable): mouse scroll selects a tile instead of
  the hotbar while seated; right-click (or Enter) discards the selected tile. Right-clicking the 3D tile
  on the table keeps working.
- **Action bar instead of the pop-up screen**: non-blocking buttons above the hand for
  Chii / Pon / Kan / Riichi / Tsumo / Ron / Skip, each with a key hint, plus the thinking timer.
  Click or press the key; you can still look around.
- **Contextual prompts**: action-bar hints such as "Your turn: scroll to pick a tile, right-click to
  discard" and "Pon available [P]".

## Phase 2: tile HUD, table feedback, sounds

- **Tile labels**: icon plus short label everywhere tiles appear in UI (5m, 3p, 7s, E/S/W/N,
  Wh/Gr/Rd); tile hints HUD uses the same.
- **Table feedback**: highlight the last discard, show whose turn it is, dora indicators and riichi
  sticks on the HUD, and the waiting tiles when tenpai.
- **Sounds and animations**: tile click/slide sounds, small draw/discard animations, wider use of the
  existing call voice lines.

## Phase 3: survival

- **Mahjong Tile Set** item (all 136 tiles). Right-click the table with it to load it (shown in the
  table labels); Start needs a loaded set. It stays in the table and drops when the table breaks.
  Creative mode and a gamerule skip the requirement. Single tiles stay as decoration.
- **Recipes**: add the Tile Set recipe; keep table/stool/dice/sticks at similar costs; unlock recipes
  in the recipe book when the player picks up an ingredient.
- **Mahjong Guide** item: right-click opens a paged screen (Crafting, Controls, Rules, Yaku, Stats);
  links the existing Yaku Overview screen.
- **Advancements** (MahjongCraft tab, 6): craft a table; play your first hand; win a hand; declare
  riichi; win with a yakuman; win a full game.
- **Wandering trader** sometimes sells a Tile Set, dice or the Guide; **village house chests** have a
  small chance of dice, scoring sticks or a Guide.

## Phase 4: stats and history

Per world/server, per player: games, wins, deal-ins, riichi count, top yaku, last 10 games.
Shown with `/mahjong stats [player]` and the Guide's Stats tab.

## Phase 5: README

Full player guide: fork notice; credits to the original author doublemoon1119 with the upstream link,
original dependency badges and the original Chinese support note kept; install for 26.3; crafting and
table setup; controls table; HUD explained; screenshots from the dev client; build from source.

## Verification per phase

`./gradlew build` (with tests for new pure logic: tile labels, stats aggregation, set-loading rules),
then a dev-client playtest of a bot hand covering the phase's features. One commit per phase.

## Later (not in this plan)

- Mahjong Master villager profession with the table as job site.
- Bot difficulty levels.

## Phase 6: 3-player (sanma)

Decided 2026-11-19:
- A "Players: 4 / 3" rule in Edit Rules; the same table block. 3-player tables show 3 seats and
  leave one side of the table empty. Bots fill empty seats as usual.
- 108 tiles: 2m–8m removed. No Chii. With a 1m indicator the dora is 9m.
- North is kita / nukidora: set it aside for a bonus dora and draw a replacement (new Kita key
  and HUD button).
- Tsumo loss: the two others pay their normal 4-player share; the missing seat's share is lost.
- Defaults 35,000 starting points and 40,000 to win (still editable).
