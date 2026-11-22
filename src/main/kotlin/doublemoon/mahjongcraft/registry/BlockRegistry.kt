package doublemoon.mahjongcraft.registry

import doublemoon.mahjongcraft.block.MahjongStool
import doublemoon.mahjongcraft.block.MahjongTileBlock
import doublemoon.mahjongcraft.block.MahjongTable
import doublemoon.mahjongcraft.id
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.PushReaction

/** Block properties that already carry the block's registry id, which Minecraft requires before construction. */
private fun blockProperties(name: String): BlockBehaviour.Properties =
    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD).setId(ResourceKey.create(Registries.BLOCK, id(name)))

object BlockRegistry {

    val mahjongStool: MahjongStool = MahjongStool(
        blockProperties("mahjong_stool")
            .pushReaction(PushReaction.POPPED)
            .noOcclusion()
            .strength(0.3f)
            .sound(SoundType.WOOL)
    )

    val mahjongTable: MahjongTable = MahjongTable(
        blockProperties("mahjong_table")
            .pushReaction(PushReaction.IGNORE_ENTITY)
            .noOcclusion()
            .strength(0.7f)
            .sound(SoundType.WOOL)
    )

    val mahjongTileBlock = MahjongTileBlock(
        blockProperties("mahjong_tile_block")
            .pushReaction(PushReaction.POPPED)
            .noCollision()
            .noOcclusion()
            .instabreak()
            .noLootTable()
            .sound(SoundType.BAMBOO)
    )

    fun register() {
        Registry.register(BuiltInRegistries.BLOCK, id("mahjong_tile_block"), mahjongTileBlock)
        Registry.register(BuiltInRegistries.BLOCK, id("mahjong_stool"), mahjongStool)
        Registry.register(BuiltInRegistries.BLOCK, id("mahjong_table"), mahjongTable)
    }
}
