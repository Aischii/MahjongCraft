# MahjongCraft 0.7.0 implementation plan

## Scope

Build on the v0.6.0 release for Minecraft 26.3. Add the Mahjong Master villager, eleven advancements, and an in-game Guide section covering both. Keep stats and match history unchanged. This plan does not authorize publishing a release.

## 1. Mahjong Master

- A player-placed Mahjong Table is the job site. Register only its bottom-center part as a point of interest, across all applicable state variants, with capacity for one villager. A Tile Set is optional for claiming the job site.
- Use vanilla job claiming, work schedules, trade XP, and restocking. A match does not disable the job site. Use a player-established job site with no village generation changes.
- Keep the recipes and wandering-trader offers. All Mahjong Master trades take emeralds.

| Tier | Stock |
|---|---|
| Novice | Mahjong Guide for 3 emeralds and Tile Set for 8 |
| Apprentice | Loose tiles and dice |
| Journeyman | Scoring sticks |
| Expert | Stools |
| Master | Larger loose-tile bundle |

Use standard trade XP and limited stock so purchases can progress through all five tiers. Tune decoration prices, bundle sizes, and stock limits during dev-client playtesting. Check that a player who starts with an unloaded table can recruit a villager and buy the first Tile Set immediately.

## 2. Advancements

Extend the existing MahjongCraft advancement tab. Retain IDs, parent links, criteria, and progress for the six existing main advancements. New milestones are visible and use standard Minecraft frames and notifications. They grant no items or XP. The rare goals branch from the existing yakuman advancement.

| Advancement | Parent | Frame | Server-side completion event |
|---|---|---|---|
| Load a Tile Set | Root | Task | Player successfully loads a set into an unloaded table. |
| Trade with a Mahjong Master | Root | Task | Player completes a purchase from a villager of this profession. Opening the trade screen does not count. |
| Place Four Tiles Together | Root | Task | Player successfully adds the fourth decorative tile to a tile block. |
| First Kita | Play a Hand | Task | The game accepts and applies a Kita in sanma for that player. |
| Win by Tsumo | Win a Hand | Goal | The player wins by self-draw in the settled result. |
| Win by Ron | Win a Hand | Goal | The player wins off another player's discard or an eligible robbed Kan in the settled result. |
| Finish a Sanma Match | Play a Hand | Goal | The player is seated at the normal end of a completed sanma match. |
| Win a Sanma Match | Finish a Sanma Match | Goal | The player finishes first in the final sanma ranking. |
| Thirteen Orphans | Win a Yakuman | Challenge | The winner's settled yaku include Thirteen Orphans or its double variant. |
| Four Concealed Triplets | Win a Yakuman | Challenge | The winner's settled yaku include Four Concealed Triplets or its double variant. |
| Big Three Dragons | Win a Yakuman | Challenge | The winner's settled yaku include Big Three Dragons. |

Use the existing server-side advancement award helper for game events. Add hooks at authoritative actions and settlements. Use the actual win method, settled yaku identifiers, match mode, and final ranking. If the current advancement callback lacks a fact, pass it from the game event that has it. Match completion means normal game end, including an ordinary rule-based early end. Leaving or breaking a table does not count. Handle each winner separately on multi-Ron.

Bot matches count. Award only to eligible human participants or interacting players. Existing worlds earn new advancements on their next qualifying events. Leave the stats save format alone and do not backfill advancements from it.

## 3. Guide and text

- Add a Progression section to the existing in-game Mahjong Guide. Cover how to recruit a Mahjong Master, buy the first Tile Set, unlock trade tiers, and find the Survival and Gameplay milestones in the advancement tab.
- Explain sanma and Kita briefly and list the three named yakuman challenges. State that bot matches count.
- Document the villager and advancement paths in the README. Add the 0.7.0 changelog section and set the version when implementing the release.
- Localize the profession name, all advancement titles and descriptions, and Guide text in en_us, ja_jp, zh_cn, and zh_tw. Use official Mojang names in code.

## 4. Acceptance checks

1. Run the Gradle build and all tests. Add focused coverage for event-to-award mapping, sanma versus four-player results, both win methods, multi-winner attribution, and named versus unrelated yakuman including double variants. Validate advancement parent IDs and matching keys in all four language files.
2. In the dev client only, place an unloaded table, recruit a villager, confirm that a second villager cannot claim the same table, and repeat with a rotated table. Buy the Novice supplies, load the set, level through the trade tiers, and check restocking during an active match. Save and reload, then check profession and job-site persistence. Remove the table and verify normal vanilla job-site behavior.
3. In the dev client, earn the three survival milestones. Exercise Kita, Tsumo, Ron, and a completed bot sanma match. Verify that a bot win does not award the human a match win, a four-player match does not award sanma credit, and a canceled match does not award completion. Validate rare yaku deterministically through settled-result tests and check the challenge frames and localized text in the dev client.
4. Verify existing advancement progress in an upgraded test world and confirm that the new Guide section works. Review the dev-client log for errors. Keep the personal Minecraft installation out of the test process.
