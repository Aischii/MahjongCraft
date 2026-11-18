package doublemoon.mahjongcraft.entity

import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.level.storage.ValueInput
import doublemoon.mahjongcraft.blockentity.MahjongTableBlockEntity
import doublemoon.mahjongcraft.game.GameManager
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongBoard
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongGame
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongGameBehavior
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import doublemoon.mahjongcraft.game.mahjong.riichi.player.MahjongPlayer
import doublemoon.mahjongcraft.network.mahjong_tile_code.MahjongTileCodePayload
import doublemoon.mahjongcraft.network.mahjong_tile_code.MahjongTileCodePayloadListener
import doublemoon.mahjongcraft.network.sendPayloadToServer
import doublemoon.mahjongcraft.registry.EntityTypeRegistry
import doublemoon.mahjongcraft.registry.ItemRegistry
import doublemoon.mahjongcraft.scheduler.client.OptionalBehaviorHandler
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.Pose
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
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import org.mahjong4j.tile.Tile


//方便用
fun List<MahjongTileEntity>.toMahjongTileList(): List<MahjongTile> = this.map { it.mahjongTile }

/**
 * 麻將牌實體
 * */
class MahjongTileEntity(
    type: EntityType<*> = EntityTypeRegistry.mahjongTile,
    world: Level,
) : GameEntity(type, world) {

    constructor(world: Level, code: Int) : this(world = world) {
        this.code = code
    }

    /**
     * 專門給 [MahjongBoard] 產生實體用
     * */
    constructor(
        world: Level,
        code: Int,
        gameBlockPos: BlockPos,
        isSpawnedByGame: Boolean,
        inGameTilePosition: TilePosition,
        gamePlayers: List<String>,
        canSpectate: Boolean,
        facing: TileFacing,
    ) : this(world = world) {
        this.gameBlockPos = gameBlockPos
        this.isSpawnedByGame = isSpawnedByGame
        this.code = code
        this.inGameTilePosition = inGameTilePosition
        this.gamePlayers = gamePlayers
        this.canSpectate = canSpectate
        this.facing = facing
    }

    /**
     * 暫時存取 由遊戲產生且在伺服端中 的麻將牌的 code,
     * 不能用 [dataTracker] 直接更新, 這樣只能一次對所有玩家, 沒辦法針對個體
     * */
    private var spawnedByGameServerSideCode: Int = MahjongTile.UNKNOWN.code

    /**
     * 暫時存取 由遊戲產生且在客戶端中 的麻將牌的 code,不是每個玩家都一樣, 透過數據包對 [code] 操作時進行同步
     * */
    private var spawnedByGameClientSideCode: Int = MahjongTile.UNKNOWN.code

    /**
     * 麻將牌編號,
     * 控制麻將牌外觀
     * */
    var code: Int
        set(value) {
            if (isSpawnedByGame) {
                if (!level().isClientSide) spawnedByGameServerSideCode = value
                else {
                    if (value != MahjongTile.UNKNOWN.code) mahjongTable?.calculateRemainingTiles(value)
                    spawnedByGameClientSideCode = value
                }
            } else {
                entityData.set(CODE, value)
            }
        }
        get() = if (isSpawnedByGame) {
            if (!level().isClientSide) spawnedByGameServerSideCode
            else spawnedByGameClientSideCode
        } else {
            entityData[CODE]
        }

    /**
     * 這張牌主人的 UUID "字串"
     * 如果這張牌是遊戲產生的, 只有主人跟其他非遊戲中玩家才看得見,
     * 字串為空白表示沒有人能看見(還在牌山中)
     * */
    var ownerUUID: String
        set(value) = entityData.set(OWNER_UUID, value)
        get() = entityData[OWNER_UUID]

    /**
     * 麻將遊戲中的玩家的 UUID 字串列表,
     * 利用 [Json.encodeToString] 編譯成 [Json] 格式的字串, 再進行儲存
     * 再利用 [Json.decodeFromString] 解譯成 UUID 字串
     * */
    var gamePlayers: List<String>
        set(value) = entityData.set(GAME_PLAYERS, Json.encodeToString(value))
        get() = Json.decodeFromString(entityData[GAME_PLAYERS])

    /**
     * 面朝的方向,水平或垂直
     * 改變的時候會影響 hitbox,
     * 也會影響渲染的時候, 牌的渲染位置
     * */
    var facing: TileFacing
        set(value) = entityData.set(FACING, Json.encodeToString(value))
        get() = Json.decodeFromString(entityData[FACING])

    /**
     * 這張牌所在的麻將遊戲是否允許旁觀
     * */
    var canSpectate: Boolean
        set(value) = entityData.set(GAME_CAN_SPECTATE, value)
        get() = entityData[GAME_CAN_SPECTATE]

    /**
     * 這張牌在遊戲中的位置
     * */
    var inGameTilePosition: TilePosition
        set(value) = entityData.set(GAME_TILE_POSITION, Json.encodeToString(value))
        get() = Json.decodeFromString(entityData[GAME_TILE_POSITION])

    /**
     * 取得對應 [code] 的 [MahjongTile]
     * */
    val mahjongTile: MahjongTile
        get() = MahjongTile.entries[code]

    /**
     * 取得對應 [code] 的 [Tile]
     * */
    val mahjong4jTile: Tile
        get() = mahjongTile.mahjong4jTile

    /**
     * 這張牌如果是由遊戲產生的,
     * 這個會指到遊戲的 [MahjongTableBlockEntity] 上
     * */
    val mahjongTable: MahjongTableBlockEntity?
        get() = if (isSpawnedByGame) level().getBlockEntity(gameBlockPos) as MahjongTableBlockEntity? else null

    /**
     * 伺服端的遊戲專用,
     * 用來取得 [player] 應該看到這張麻將牌的 code, 通常在 [MahjongTileCodePayloadListener] 使用
     * */
    fun getCodeForPlayer(player: ServerPlayer): Int {
        if (level().isClientSide) throw IllegalStateException("Cannot get code from client side")
        return if (!isSpawnedByGame) {  // 不是遊戲生成的
            throw IllegalStateException("This MahjongTileEntity must be spawned by game")
        } else when (inGameTilePosition) { // 是遊戲生成的
            TilePosition.WALL -> MahjongTile.UNKNOWN.code // 是牌山的牌
            TilePosition.HAND -> // 是手牌
                when {
                    player.stringUUID == ownerUUID -> code  // 這張牌是這玩家的
                    player.stringUUID in gamePlayers -> MahjongTile.UNKNOWN.code  //如果是這遊戲中的其他玩家
                    canSpectate -> code   //開放觀戰
                    else -> MahjongTile.UNKNOWN.code     // 不開放觀戰
                }
            else -> code  // 是其他剩下的牌 (ex: 副露, 或者已經打出去的牌)
        }
    }

    override fun onSyncedDataUpdated(data: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(data)

        // 確保在 FACING 更新時, boundingBox 馬上更新
        if (FACING == data) boundingBox = makeBoundingBox()

        // 以下在所有 tracked 的資料改變的時候都會執行, 以便盡可能拿到最新與正確的 tile code
        if (!level().isClientSide || !isSpawnedByGame) return // 只限定在客戶端且必須是遊戲產生的牌
        sendPayloadToServer(
            payload = MahjongTileCodePayload(id = id)
        )
    }

    //實體碰撞
    override fun canBeCollidedWith(other: Entity?): Boolean = true

    override fun getDimensions(pose: Pose): EntityDimensions =
        if (facing == TileFacing.HORIZONTAL) {
            super.getDimensions(pose)
        } else {
            EntityDimensions.fixed(MAHJONG_TILE_HEIGHT, MAHJONG_TILE_DEPTH)
        }

    //玩家與實體互動 (右鍵)
    override fun interact(player: Player, hand: InteractionHand, location: Vec3): InteractionResult {
        if (!level().isClientSide) {
            player as ServerPlayer
            if (!isSpawnedByGame) {  // 不是由遊戲產生, 是手動放置的一般實體
                if (player.isShiftKeyDown) {  // 有蹲下, 把牌撿起來
                    val itemStack = ItemRegistry.mahjongTile.defaultInstance.also {
                        val nbt = CompoundTag()
                        nbt.putInt("code", code)
                        it.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
                    }
                    player.addItem(itemStack)
                    playSound(SoundEvents.ITEM_PICKUP, 1f, 1f)
                    remove(RemovalReason.DISCARDED)
                } else {  // 沒蹲下, 改變牌立著的方向
                    facing = facing.next
                }
            } else if (inGameTilePosition == TilePosition.HAND && ownerUUID == player.stringUUID) {
                // 由遊戲產生的, 自己手牌以外的牌不會執行任何動作
                val game = GameManager.getGame<MahjongGame>(player) ?: return InteractionResult.FAIL
                val mjPlayer = game.getPlayer(player) as MahjongPlayer? ?: return InteractionResult.FAIL
                if (MahjongGameBehavior.DISCARD in mjPlayer.waitingBehavior) { //客戶端正在等待丟牌
                    if (this.mahjongTile !in mjPlayer.cannotDiscardTiles) { //這張牌是可以丟的
                        mjPlayer.behaviorResult = MahjongGameBehavior.DISCARD to "$code" //丟牌
                    }
                }
            }
        } else {
            if (OptionalBehaviorHandler.waiting) OptionalBehaviorHandler.setScreen()
        }
        return InteractionResult.SUCCESS
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(CODE, MahjongTile.UNKNOWN.code)
        builder.define(OWNER_UUID, "")
        builder.define(GAME_PLAYERS, Json.encodeToString(mutableListOf<String>()))
        builder.define(FACING, Json.encodeToString(TileFacing.HORIZONTAL))
        builder.define(GAME_CAN_SPECTATE, true)
        builder.define(GAME_TILE_POSITION, Json.encodeToString(TilePosition.WALL))
    }

    override fun readAdditionalSaveData(nbt: ValueInput) {
        super.readAdditionalSaveData(nbt)
        code = nbt.getIntOr("Code", 0)
        ownerUUID = nbt.getStringOr("OwnerUUID", "")
        gamePlayers = Json.decodeFromString(nbt.getStringOr("GamePlayers", ""))
        facing = Json.decodeFromString(nbt.getStringOr("Facing", ""))
        canSpectate = nbt.getBooleanOr("GameCanSpectate", false)
        inGameTilePosition = Json.decodeFromString(nbt.getStringOr("GameTilePosition", ""))
    }

    override fun addAdditionalSaveData(nbt: ValueOutput) {
        super.addAdditionalSaveData(nbt)
        nbt.putInt("Code", code)
        nbt.putString("OwnerUUID", ownerUUID)
        nbt.putString("GamePlayers", Json.encodeToString(gamePlayers))
        nbt.putString("Facing", Json.encodeToString(facing))
        nbt.putBoolean("GameCanSpectate", canSpectate)
        nbt.putString("GameTilePosition", Json.encodeToString(inGameTilePosition))
    }

    companion object {
        //麻將牌的寬高深和比例
        const val MAHJONG_TILE_SCALE = 0.15f
        const val MAHJONG_TILE_WIDTH = 1f / 16 * 12 * MAHJONG_TILE_SCALE
        const val MAHJONG_TILE_HEIGHT = 1f / 16 * 16 * MAHJONG_TILE_SCALE
        const val MAHJONG_TILE_DEPTH = 1f / 16 * 8 * MAHJONG_TILE_SCALE
        const val MAHJONG_TILE_SMALL_PADDING = 0.0025

        private val CODE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(MahjongTileEntity::class.java, EntityDataSerializers.INT)

        private val OWNER_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(MahjongTileEntity::class.java, EntityDataSerializers.STRING)

        private val GAME_PLAYERS: EntityDataAccessor<String> =
            SynchedEntityData.defineId(MahjongTileEntity::class.java, EntityDataSerializers.STRING)

        private val FACING: EntityDataAccessor<String> =
            SynchedEntityData.defineId(MahjongTileEntity::class.java, EntityDataSerializers.STRING)

        private val GAME_CAN_SPECTATE: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(MahjongTileEntity::class.java, EntityDataSerializers.BOOLEAN)

        private val GAME_TILE_POSITION: EntityDataAccessor<String> =
            SynchedEntityData.defineId(MahjongTileEntity::class.java, EntityDataSerializers.STRING)
    }
}

/**
 * 麻將牌面向的方向, 會牽涉到 HitBox, 以及實體的渲染
 *
 * @param angleForDegreesQuaternionFromPositiveX 渲染時選轉的角度
 * */
enum class TileFacing(
    val angleForDegreesQuaternionFromPositiveX: Float,
) {
    HORIZONTAL(0f),
    UP(90f),
    DOWN(-90f);

    val next: TileFacing
        get() = entries[(this.ordinal + 1) % entries.size]
}


/**
 * 麻將牌的位置
 *
 * 牌山, 手牌, 或者其他
 * */
enum class TilePosition {
    WALL, HAND, OTHER
}