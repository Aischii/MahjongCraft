package doublemoon.mahjongcraft.game.progress

import kotlinx.serialization.Serializable

/** One finished game, from one player's point of view. */
@Serializable
data class GameRecord(
    /** When the game ended (epoch milliseconds). */
    val endedAt: Long,
    /** 1 = first place … 4 = last. */
    val placement: Int,
    val points: Int,
)

/** A player's mahjong record in one world (or on one server). */
@Serializable
data class PlayerStats(
    var name: String = "",
    var games: Int = 0,
    var firsts: Int = 0,
    var hands: Int = 0,
    var handsWon: Int = 0,
    var dealIns: Int = 0,
    var riichi: Int = 0,
    /** How many winning hands contained each yaku, by yaku key (lowercase enum name). */
    val yaku: MutableMap<String, Int> = mutableMapOf(),
    /** Newest first, at most [RECENT_GAMES]. */
    val recent: MutableList<GameRecord> = mutableListOf(),
) {
    fun recordHand() {
        hands++
    }

    fun recordWin(yakuKeys: Collection<String>) {
        handsWon++
        yakuKeys.forEach { yaku[it] = (yaku[it] ?: 0) + 1 }
    }

    fun recordDealIn() {
        dealIns++
    }

    fun recordRiichi() {
        riichi++
    }

    fun recordGame(record: GameRecord) {
        games++
        if (record.placement == 1) firsts++
        recent.add(0, record)
        while (recent.size > RECENT_GAMES) recent.removeAt(recent.lastIndex)
    }

    /** The [count] yaku won most often, most frequent first; ties keep alphabetical order. */
    fun topYaku(count: Int = 3): List<Pair<String, Int>> =
        yaku.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(count).map { it.key to it.value }

    /** Share of finished games placed first, 0..100 (0 when no games). */
    val firstRate: Int get() = if (games == 0) 0 else firsts * 100 / games

    /** Share of hands won, 0..100. */
    val winRate: Int get() = if (hands == 0) 0 else handsWon * 100 / hands

    /** Share of hands where this player dealt into someone's Ron, 0..100. */
    val dealInRate: Int get() = if (hands == 0) 0 else dealIns * 100 / hands

    companion object {
        const val RECENT_GAMES = 10
    }
}

/** Everyone's [PlayerStats], keyed by player UUID string. */
@Serializable
data class StatsBook(val players: MutableMap<String, PlayerStats> = mutableMapOf()) {
    /** The stats for [uuid], created if new; the stored name follows the player's current name. */
    fun of(uuid: String, name: String): PlayerStats =
        players.getOrPut(uuid) { PlayerStats() }.also { it.name = name }

    fun findByName(name: String): PlayerStats? = players.values.firstOrNull { it.name.equals(name, ignoreCase = true) }
}

/**
 * Stats keys for a winning hand's yaku, each once: yakuman replace the normal yaku (as on the
 * settlement screen), and dora / ura dora are not yaku so they are left out.
 */
fun statsYakuKeys(normal: List<String>, yakuman: List<String>, nagashiMangan: Boolean): List<String> = when {
    yakuman.isNotEmpty() -> yakuman.map { it.lowercase() }.distinct()
    nagashiMangan -> listOf("nagashi_mangan")
    else -> normal.map { it.lowercase() }.filterNot { "dora" in it }.distinct()
}
