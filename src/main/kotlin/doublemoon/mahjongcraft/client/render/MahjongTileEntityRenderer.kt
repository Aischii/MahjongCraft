package doublemoon.mahjongcraft.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import doublemoon.mahjongcraft.entity.MahjongTileEntity
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_DEPTH
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_HEIGHT
import doublemoon.mahjongcraft.entity.TileFacing
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import doublemoon.mahjongcraft.registry.ItemRegistry
import doublemoon.mahjongcraft.registry.withCode
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

@Environment(EnvType.CLIENT)
class MahjongTileEntityRenderer(
    context: EntityRendererProvider.Context,
) : ItemModelEntityRenderer<MahjongTileEntity>(context) {

    override val displayContext: ItemDisplayContext = ItemDisplayContext.HEAD

    override fun itemFor(entity: MahjongTileEntity): ItemStack = mahjongTiles[entity.code]

    override fun place(entity: MahjongTileEntity, pose: PoseStack) {
        pose.rotate(Axis.YP.rotationDegrees(-entity.yRot + 180))
        pose.rotate(Axis.XP.rotationDegrees(entity.facing.angleForDegreesQuaternionFromPositiveX))
        when (entity.facing) {  //偏移材質用
            TileFacing.HORIZONTAL -> pose.translate(0.0, MAHJONG_TILE_HEIGHT / 2.0, 0.0)
            TileFacing.UP -> pose.translate(0.0, 0.0, -MAHJONG_TILE_DEPTH / 2.0)
            TileFacing.DOWN -> pose.translate(0.0, 0.0, MAHJONG_TILE_DEPTH / 2.0)
        }
    }

    companion object {
        /**
         * 所有的麻將牌物品, 先存著渲染的時候直接用, 省下每次實例化 ItemStack 的效能
         * */
        val mahjongTiles: List<ItemStack> by lazy {
            MahjongTile.entries.map { tile -> ItemRegistry.mahjongTile.withCode(tile.code) }
        }
    }
}
