package doublemoon.mahjongcraft.registry

import doublemoon.mahjongcraft.block.MahjongStool
import doublemoon.mahjongcraft.block.MahjongTable
import doublemoon.mahjongcraft.id
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.material.PushReaction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.world.level.block.SoundType

object BlockRegistry {

    val mahjongStool: MahjongStool = MahjongStool(
        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)
            .pushReaction(PushReaction.DESTROY)
            .noOcclusion()
            .strength(0.3f)
            .sound(SoundType.WOOL)
    )

    val mahjongTable: MahjongTable = MahjongTable(
        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)
            .pushReaction(PushReaction.IGNORE)
            .noOcclusion()
            .strength(0.7f)
            .sound(SoundType.WOOL)
    )

    fun register() {
        Registry.register(BuiltInRegistries.BLOCK, id("mahjong_stool"), mahjongStool)
        Registry.register(BuiltInRegistries.BLOCK, id("mahjong_table"), mahjongTable)
    }
}