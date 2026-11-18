package doublemoon.mahjongcraft.registry

import doublemoon.mahjongcraft.blockentity.MahjongTableBlockEntity
import doublemoon.mahjongcraft.id
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry

object BlockEntityTypeRegistry {

    val mahjongTable: BlockEntityType<MahjongTableBlockEntity> =
        FabricBlockEntityTypeBuilder.create(::MahjongTableBlockEntity, BlockRegistry.mahjongTable).build()

    fun register() {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("mahjong_table"), mahjongTable)
    }

}