package doublemoon.mahjongcraft.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import doublemoon.mahjongcraft.entity.MahjongBotEntity
import doublemoon.mahjongcraft.entity.MahjongTileEntity
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

/** A bot is drawn as one big mahjong tile. */
@Environment(EnvType.CLIENT)
class MahjongBotEntityRenderer(
    context: EntityRendererProvider.Context,
) : ItemModelEntityRenderer<MahjongBotEntity>(context) {

    override val displayContext: ItemDisplayContext = ItemDisplayContext.HEAD

    override fun itemFor(entity: MahjongBotEntity): ItemStack = MahjongTileEntityRenderer.mahjongTiles[entity.code]

    override fun place(entity: MahjongBotEntity, pose: PoseStack) {
        pose.rotate(Axis.YP.rotationDegrees(-entity.yRot + 180))
        val scale = MahjongBotEntity.MAHJONG_BOT_HEIGHT / MahjongTileEntity.MAHJONG_TILE_HEIGHT
        pose.scale(scale, scale, scale)
        pose.translate(0.0, MahjongBotEntity.MAHJONG_BOT_HEIGHT / 2.0 / scale, 0.0)
    }
}
