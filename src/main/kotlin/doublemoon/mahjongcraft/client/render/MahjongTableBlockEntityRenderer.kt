package doublemoon.mahjongcraft.client.render

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.MahjongCraftClient
import doublemoon.mahjongcraft.block.MahjongTable
import doublemoon.mahjongcraft.block.enums.MahjongTablePart
import doublemoon.mahjongcraft.blockentity.MahjongTableBlockEntity
import doublemoon.mahjongcraft.game.GameStatus
import doublemoon.mahjongcraft.game.mahjong.riichi.model.Wind
import doublemoon.mahjongcraft.util.RenderHelper
import doublemoon.mahjongcraft.util.plus
import io.github.cottonmc.cotton.gui.widget.data.Color
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.network.chat.Component

@Environment(EnvType.CLIENT)
class MahjongTableBlockEntityRenderer(
    context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<MahjongTableBlockEntity> {
    private val textRenderer = context.font

    override fun render(
        blockEntity: MahjongTableBlockEntity,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (!MahjongCraftClient.config.displayTableLabels) return
        if (blockEntity.blockState.getValue(MahjongTable.PART) != MahjongTablePart.BOTTOM_CENTER) return //只有 BOTTOM_CENTER 才渲染
        renderCenterLabels(blockEntity, matrices, vertexConsumers)
        renderPlayerLabels(blockEntity, matrices, vertexConsumers)
    }

    /**
     * 渲染玩家的標籤,
     * 印出以莊家為東, 每個玩家的風, 以及分數, 莊家(東)風的顏色不一樣
     * */
    private fun renderPlayerLabels(
        blockEntity: MahjongTableBlockEntity,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource
    ) {
        if (!blockEntity.playing || blockEntity.seat.size != 4) return //只有遊戲中才能渲染
        val blockPos = blockEntity.blockPos
        val dealer = blockEntity.dealer
        val seat = blockEntity.seat
        val dealerSeatIndex = seat.indexOf(dealer).also { if (it == -1) return }
        seat.forEachIndexed { index, stringUUID ->
            val bPos = when (index) {
                0 -> blockPos.east().east()
                1 -> blockPos.north().north()
                2 -> blockPos.west().west()
                else -> blockPos.south().south()
            }
            val windIndex = (dealerSeatIndex - index).let { if (it >= 0) 4 - it else -it } % 4
            buildList {
                this += Wind.entries[windIndex].toText()
                this += Component.nullToEmpty(blockEntity.points[index].toString())
                reverse()
                forEachIndexed { index1, text ->
                    RenderHelper.renderLabel(
                        textRenderer = textRenderer,
                        matrices = matrices,
                        offsetX = 0.5 + (bPos.x - blockPos.x),
                        offsetY = 1.0 + (bPos.y - blockPos.y) + WIND_PADDING + (LABEL_INTERVAL * index1),
                        offsetZ = 0.5 + (bPos.z - blockPos.z),
                        text = text,
                        color = if (stringUUID == dealer) 0xEEB24F else Color.WHITE.toRgb(), //東風的顏色會不一樣
                        light = RenderHelper.getLightLevel(blockEntity.level!!, blockEntity.blockPos.above()),
                        vertexConsumers = vertexConsumers
                    )
                }
            }
        }
    }

    /**
     * 渲染位於桌子正中央上方的標籤
     * */
    private fun renderCenterLabels(
        blockEntity: MahjongTableBlockEntity,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource
    ) {
        val playerAmount = blockEntity.players.count { it.isNotEmpty() }
        val readyAmount = blockEntity.ready.count { it }
        val currentReady = READY + ": $readyAmount/4"
        val currentPlayers = PLAYER + ": $playerAmount/4"
        val currentStatus = STATUS + ": " + (if (blockEntity.playing) PLAYING else WAITING)
        val labelPadding = if (blockEntity.playing) PLAYING_PADDING else WAITING_PADDING
        buildList {
            this += currentStatus
            if (!blockEntity.playing) { //沒在遊戲中才會顯示
                this += currentPlayers
                this += currentReady
            } else { //在遊戲中才顯示
                val round = blockEntity.round
                val windText = round.wind.toText()
                this += Component.translatable("$MOD_ID.game.round.title", windText, round.round + 1) //ex: 東 4 局
            }
            reverse()
            forEachIndexed { index, text -> //渲染文字
                RenderHelper.renderLabel(
                    textRenderer = textRenderer,
                    matrices = matrices,
                    offsetX = 0.5,
                    offsetY = 1.0 + labelPadding + (LABEL_INTERVAL * index),
                    offsetZ = 0.5,
                    text = text,
                    color = Color.WHITE.toRgb(),
                    light = RenderHelper.getLightLevel(blockEntity.level!!, blockEntity.blockPos.above()),
                    vertexConsumers = vertexConsumers
                )
            }
        }
    }

    companion object {
        private const val WAITING_PADDING = 0.4
        private const val PLAYING_PADDING = 1.6
        private const val WIND_PADDING = 1.6
        private const val LABEL_INTERVAL = 0.25
        private val READY get() = Component.translatable("$MOD_ID.gui.button.ready")
        private val PLAYER get() = Component.translatable("$MOD_ID.game.player")
        private val STATUS get() = Component.translatable("$MOD_ID.game.status")
        private val WAITING = GameStatus.WAITING.toText()
        private val PLAYING = GameStatus.PLAYING.toText()
    }
}