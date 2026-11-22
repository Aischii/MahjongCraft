package doublemoon.mahjongcraft.game.progress

import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongGameBehavior
import doublemoon.mahjongcraft.game.mahjong.riichi.model.YakuSettlement
import doublemoon.mahjongcraft.game.mahjong.riichi.player.MahjongPlayer
import doublemoon.mahjongcraft.id
import net.minecraft.server.level.ServerPlayer

/**
 * Grants the mod's advancements from game events. Each advancement has a single `minecraft:impossible`
 * criterion named [CRITERION], so only this code can complete it.
 */
object MahjongAdvancements {
    private const val CRITERION = "done"

    fun award(player: ServerPlayer, name: String) {
        val holder = player.level().server.advancements.get(id("main/$name")) ?: return
        player.advancements.award(holder, CRITERION)
    }

    fun onRiichi(player: MahjongPlayer) = award(player.entity, "declare_riichi")

    /** A hand finished (win or draw) with these players seated. */
    fun onHandEnd(players: List<MahjongPlayer>) = players.forEach { award(it.entity, "play_a_hand") }

    /** Hands were won; [settlements] say who won and with which yaku. */
    fun onWins(players: List<MahjongPlayer>, settlements: List<YakuSettlement>, winMethod: MahjongGameBehavior?) {
        val awards = winningAwards(settlements, winMethod)
        for (player in players) {
            awards[player.uuid]?.forEach { award(player.entity, it) }
        }
    }

    internal fun winningAwards(settlements: List<YakuSettlement>, winMethod: MahjongGameBehavior?) =
        settlements.filter { it.isRealPlayer }.associate { settlement ->
            settlement.uuid to buildSet {
                add("win_a_hand")
                when (winMethod) {
                    MahjongGameBehavior.RON -> add("win_by_ron")
                    MahjongGameBehavior.TSUMO -> add("win_by_tsumo")
                    else -> Unit
                }
                val yakuman = settlement.yakumanList.map { it.name } + settlement.doubleYakumanList.map { it.name }
                if (yakuman.isNotEmpty()) add("win_a_yakuman")
                addAll(rareAwards(yakuman))
            }
        }

    internal fun rareAwards(yaku: List<String>): Set<String> = yaku.mapNotNull {
        when (it) {
            "KOKUSHIMUSO", "KOKUSHIMUSO_JUSANMENMACHI" -> "thirteen_orphans"
            "SUANKO", "SUANKO_TANKI" -> "four_concealed_triplets"
            "DAISANGEN" -> "big_three_dragons"
            else -> null
        }
    }.toSet()

    internal fun matchAwards(sanma: Boolean, rankIndex: Int): List<String> = when {
        !sanma -> emptyList()
        rankIndex == 0 -> listOf("finish_sanma", "win_sanma")
        else -> listOf("finish_sanma")
    }

    /** A game ran to its end; [winner] finished first, or is null when a bot did. */
    fun onGameWon(winner: MahjongPlayer?) {
        winner?.let { award(it.entity, "win_a_game") }
    }
}
