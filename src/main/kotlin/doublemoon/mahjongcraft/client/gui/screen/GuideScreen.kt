package doublemoon.mahjongcraft.client.gui.screen

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.MahjongCraftClient
import doublemoon.mahjongcraft.client.game.MahjongKeys
import doublemoon.mahjongcraft.client.gui.screen.yaku_overview.YakuOverviewScreen
import doublemoon.mahjongcraft.game.mahjong.riichi.model.ActionGroup
import doublemoon.mahjongcraft.game.progress.PlayerStats
import doublemoon.mahjongcraft.game.progress.StatsText
import doublemoon.mahjongcraft.network.mahjong_stats.MahjongStatsPayloadListener
import doublemoon.mahjongcraft.registry.BlockRegistry
import doublemoon.mahjongcraft.registry.ItemRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ItemLike

/**
 * The Mahjong Guide: a paged screen opened by the Guide item. Tabs along the top, a scrollable
 * translucent panel below. Text comes from the `mahjongcraft.guide.*` lang keys; the Controls tab
 * reads the player's current key bindings so it is always right.
 */
@Environment(EnvType.CLIENT)
class GuideScreen(private var tab: Tab = Tab.CRAFTING) :
    Screen(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("item.$MOD_ID.mahjong_book")) {

    enum class Tab { CRAFTING, CONTROLS, RULES, YAKU, STATS }

    /** One line of the page: optional item icon, then text. */
    private class Line(
        val text: FormattedCharSequence,
        val icon: ItemStack? = null,
        val color: Int = WHITE,
        /** Extra left offset, so recipe lines line up beside the item icon. */
        val indent: Int = 0,
    )

    private var lines: List<Line> = emptyList()
    private var scroll = 0
    /** This player's stats from the server; null until the reply arrives. */
    private var stats: PlayerStats? = null

    private val panelW get() = minOf(320, width - 32)
    private val panelX get() = (width - panelW) / 2
    private val panelTop = 52
    private val panelBottom get() = height - 36

    override fun init() {
        if (tab == Tab.STATS && stats == null) MahjongStatsPayloadListener.request()
        val tabW = (panelW - (Tab.entries.size - 1) * 4) / Tab.entries.size
        Tab.entries.forEachIndexed { i, t ->
            addRenderableWidget(
                Button.builder(Component.translatable("$MOD_ID.guide.tab.${t.name.lowercase()}")) { select(t) }
                    .bounds(panelX + i * (tabW + 4), 26, tabW, 20).build()
            ).also { it.active = t != tab }
        }
        if (tab == Tab.YAKU) {
            addRenderableWidget(
                Button.builder(Component.translatable("$MOD_ID.guide.yaku.open")) {
                    minecraft.gui.setScreen(YakuOverviewScreen())
                }.bounds(width / 2 - 80, panelBottom + 8, 160, 20).build()
            )
        }
        lines = buildLines()
        scroll = scroll.coerceIn(0, maxScroll())
    }

    private fun select(t: Tab) {
        tab = t
        scroll = 0
        rebuildWidgets()
    }

    /** Called when the server's reply to the Stats tab's request arrives. */
    fun showStats(stats: PlayerStats) {
        this.stats = stats
        if (tab == Tab.STATS) rebuildWidgets()
    }

    private fun maxScroll(): Int = maxOf(0, lines.size * LINE_H - (panelBottom - panelTop - PAD * 2))

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        scroll = (scroll - (scrollY * LINE_H * 2).toInt()).coerceIn(0, maxScroll())
        return true
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        graphics.centeredText(font, title, width / 2, 10, GOLD)
        graphics.fill(panelX, panelTop, panelX + panelW, panelBottom, PANEL)
        graphics.outline(panelX, panelTop, panelW, panelBottom - panelTop, BORDER)
        graphics.enableScissor(panelX + 1, panelTop + 1, panelX + panelW - 1, panelBottom - 1)
        var y = panelTop + PAD - scroll
        lines.forEach { line ->
            var x = panelX + PAD + line.indent
            if (line.icon != null) {
                graphics.item(line.icon, x, y - 2)
                x += ICON_W
            }
            graphics.text(font, line.text, x, y, line.color, true)
            y += LINE_H
        }
        graphics.disableScissor()
        if (maxScroll() > 0) {
            val trackH = panelBottom - panelTop - 4
            val barH = maxOf(12, trackH * trackH / (trackH + maxScroll()))
            val barY = panelTop + 2 + (trackH - barH) * scroll / maxScroll()
            graphics.fill(panelX + panelW - 4, barY, panelX + panelW - 2, barY + barH, BORDER)
        }
    }

    override fun isPauseScreen(): Boolean = false

    private fun buildLines(): List<Line> {
        val wrapW = panelW - PAD * 2 - 6
        val out = mutableListOf<Line>()
        fun heading(key: String) {
            if (out.isNotEmpty()) out += Line(FormattedCharSequence.EMPTY)
            out += Line(Component.translatable(key).visualOrderText, color = GOLD)
        }
        fun paragraph(text: Component, color: Int = WHITE) =
            font.split(text, wrapW).forEach { out += Line(it, color = color) }
        fun item(item: ItemLike, recipeKey: String) {
            val stack = ItemStack(item)
            if (out.isNotEmpty() && out.last().text != FormattedCharSequence.EMPTY) out += Line(FormattedCharSequence.EMPTY)
            out += Line(stack.hoverName.visualOrderText, icon = stack, color = AQUA)
            font.split(Component.translatable(recipeKey), wrapW - ICON_W).forEach {
                out += Line(it, color = GREY, indent = ICON_W)
            }
        }
        fun key(label: Component, vararg keys: KeyMapping) {
            val bound = Component.empty()
            keys.forEachIndexed { i, k ->
                if (i > 0) bound.append(" / ")
                bound.append(k.translatedKeyMessage)
            }
            out += Line(Component.empty().append(label).append(": ").append(bound.withColor(YELLOW)).visualOrderText)
        }

        when (tab) {
            Tab.CRAFTING -> {
                paragraph(Component.translatable("$MOD_ID.guide.crafting.intro"), GREY)
                out += Line(FormattedCharSequence.EMPTY)
                item(BlockRegistry.mahjongTable, "$MOD_ID.guide.recipe.mahjong_table")
                item(ItemRegistry.mahjongTileSet, "$MOD_ID.guide.recipe.mahjong_tile_set")
                item(BlockRegistry.mahjongStool, "$MOD_ID.guide.recipe.mahjong_stool")
                item(ItemRegistry.mahjongGuide, "$MOD_ID.guide.recipe.mahjong_book")
                item(ItemRegistry.mahjongTile, "$MOD_ID.guide.recipe.mahjong_tile")
                item(ItemRegistry.mahjongScoringStick, "$MOD_ID.guide.recipe.mahjong_scoring_stick")
                item(ItemRegistry.dice, "$MOD_ID.guide.recipe.dice")
            }
            Tab.CONTROLS -> {
                paragraph(Component.translatable("$MOD_ID.guide.controls.intro"), GREY)
                heading("$MOD_ID.guide.controls.hand")
                key(Component.translatable("$MOD_ID.guide.controls.choose"), MahjongKeys.selectLeft, MahjongKeys.selectRight)
                key(Component.translatable("$MOD_ID.guide.controls.discard"), minecraft.options.keyUse, MahjongKeys.discard)
                heading("$MOD_ID.guide.controls.calls")
                ActionGroup.entries.forEach { group ->
                    key(Component.translatable("$MOD_ID.hud.action.${group.name.lowercase()}"), MahjongKeys.keyFor(group))
                }
                heading("$MOD_ID.guide.controls.other")
                key(Component.translatable("key.$MOD_ID.open_config_gui"), MahjongCraftClient.configKey)
                key(Component.translatable("key.$MOD_ID.open_yaku_overview"), MahjongCraftClient.yakuOverviewKey)
                out += Line(FormattedCharSequence.EMPTY)
                paragraph(Component.translatable("$MOD_ID.guide.controls.rebind"), GREY)
            }
            Tab.RULES -> listOf("start", "goal", "turn", "calls", "riichi", "win", "furiten").forEach {
                heading("$MOD_ID.guide.rules.$it.title")
                paragraph(Component.translatable("$MOD_ID.guide.rules.$it"))
            }
            Tab.YAKU -> paragraph(Component.translatable("$MOD_ID.guide.yaku.intro"))
            Tab.STATS -> {
                val current = stats
                if (current == null) {
                    paragraph(Component.translatable("$MOD_ID.guide.stats.loading"), GREY)
                } else {
                    StatsText.lines(current).forEach { paragraph(it) }
                    out += Line(FormattedCharSequence.EMPTY)
                    paragraph(Component.translatable("$MOD_ID.guide.stats.command"), GREY)
                }
            }
        }
        return out
    }

    companion object {
        private const val PAD = 8
        private const val LINE_H = 11
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val GREY = 0xFFB0B0B0.toInt()
        private const val GOLD = 0xFFFFCC33.toInt()
        private const val AQUA = 0xFF66DDEE.toInt()
        private const val YELLOW = 0xFFFFFF77.toInt()
        private const val PANEL = 0xB0000000.toInt()
        private const val BORDER = 0xFF555555.toInt()
        private const val ICON_W = 20
    }
}
