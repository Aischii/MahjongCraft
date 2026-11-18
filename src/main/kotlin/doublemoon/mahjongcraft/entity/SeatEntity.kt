package doublemoon.mahjongcraft.entity

import doublemoon.mahjongcraft.registry.EntityTypeRegistry
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level

/**
 * 這只是一個實體, 並沒有實際的模型, 主要是用來製造出椅子的效果,
 *
 * @param sourceBlock [SeatEntity] 所在的那格 [BlockPos], 用來判斷椅子有沒有被摧毀, 這格方塊如果消失了, [SeatEntity] 也會消失
 * @param sitOffsetY 玩家坐在椅子上的偏移量
 * @param stopSitOffsetY 玩家離開椅子的偏移量 (會往椅子上方傳送)
 * */
class SeatEntity(
    type: EntityType<SeatEntity> = EntityTypeRegistry.seat,
    world: Level,
    private var sourceBlock: BlockPos? = null,
    private val sitOffsetY: Double = 0.3,
    private val stopSitOffsetY: Double = 0.8,
) : Entity(type, world) {

    init {
        isInvisible = true  //將椅子設為隱形的實體
        sourceBlock?.apply { setPos(x + 0.5, y + sitOffsetY, z + 0.5) }
    }

    override fun tick() {
        super.tick()
        if (sourceBlock == null) sourceBlock = blockPosition()
        if (!level().isClientSide && (passengers.isEmpty() || level().isEmptyBlock(sourceBlock))) remove(RemovalReason.DISCARDED)
    }

    override fun getOnPos(offset: Float): BlockPos {
        return super.getOnPos(0.0f)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder?) {}

    override fun readAdditionalSaveData(nbt: CompoundTag?) {}

    override fun addAdditionalSaveData(nbt: CompoundTag?) {}


    override fun getDismountLocationForPassenger(passenger: LivingEntity): Vec3 =
        Vec3(blockPosition().x + 0.5, blockPosition().y + stopSitOffsetY, blockPosition().z + 0.5)

    companion object {
        /**
         * 檢查 [world] 的位置 [pos] 上能不能生成 [SeatEntity] (即有沒有相同的 [SeatEntity] 實體,以及高度足夠),
         * 必須在伺服端調用
         *
         * @param pos 要生成 [SeatEntity] 的位置
         * @param height 計算用的高度, 除了 [pos] 往上開始算到 [height] 空間足夠的話就表示高度可以, 預設為 2
         * @param checkEntity 檢查 [pos] 上否已經有 [SeatEntity] 存在 (即檢查是否有人已經坐在這)
         * */
        fun canSpawnAt(
            world: ServerLevel,
            pos: BlockPos,
            height: Int = 2,
            checkEntity: Boolean = true,
        ): Boolean {
            val heightEnough =
                if (height <= 0) true  //不應該出現負數的情況
                else (1..height).all {  //檢查上方是否有足夠空間
                    val blockState = world.getBlockState(pos.relative(Direction.UP, it))
                    blockState.getCollisionShape(world, pos).isEmpty
                }

            return if (checkEntity) {
                val seatEntitiesAtThisPos =
                    world.getEntities(EntityTypeRegistry.seat) { it.blockPosition() == pos && it.isAlive }
                seatEntitiesAtThisPos.isEmpty() && heightEnough //同一格不能有其他 SeatEntity 實體 & 高度足夠
            } else {
                heightEnough
            }
        }

        /**
         * 讓 [entity] 坐在 [pos] 的位置上,
         * 使用前請使用 [canSpawnAt] 檢查,
         * 必須在伺服端調用
         * */
        fun spawnAt(
            world: ServerLevel,
            pos: BlockPos,
            entity: Entity,
            sitOffsetY: Double = 0.3,
            stopSitOffsetY: Double = 0.8,
        ) {
            val seatEntity = SeatEntity(
                world = world,
                sourceBlock = pos,
                sitOffsetY = sitOffsetY,
                stopSitOffsetY = stopSitOffsetY
            )
            world.addFreshEntity(seatEntity)
            entity.startRiding(seatEntity, false)
        }

        /**
         * 基本與 [spawnAt] 一樣,
         * 會強制讓原本坐在 [pos] 上的 [SeatEntity] 上的實體下來,
         * 必須在伺服端調用
         * */
        fun forceSpawnAt(
            world: ServerLevel,
            pos: BlockPos,
            entity: Entity,
            sitOffsetY: Double = 0.3,
            stopSitOffsetY: Double = 0.8,
        ) {
            val seatEntitiesAtThisPos = world.getEntities(EntityTypeRegistry.seat) {  //取得在 pos 上相同的實體
                it.blockPosition() == pos && it.isAlive
            }
            seatEntitiesAtThisPos.forEach { it.remove(RemovalReason.DISCARDED) }
            spawnAt(world, pos, entity, sitOffsetY, stopSitOffsetY)
        }
    }
}