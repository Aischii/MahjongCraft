package doublemoon.mahjongcraft.registry

//名稱重複, 另寫為 RiichiMahjongTile

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.id
import doublemoon.mahjongcraft.item.Dice
import doublemoon.mahjongcraft.item.MahjongScoringStick
import doublemoon.mahjongcraft.item.MahjongTile
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component


//參考: https://fabricmc.net/wiki/tutorial:items_docs
object ItemRegistry {

    val dice: Dice = Dice(Item.Properties())
    val mahjongTile: MahjongTile = MahjongTile(
        Item.Properties().component(
            DataComponents.CUSTOM_DATA,
            CustomData.of(CompoundTag().also { it.putInt("code", 0) })
        )
    )
    val mahjongScoringStick: MahjongScoringStick = MahjongScoringStick(
        Item.Properties().component(
            DataComponents.CUSTOM_DATA,
            CustomData.of(CompoundTag().also { it.putInt("code", 0) })
        )
    )
    //考慮加入牌尺

    private val itemGroup = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("group"))

    fun register() {
        // Items
        Registry.register(BuiltInRegistries.ITEM, id("dice"), dice)
        Registry.register(BuiltInRegistries.ITEM, id("mahjong_tile"), mahjongTile)
        Registry.register(BuiltInRegistries.ITEM, id("mahjong_scoring_stick"), mahjongScoringStick)

        // BlockItems
        val mahjongStoolBlockItem = BlockItem(BlockRegistry.mahjongStool, Item.Properties())
        val mahjongTableBlockItem = BlockItem(BlockRegistry.mahjongTable, Item.Properties())
        Registry.register(BuiltInRegistries.ITEM, id("mahjong_stool"), mahjongStoolBlockItem)
        Registry.register(BuiltInRegistries.ITEM, id("mahjong_table"), mahjongTableBlockItem)

        // ItemGroup
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, itemGroup, FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.$MOD_ID.group"))
            .icon { mahjongTile.defaultInstance }
            .displayItems { _, entries ->
                entries.accept(dice)
                entries.accept(mahjongTile)
                entries.accept(mahjongScoringStick)
                entries.accept(mahjongStoolBlockItem)
                entries.accept(mahjongTableBlockItem)
            }.build()
        )
    }
}