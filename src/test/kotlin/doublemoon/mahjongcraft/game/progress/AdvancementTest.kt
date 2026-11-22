package doublemoon.mahjongcraft.game.progress

import doublemoon.mahjongcraft.game.mahjong.riichi.model.DoubleYakuman
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongGameBehavior
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import doublemoon.mahjongcraft.game.mahjong.riichi.model.YakuSettlement
import org.mahjong4j.yaku.yakuman.Yakuman
import kotlin.test.Test
import kotlin.test.assertEquals

class AdvancementTest {
    private val win = YakuSettlement(
        displayName = "East", uuid = "east", isRealPlayer = true, yakuList = emptyList(),
        yakumanList = emptyList(), doubleYakumanList = emptyList(), riichi = false,
        winningTile = MahjongTile.UNKNOWN, hands = emptyList(), fuuroList = emptyList(),
        doraIndicators = emptyList(), uraDoraIndicators = emptyList(), fu = 30, han = 1, score = 1000,
    )

    @Test
    fun multiRonKeepsEachHumanWinnersYakuSeparate() {
        val east = win.copy(yakumanList = listOf(Yakuman.DAISANGEN))
        val south = win.copy(uuid = "south", doubleYakumanList = listOf(DoubleYakuman.SUANKO_TANKI))
        val bot = win.copy(uuid = "bot", isRealPlayer = false)
        val awards = MahjongAdvancements.winningAwards(listOf(east, bot, south), MahjongGameBehavior.RON)
        assertEquals(mapOf(
            "east" to setOf("win_a_hand", "win_by_ron", "win_a_yakuman", "big_three_dragons"),
            "south" to setOf("win_a_hand", "win_by_ron", "win_a_yakuman", "four_concealed_triplets"),
        ), awards)
    }

    @Test
    fun selfDrawAndNagashiUseDifferentAwards() {
        assertEquals(setOf("win_a_hand", "win_by_tsumo"),
            MahjongAdvancements.winningAwards(listOf(win), MahjongGameBehavior.TSUMO)["east"])
        assertEquals(setOf("win_a_hand"),
            MahjongAdvancements.winningAwards(listOf(win.copy(nagashiMangan = true)), null)["east"])
    }

    @Test
    fun namedChallengesIncludeDoubleVariants() {
        for (name in listOf("KOKUSHIMUSO", "KOKUSHIMUSO_JUSANMENMACHI")) {
            assertEquals(setOf("thirteen_orphans"), MahjongAdvancements.rareAwards(listOf(name)))
        }
        assertEquals(setOf("four_concealed_triplets"), MahjongAdvancements.rareAwards(listOf("SUANKO")))
        assertEquals(emptySet(), MahjongAdvancements.rareAwards(listOf("TSUISO")))
    }

    @Test
    fun onlySanmaWinnerGetsBothMatchAwards() {
        assertEquals(emptyList(), MahjongAdvancements.matchAwards(false, 0))
        assertEquals(listOf("finish_sanma"), MahjongAdvancements.matchAwards(true, 1))
        assertEquals(listOf("finish_sanma", "win_sanma"), MahjongAdvancements.matchAwards(true, 0))
    }
}
