package doublemoon.mahjongcraft.client.render

import doublemoon.mahjongcraft.entity.MahjongScoringStickEntity
import doublemoon.mahjongcraft.entity.MahjongScoringStickEntity.Companion.MAHJONG_POINT_STICK_HEIGHT
import doublemoon.mahjongcraft.entity.MahjongScoringStickEntity.Companion.MAHJONG_POINT_STICK_SCALE
import doublemoon.mahjongcraft.game.mahjong.riichi.model.ScoringStick
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
class MahjongScoringStickEntityRenderer(
    context: EntityRendererProvider.Context,
) : EntityRenderer<MahjongScoringStickEntity>(context) {

    private val itemRenderer = context.itemRenderer

    override fun shouldRender(
        entity: MahjongScoringStickEntity,
        frustum: Frustum,
        x: Double,
        y: Double,
        z: Double,
    ): Boolean {
        return !entity.isInvisible && super.shouldRender(entity, frustum, x, y, z)
    }

    override fun render(
        entity: MahjongScoringStickEntity,
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
            RenderHelper.renderItem(
                itemRenderer = itemRenderer,
                matrices = this,
                stack = scoringSticks[entity.code],
                offsetX = 0.0,
                offsetY = MAHJONG_POINT_STICK_HEIGHT / 2.0 + (1f * MAHJONG_POINT_STICK_SCALE - MAHJONG_POINT_STICK_HEIGHT) / 2.0,
                offsetZ = 0.0,
                light = light,
                mode = ItemDisplayContext.HEAD,
                vertexConsumer = vertexConsumers
            )
            popPose()
        }
    }

    override fun getTextureLocation(entity: MahjongScoringStickEntity): ResourceLocation? = null

    companion object {
        private val scoringSticks = ScoringStick.entries.map { stick->
            ItemRegistry.mahjongScoringStick.defaultInstance.also {
                val nbt = CompoundTag()
                nbt.putInt("code", stick.code)
                it.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
            }
        }
    }
}