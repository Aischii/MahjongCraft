package doublemoon.mahjongcraft.game.progress

import doublemoon.mahjongcraft.MOD_ID
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/** The lines that describe a player's [PlayerStats]; shared by `/mahjong stats` and the Guide. */
object StatsText {
    private fun t(key: String, vararg args: Any): MutableComponent = Component.translatable("$MOD_ID.stats.$key", *args)

    private fun ordinal(placement: Int): MutableComponent = t("place.$placement")

    fun lines(stats: PlayerStats): List<Component> = buildList {
        if (stats.games == 0 && stats.hands == 0) {
            add(t("none").withStyle(ChatFormatting.GRAY))
            return@buildList
        }
        add(t("games", stats.games, stats.firsts, stats.firstRate))
        add(t("hands", stats.hands, stats.handsWon, stats.winRate, stats.dealIns, stats.dealInRate))
        add(t("riichi", stats.riichi))
        val top = stats.topYaku()
        if (top.isNotEmpty()) {
            val line = t("top_yaku")
            top.forEachIndexed { i, (key, count) ->
                if (i > 0) line.append(", ")
                line.append(Component.translatable("$MOD_ID.game.yaku.$key").withStyle(ChatFormatting.AQUA))
                    .append(" ×$count")
            }
            add(line)
        }
        if (stats.recent.isNotEmpty()) {
            add(t("recent").withStyle(ChatFormatting.GOLD))
            stats.recent.forEach { game ->
                val color = if (game.placement == 1) ChatFormatting.GREEN else ChatFormatting.WHITE
                add(
                    Component.literal(" - ").append(ordinal(game.placement).withStyle(color))
                        .append("  ").append(t("points", "%,d".format(game.points)).withStyle(ChatFormatting.GRAY))
                )
            }
        }
    }
}
