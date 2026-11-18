package doublemoon.mahjongcraft.entity

import doublemoon.mahjongcraft.blockentity.MahjongTableBlockEntity
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_HEIGHT
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_SCALE
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_WIDTH
import doublemoon.mahjongcraft.game.mahjong.riichi.player.MahjongBot
import doublemoon.mahjongcraft.registry.EntityTypeRegistry
import net.minecraft.world.entity.EntityType
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level

/**
 * 麻將機器人 [MahjongBot] 的實體,
 * (目前就只是一個比較大的麻將牌)
 * */
class MahjongBotEntity(
    type: EntityType<*> = EntityTypeRegistry.mahjongBot,
    world: Level
) : GameEntity(type, world) {

    /**
     * 暫時與 [MahjongTileEntity] 一樣, 都是顯示麻將牌外觀的
     * */
    var code: Int
        set(value) = entityData.set(CODE, value)
        get() = entityData[CODE]

    init {
        isInvulnerable = true
    }

    //可以看到實體才有實體碰撞
    override fun canBeCollidedWith(): Boolean = !isInvisible && !isRemoved

    override fun isPickable(): Boolean = canBeCollidedWith()

    //目前機器人實體在麻將桌按下 加入機器人 的時候 (即遊戲還沒開始的時候) 就存在了,
    override fun autoRemove() {
        if (!level().isClientSide && isSpawnedByGame && isAlive) {
            val blockEntity = level().getBlockEntity(gameBlockPos)
            if (blockEntity !is MahjongTableBlockEntity) {  //這方塊不是麻將桌
                remove(RemovalReason.DISCARDED)
            }
        }
    }

    override fun shouldShowName(): Boolean = true

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(CODE, 0)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)
        code = nbt.getInt("Code")
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)
        nbt.putInt("Code", code)
    }

    companion object {
        const val MAHJONG_BOT_WIDTH = MAHJONG_TILE_WIDTH / MAHJONG_TILE_SCALE
        const val MAHJONG_BOT_HEIGHT = MAHJONG_TILE_HEIGHT / MAHJONG_TILE_SCALE

        private val CODE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(MahjongBotEntity::class.java, EntityDataSerializers.INT)
    }
}