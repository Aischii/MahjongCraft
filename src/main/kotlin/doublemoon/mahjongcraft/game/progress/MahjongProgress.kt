package doublemoon.mahjongcraft.game.progress

import doublemoon.mahjongcraft.game.mahjong.riichi.model.YakuSettlement
import doublemoon.mahjongcraft.game.mahjong.riichi.player.MahjongPlayer
import doublemoon.mahjongcraft.game.mahjong.riichi.player.MahjongPlayerBase

/**
 * What a game reports when something worth remembering happens. Each event updates the
 * advancements and the world's stats; only real players are recorded, never bots.
 */
object MahjongProgress {

    private fun stats(player: MahjongPlayer, block: PlayerStats.() -> Unit) {
        val server = player.entity.level().server
        MahjongStatsData.update(server) { of(player.uuid, player.entity.name.string).block() }
    }

    fun onRiichi(player: MahjongPlayer) {
        MahjongAdvancements.onRiichi(player)
        stats(player) { recordRiichi() }
    }

    fun onHandEnd(players: List<MahjongPlayer>) {
        MahjongAdvancements.onHandEnd(players)
        players.forEach { stats(it) { recordHand() } }
    }

    fun onWins(players: List<MahjongPlayer>, settlements: List<YakuSettlement>) {
        MahjongAdvancements.onWins(players, settlements)
        settlements.forEach { settlement ->
            val winner = players.find { it.uuid == settlement.uuid } ?: return@forEach
            stats(winner) { recordWin(yakuKeys(settlement)) }
        }
    }

    /** [player] discarded the tile someone won on by Ron. */
    fun onDealIn(player: MahjongPlayer) = stats(player) { recordDealIn() }

    /** The game reached its end; [ranking] is every seat, first place first. */
    fun onGameFinished(ranking: List<MahjongPlayerBase>) {
        MahjongAdvancements.onGameWon(ranking.firstOrNull() as? MahjongPlayer)
        val now = System.currentTimeMillis()
        ranking.forEachIndexed { index, player ->
            if (player is MahjongPlayer) {
                stats(player) { recordGame(GameRecord(endedAt = now, placement = index + 1, points = player.points)) }
            }
        }
    }

    /**
     * The yaku of a winning hand as stats keys (the `mahjongcraft.game.yaku.<key>` names), each once.
     * Like the settlement screen: yakuman replace the normal yaku, and dora are not counted as yaku.
     */
    fun yakuKeys(settlement: YakuSettlement): List<String> = statsYakuKeys(
        normal = settlement.yakuList.map { it.name },
        yakuman = settlement.yakumanList.map { it.name } + settlement.doubleYakumanList.map { it.name },
        nagashiMangan = settlement.nagashiMangan,
    )

}
