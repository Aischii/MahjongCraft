package doublemoon.mahjongcraft.registry

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.id
import doublemoon.mahjongcraft.item.Dice
import doublemoon.mahjongcraft.item.MahjongScoringStick
import doublemoon.mahjongcraft.item.MahjongTile
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

//名稱重複, 另寫為 RiichiMahjongTile

/** Item properties that already carry the item's registry id, which Minecraft requires before construction. */
private fun itemProperties(name: String): Item.Properties =
    Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)))

private fun codeData(code: Int): CustomData = CustomData.of(CompoundTag().also { it.putInt("code", code) })

/** A stack of this item whose `code` (which tile or stick it is) is [code]. */
fun Item.withCode(code: Int): ItemStack = defaultInstance.also { it.set(DataComponents.CUSTOM_DATA, codeData(code)) }

/** Which tile or scoring stick this stack is; also drives the item model (see `mahjongcraft:code`). */
val ItemStack.code: Int
    get() = get(DataComponents.CUSTOM_DATA)?.copyTag()?.getIntOr("code", 0) ?: 0

//參考: https://fabricmc.net/wiki/tutorial:items_docs
object ItemRegistry {

    val dice: Dice = Dice(itemProperties("dice"))
    val mahjongTile: MahjongTile = MahjongTile(
        itemProperties("mahjong_tile").component(DataComponents.CUSTOM_DATA, codeData(0))
    )
    val mahjongScoringStick: MahjongScoringStick = MahjongScoringStick(
        itemProperties("mahjong_scoring_stick").component(DataComponents.CUSTOM_DATA, codeData(0))
    )
    /** A full set of 136 tiles; a table needs one loaded before a game can start (see [MahjongGameRules]). */
    val mahjongTileSet: Item = Item(itemProperties("mahjong_tile_set").stacksTo(1))
    /** Opens the Mahjong Guide screen (crafting, controls, rules, yaku) on the client. */
    val mahjongGuide: Item = Item(itemProperties("mahjong_book").stacksTo(1))
    //考慮加入牌尺

    private val itemGroup = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("group"))

    fun register() {
        // Items
        Registry.register(BuiltInRegistries.ITEM, id("dice"), dice)
        Registry.register(BuiltInRegistries.ITEM, id("mahjong_tile"), mahjongTile)
        Registry.register(BuiltInRegistries.ITEM, id("mahjong_scoring_stick"), mahjongScoringStick)
        Registry.register(BuiltInRegistries.ITEM, id("mahjong_tile_set"), mahjongTileSet)
        Registry.register(BuiltInRegistries.ITEM, id("mahjong_book"), mahjongGuide)

        // BlockItems
        val mahjongStoolBlockItem =
            BlockItem(BlockRegistry.mahjongStool, itemProperties("mahjong_stool").useBlockDescriptionPrefix())
        val mahjongTableBlockItem =
            BlockItem(BlockRegistry.mahjongTable, itemProperties("mahjong_table").useBlockDescriptionPrefix())
        Registry.register(BuiltInRegistries.ITEM, id("mahjong_stool"), mahjongStoolBlockItem)
        Registry.register(BuiltInRegistries.ITEM, id("mahjong_table"), mahjongTableBlockItem)

        // ItemGroup
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, itemGroup, FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.$MOD_ID.group"))
            .icon { mahjongTile.defaultInstance }
            .displayItems { _, entries ->
                entries.accept(dice)
                entries.accept(mahjongTile)
                entries.accept(mahjongScoringStick)
                entries.accept(mahjongStoolBlockItem)
                entries.accept(mahjongTableBlockItem)
                entries.accept(mahjongTileSet)
                entries.accept(mahjongGuide)
            }.build()
        )
    }
}
