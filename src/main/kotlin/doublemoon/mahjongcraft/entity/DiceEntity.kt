package doublemoon.mahjongcraft.entity

import doublemoon.mahjongcraft.registry.EntityTypeRegistry
import doublemoon.mahjongcraft.registry.ItemRegistry
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.entity.player.Player
import net.minecraft.nbt.CompoundTag
import net.minecraft.tags.FluidTags
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import kotlin.math.min

/**
 * 骰子實體
 * */
class DiceEntity(
    type: EntityType<DiceEntity> = EntityTypeRegistry.dice,
    world: Level,
    pos: Vec3? = null,
    yaw: Float? = null,
) : GameEntity(type, world) {

    /**
     * 骰子當前的點數 (與擺的方向有關)
     * */
    var point: DicePoint
        set(value) = entityData.set(POINT, value.name)
        get() = DicePoint.valueOf(entityData[POINT])

    val rolling: Boolean
        get() = !onGround()

    init {
        pos?.let { setPos(it.x, it.y, it.z) }
        yaw?.let { setRot(it, 0f) }
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (!player.level().isClientSide) {
            player as ServerPlayer
            if (!isSpawnedByGame && player.isShiftKeyDown) { //不是由遊戲產生, 是手動放置的一般實體
                //蹲下才能把骰子撿起來
                player.addItem(ItemRegistry.dice.defaultInstance)
                playSound(SoundEvents.ITEM_PICKUP, 1f, 1f)
                remove(RemovalReason.DISCARDED)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun tick() {
        super.tick()
        xo = x
        yo = y
        zo = z
        if (isEyeInFluid(FluidTags.WATER)) {
            setUnderwaterMovement()
        } else if (!this.isNoGravity()) {
            deltaMovement = deltaMovement.add(0.0, -0.03, 0.0)
        }
        if (level().getFluidState(blockPosition()).`is`(FluidTags.LAVA)) {
            setDeltaMovement(
                random.nextDouble() - random.nextDouble() * 0.2,
                0.2,
                random.nextDouble() - random.nextDouble() * 0.2
            )
            playSound(SoundEvents.GENERIC_BURN, 0.4f, 2.0f + random.nextFloat() * 0.4f)
        }
        if (!level().noCollision(boundingBox)) {
            moveTowardsClosestSpace(x, (boundingBox.minY + boundingBox.maxY) / 2.0, z)
        }
        move(MoverType.SELF, deltaMovement)
        var f = 0.98f
        if (onGround()) {
            val pos = BlockPos(x.toInt(), (y - 1.0).toInt(), z.toInt())
            f = level().getBlockState(pos).block.friction * 0.98f
        }
        deltaMovement = deltaMovement.multiply(f.toDouble(), 0.98, f.toDouble())
        if (onGround()) {
            deltaMovement = deltaMovement.multiply(1.0, -0.9, 1.0)
        }
        ++tickCount
        if (rolling) { //骰子在滾動
            point = DicePoint.random()
        }
    }

    private fun setUnderwaterMovement() {
        val vec3d = deltaMovement
        setDeltaMovement(
            vec3d.x * 0.99,
            min(vec3d.y + 5.0E-4, 0.06),
            vec3d.z * 0.99
        )
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(POINT, DicePoint.random().name)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)
        val pointStr = nbt.get("Point")?.asString ?: DicePoint.random().name //避免指令召喚的骰子沒有 nbt 導致無法召喚
        point = DicePoint.valueOf(pointStr)
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)
        nbt.putString("Point", point.name)
    }

    companion object {
        //麻將牌的寬高深和比例
        const val DICE_SCALE = 0.2f
        const val DICE_WIDTH = 1f / 16 * 10 * DICE_SCALE
        const val DICE_HEIGHT = 1f / 16 * 10 * DICE_SCALE

        val POINT: EntityDataAccessor<String> =
            SynchedEntityData.defineId(DiceEntity::class.java, EntityDataSerializers.STRING)
    }
}

enum class DicePoint(
    val xpRotDegrees: Float,
    val ypRotDegrees: Float,
    val value: Int,
) {
    ONE(0f, 0f, 1),
    TWO(90f, 0f, 2),
    THREE(90f, 90f, 3),
    FOUR(-90f, 0f, 4),
    FIVE(90f, -90f, 5),
    SIX(-180f, 0f, 6);

    val next: DicePoint
        get() = entries[(this.ordinal + 1) % entries.size]

    companion object {
        fun random(): DicePoint = entries[(0..entries.toTypedArray().lastIndex).random()]
    }
}
