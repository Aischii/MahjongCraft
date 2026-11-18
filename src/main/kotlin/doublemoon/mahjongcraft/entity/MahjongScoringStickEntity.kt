package doublemoon.mahjongcraft.entity

import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.level.storage.ValueInput
import doublemoon.mahjongcraft.game.mahjong.riichi.model.ScoringStick
import doublemoon.mahjongcraft.registry.EntityTypeRegistry
import doublemoon.mahjongcraft.registry.ItemRegistry
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.entity.EntityType
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.entity.player.Player
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.Level

class MahjongScoringStickEntity(
    type: EntityType<*> = EntityTypeRegistry.mahjongScoringStick,
    world: Level
) : GameEntity(type, world) {

    /**
     * 點棒編號 (0~3),
     * 控制點棒外觀
     * */
    var code: Int
        set(value) = entityData.set(CODE, value)
        get() = entityData[CODE]

    val scoringStick: ScoringStick
        get() = ScoringStick.entries[code]

    override fun canBeCollidedWith(other: Entity?): Boolean = true

    override fun interact(player: Player, hand: InteractionHand, location: Vec3): InteractionResult {
        if (!level().isClientSide) {
            player as ServerPlayer
            if (!isSpawnedByGame && player.isShiftKeyDown) {  //不是由遊戲產生, 是手動放置的一般實體
                //有 蹲下, 把棒子撿起來
                val itemStack = ItemRegistry.mahjongScoringStick.defaultInstance.also {
                    val nbt = CompoundTag()
                    nbt.putInt("code", code)
                    it.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
                }
                player.addItem(itemStack)
                playSound(SoundEvents.ITEM_PICKUP, 1f, 1f)
                remove(RemovalReason.DISCARDED)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(CODE, ScoringStick.P100.code)
    }

    override fun readAdditionalSaveData(nbt: ValueInput) {
        super.readAdditionalSaveData(nbt)
        code = nbt.getIntOr("Code", 0)
    }

    override fun addAdditionalSaveData(nbt: ValueOutput) {
        super.addAdditionalSaveData(nbt)
        nbt.putInt("Code", code)
    }

    companion object {
        const val MAHJONG_POINT_STICK_SCALE = 0.4f
        const val MAHJONG_POINT_STICK_WIDTH = 1f / 16 * 16 * MAHJONG_POINT_STICK_SCALE
        const val MAHJONG_POINT_STICK_HEIGHT = 1f / 16 * 0.5f * MAHJONG_POINT_STICK_SCALE
        const val MAHJONG_POINT_STICK_DEPTH = 1f / 16 * 2.5f * MAHJONG_POINT_STICK_SCALE
//        const val MAHJONG_POINT_STICK_SMALL_PADDING = 0.0025

        private val CODE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(MahjongScoringStickEntity::class.java, EntityDataSerializers.INT)
    }
}