package doublemoon.mahjongcraft.client.render

import com.mojang.serialization.MapCodec
import doublemoon.mahjongcraft.id
import doublemoon.mahjongcraft.registry.code
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty
import net.minecraft.world.entity.ItemOwner
import net.minecraft.world.item.ItemStack

/**
 * Item-model property `mahjongcraft:code`: which tile or scoring stick a stack is.
 *
 * The item definitions in `assets/mahjongcraft/items/` use it in a `range_dispatch`, with one
 * threshold per code, to pick the model. It replaces the pre-1.21.4 `code` model predicate.
 */
@Environment(EnvType.CLIENT)
object CodeItemModelProperty : RangeSelectItemModelProperty {
    private val MAP_CODEC: MapCodec<CodeItemModelProperty> = MapCodec.unit(CodeItemModelProperty)

    override fun get(itemStack: ItemStack, level: ClientLevel?, owner: ItemOwner?, seed: Int): Float =
        itemStack.code.toFloat()

    override fun type(): MapCodec<CodeItemModelProperty> = MAP_CODEC

    fun register() {
        RangeSelectItemModelProperties.ID_MAPPER.put(id("code"), MAP_CODEC)
    }
}
