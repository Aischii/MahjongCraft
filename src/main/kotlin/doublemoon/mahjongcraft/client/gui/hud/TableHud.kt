package doublemoon.mahjongcraft.client.gui.hud

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.client.game.ClientMahjongState
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

/**
 * Top-right panel while playing: whose turn it is (players by position relative to you),
 * who is in riichi, dora indicators, tiles left, honba and riichi sticks, and your waits.
 */
@Environment(EnvType.CLIENT)
object TableHud {
    private const val PAD = 4
    private const val LINE = 11
    private const val SMALL_W = 11
    private const val SMALL_H = 15
    private const val MARGIN = 4

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val GREY = 0xFFB0B0B0.toInt()
    private const val GOLD = 0xFFFFCC33.toInt()
    private const val RED = 0xFFFF5555.toInt()
    private const val PANEL = 0x90000000.toInt()

    private val POSITIONS = listOf("you", "right", "across", "left")

    fun extract(graphics: GuiGraphicsExtractor, @Suppress("UNUSED_PARAMETER") delta: DeltaTracker) {
        if (!ClientMahjongState.active) return
        val table = ClientMahjongState.table ?: return
        if (table.mySeat < 0) return
        val font = Minecraft.getInstance().font

        val seatLines = (0 until table.seatNames.size).map { offset ->
            val seat = (table.mySeat + offset) % table.seatNames.size
            seat to Component.translatable("$MOD_ID.hud.table.${POSITIONS[offset]}")
        }
        val counters = Component.translatable(
            "$MOD_ID.hud.table.counters", table.wallRemaining, table.honba, table.riichiSticks
        )
        val doraLabel = Component.translatable("$MOD_ID.hud.table.dora")
        val waitsLabel = Component.translatable("$MOD_ID.hud.table.waits")

        val tilesWidth = { tiles: List<MahjongTile> -> tiles.size * (SMALL_W + 1) }
        val width = listOf(
            seatLines.maxOf { font.width(it.second) } + 22,
            font.width(counters),
            font.width(doraLabel) + 4 + tilesWidth(table.doraIndicators),
            if (table.waits.isEmpty()) 0 else font.width(waitsLabel) + 4 + tilesWidth(table.waits),
        ).max() + PAD * 2
        val height = PAD * 2 + seatLines.size * LINE + 2 + LINE + (SMALL_H + 2) * (if (table.waits.isEmpty()) 1 else 2)

        val x0 = graphics.guiWidth() - width - MARGIN
        var y = MARGIN
        graphics.fill(x0, y, x0 + width, y + height, PANEL)
        y += PAD

        seatLines.forEach { (seat, label) ->
            val isTurn = seat == table.turnSeat
            if (isTurn) graphics.text(font, "▶", x0 + PAD, y, GOLD, true)
            graphics.text(font, label, x0 + PAD + 9, y, if (isTurn) GOLD else WHITE, true)
            if (table.riichiSeats.getOrElse(seat) { false }) {
                graphics.text(font, "R", x0 + width - PAD - 6, y, RED, true)
            }
            if (seat == table.lastDiscardSeat) {
                graphics.text(font, "•", x0 + width - PAD - 14, y, GOLD, true)
            }
            y += LINE
        }
        y += 2
        graphics.text(font, counters, x0 + PAD, y, GREY, true)
        y += LINE
        y = tileRow(graphics, doraLabel, table.doraIndicators, x0 + PAD, y)
        if (table.waits.isNotEmpty()) tileRow(graphics, waitsLabel, table.waits, x0 + PAD, y)
    }

    private fun tileRow(graphics: GuiGraphicsExtractor, label: Component, tiles: List<MahjongTile>, x: Int, y: Int): Int {
        val font = Minecraft.getInstance().font
        graphics.text(font, label, x, y + (SMALL_H - 8) / 2, WHITE, true)
        var tx = x + font.width(label) + 4
        tiles.forEach {
            ScreenDrawing.texturedRect(graphics, tx, y, SMALL_W, SMALL_H, it.surfaceIdentifier, WHITE)
            tx += SMALL_W + 1
        }
        return y + SMALL_H + 2
    }
}
