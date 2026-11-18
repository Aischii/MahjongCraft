package doublemoon.mahjongcraft.client.render

import doublemoon.mahjongcraft.entity.MahjongBotEntity
import doublemoon.mahjongcraft.entity.MahjongTileEntity
import doublemoon.mahjongcraft.util.RenderHelper
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.item.ItemDisplayContext
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.resources.ResourceLocation
import com.mojang.math.Axis

@Environment(EnvType.CLIENT)
class MahjongBotEntityRenderer(
    context: EntityRendererProvider.Context
) : EntityRenderer<MahjongBotEntity>(context) {

    private val itemRenderer = context.itemRenderer

    override fun shouldRender(entity: MahjongBotEntity, frustum: Frustum, x: Double, y: Double, z: Double): Boolean {
        return !entity.isInvisible && super.shouldRender(entity, frustum, x, y, z)
    }

    override fun render(
        entity: MahjongBotEntity,
        yaw: Float,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int
    ) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light)
        with(matrices) {
            pushPose()
            mulPose(Axis.YP.rotationDegrees(-entity.yRot + 180))
            val scale = MahjongBotEntity.MAHJONG_BOT_HEIGHT / MahjongTileEntity.MAHJONG_TILE_HEIGHT
            scale(scale, scale, scale)
            RenderHelper.renderItem(
                itemRenderer = itemRenderer,
                matrices = this,
                stack = MahjongTileEntityRenderer.mahjongTiles[entity.code],
                offsetX = 0.0,
                offsetY = MahjongBotEntity.MAHJONG_BOT_HEIGHT / 2.0 / scale,
                offsetZ = 0.0,
                light = light,
                vertexConsumer = vertexConsumers,
                mode = ItemDisplayContext.HEAD
            )
            popPose()
        }
    }

    override fun getTextureLocation(entity: MahjongBotEntity): ResourceLocation? = null
}