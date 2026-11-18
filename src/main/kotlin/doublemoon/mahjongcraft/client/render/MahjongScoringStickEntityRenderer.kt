package doublemoon.mahjongcraft.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import doublemoon.mahjongcraft.entity.MahjongScoringStickEntity
import doublemoon.mahjongcraft.entity.MahjongScoringStickEntity.Companion.MAHJONG_POINT_STICK_HEIGHT
import doublemoon.mahjongcraft.entity.MahjongScoringStickEntity.Companion.MAHJONG_POINT_STICK_SCALE
import doublemoon.mahjongcraft.game.mahjong.riichi.model.ScoringStick
import doublemoon.mahjongcraft.registry.ItemRegistry
import doublemoon.mahjongcraft.registry.withCode
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

@Environment(EnvType.CLIENT)
class MahjongScoringStickEntityRenderer(
    context: EntityRendererProvider.Context,
) : ItemModelEntityRenderer<MahjongScoringStickEntity>(context) {

    override val displayContext: ItemDisplayContext = ItemDisplayContext.HEAD

    override fun itemFor(entity: MahjongScoringStickEntity): ItemStack = scoringSticks[entity.code]

    override fun place(entity: MahjongScoringStickEntity, pose: PoseStack) {
        pose.rotate(Axis.YP.rotationDegrees(-entity.yRot + 180))
        pose.translate(
            0.0,
            MAHJONG_POINT_STICK_HEIGHT / 2.0 + (1f * MAHJONG_POINT_STICK_SCALE - MAHJONG_POINT_STICK_HEIGHT) / 2.0,
            0.0,
        )
    }

    companion object {
        private val scoringSticks by lazy {
            ScoringStick.entries.map { stick -> ItemRegistry.mahjongScoringStick.withCode(stick.code) }
        }
    }
}
