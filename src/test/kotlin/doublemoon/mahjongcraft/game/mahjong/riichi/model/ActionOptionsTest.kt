package doublemoon.mahjongcraft.game.mahjong.riichi.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ActionOptionsTest {
    private val hand = listOf(MahjongTile.M2, MahjongTile.M3, MahjongTile.M4, MahjongTile.P5, MahjongTile.EAST)

    @Test
    fun `short labels name suits, winds and dragons`() {
        assertEquals("1m", MahjongTile.M1.shortLabel)
        assertEquals("9p", MahjongTile.P9.shortLabel)
        assertEquals("7s", MahjongTile.S7.shortLabel)
        assertEquals("5s", MahjongTile.S5_RED.shortLabel)
        assertEquals("E", MahjongTile.EAST.shortLabel)
        assertEquals("N", MahjongTile.NORTH.shortLabel)
        assertEquals("Wh", MahjongTile.WHITE_DRAGON.shortLabel)
        assertEquals("Rd", MahjongTile.RED_DRAGON.shortLabel)
    }

    @Test
    fun `each chii pair is its own option and answers with that pair`() {
        val data = Json.encodeToString(
            listOf(
                Json.encodeToString(MahjongTile.M3),
                Json.encodeToString(listOf(MahjongTile.M2 to MahjongTile.M4, MahjongTile.M4 to MahjongTile.M5)),
            )
        )
        val options = ActionOptions.build(MahjongGameBehavior.CHII, hand, ClaimTarget.LEFT, data)

        assertEquals(2, options.size)
        assertEquals(listOf(ActionGroup.CHII), options.map { it.group }.distinct())
        assertEquals(listOf(MahjongTile.M2, MahjongTile.M4, MahjongTile.M3), options[0].tiles)
        assertEquals(
            MahjongGameBehavior.CHII to Json.encodeToString(MahjongTile.M4 to MahjongTile.M5),
            options[1].response(),
        )
    }

    @Test
    fun `pon or chii offers both groups`() {
        val data = Json.encodeToString(
            listOf(
                Json.encodeToString(MahjongTile.M3),
                Json.encodeToString(listOf(MahjongTile.M2 to MahjongTile.M4)),
                Json.encodeToString(MahjongTile.M3 to MahjongTile.M3),
            )
        )
        val options = ActionOptions.build(MahjongGameBehavior.PON_OR_CHII, hand, ClaimTarget.LEFT, data)
        assertEquals(listOf(ActionGroup.PON, ActionGroup.CHII), options.map { it.group })
        assertEquals(MahjongGameBehavior.PON to "", options[0].response())
    }

    @Test
    fun `riichi offers one option per discardable tile and answers with the tile`() {
        val data = Json.encodeToString(
            listOf(MahjongTile.EAST to listOf(MahjongTile.M1), MahjongTile.P5 to listOf(MahjongTile.M5))
        )
        val options = ActionOptions.build(MahjongGameBehavior.RIICHI, hand, ClaimTarget.SELF, data)
        assertEquals(listOf(MahjongTile.EAST, MahjongTile.P5), options.map { it.tiles.single() })
        assertEquals(MahjongGameBehavior.RIICHI to Json.encodeToString(MahjongTile.P5), options[1].response())
    }

    @Test
    fun `ron and tsumo are win options with an empty answer`() {
        val ron = ActionOptions.build(MahjongGameBehavior.RON, hand, ClaimTarget.RIGHT, Json.encodeToString(MahjongTile.M1))
        val tsumo = ActionOptions.build(MahjongGameBehavior.TSUMO, hand, ClaimTarget.SELF, "")
        assertEquals(ActionGroup.WIN, ron.single().group)
        assertEquals(listOf(MahjongTile.EAST), tsumo.single().tiles)
        assertEquals(MahjongGameBehavior.TSUMO to "", tsumo.single().response())
    }

    @Test
    fun `requests without choices build no options`() {
        assertEquals(emptyList(), ActionOptions.build(MahjongGameBehavior.DISCARD, hand, ClaimTarget.SELF, "{}"))
    }
}
