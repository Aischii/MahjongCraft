package doublemoon.mahjongcraft.game.mahjong.riichi.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SanmaTest {

    @Test
    fun `sanma wall drops 2m to 8m, leaving 108 tiles`() {
        val wall = MahjongTile.sanmaWall(MahjongTile.normalWall)
        assertEquals(108, wall.size)
        assertEquals(4, wall.count { it == MahjongTile.M1 })
        assertEquals(4, wall.count { it == MahjongTile.M9 })
        assertTrue(wall.none { it == MahjongTile.M5 || it == MahjongTile.M2 || it == MahjongTile.M8 })
        assertEquals(4, wall.count { it == MahjongTile.NORTH })
    }

    @Test
    fun `sanma keeps only the red 5p and 5s`() {
        val wall = MahjongTile.sanmaWall(MahjongTile.redFive3Wall)
        assertEquals(108, wall.size)
        assertFalse(MahjongTile.M5_RED in wall)
        assertTrue(MahjongTile.P5_RED in wall && MahjongTile.S5_RED in wall)
    }

    @Test
    fun `rounds rotate through three dealers per wind`() {
        val round = MahjongRound()
        round.nextRound(3); round.nextRound(3)
        assertEquals(Wind.EAST to 2, round.wind to round.round)
        round.nextRound(3)
        assertEquals(Wind.SOUTH to 0, round.wind to round.round)
    }

    @Test
    fun `game lengths count one hand per seat per wind`() {
        val rule = MahjongRule(players = MahjongRule.PlayerCount.THREE)
        assertEquals(6, rule.totalRounds) // two winds
        assertEquals(Wind.WEST to 2, rule.finalRound)
        assertEquals(8, MahjongRule().totalRounds)
        assertEquals(Wind.WEST to 3, MahjongRule().finalRound)
        assertEquals(1, MahjongRule(length = MahjongRule.GameLength.ONE_GAME, players = MahjongRule.PlayerCount.THREE).totalRounds)
    }

    @Test
    fun `player counts carry their default points`() {
        assertEquals(35000, MahjongRule.PlayerCount.THREE.startingPoints)
        assertEquals(40000, MahjongRule.PlayerCount.THREE.minPointsToWin)
        assertEquals(3, MahjongRule(players = MahjongRule.PlayerCount.THREE).seats)
        assertTrue(MahjongRule(players = MahjongRule.PlayerCount.THREE).isSanma)
    }

    @Test
    fun `kita is its own action with the North tile`() {
        val options = ActionOptions.build(MahjongGameBehavior.KITA, listOf(MahjongTile.NORTH), ClaimTarget.SELF, "")
        assertEquals(1, options.size)
        assertEquals(ActionGroup.KITA, options[0].group)
        assertEquals(MahjongGameBehavior.KITA to "", options[0].response())
    }

    @Test
    fun `old saved rules without a player count still load as 4-player`() {
        val rule = MahjongRule.fromJsonString("""{"length":"EAST","startingPoints":25000}""")
        assertEquals(MahjongRule.PlayerCount.FOUR, rule.players)
    }
}
