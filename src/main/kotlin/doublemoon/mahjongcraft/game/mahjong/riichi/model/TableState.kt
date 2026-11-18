package doublemoon.mahjongcraft.game.mahjong.riichi.model

import kotlinx.serialization.Serializable

/**
 * What one player's HUD shows about the table, sent whenever it changes.
 *
 * Seats are indexed like `MahjongGame.seat`; [mySeat] is the receiving player's seat.
 */
@Serializable
data class TableState(
    val mySeat: Int = -1,
    /** Seat whose turn it is, or -1 between turns. */
    val turnSeat: Int = -1,
    val seatNames: List<String> = emptyList(),
    val riichiSeats: List<Boolean> = emptyList(),
    /** Face-up dora indicators, oldest first. */
    val doraIndicators: List<MahjongTile> = emptyList(),
    val riichiSticks: Int = 0,
    val honba: Int = 0,
    val wallRemaining: Int = 0,
    /** Entity id of the most recent discard still in a pond, so the client can highlight it; -1 if none. */
    val lastDiscardEntityId: Int = -1,
    val lastDiscardSeat: Int = -1,
    /** Tiles that complete the receiving player's hand right now (empty when not tenpai). */
    val waits: List<MahjongTile> = emptyList(),
)
