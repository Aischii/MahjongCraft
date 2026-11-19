package doublemoon.mahjongcraft.game.progress

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

    private fun award(player: ServerPlayer, name: String) {
        val holder = player.level().server.advancements.get(id("main/$name")) ?: return
        player.advancements.award(holder, CRITERION)
    }

    fun onRiichi(player: MahjongPlayer) = award(player.entity, "declare_riichi")

    /** A hand finished (win or draw) with these players seated. */
    fun onHandEnd(players: List<MahjongPlayer>) = players.forEach { award(it.entity, "play_a_hand") }

    /** Hands were won; [settlements] say who won and with which yaku. */
    fun onWins(players: List<MahjongPlayer>, settlements: List<YakuSettlement>) {
        settlements.forEach { settlement ->
            val winner = players.find { it.uuid == settlement.uuid } ?: return@forEach
            award(winner.entity, "win_a_hand")
            if (settlement.yakumanList.isNotEmpty() || settlement.doubleYakumanList.isNotEmpty()) {
                award(winner.entity, "win_a_yakuman")
            }
        }
    }

    /** A game ran to its end; [winner] finished first, or is null when a bot did. */
    fun onGameWon(winner: MahjongPlayer?) {
        winner?.let { award(it.entity, "win_a_game") }
    }
}
