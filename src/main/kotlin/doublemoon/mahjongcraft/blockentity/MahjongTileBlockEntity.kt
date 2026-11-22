package doublemoon.mahjongcraft.blockentity

import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_DEPTH
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_HEIGHT
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_WIDTH
import doublemoon.mahjongcraft.registry.BlockEntityTypeRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3

enum class TileBlockPose { STANDING, FLAT }

class MahjongTileBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(BlockEntityTypeRegistry.mahjongTileBlock, pos, state) {

    private val tiles = mutableListOf<Int>()
    val codes: List<Int> get() = tiles
    var pose = TileBlockPose.STANDING
        private set
    var facing = Direction.NORTH
        private set

    val isFull: Boolean get() = tiles.size >= MAX_TILES

    fun addTile(code: Int, facing: Direction): Boolean {
        if (isFull || code !in 0..37) return false
        if (tiles.isEmpty()) this.facing = facing
        tiles.add(code)
        sync()
        return true
    }

    fun flip() {
        pose = if (pose == TileBlockPose.STANDING) TileBlockPose.FLAT else TileBlockPose.STANDING
        sync()
    }

    fun takeLast(): Int? = tiles.removeLastOrNull()?.also { sync() }

    private fun sync() {
        setChanged()
        level?.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS)
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        output.putIntArray("Tiles", tiles.toIntArray())
        output.putString("Pose", pose.name)
        output.putString("Facing", facing.name)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        tiles.clear()
        tiles.addAll(input.getIntArray("Tiles").orElseGet { intArrayOf() }.filter { it in 0..37 }.take(MAX_TILES))
        val savedPose = input.getStringOr("Pose", "")
        pose = TileBlockPose.entries.firstOrNull { it.name == savedPose } ?: TileBlockPose.STANDING
        val savedFacing = input.getStringOr("Facing", "")
        facing = Direction.entries.firstOrNull { it.axis.isHorizontal && it.name == savedFacing } ?: Direction.NORTH
    }

    override fun getUpdateTag(registryLookup: HolderLookup.Provider): CompoundTag =
        saveWithoutMetadata(registryLookup)

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)

    // The tile model faces north before rotation. Both rendering and selection use these centers.
    fun tileCenter(index: Int): Vec3 {
        val widthStep = MAHJONG_TILE_WIDTH + GAP
        val x = if (pose == TileBlockPose.STANDING) {
            (index - (tiles.size - 1) / 2.0) * widthStep
        } else {
            (index % 2 - (minOf(tiles.size, 2) - 1) / 2.0) * widthStep
        }
        val z = if (pose == TileBlockPose.FLAT && tiles.size > 2) {
            (index / 2 - 0.5) * (MAHJONG_TILE_HEIGHT + GAP)
        } else 0.0
        val y = if (pose == TileBlockPose.STANDING) MAHJONG_TILE_HEIGHT else MAHJONG_TILE_DEPTH
        val right = facing.clockWise
        return Vec3(0.5 + right.stepX * x - facing.stepX * z, y / 2.0, 0.5 + right.stepZ * x - facing.stepZ * z)
    }

    companion object {
        private const val MAX_TILES = 4
        private const val GAP = 0.025
    }
}
