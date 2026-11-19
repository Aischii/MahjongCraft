# Changelog

All notable changes to this fork of MahjongCraft. Each release's notes on GitHub are taken from its section here.

## 0.5.0

The first release of this fork, for **Minecraft 26.3**. It includes everything since the original mod's 0.3.13 (Minecraft 1.20.6).

### Minecraft 26.3
- Ported to Minecraft 26.3 (Fabric), Java 25.
- Fixed a server crash with C2ME and similar mods: game logic now runs on the server thread ([upstream issue #21](https://github.com/doublemoon1119/MahjongCraft/issues/21)).

### Playing
- **Hand HUD:** your hand is shown along the bottom of the screen with short labels (`1m`, `5p`, `E`, `Wh`…). Pick a tile with the mouse wheel or arrow keys, discard with right-click or Enter.
- **Action bar:** Chii, Pon, Kan, Riichi, Win, Draw and Skip appear as buttons above your hand, each with its key (J, B, K, R, V, N, Z). Nothing blocks your screen anymore.
- **Table panel:** whose turn it is, who is in riichi, wall count, honba, riichi sticks, dora, and your waits when tenpai.
- **Tile hints:** look at a tile to see its name and how many are left. The last discard glows gold.
- **Sounds** for discards, your turn and tile selection.
- **3-player mahjong (sanma):** set Players to 3 in Edit Rules. 108 tiles (no 2m–8m), no Chii, Kita (set a North aside as a dora with X), Tsumo loss, 35,000 / 40,000 points.

### Survival
- **Mahjong Tile Set:** craft it (8 tiles around a chest) and load it into a table to play. It drops when the table breaks. Creative players and `/gamerule mahjongcraft:require_tile_set false` skip it.
- **Mahjong Guide:** a book with Crafting, Controls (your current keys), Rules, Yaku and Stats tabs.
- Recipes unlock in the recipe book when you pick up an ingredient.
- Wandering traders sometimes sell a Tile Set, dice or a Guide; village houses sometimes hold them.
- **Advancements:** a MahjongCraft tab with six goals.
- **Stats:** per-world records (games, firsts, wins, deal-ins, riichi, top yaku, last 10 games) with `/mahjong stats [player]`.

### Interface
- Tooltips have a title and wrap instead of running off the screen.
- Rule options show the current choice and the alternatives.
- All new text in English, Japanese, Simplified Chinese and Traditional Chinese.
