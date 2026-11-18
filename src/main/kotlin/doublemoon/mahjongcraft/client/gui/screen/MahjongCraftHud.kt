package doublemoon.mahjongcraft.client.gui.screen

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.MahjongCraftClient
import doublemoon.mahjongcraft.id
import doublemoon.mahjongcraft.client.gui.widget.WTileHints
import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.client.WidgetHudElement
import io.github.cottonmc.cotton.gui.widget.WDynamicLabel
import io.github.cottonmc.cotton.gui.widget.WPlainPanel
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

/**
 * 請參考 [CottonHud]
 * */
@Environment(EnvType.CLIENT)
class MahjongCraftHud {
    val quickActionsRootSize: Pair<Int, Int> get() = quickActionsRoot.let { it.width to it.height }
    val tileHintsRootSize: Pair<Int, Int> get() = tileHintsRoot.let { it.width to it.height }
    private val config = MahjongCraftClient.config
    private val client = Minecraft.getInstance()
    private val window = client.window
    private val textRenderer = client.font
    private val fontHeight = textRenderer.lineHeight
    private val quickActions = config.quickActions
    private val quickActionsRoot = WPlainPanel()
    private val quickActionsTextAndLabel = listOf(
        { colorPrefix(quickActions.autoArrange) + AUTO_ARRANGE.string },
        { colorPrefix(quickActions.autoCallWin) + AUTO_CALL_WIN.string },
        { colorPrefix(quickActions.noChiiPonKan) + NO_CHII_PON_KAN.string },
        { colorPrefix(quickActions.autoDrawAndDiscard) + AUTO_DRAW_AND_DISCARD.string }
    ).associateWith { WDynamicLabel(it) }
    private val tileHints = config.tileHints
    private val tileHintsRoot = WTileHints(tileHints)

    // Each panel is registered once as a HUD element and drawn only while visible.
    private var showQuickActions = false
    private var showTileHints = false
    private val quickActionsElement = WidgetHudElement(quickActionsRoot)
    private val tileHintsElement = WidgetHudElement(tileHintsRoot)

    init {
        initQuickActions()
        HudElementRegistry.addLast(id("quick_actions")) { graphics, delta ->
            if (showQuickActions) quickActionsElement.extractRenderState(graphics, delta)
        }
        HudElementRegistry.addLast(id("tile_hints")) { graphics, delta ->
            if (showTileHints) tileHintsElement.extractRenderState(graphics, delta)
        }
        refresh()
        reposition()
    }

    fun refresh() {
        quickActionsRoot.backgroundPainter = BackgroundPainter.createColorful(quickActions.hudAttribute.backgroundColor)
        tileHintsRoot.backgroundPainter = BackgroundPainter.createColorful(tileHints.hudAttribute.backgroundColor)
        showQuickActions = MahjongCraftClient.playing && quickActions.displayHudWhenPlaying
        showTileHints = tileHints.displayHud
    }

    fun reposition() {
        val width = window.guiScaledWidth
        val height = window.guiScaledHeight
        repositionQuickActions(width, height)
        repositionTileHints(width, height)
    }

    private fun initQuickActions() {
        quickActionsTextAndLabel.forEach { (_, label) -> quickActionsRoot.add(label, 0, 0, 0, 0) }
    }

    private fun repositionQuickActions(width: Int, height: Int) {
        quickActionsTextAndLabel.forEach { (text, label) ->
            val index = quickActionsTextAndLabel.values.indexOf(label)
            label.setSize(textWidth(text.invoke()), fontHeight)
            label.setLocation(INSET, INSET + index * (fontHeight + LABEL_INTERVAL))
        }
        //Root Size
        val widgetWidth = INSET * 2 + quickActionsTextAndLabel.maxOf { it.value.width }
        val widgetHeight =
            INSET * 2 + quickActionsTextAndLabel.values.size * (fontHeight + LABEL_INTERVAL) - LABEL_INTERVAL
        quickActionsRoot.setSize(widgetWidth, widgetHeight)
        //Root Location
        val (hudX, hudY) = quickActions.hudAttribute
        val x = (width * hudX).toInt()
        val y = (height * hudY).toInt()
        quickActionsRoot.setLocation(x, y)
    }

    private fun repositionTileHints(width: Int, height: Int) {
        tileHintsRoot.reposition()
        val x = ((width - tileHintsRoot.width) / 2.0).toInt()
        val y = (height * tileHints.hudAttribute.y).toInt()
        tileHintsRoot.setLocation(x, y)
    }

    private fun colorPrefix(settingEnabled: Boolean) = if (settingEnabled) "§a" else "§c"

    private fun textWidth(text: String) = textRenderer.width(text)

    companion object {
        private const val INSET = 6
        private const val LABEL_INTERVAL = 2
        private val AUTO_ARRANGE = Component.translatable("config.$MOD_ID.quick_actions.auto_arrange")
        private val AUTO_CALL_WIN = Component.translatable("config.$MOD_ID.quick_actions.auto_call_win")
        private val NO_CHII_PON_KAN = Component.translatable("config.$MOD_ID.quick_actions.no_chii_pon_kan")
        private val AUTO_DRAW_AND_DISCARD = Component.translatable("config.$MOD_ID.quick_actions.auto_draw_and_discard")
    }
}
