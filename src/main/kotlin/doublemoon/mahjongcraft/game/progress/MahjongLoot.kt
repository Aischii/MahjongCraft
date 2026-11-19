package doublemoon.mahjongcraft.game.progress

import doublemoon.mahjongcraft.registry.ItemRegistry
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.minecraft.world.level.storage.loot.BuiltInLootTables
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.core.Holder
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator

/** Village house chests sometimes hold dice, scoring sticks or a Mahjong Guide (about one chest in five). */
object MahjongLoot {
    private val villageHouses = setOf(
        BuiltInLootTables.VILLAGE_PLAINS_HOUSE,
        BuiltInLootTables.VILLAGE_DESERT_HOUSE,
        BuiltInLootTables.VILLAGE_TAIGA_HOUSE,
        BuiltInLootTables.VILLAGE_SNOWY_HOUSE,
        BuiltInLootTables.VILLAGE_SAVANNA_HOUSE,
    )

    private fun exactly(n: Int): Holder<ContextIntProvider> = Holder.direct(ConstantValue(n))
    private fun between(min: Int, max: Int): Holder<ContextIntProvider> = Holder.direct(UniformGenerator(exactly(min), exactly(max)))

    fun register() {
        LootTableEvents.MODIFY.register { key, builder, source, _ ->
            if (!source.isBuiltin || key !in villageHouses) return@register
            builder.withPool(
                LootPool.lootPool()
                    .setRolls(exactly(1))
                    .add(EmptyLootItem.emptyItem().setWeight(80))
                    .add(LootItem.lootTableItem(ItemRegistry.dice).setWeight(8)
                        .apply(SetItemCountFunction.setCount(between(1, 2))))
                    .add(LootItem.lootTableItem(ItemRegistry.mahjongScoringStick).setWeight(6)
                        .apply(SetItemCountFunction.setCount(between(1, 4))))
                    .add(LootItem.lootTableItem(ItemRegistry.mahjongGuide).setWeight(6))
            )
        }
    }
}
