package doublemoon.mahjongcraft.client.gui.widget

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.MahjongCraftClient
import doublemoon.mahjongcraft.client.ModConfig
import doublemoon.mahjongcraft.client.gui.hud.HandHud
import doublemoon.mahjongcraft.entity.MahjongTileEntity
import doublemoon.mahjongcraft.entity.TileFacing
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongGameBehavior
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import doublemoon.mahjongcraft.network.mahjong_game.MahjongGamePayload
import doublemoon.mahjongcraft.network.sendPayloadToServer
import io.github.cottonmc.cotton.gui.widget.WLabel
import io.github.cottonmc.cotton.gui.widget.WPlainPanel
import io.github.cottonmc.cotton.gui.widget.data.Color
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment
import io.github.cottonmc.cotton.gui.widget.data.VerticalAlignment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

/**
 * 專門在 HUD 渲染牌的提示時使用的
 * */
class WTileHints(
    private val tileHints: ModConfig.TileHints,
) : WPlainPanel() {
    /**
     * 玩家指著的牌, 會顯示這張牌的一些資訊
     * */
    private var tileHintItem: TileHintItem? = null
        set(value) {
            field = value
            MahjongCraftClient.hud?.reposition()
        }

    /**
     * 玩家指著的牌, 丟掉後會聽什麼
     * */
    private var machiOfTargetItems: List<TileHintItem>? = null
    private val backgroundVisible: Boolean get() = tileHintItem != null
    private var hitResult: EntityHitResult? = null

    @Environment(EnvType.CLIENT)
    override fun paint(context: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        val target =
            Minecraft.getInstance().hitResult?.takeIf { it.type == HitResult.Type.ENTITY } as EntityHitResult?
        hitResult =
            target?.takeIf { it.entity is MahjongTileEntity && (it.entity as MahjongTileEntity).mahjongTile != MahjongTile.UNKNOWN }
        updateHints(hitResult)
        // While seated, keep the card above the hand HUD instead of on top of it.
        val handTop = HandHud.topEdge
        val y = if (handTop != null) minOf(y, handTop - height - 4) else y
        //下面是改寫 super.paint() 的部分
        backgroundPainter?.takeIf { backgroundVisible }?.paintBackground(context, x, y, this)
        children.forEach { it.paint(context, x + it.x, y + it.y, mouseX - it.x, mouseY - it.y) }
    }

    private fun updateHints(hitResult: EntityHitResult?) {
        if (hitResult == null) { //沒有看向任何麻將牌, 清除一些東西
            tileHintItem?.let { remove(it) }
            tileHintItem = null
            machiOfTargetItems?.forEach { remove(it) }
            machiOfTarget = null
            machiOfTargetItems = null
            return
        }
        val mahjongTileEntity = hitResult.entity as MahjongTileEntity
        val mahjongTile = mahjongTileEntity.mahjongTile
        if (mahjongTile == MahjongTile.UNKNOWN) return
        val mahjongTable = mahjongTileEntity.mahjongTable
        val remainingAmount = mahjongTable?.remainingTiles?.get(mahjongTileEntity.mahjong4jTile.code) ?: 0
        val player = Minecraft.getInstance().player
        val playerUuidString = player?.stringUUID
        val remainingAmountVisible =
            playerUuidString != null && mahjongTable != null && playerUuidString in mahjongTable.players
        if (tileHintItem == null) {
            val hintItem = TileHintItem(
                scale = tileHints.hudAttribute.scale,
                tile = mahjongTile,
                remainingAmount = remainingAmount,
                remainingAmountVisible = remainingAmountVisible
            )
            add(hintItem, 0, 0, hintItem.width, hintItem.height)
            tileHintItem = hintItem
        }
        tileHintItem?.also {
            it.scale = tileHints.hudAttribute.scale
            val isOwner = playerUuidString != null && mahjongTileEntity.ownerUUID == playerUuidString //這張牌的主人是這個玩家
            val isFacingHorizontal = mahjongTileEntity.facing == TileFacing.HORIZONTAL

            // 是牌的主人, 且牌是面向水平的, 且牌有改變或者沒有計算過 machi
            if (isOwner && isFacingHorizontal && (it.tile != mahjongTile || (machiOfTarget == null && machiOfTargetItems == null))) {
                //傳送數據包請求 machi 列表
                sendPayloadToServer(
                    payload = MahjongGamePayload(
                        behavior = MahjongGameBehavior.MACHI,
                        extraData = Json.encodeToString(mahjongTile)
                    )
                )
            }
            it.tile = mahjongTile
            it.setRemainingTiles(
                amount = remainingAmount,
                visible = remainingAmountVisible,
                noYaku = false,
                machi = false
            )
        }
    }

    fun reposition() {
        val hitResult = hitResult ?: return
        val mahjongTileEntity = hitResult.entity as MahjongTileEntity
        val mahjongTable = mahjongTileEntity.mahjongTable
        val remainingAmount = { code: Int -> mahjongTable?.remainingTiles?.get(code) ?: 0 }
        machiOfTarget?.also { options ->
            machiOfTargetItems?.forEach { remove(it) }
            machiOfTargetItems = buildList {
                options.forEach { tile, (han, furiten) ->
                    val hintItem = TileHintItem(
                        scale = tileHints.hudAttribute.scale,
                        tile = tile,
                        remainingAmount = remainingAmount(tile.mahjong4jTile.code),
                        remainingAmountVisible = true,
                        noYaku = han == 0,
                        machi = true,
                        furiten = furiten
                    )
                    add(hintItem, 0, 0, hintItem.width, hintItem.height)
                    this += hintItem
                }
            }
        }
        children.forEachIndexed { index, widget ->
            val x = if (index > 0) children[index - 1].let { it.x + it.width } + INTERVAL else 0
            widget.setLocation(x, widget.y)
        }
        val rootX = children.maxOf { it.x + it.width }
        val rootY = children.maxOf { it.y + it.height }
        setSize(rootX, rootY)
    }

    companion object {
        private const val INTERVAL = 12

        /**
         * 會聽的牌跟對應的翻數
         * */
        var machiOfTarget: Map<MahjongTile, Pair<Int, Boolean>>? = null
            set(value) {
                field = value
                MahjongCraftClient.hud?.reposition()
            }
    }

    /**
     * One tile card: "Furiten" above the tile when it applies, the tile, its name, then a detail
     * line (how many are left, or "No Yaku"). The card resizes whenever its text changes, so
     * nothing spills outside the background.
     *
     * @param scale 渲染的比例, 不適用於文字
     * @param machi this tile is one the player is waiting on (detail line is green)
     * */
    class TileHintItem(
        var scale: Double,
        tile: MahjongTile,
        furiten: Boolean = false,
        remainingAmount: Int,
        remainingAmountVisible: Boolean,
        noYaku: Boolean = false,
        machi: Boolean = false,
    ) : WPlainPanel() {
        private val client = Minecraft.getInstance()
        private val furitenText get() = Component.translatable("$MOD_ID.game.furiten")
        private val noYakuText get() = Component.translatable("$MOD_ID.game.no_yaku")

        private val furitenWidget = WLabel(Component.empty(), Color.PURPLE_DYE.toRgb())
        private val tileWidget = WMahjongTile(tile)
        private val nameWidget = WLabel(tile.toText(), Color.WHITE.toRgb())
        private val detailWidget = WLabel(Component.empty(), DETAIL_COLOR)
        private var furiten = furiten
        /** False while constructing, so a new card doesn't ask the HUD to re-layout (which builds cards). */
        private var built = false

        init {
            listOf(furitenWidget, nameWidget, detailWidget).forEach {
                it.horizontalAlignment = HorizontalAlignment.CENTER
                it.verticalAlignment = VerticalAlignment.CENTER
            }
            add(furitenWidget, 0, 0)
            add(tileWidget, 0, 0)
            add(nameWidget, 0, 0)
            add(detailWidget, 0, 0)
            setRemainingTiles(remainingAmount, remainingAmountVisible, noYaku, machi)
            built = true
        }

        var tile: MahjongTile
            get() = tileWidget.mahjongTile
            set(value) {
                if (tileWidget.mahjongTile == value) return
                tileWidget.mahjongTile = value
                nameWidget.text = value.toText()
                relayout()
            }

        fun setRemainingTiles(amount: Int, visible: Boolean, noYaku: Boolean, machi: Boolean) {
            detailWidget.text = when {
                noYaku -> noYakuText
                visible -> Component.translatable("$MOD_ID.game.tiles_left", amount)
                else -> Component.empty()
            }
            detailWidget.color = when {
                noYaku -> NO_YAKU_COLOR
                machi -> Color.GREEN.toRgb()
                else -> DETAIL_COLOR
            }
            relayout()
        }

        /** Sizes the card to its widest line and stacks the lines; tells the HUD if the size changed. */
        private fun relayout() {
            val font = client.font
            furitenWidget.text = if (furiten) furitenText else Component.empty()
            val tileWidth = (HINT_WIDTH * scale).toInt()
            val tileHeight = (HINT_HEIGHT * scale).toInt()
            val lines = listOf(furitenWidget, nameWidget, detailWidget)
            val textWidth = lines.maxOf { font.width(it.text) }
            val width = maxOf(tileWidth, textWidth) + INSET * 2
            var y = INSET
            if (furiten) {
                furitenWidget.setLocation(0, y); furitenWidget.setSize(width, font.lineHeight)
                y += font.lineHeight + MARGIN
            } else {
                furitenWidget.setSize(width, 0)
            }
            tileWidget.setLocation((width - tileWidth) / 2, y); tileWidget.setSize(tileWidth, tileHeight)
            y += tileHeight + MARGIN
            nameWidget.setLocation(0, y); nameWidget.setSize(width, font.lineHeight)
            y += font.lineHeight + 1
            val hasDetail = detailWidget.text.string.isNotEmpty()
            if (hasDetail) {
                detailWidget.setLocation(0, y); detailWidget.setSize(width, font.lineHeight)
                y += font.lineHeight
            } else {
                detailWidget.setSize(width, 0)
            }
            val height = y + INSET
            if (width != this.width || height != this.height) {
                setSize(width, height)
                if (built) MahjongCraftClient.hud?.reposition()
            }
        }

        companion object {
            const val HINT_WIDTH = 24
            const val HINT_HEIGHT = 32
            const val MARGIN = 3
            const val INSET = 4
            private val DETAIL_COLOR = 0xFFB0B0B0.toInt()
            private val NO_YAKU_COLOR = 0xFFFF5555.toInt()
        }
    }
}