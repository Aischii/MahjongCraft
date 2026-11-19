package doublemoon.mahjongcraft.game.mahjong.riichi.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * One thing a player can answer to a request from the game, such as one specific chii pair,
 * one specific kan, or one tile to discard for riichi.
 *
 * [tiles] are the tiles to show for this option; for calls the claimed tile is last
 * (for kakan the added tile is last).
 */
data class ActionOption(
    val behavior: MahjongGameBehavior,
    val target: ClaimTarget,
    val tiles: List<MahjongTile>,
) {
    /** The action group a player picks with one key: all chii pairs are one group, and so on. */
    val group: ActionGroup
        get() = when (behavior) {
            MahjongGameBehavior.CHII -> ActionGroup.CHII
            MahjongGameBehavior.PON -> ActionGroup.PON
            MahjongGameBehavior.MINKAN, MahjongGameBehavior.ANKAN, MahjongGameBehavior.KAKAN -> ActionGroup.KAN
            MahjongGameBehavior.RIICHI -> ActionGroup.RIICHI
            MahjongGameBehavior.TSUMO, MahjongGameBehavior.RON -> ActionGroup.WIN
            MahjongGameBehavior.KYUUSHU_KYUUHAI -> ActionGroup.DRAW
            MahjongGameBehavior.KITA -> ActionGroup.KITA
            else -> ActionGroup.SKIP
        }

    /** The behavior and extra data to send to the server when this option is chosen. */
    fun response(): Pair<MahjongGameBehavior, String> = when (behavior) {
        MahjongGameBehavior.CHII -> behavior to Json.encodeToString(tiles[0] to tiles[1])
        MahjongGameBehavior.KAKAN, MahjongGameBehavior.ANKAN ->
            MahjongGameBehavior.ANKAN_OR_KAKAN to Json.encodeToString(tiles.last())
        MahjongGameBehavior.RIICHI -> behavior to Json.encodeToString(tiles.first())
        else -> behavior to ""
    }
}

/** The actions a player chooses between, each with its own key. */
enum class ActionGroup { CHII, PON, KAN, KITA, RIICHI, WIN, DRAW, SKIP }

/** What the server sends with a discard request. */
@Serializable
data class DiscardRequest(
    val skippable: Boolean,
    val cannotDiscard: List<MahjongTile> = emptyList(),
    /** In riichi: the server discards the drawn tile itself; the client only shows it. */
    val auto: Boolean = false,
)

object ActionOptions {

    /**
     * Builds every option the player can answer to [behavior] (sent with [hands], [target] and the
     * request's extra [data]). Returns an empty list for requests that have no options.
     */
    fun build(
        behavior: MahjongGameBehavior,
        hands: List<MahjongTile>,
        target: ClaimTarget,
        data: String,
    ): List<ActionOption> = buildList {
        when (behavior) {
            MahjongGameBehavior.CHII -> {
                val dataList = Json.decodeFromString<List<String>>(data)
                val claimingTile = Json.decodeFromString<MahjongTile>(dataList[0])
                Json.decodeFromString<List<Pair<MahjongTile, MahjongTile>>>(dataList[1]).forEach {
                    this += ActionOption(behavior, target, listOf(it.first, it.second, claimingTile))
                }
            }
            MahjongGameBehavior.PON_OR_CHII -> {
                val dataList = Json.decodeFromString<List<String>>(data)
                val claimingTile = Json.decodeFromString<MahjongTile>(dataList[0])
                val tilePairsForChii = Json.decodeFromString<List<Pair<MahjongTile, MahjongTile>>>(dataList[1])
                val tilePairForPon = Json.decodeFromString<Pair<MahjongTile, MahjongTile>>(dataList[2])
                this += ActionOption(
                    MahjongGameBehavior.PON, target,
                    listOf(tilePairForPon.first, tilePairForPon.second, claimingTile),
                )
                tilePairsForChii.forEach {
                    this += ActionOption(MahjongGameBehavior.CHII, target, listOf(it.first, it.second, claimingTile))
                }
            }
            MahjongGameBehavior.PON -> {
                val dataList = Json.decodeFromString<List<String>>(data)
                val claimingTile = Json.decodeFromString<MahjongTile>(dataList[0])
                val pair = Json.decodeFromString<Pair<MahjongTile, MahjongTile>>(dataList[1])
                this += ActionOption(MahjongGameBehavior.PON, target, listOf(pair.first, pair.second, claimingTile))
            }
            MahjongGameBehavior.ANKAN_OR_KAKAN -> {
                val dataList = Json.decodeFromString<List<String>>(data)
                val canAnkanTiles = Json.decodeFromString<Set<MahjongTile>>(dataList[0])
                val canKakanTiles = Json.decodeFromString<Set<Pair<MahjongTile, ClaimTarget>>>(dataList[1])
                val redFiveQuantity = MahjongRule.fromJsonString(dataList[2]).redFive.quantity
                canAnkanTiles.distinctBy { it.mahjong4jTile.code }  // 避免赤牌重複出現槓的選項
                    .forEach { tile ->
                        val redFiveTile = redFiveOf(tile)
                        if (redFiveQuantity == 0 || redFiveTile == null) {
                            this += ActionOption(MahjongGameBehavior.ANKAN, target, List(4) { tile })
                        } else {
                            val notRedFiveTile = MahjongTile.entries[redFiveTile.mahjong4jTile.code]
                            val tiles = List(4) { if (it < redFiveQuantity) redFiveTile else notRedFiveTile }
                            this += ActionOption(MahjongGameBehavior.ANKAN, target, tiles)
                        }
                    }
                canKakanTiles.forEach { (tile, oTarget) ->
                    val redFiveTile = redFiveOf(tile)
                    if (redFiveQuantity == 0 || redFiveTile == null) {
                        this += ActionOption(MahjongGameBehavior.KAKAN, oTarget, List(4) { tile })
                    } else {
                        val redFiveAmount = redFiveQuantity - if (tile.isRed) 1 else 0
                        val notRedFiveTile = MahjongTile.entries[redFiveTile.mahjong4jTile.code]
                        val tiles = List(4) { index ->
                            if (index < 3) {
                                if (index < redFiveAmount) redFiveTile else notRedFiveTile  // 赤牌擺在第一位
                            } else {
                                tile //最後一張是加槓牌
                            }
                        }
                        this += ActionOption(MahjongGameBehavior.KAKAN, oTarget, tiles)
                    }
                }
            }
            MahjongGameBehavior.MINKAN -> { //能出現明槓一定是同時能槓跟碰
                val dataList = Json.decodeFromString<List<String>>(data)
                val claimingTile = Json.decodeFromString<MahjongTile>(dataList[0])
                val redFiveQuantity = MahjongRule.fromJsonString(dataList[1]).redFive.quantity
                val redFiveTile = redFiveOf(claimingTile)
                if (redFiveQuantity == 0 || redFiveTile == null) {
                    this += ActionOption(MahjongGameBehavior.MINKAN, target, List(4) { claimingTile })
                    this += ActionOption(MahjongGameBehavior.PON, target, List(3) { claimingTile })
                } else {
                    val redFiveAmount = redFiveQuantity - if (claimingTile.isRed) 1 else 0
                    val notRedFiveTile = MahjongTile.entries[redFiveTile.mahjong4jTile.code]
                    val tilesForPon = MutableList(2) { if (it < redFiveAmount) redFiveTile else notRedFiveTile }
                    val tilesForKan = tilesForPon.toMutableList()
                    tilesForKan += notRedFiveTile
                    tilesForPon += claimingTile
                    tilesForKan += claimingTile
                    this += ActionOption(MahjongGameBehavior.MINKAN, target, tilesForKan)
                    this += ActionOption(MahjongGameBehavior.PON, target, tilesForPon)
                }
            }
            MahjongGameBehavior.RIICHI -> {
                Json.decodeFromString<List<Pair<MahjongTile, List<MahjongTile>>>>(data).forEach { (tile, _) ->
                    this += ActionOption(MahjongGameBehavior.RIICHI, target, listOf(tile))
                }
            }
            MahjongGameBehavior.KYUUSHU_KYUUHAI -> {
                this += ActionOption(behavior, target, hands.filter { it.mahjong4jTile.isYaochu })
            }
            MahjongGameBehavior.KITA -> {
                this += ActionOption(behavior, target, listOf(MahjongTile.NORTH))
            }
            MahjongGameBehavior.RON -> {
                this += ActionOption(behavior, target, listOf(Json.decodeFromString<MahjongTile>(data)))
            }
            MahjongGameBehavior.TSUMO -> {
                this += ActionOption(behavior, target, listOfNotNull(hands.lastOrNull()))
            }
            else -> {}
        }
    }

    /** The red five of [tile]'s suit, or null if [tile] is not a five. */
    fun redFiveOf(tile: MahjongTile): MahjongTile? = when (tile) {
        MahjongTile.M5, MahjongTile.M5_RED -> MahjongTile.M5_RED
        MahjongTile.S5, MahjongTile.S5_RED -> MahjongTile.S5_RED
        MahjongTile.P5, MahjongTile.P5_RED -> MahjongTile.P5_RED
        else -> null
    }
}
