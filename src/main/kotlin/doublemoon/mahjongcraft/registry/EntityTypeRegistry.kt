package doublemoon.mahjongcraft.registry

import doublemoon.mahjongcraft.entity.*
import doublemoon.mahjongcraft.id
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey

//參考: https://fabricmc.net/wiki/tutorial:entity
private fun entityKey(name: String): ResourceKey<EntityType<*>> = ResourceKey.create(Registries.ENTITY_TYPE, id(name))

object EntityTypeRegistry {

    val seat: EntityType<SeatEntity> = EntityType.Builder.of(::SeatEntity, MobCategory.MISC)
        .sized(0f, 0f)
        .build(entityKey("seat"))

    val dice: EntityType<DiceEntity> = EntityType.Builder.of(::DiceEntity, MobCategory.MISC)
        .sized(DiceEntity.DICE_WIDTH, DiceEntity.DICE_HEIGHT)
        .build(entityKey("dice"))

    val mahjongTile: EntityType<MahjongTileEntity> = EntityType.Builder.of(::MahjongTileEntity, MobCategory.MISC)
        .sized(MahjongTileEntity.MAHJONG_TILE_WIDTH, MahjongTileEntity.MAHJONG_TILE_HEIGHT)
        .build(entityKey("mahjong_tile"))

    val mahjongScoringStick: EntityType<MahjongScoringStickEntity> =
        EntityType.Builder.of(::MahjongScoringStickEntity, MobCategory.MISC)
            .sized(
                MahjongScoringStickEntity.MAHJONG_POINT_STICK_WIDTH,
                MahjongScoringStickEntity.MAHJONG_POINT_STICK_HEIGHT
            ).build(entityKey("mahjong_scoring_stick"))

    val mahjongBot: EntityType<MahjongBotEntity> = EntityType.Builder.of(::MahjongBotEntity, MobCategory.MISC)
        .sized(MahjongBotEntity.MAHJONG_BOT_WIDTH, MahjongBotEntity.MAHJONG_BOT_HEIGHT)
        .build(entityKey("mahjong_bot"))

    fun register() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("seat"), seat)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("dice"), dice)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("mahjong_bot"), mahjongBot)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("mahjong_tile"), mahjongTile)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("mahjong_scoring_stick"), mahjongScoringStick)
    }

}