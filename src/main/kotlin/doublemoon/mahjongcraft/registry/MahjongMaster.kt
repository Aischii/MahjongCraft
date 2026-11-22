package doublemoon.mahjongcraft.registry

import com.google.common.collect.ImmutableSet
import doublemoon.mahjongcraft.block.MahjongTable
import doublemoon.mahjongcraft.block.enums.MahjongTablePart
import doublemoon.mahjongcraft.id
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.fabricmc.fabric.api.`object`.builder.v1.world.poi.PoiHelper
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.npc.villager.VillagerProfession
import net.minecraft.world.item.trading.TradeSet

object MahjongMaster {
    fun register() {
        val name = id("mahjong_master")
        val centerStates = BlockRegistry.mahjongTable.stateDefinition.possibleStates.filter {
            it.getValue(MahjongTable.PART) == MahjongTablePart.BOTTOM_CENTER
        }
        PoiHelper.register(name, 1, 1, centerStates)
        val jobSite = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, name)
        val trades = Int2ObjectOpenHashMap<ResourceKey<TradeSet>>()
        for (level in 1..5) {
            trades.put(level, ResourceKey.create(Registries.TRADE_SET, id("mahjong_master/level_$level")))
        }
        val profession = VillagerProfession(
            Component.translatable("entity.mahjongcraft.villager.mahjong_master"),
            { it.`is`(jobSite) }, { it.`is`(jobSite) },
            ImmutableSet.of(), ImmutableSet.of(), SoundEvents.BAMBOO_WOOD_PLACE, trades,
        )
        Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, name, profession)
    }
}
