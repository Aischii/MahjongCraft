package doublemoon.mahjongcraft.client.render

import doublemoon.mahjongcraft.entity.MahjongTileEntity
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_DEPTH
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_HEIGHT
import doublemoon.mahjongcraft.entity.TileFacing
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import doublemoon.mahjongcraft.registry.ItemRegistry
import doublemoon.mahjongcraft.util.RenderHelper
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.item.ItemDisplayContext
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import com.mojang.math.Axis

@Environment(EnvType.CLIENT)
class MahjongTileEntityRenderer(
    context: EntityRendererProvider.Context,
) : EntityRenderer<MahjongTileEntity>(context) {

    private val itemRenderer = context.itemRenderer

    override fun shouldRender(entity: MahjongTileEntity, frustum: Frustum, x: Double, y: Double, z: Double): Boolean {
        return !entity.isInvisible && super.shouldRender(entity, frustum, x, y, z)
    }

    override fun render(
        entity: MahjongTileEntity,
        yaw: Float,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int,
    ) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light)
        with(matrices) {
            pushPose()
            mulPose(Axis.YP.rotationDegrees(-entity.yRot + 180))
            mulPose(Axis.XP.rotationDegrees(entity.facing.angleForDegreesQuaternionFromPositiveX))
            var offsetY = 0.0
            var offsetZ = 0.0
            when (entity.facing) {  //偏移材質用
                TileFacing.HORIZONTAL -> offsetY = MAHJONG_TILE_HEIGHT / 2.0
                TileFacing.UP -> offsetZ = -MAHJONG_TILE_DEPTH / 2.0
                TileFacing.DOWN -> offsetZ = MAHJONG_TILE_DEPTH / 2.0
            }
            RenderHelper.renderItem(
                itemRenderer = itemRenderer,
                matrices = this,
                stack = mahjongTiles[entity.code],
                offsetX = 0.0,
                offsetY = offsetY,
                offsetZ = offsetZ,
                light = light,
                vertexConsumer = vertexConsumers,
                mode = ItemDisplayContext.HEAD
            )
            popPose()
        }
    }

    override fun getTextureLocation(entity: MahjongTileEntity): ResourceLocation? = null

    companion object {
        /**
         * 所有的麻將牌物品, 先存著渲染的時候直接用, 省下每次實例化 ItemStack 的效能
         * */
        @Environment(EnvType.CLIENT)
        val mahjongTiles = MahjongTile.entries.map { tile ->
            ItemRegistry.mahjongTile.defaultInstance.also {
                val nbt = CompoundTag()
                nbt.putInt("code", tile.code)
                it.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
            }
        }
    }
}