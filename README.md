# MahjongCraft

A Fabric mod for Minecraft that lets you play Japanese (Riichi) Mahjong in game, alone against bots or with friends in multiplayer.

> **This is a fork.** MahjongCraft was created by **[doublemoon1119](https://github.com/doublemoon1119)**. The original project lives at **[doublemoon1119/MahjongCraft](https://github.com/doublemoon1119/MahjongCraft)**, and all credit for the mod, its game engine, models and textures goes there.
>
> This fork ([Aischii/MahjongCraft](https://github.com/Aischii/MahjongCraft)) ports the mod to **Minecraft 26.3**, fixes a server crash with C2ME and similar mods ([upstream issue #21](https://github.com/doublemoon1119/MahjongCraft/issues/21)), and adds an in-game HUD and key controls so the game can be played without memorising anything.

## Requirements

| | Version |
|---|---|
| Minecraft | 26.3 |
| Java | 25 |
| [Fabric Loader](https://fabricmc.net/use/) | 0.20.1 or newer |

## Dependencies
<div>
    <a href="https://www.curseforge.com/minecraft/mc-mods/fabric-api"><img alt="" src="https://i.imgur.com/Ol1Tcf8.png" height="50"/></a>
    <a href="https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin"><img alt="" src="https://i.imgur.com/c1DH9VL.png" height="50"/></a>
</div>

- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
- [Fabric Language Kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin)
- [Cloth Config API (Fabric)](https://www.curseforge.com/minecraft/mc-mods/cloth-config)
- [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) (Optional)

## Installing

1. Install Fabric Loader for Minecraft 26.3.
2. Put these jars in your `mods` folder: MahjongCraft, Fabric API, Fabric Language Kotlin and Cloth Config. Mod Menu is optional.
3. Start the game. On a server, every player needs the mod too.

## Getting started

1. **Craft a Mahjong Table and place it.** It takes up a 3×3 area, so leave room around it.
2. **Right-click the table.** The waiting room opens, and the first player becomes the host.
3. **Fill the seats.** Friends right-click the same table to join, or the host clicks **Add Bot** for each empty seat.
4. **Set the rules (optional).** The host clicks **Edit Rules** to change the game length, thinking time, starting points and more.
5. **Ready up and start.** Everyone clicks **Ready**, then the host clicks **Start**. Seats and hands are set up automatically.

## Crafting

| Item | Recipe | Makes |
|---|---|---|
| Mahjong Table | 3 lime or green carpet across the top, a wooden fence in the middle, a wooden slab at the bottom | 1 |
| Mahjong Stool | 2 gray carpet on top of 2 sticks | 1 |
| Mahjong Tile | 6 white concrete beside 3 lime or green concrete (two columns of white, one of green) | 16 |
| Mahjong Scoring Stick | 1 red or black dye above 3 white concrete | 6 |
| Dice | white concrete, red dye and black dye (shapeless) | 2 |

The table is all you need to play: tiles, sticks and dice are spawned on it for each game. The loose items are for decoration. Right-click a tile or stick to change which one it is, and sneak + right-click a block to place it.

## Controls

While you are seated at a game, your hand is shown along the bottom of the screen and the available actions appear as buttons above it, each labelled with its key. You never need to aim at tiles on the table.

| Action | Default key |
|---|---|
| Choose a tile | Mouse wheel, or ← / → |
| Discard the chosen tile, or confirm | Right-click (Use), or Enter |
| Chii | J |
| Pon | B |
| Kan | K |
| Riichi | R |
| Win (Tsumo / Ron) | V |
| Abortive draw (Kyuushu Kyuuhai) | N |
| Skip / pass | Z |

When an action can be done more than one way (for example two different Chii pairs), press its key again to cycle through them, then confirm.

| Other action | Default key |
|---|---|
| Open the mod config | ; |
| Move the HUD | unbound |
| Yaku overview | unbound |

Every key can be changed under **Options → Controls → Key Binds → MahjongCraft**.

## The HUD

**Your hand (bottom of the screen)**

Each tile is drawn with a short label underneath:

| Label | Meaning |
|---|---|
| `1m`–`9m` | Man (characters) |
| `1p`–`9p` | Pin (circles) |
| `1s`–`9s` | Sou (bamboo) |
| `E` `S` `W` `N` | East, South, West and North winds |
| `Wh` `Gr` `Rd` | White, Green and Red dragons |

- Red fives have a red label.
- The tile you just drew sits slightly apart from the rest of your hand.
- The chosen tile is raised and outlined in gold. Tiles you may not discard right now are greyed out.
- A hint line says what you can do, and the timer beside it shows your thinking time.

**Table panel (top right)**

- Players by position: You, Right, Across and Left. A gold ▶ marks whose turn it is, a red **R** marks anyone in riichi, and a dot marks who discarded last.
- Tiles left in the wall, honba and riichi sticks.
- The dora indicator tiles.
- Your waits: the tiles that would complete your hand, shown when you are one tile away (tenpai).

**On the table**, the most recent discard glows gold until someone claims it or the next tile is thrown. Looking at a tile shows its full name and how many of it you have not seen yet.

## Building from source

You need JDK 21 or newer to run Gradle; the build downloads Java 25 for itself.

```sh
git clone https://github.com/Aischii/MahjongCraft.git
cd MahjongCraft
./gradlew build      # the mod jar is written to build/libs/
./gradlew test       # unit tests
./gradlew runClient  # start a development client
```

## Credits

- **[doublemoon1119](https://github.com/doublemoon1119)**: original author of MahjongCraft, including the Riichi game engine, bots, models and textures. Original project: [doublemoon1119/MahjongCraft](https://github.com/doublemoon1119/MahjongCraft).
- [mahjong4j](https://github.com/mahjong4j/mahjong4j), used for hand evaluation, and [LibGui](https://github.com/CottonMC/LibGui), used for the screens.
- Licensed under the MIT License; see [LICENSE](LICENSE).

## Support
Please report any issues to [the issue tracker](https://github.com/doublemoon1119/MahjongCraft/issues) on Github.

我是臺灣人，除了英文以外你也可用中文發表 [問題](https://github.com/doublemoon1119/MahjongCraft/issues) 或留言
