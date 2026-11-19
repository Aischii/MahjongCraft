package doublemoon.mahjongcraft.client.render

import com.mojang.blaze3d.vertex.PoseStack
import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.MahjongCraftClient
import doublemoon.mahjongcraft.block.MahjongTable
import doublemoon.mahjongcraft.block.enums.MahjongTablePart
import doublemoon.mahjongcraft.blockentity.MahjongTableBlockEntity
import doublemoon.mahjongcraft.game.GameStatus
import doublemoon.mahjongcraft.game.mahjong.riichi.model.Wind
import doublemoon.mahjongcraft.util.RenderHelper
import io.github.cottonmc.cotton.gui.widget.data.Color
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3

/** One floating text line above the table, relative to the table's block. */
@Environment(EnvType.CLIENT)
data class TableLabel(val x: Double, val y: Double, val z: Double, val text: Component, val color: Int)

@Environment(EnvType.CLIENT)
class MahjongTableRenderState : BlockEntityRenderState() {
    val labels = mutableListOf<TableLabel>()
    var labelLight = 0
}

/**
 * Draws the table's floating labels: status, player count and ready count while waiting, and the
 * round plus each seat's wind and points while playing.
 */
@Environment(EnvType.CLIENT)
class MahjongTableBlockEntityRenderer(
    context: BlockEntityRendererProvider.Context,
) : BlockEntityRenderer<MahjongTableBlockEntity, MahjongTableRenderState> {
    private val font: Font = context.font()

    override fun createRenderState(): MahjongTableRenderState = MahjongTableRenderState()

    override fun extractRenderState(
        blockEntity: MahjongTableBlockEntity,
        state: MahjongTableRenderState,
        partialTicks: Float,
        cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?,
    ) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)
        state.labels.clear()
        if (!MahjongCraftClient.config.displayTableLabels) return
        if (blockEntity.blockState.getValue(MahjongTable.PART) != MahjongTablePart.BOTTOM_CENTER) return //只有 BOTTOM_CENTER 才渲染
        val level = blockEntity.level ?: return
        state.labelLight = RenderHelper.getLightLevel(level, blockEntity.blockPos.above())
        addCenterLabels(blockEntity, state.labels)
        addPlayerLabels(blockEntity, state.labels)
    }

    override fun submit(
        state: MahjongTableRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState,
    ) {
        state.labels.forEach {
            RenderHelper.submitLabel(
                font = font,
                poseStack = poseStack,
                collector = submitNodeCollector,
                cameraOrientation = camera.orientation,
                offsetX = it.x,
                offsetY = it.y,
                offsetZ = it.z,
                text = it.text,
                color = it.color,
                light = state.labelLight,
            )
        }
    }

    /**
     * 渲染玩家的標籤,
     * 印出以莊家為東, 每個玩家的風, 以及分數, 莊家(東)風的顏色不一樣
     * */
    private fun addPlayerLabels(blockEntity: MahjongTableBlockEntity, labels: MutableList<TableLabel>) {
        val seats = blockEntity.rule.seats
        if (!blockEntity.playing || blockEntity.seat.size < seats) return //只有遊戲中才能渲染
        val blockPos = blockEntity.blockPos
        val dealer = blockEntity.dealer
        val seat = blockEntity.seat
        val dealerSeatIndex = seat.indexOf(dealer).also { if (it == -1) return }
        seat.take(seats).forEachIndexed { index, stringUUID ->
            val bPos = when (index) {
                0 -> blockPos.east().east()
                1 -> blockPos.north().north()
                2 -> blockPos.west().west()
                else -> blockPos.south().south()
            }
            val windIndex = (index - dealerSeatIndex + seats) % seats //莊家是東, 往下家依序是南西(北)
            val lines = listOf(
                Component.nullToEmpty(blockEntity.points[index].toString()),
                Wind.entries[windIndex].toText(),
            )
            lines.forEachIndexed { line, text ->
                labels += TableLabel(
                    x = 0.5 + (bPos.x - blockPos.x),
                    y = 1.0 + (bPos.y - blockPos.y) + WIND_PADDING + (LABEL_INTERVAL * line),
                    z = 0.5 + (bPos.z - blockPos.z),
                    text = text,
                    color = if (stringUUID == dealer) 0xEEB24F else Color.WHITE.toRgb(), //東風的顏色會不一樣
                )
            }
        }
    }

    /**
     * 渲染位於桌子正中央上方的標籤
     * */
    private fun addCenterLabels(blockEntity: MahjongTableBlockEntity, labels: MutableList<TableLabel>) {
        val playerAmount = blockEntity.players.count { it.isNotEmpty() }
        val readyAmount = blockEntity.ready.count { it }
        val labelPadding = if (blockEntity.playing) PLAYING_PADDING else WAITING_PADDING
        val lines = buildList {
            this += Component.empty().append(STATUS).append(": ").append(if (blockEntity.playing) PLAYING else WAITING)
            if (!blockEntity.playing) { //沒在遊戲中才會顯示
                this += Component.empty().append(PLAYER).append(": $playerAmount/4")
                this += Component.empty().append(READY).append(": $readyAmount/4")
                if (blockEntity.tileSetRequired) {
                    this += Component.empty().append(TILE_SET).append(": ").append(
                        if (blockEntity.tileSetLoaded) Component.literal("✔").withStyle(ChatFormatting.GREEN)
                        else Component.literal("✘").withStyle(ChatFormatting.RED)
                    )
                }
            } else { //在遊戲中才顯示
                val round = blockEntity.round
                this += Component.translatable("$MOD_ID.game.round.title", round.wind.toText(), round.round + 1) //ex: 東 4 局
            }
        }.reversed()
        lines.forEachIndexed { index, text ->
            labels += TableLabel(0.5, 1.0 + labelPadding + (LABEL_INTERVAL * index), 0.5, text, Color.WHITE.toRgb())
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
        private val TILE_SET get() = Component.translatable("item.$MOD_ID.mahjong_tile_set")
        private val WAITING get() = GameStatus.WAITING.toText()
        private val PLAYING get() = GameStatus.PLAYING.toText()
    }
}
