package doublemoon.mahjongcraft.client.gui.hud

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.client.game.ClientMahjongState
import doublemoon.mahjongcraft.client.game.MahjongKeys
import doublemoon.mahjongcraft.game.mahjong.riichi.model.ActionGroup
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/**
 * The player's hand along the bottom of the screen while seated at a game, with the actions they
 * can take and a one-line hint. Styled after vanilla: translucent dark panel, vanilla buttons and font.
 */
@Environment(EnvType.CLIENT)
object HandHud {
    private const val TILE_W = 16
    private const val TILE_H = 21
    private const val GAP = 2
    private const val DRAWN_GAP = 6
    private const val LABEL_H = 10
    private const val PAD = 4
    private const val HOTBAR_H = 24
    private const val BUTTON_H = 16
    private const val RAISE = 4

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val GREY = 0xFFA0A0A0.toInt()
    private const val RED = 0xFFFF5555.toInt()
    private const val GOLD = 0xFFFFCC33.toInt()
    private const val PANEL = 0x90000000.toInt()
    private const val DIMMED = 0xFF606060.toInt()

    /**
     * Top edge (GUI pixels) of everything the hand HUD drew this frame, hint line included, or null
     * when it isn't showing. Other HUD parts use it to stay clear of the hand.
     */
    var topEdge: Int? = null
        private set

    /** How far each hand slot is raised right now; eases toward the target so selection slides. */
    private val raise = FloatArray(20)

    private val BUTTON = Identifier.withDefaultNamespace("widget/button")
    private val BUTTON_ACTIVE = Identifier.withDefaultNamespace("widget/button_highlighted")

    fun extract(graphics: GuiGraphicsExtractor, @Suppress("UNUSED_PARAMETER") delta: DeltaTracker) {
        val state = ClientMahjongState
        val client = Minecraft.getInstance()
        if (!state.active) {
            topEdge = null
            return
        }
        val font = client.font
        val hand = state.hand

        val handWidth = hand.size * (TILE_W + GAP) - GAP + if (state.hasDrawnTile) DRAWN_GAP else 0
        val panelW = handWidth + PAD * 2
        val panelH = TILE_H + LABEL_H + PAD * 2 + RAISE
        val panelX = (graphics.guiWidth() - panelW) / 2
        val panelY = graphics.guiHeight() - HOTBAR_H - panelH - 2
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL)

        var x = panelX + PAD
        hand.forEachIndexed { index, tile ->
            if (state.hasDrawnTile && index == hand.lastIndex) x += DRAWN_GAP
            val chosen = index == state.selected && state.canChooseTile()
            if (index < raise.size) raise[index] += ((if (chosen) RAISE.toFloat() else 0f) - raise[index]) * 0.35f
            val y = panelY + PAD + RAISE - (raise.getOrElse(index) { 0f } + 0.5f).toInt()
            val tint = if (state.isSelectable(index) || !state.canChooseTile()) WHITE else DIMMED
            ScreenDrawing.texturedRect(graphics, x, y, TILE_W, TILE_H, tile.surfaceIdentifier, tint)
            if (chosen) graphics.outline(x - 1, y - 1, TILE_W + 2, TILE_H + 2, GOLD)
            val labelColor = if (tile.isRed) RED else if (tint == WHITE) WHITE else GREY
            graphics.centeredText(font, tile.shortLabel, x + TILE_W / 2, panelY + PAD + RAISE + TILE_H + 2, labelColor)
            x += TILE_W + GAP
        }

        var lineY = panelY - 2
        lineY = drawActions(graphics, font, lineY)
        drawHint(graphics, font, lineY, panelX, panelW)
        topEdge = lineY - 12
    }

    private fun ClientMahjongState.canChooseTile(): Boolean =
        discardRequest != null || activeGroup == ActionGroup.RIICHI

    /** Draws one vanilla button per available action, right above the hand. Returns the new top. */
    private fun drawActions(graphics: GuiGraphicsExtractor, font: Font, bottom: Int): Int {
        val state = ClientMahjongState
        val groups = state.availableGroups
        if (groups.isEmpty()) return bottom
        val labels = (groups + ActionGroup.SKIP).map { group ->
            group to Component.literal("[")
                .append(MahjongKeys.keyFor(group).translatedKeyMessage)
                .append("] ")
                .append(Component.translatable("$MOD_ID.hud.action.${group.name.lowercase()}"))
        }
        val widths = labels.map { font.width(it.second) + 10 }
        val total = widths.sum() + (widths.size - 1) * 4
        var x = (Minecraft.getInstance().window.guiScaledWidth - total) / 2
        val y = bottom - BUTTON_H
        labels.forEachIndexed { i, (group, label) ->
            val sprite = if (state.activeGroup == group) BUTTON_ACTIVE else BUTTON
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, widths[i], BUTTON_H)
            graphics.centeredText(font, label, x + widths[i] / 2, y + (BUTTON_H - 8) / 2, WHITE)
            x += widths[i] + 4
        }
        var top = y - 2
        // Variants of the chosen action (e.g. which chii pair): small tile rows, highlighted one framed.
        val group = state.activeGroup
        if (group != null && group != ActionGroup.RIICHI) {
            val variants = state.optionsIn(group)
            if (variants.size > 1) {
                val rowW = variants.sumOf { it.tiles.size * 12 + 6 } - 6
                var vx = (Minecraft.getInstance().window.guiScaledWidth - rowW) / 2
                val vy = top - 16
                variants.forEachIndexed { i, option ->
                    val w = option.tiles.size * 12
                    if (i == state.variantIndex) graphics.outline(vx - 2, vy - 2, w + 4, 20, GOLD)
                    option.tiles.forEachIndexed { t, tile -> drawSmallTile(graphics, tile, vx + t * 12, vy) }
                    vx += w + 6
                }
                top = vy - 4
            }
        }
        return top
    }

    private fun drawSmallTile(graphics: GuiGraphicsExtractor, tile: MahjongTile, x: Int, y: Int) {
        ScreenDrawing.texturedRect(graphics, x, y, 11, 15, tile.surfaceIdentifier, WHITE)
    }

    /** One line telling the player what they can do now, plus the thinking time. */
    private fun drawHint(graphics: GuiGraphicsExtractor, font: Font, bottom: Int, panelX: Int, panelW: Int) {
        val state = ClientMahjongState
        val use = Minecraft.getInstance().options.keyUse.translatedKeyMessage
        val skip = MahjongKeys.skip.translatedKeyMessage
        val hint: Component? = when {
            state.activeGroup == ActionGroup.RIICHI ->
                Component.translatable("$MOD_ID.hud.hint.riichi", use)
            state.activeGroup != null ->
                Component.translatable("$MOD_ID.hud.hint.variant", MahjongKeys.keyFor(state.activeGroup!!).translatedKeyMessage, skip)
            state.options.isNotEmpty() -> Component.translatable("$MOD_ID.hud.hint.options", skip)
            state.discardRequest?.auto == true -> Component.translatable("$MOD_ID.hud.hint.auto_discard")
            state.discardRequest != null -> Component.translatable("$MOD_ID.hud.hint.discard", use)
            else -> null
        }
        val (base, extra) = state.time
        val timer = when {
            base == null && extra == null -> null
            (base ?: 0) > 0 && (extra ?: 0) > 0 -> "$base + $extra"
            (base ?: 0) > 0 -> "$base"
            else -> "$extra"
        }
        val y = bottom - 10
        if (hint != null) {
            graphics.fill(panelX, y - 1, panelX + panelW, y + 9, PANEL)
            graphics.centeredText(font, hint, panelX + panelW / 2, y, WHITE)
        }
        if (timer != null) {
            graphics.text(font, timer, panelX + panelW + 4, y + 22, if ((base ?: 0) > 0) WHITE else RED, true)
        }
    }
}
