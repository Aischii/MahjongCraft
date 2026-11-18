package doublemoon.mahjongcraft.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import doublemoon.mahjongcraft.entity.DiceEntity
import doublemoon.mahjongcraft.entity.DiceEntity.Companion.DICE_HEIGHT
import doublemoon.mahjongcraft.entity.DiceEntity.Companion.DICE_SCALE
import doublemoon.mahjongcraft.entity.DicePoint
import doublemoon.mahjongcraft.registry.ItemRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.item.ItemStack

@Environment(EnvType.CLIENT)
class DiceEntityRenderer(
    context: EntityRendererProvider.Context,
) : ItemModelEntityRenderer<DiceEntity>(context) {

    override fun itemFor(entity: DiceEntity): ItemStack = diceItem

    override fun place(entity: DiceEntity, pose: PoseStack) {
        pose.rotate(Axis.YP.rotationDegrees(-entity.yRot + 180))
        pose.rotate(Axis.XP.rotationDegrees(entity.point.xpRotDegrees))
        pose.rotate(Axis.YP.rotationDegrees(entity.point.ypRotDegrees))
        val baseOffset = DICE_HEIGHT / 2.0 + (1f * DICE_SCALE - DICE_HEIGHT) / 2.0
        val halfHeight = DICE_HEIGHT / 2.0
        val offsetX = when (entity.point) {
            DicePoint.THREE -> halfHeight
            DicePoint.FIVE -> -halfHeight
            else -> 0.0
        }
        val offsetY = baseOffset - when (entity.point) {
            DicePoint.ONE -> 0.0
            DicePoint.SIX -> DICE_HEIGHT.toDouble()
            else -> halfHeight
        }
        val offsetZ = when (entity.point) {
            DicePoint.TWO -> -halfHeight
            DicePoint.FOUR -> halfHeight
            else -> 0.0
        }
        pose.translate(offsetX, offsetY, offsetZ)
    }

    companion object {
        private val diceItem by lazy { ItemRegistry.dice.defaultInstance }
    }
}
