package doublemoon.mahjongcraft.game.progress

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerStatsTest {

    @Test
    fun `games count firsts and keep the ten newest, newest first`() {
        val stats = PlayerStats()
        repeat(12) { i -> stats.recordGame(GameRecord(endedAt = i.toLong(), placement = if (i % 3 == 0) 1 else 2, points = i)) }
        assertEquals(12, stats.games)
        assertEquals(4, stats.firsts)
        assertEquals(33, stats.firstRate)
        assertEquals(10, stats.recent.size)
        assertEquals(11L, stats.recent.first().endedAt)
        assertEquals(2L, stats.recent.last().endedAt)
    }

    @Test
    fun `hand rates are shares of hands played`() {
        val stats = PlayerStats()
        repeat(8) { stats.recordHand() }
        stats.recordWin(listOf("reach"))
        stats.recordWin(listOf("tanyao"))
        stats.recordDealIn()
        assertEquals(25, stats.winRate)
        assertEquals(12, stats.dealInRate)
        assertEquals(0, PlayerStats().winRate)
    }

    @Test
    fun `top yaku are the most won, ties alphabetical`() {
        val stats = PlayerStats()
        stats.recordWin(listOf("reach", "tanyao", "pinfu"))
        stats.recordWin(listOf("reach", "tanyao"))
        stats.recordWin(listOf("reach", "chiitoitsu"))
        assertEquals(listOf("reach" to 3, "tanyao" to 2, "chiitoitsu" to 1), stats.topYaku())
    }

    @Test
    fun `yaku keys skip dora, count once, and let yakuman replace normal yaku`() {
        assertEquals(
            listOf("reach", "tsumo"),
            statsYakuKeys(listOf("REACH", "DORA", "DORA", "TSUMO", "URADORA"), emptyList(), nagashiMangan = false),
        )
        assertEquals(listOf("kokushimuso"), statsYakuKeys(listOf("REACH"), listOf("KOKUSHIMUSO"), nagashiMangan = false))
        assertEquals(listOf("nagashi_mangan"), statsYakuKeys(emptyList(), emptyList(), nagashiMangan = true))
    }

    @Test
    fun `book finds players by name and keeps their latest name`() {
        val book = StatsBook()
        book.of("uuid-1", "Alice").recordHand()
        book.of("uuid-1", "Alicia")
        assertEquals("Alicia", book.findByName("alicia")?.name)
        assertNull(book.findByName("Alice"))
    }

    @Test
    fun `book survives a JSON round trip`() {
        val book = StatsBook()
        book.of("uuid-1", "Alice").apply {
            recordHand(); recordWin(listOf("reach")); recordGame(GameRecord(5L, 1, 32000))
        }
        val json = Json { encodeDefaults = true }
        val copy = json.decodeFromString<StatsBook>(json.encodeToString(StatsBook.serializer(), book))
        assertEquals(book, copy)
    }
}
