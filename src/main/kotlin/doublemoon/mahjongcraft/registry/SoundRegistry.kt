package doublemoon.mahjongcraft.registry

import doublemoon.mahjongcraft.id
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.sounds.SoundEvent

object SoundRegistry {

    //TODO 缺少了一些音效，待日後補上
    val chii = SoundEvent.createVariableRangeEvent(id("chii"))
    val pon = SoundEvent.createVariableRangeEvent(id("pon"))
    val kan = SoundEvent.createVariableRangeEvent(id("kan"))
    val riichi = SoundEvent.createVariableRangeEvent(id("riichi"))
    val ron = SoundEvent.createVariableRangeEvent(id("ron"))
    val tsumo = SoundEvent.createVariableRangeEvent(id("tsumo"))


    fun register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, id("chii"), chii)
        Registry.register(BuiltInRegistries.SOUND_EVENT, id("pon"), pon)
        Registry.register(BuiltInRegistries.SOUND_EVENT, id("kan"), kan)
        Registry.register(BuiltInRegistries.SOUND_EVENT, id("riichi"), riichi)
        Registry.register(BuiltInRegistries.SOUND_EVENT, id("ron"), ron)
        Registry.register(BuiltInRegistries.SOUND_EVENT, id("tsumo"), tsumo)
    }

}