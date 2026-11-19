package doublemoon.mahjongcraft.blockentity

import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.level.storage.ValueInput
import doublemoon.mahjongcraft.block.MahjongTable
import doublemoon.mahjongcraft.block.enums.MahjongTablePart
import doublemoon.mahjongcraft.client.gui.screen.MahjongTableGui
import doublemoon.mahjongcraft.client.gui.screen.MahjongTableWaitingScreen
import doublemoon.mahjongcraft.entity.MahjongTileEntity
import doublemoon.mahjongcraft.game.GameManager
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongGame
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongRound
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongRule
import doublemoon.mahjongcraft.network.mahjong_table.MahjongTablePayloadListener
import doublemoon.mahjongcraft.registry.BlockEntityTypeRegistry
import doublemoon.mahjongcraft.registry.ItemRegistry
import net.minecraft.world.Containers
import net.minecraft.world.entity.player.Player
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.core.HolderLookup
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import org.mahjong4j.tile.Tile
import java.util.concurrent.ConcurrentHashMap

class MahjongTableBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(BlockEntityTypeRegistry.mahjongTable, pos, state) {
    private var gameInitialized = false
    val players = arrayListOf("", "", "", "") //以玩家的 stringUUID 儲存, 先以 4 個空字串儲存, (空字串表示空位)
    val playerEntityNames = arrayListOf("", "", "", "") //以實體的 entityName 儲存
    val bots = arrayListOf(false, false, false, false) //這個玩家是否是機器人
    val ready = arrayListOf(false, false, false, false) //這個玩家是否準備了
    var rule = MahjongRule() //這是為了能讓 rule 能夠儲存在桌子上, 直到開啟麻將桌時套用到新遊戲上
    var playing = false
    var round = MahjongRound() //當前的回合
    var seat = arrayListOf("", "", "", "") //座位上的玩家的 stringUUID, 按照遊戲開始的座位順序編排
    var dealer = "" //莊家的 stringUUID
    var points = arrayListOf(0, 0, 0, 0) //玩家的點數, 排列順序比照 seat

    /** A Mahjong Tile Set has been loaded into this table; it drops when the table is broken. */
    var tileSetLoaded = false
    /** Mirrors the `mahjongcraft:require_tile_set` game rule so clients can show it; set by the server. */
    var tileSetRequired = true

    /** Whether [player] may start a game here: a set is loaded, it isn't required, or they are in creative. */
    fun canStart(player: Player): Boolean = tileSetLoaded || !tileSetRequired || player.hasInfiniteMaterials()

    override fun preRemoveSideEffects(pos: BlockPos, state: BlockState) {
        super.preRemoveSideEffects(pos, state)
        val level = level ?: return
        if (tileSetLoaded && !level.isClientSide) {
            tileSetLoaded = false
            Containers.dropItemStack(level, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, ItemRegistry.mahjongTileSet.defaultInstance)
        }
    }

    /**
     * 剩餘的牌, index 為 [Tile.code], value 為剩下的張數,
     * 這裡存的資料是只有面向客戶端的, 每個人看到剩下的張數都不一樣, 要經過計算才能確定
     * */
    val remainingTiles = ConcurrentHashMap<Int, Int>().apply {
        Tile.entries.forEach { this[it.code] = 0 }
    }

    /**
     * 從桌子中心取得牌桌大小內所有 牌的實體,
     * 經過計算後修正 [remainingTiles]
     * */
    @Environment(EnvType.CLIENT)
    fun calculateRemainingTiles(code: Int) {
        val tableCenter = with(this.blockPos) { Vec3(x + 0.5, y + 1.0, z + 0.5) }
        val tiles = level?.getEntitiesOfClass(
            MahjongTileEntity::class.java,
            AABB.ofSize(tableCenter, 3.0, 2.0, 3.0)
        ) { it.isSpawnedByGame && it.gameBlockPos == this.blockPos && it.mahjongTile.code == code } ?: return
        remainingTiles[code] = 4 - tiles.size
    }

    override fun setChanged() {
        super.setChanged()
        //對伺服端要同步的方塊實體 markDirty() 就好
        if (this.hasLevel() && !this.level!!.isClientSide) {
            (level as ServerLevel).chunkSource.blockChanged(getBlockPos())
        }
    }

    override fun loadAdditional(nbt: ValueInput) {
        with(nbt) {
            repeat(4) {
                players[it] = getStringOr("PlayerStringUUID$it", "")
                playerEntityNames[it] = getStringOr("PlayerEntityName$it", "")
                bots[it] = getBooleanOr("Bot$it", false)
                ready[it] = getBooleanOr("PlayerReady$it", false)
                seat[it] = getStringOr("Seat$it", "")
                points[it] = getIntOr("Point$it", 0)
            }
            dealer = getStringOr("Dealer", "")
            rule = MahjongRule.fromJsonString(getStringOr("Rule", ""))
            playing = getBooleanOr("Playing", false)
            tileSetLoaded = getBooleanOr("TileSetLoaded", false)
            tileSetRequired = getBooleanOr("TileSetRequired", true)
            round = Json.decodeFromString(getStringOr("Round", ""))
        }
        level?.isClientSide?.let { isClient ->
            if (isClient) { //當有同步 tag 的情況出現
                val screen = Minecraft.getInstance().gui.screen()
                if (screen is MahjongTableWaitingScreen && (screen.description as MahjongTableGui).mahjongTable == this) {
                    if (!playing) {  //遊戲還沒開始, 刷新開著這個麻將桌視窗的玩家的視窗
                        screen.refresh()
                    } else {  //遊戲開始就關閉遊戲視窗
                        screen.onClose()
                    }
                }
            }
        }
    }

    override fun saveAdditional(nbt: ValueOutput) {
        if (blockState.getValue(MahjongTable.PART) == MahjongTablePart.BOTTOM_CENTER) {
            with(nbt) {
                repeat(4) {
                    putString("PlayerStringUUID$it", players[it])
                    putString("PlayerEntityName$it", playerEntityNames[it])
                    putBoolean("Bot$it", bots[it])
                    putBoolean("PlayerReady$it", ready[it])
                    putString("Seat$it", seat[it])
                    putInt("Point$it", points[it])
                }
                putString("Dealer", dealer)
                putString("Rule", rule.toJsonString())
                putBoolean("Playing", playing)
                putBoolean("TileSetLoaded", tileSetLoaded)
                putBoolean("TileSetRequired", tileSetRequired)
                putString("Round", Json.encodeToString(round))
            }
        }
    }

    override fun getUpdateTag(registryLookup: HolderLookup.Provider): CompoundTag =
        saveCustomOnly(registryLookup)

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

    companion object {
        fun tick(world: Level, pos: BlockPos, blockEntity: MahjongTableBlockEntity) {
            if (!world.isClientSide && !blockEntity.gameInitialized) {
                MahjongTablePayloadListener.syncBlockEntityDataWithGame(
                    blockEntity = blockEntity,
                    game = GameManager.getGameOrDefault(
                        world = world as ServerLevel,
                        pos = pos,
                        default = MahjongGame(world, pos, blockEntity.rule)
                    )
                )
                blockEntity.gameInitialized = true
            }
        }
    }
}