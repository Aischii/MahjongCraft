package doublemoon.mahjongcraft.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import doublemoon.mahjongcraft.blockentity.MahjongTileBlockEntity
import doublemoon.mahjongcraft.blockentity.TileBlockPose
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.Vec3

@Environment(EnvType.CLIENT)
class MahjongTileBlockRenderState : BlockEntityRenderState() {
    val tiles = mutableListOf<Pair<Vec3, ItemStackRenderState>>()
    var yaw = 0f
    var flat = false
}

@Environment(EnvType.CLIENT)
class MahjongTileBlockEntityRenderer(context: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<MahjongTileBlockEntity, MahjongTileBlockRenderState> {
    private val itemModelResolver = context.itemModelResolver()

    override fun createRenderState() = MahjongTileBlockRenderState()

    override fun extractRenderState(
        blockEntity: MahjongTileBlockEntity, state: MahjongTileBlockRenderState, partialTicks: Float,
        cameraPosition: Vec3, breakProgress: ModelFeatureRenderer.CrumblingOverlay?,
    ) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)
        state.yaw = 180f - blockEntity.facing.toYRot()
        state.flat = blockEntity.pose == TileBlockPose.FLAT
        state.tiles.clear()
        blockEntity.codes.forEachIndexed { index, code ->
            val item = ItemStackRenderState()
            itemModelResolver.updateForTopItem(item, MahjongTileEntityRenderer.mahjongTiles[code],
                ItemDisplayContext.HEAD, blockEntity.level, null, index)
            state.tiles += blockEntity.tileCenter(index) to item
        }
    }

    override fun submit(
        state: MahjongTileBlockRenderState, poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector, camera: CameraRenderState,
    ) {
        for ((center, item) in state.tiles) {
            poseStack.pushPose()
            poseStack.translate(center.x, center.y, center.z)
            poseStack.rotate(Axis.YP.rotationDegrees(state.yaw))
            if (state.flat) poseStack.rotate(Axis.XP.rotationDegrees(90f))
            item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0)
            poseStack.popPose()
        }
    }
}
