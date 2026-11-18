package doublemoon.mahjongcraft.entity

import doublemoon.mahjongcraft.blockentity.MahjongTableBlockEntity
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

abstract class GameEntity(
    type: EntityType<*>,
    world: Level,
) : Entity(type, world) {

    // 沒這行選不到實體
    override fun isPickable(): Boolean = !isRemoved

    /**
     * 遊戲所在的座標,
     * 不是遊戲產生的話會存入實體所在的 [blockPos]
     * */
    var gameBlockPos: BlockPos
        set(value) = entityData.set(GAME_BLOCK_POS, value)
        get() = entityData[GAME_BLOCK_POS]

    /**
     * 是否由遊戲產生
     * */
    open var isSpawnedByGame: Boolean
        set(value) = entityData.set(SPAWNED_BY_GAME, value)
        get() = entityData[SPAWNED_BY_GAME]

    /**
     * 當麻將桌 [MahjongTableBlockEntity] 不存在或者遊戲不是開始時自動移除
     * */
    open fun autoRemove() {
        if (!level().isClientSide && isSpawnedByGame && isAlive) {
            val blockEntity = level().getBlockEntity(gameBlockPos)
            if (blockEntity !is MahjongTableBlockEntity) {  //這方塊不是麻將桌
                remove(RemovalReason.DISCARDED)
            } else if (!blockEntity.playing) {  //這方塊是麻將桌, 沒有在遊玩狀態
                remove(RemovalReason.DISCARDED)
            }
        }
    }

    override fun tick() {
        super.tick()
        autoRemove()
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(GAME_BLOCK_POS, blockPosition())
        builder.define(SPAWNED_BY_GAME, false)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        gameBlockPos = NbtUtils.readBlockPos(nbt, "GameBlockPos").get()
        isSpawnedByGame = nbt.getBoolean("SpawnedByGame")
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        nbt.put("GameBlockPos", NbtUtils.writeBlockPos(gameBlockPos))
        nbt.putBoolean("SpawnedByGame", isSpawnedByGame)
    }

    companion object {
        private val GAME_BLOCK_POS: EntityDataAccessor<BlockPos> = SynchedEntityData.defineId(
            GameEntity::class.java,
            EntityDataSerializers.BLOCK_POS
        )
        private val SPAWNED_BY_GAME: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(GameEntity::class.java, EntityDataSerializers.BOOLEAN)
    }
}