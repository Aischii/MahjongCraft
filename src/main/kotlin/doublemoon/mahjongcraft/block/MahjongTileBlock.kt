package doublemoon.mahjongcraft.block

import doublemoon.mahjongcraft.blockentity.MahjongTileBlockEntity
import doublemoon.mahjongcraft.blockentity.TileBlockPose
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_DEPTH
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_HEIGHT
import doublemoon.mahjongcraft.entity.MahjongTileEntity.Companion.MAHJONG_TILE_WIDTH
import doublemoon.mahjongcraft.registry.ItemRegistry
import doublemoon.mahjongcraft.registry.withCode
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Prediction
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class MahjongTileBlock(properties: Properties) : Block(properties), EntityBlock, SimpleWaterloggedBlock {
    init {
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(WATERLOGGED)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = MahjongTileBlockEntity(pos, state)

    override fun getFluidState(state: BlockState): FluidState =
        if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(state)

    override fun updateShape(
        state: BlockState, level: LevelReader, ticks: ScheduledTickAccess, pos: BlockPos,
        direction: Direction, neighborPos: BlockPos, neighborState: BlockState, random: RandomSource,
    ): BlockState {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val tiles = level.getBlockEntity(pos) as? MahjongTileBlockEntity ?: return EMPTY_TILE_SHAPE
        val laidFlat = tiles.pose == TileBlockPose.FLAT
        val depth = if (laidFlat) MAHJONG_TILE_HEIGHT else MAHJONG_TILE_DEPTH
        val xAxis = tiles.facing.axis == Direction.Axis.X
        val halfX = (if (xAxis) depth else MAHJONG_TILE_WIDTH) / 2.0
        val halfZ = (if (xAxis) MAHJONG_TILE_WIDTH else depth) / 2.0
        return tiles.codes.indices.fold(Shapes.empty()) { shape, index ->
            val center = tiles.tileCenter(index)
            Shapes.or(shape, Shapes.box(center.x - halfX, 0.0, center.z - halfZ,
                center.x + halfX, center.y * 2, center.z + halfZ))
        }.takeUnless { it.isEmpty } ?: EMPTY_TILE_SHAPE
    }

    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult,
    ): InteractionResult {
        if (!player.mainHandItem.isEmpty) return InteractionResult.PASS
        val tiles = level.getBlockEntity(pos) as? MahjongTileBlockEntity ?: return InteractionResult.PASS
        if (!level.isClientSide) {
            if (player.isShiftKeyDown) {
                val code = tiles.takeLast() ?: return InteractionResult.PASS
                val stack = ItemRegistry.mahjongTile.withCode(code)
                if (tiles.codes.isEmpty()) level.removeBlock(pos, false)
                if (!player.inventory.add(stack)) player.drop(stack, false, Prediction.SERVER_ONLY)
            } else {
                tiles.flip()
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): List<ItemStack> =
        (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) as? MahjongTileBlockEntity)
            ?.codes.orEmpty().map { ItemRegistry.mahjongTile.withCode(it) }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        // Creative breaking skips the ordinary loot path.
        if (!level.isClientSide && player.abilities.instabuild) {
            val tiles = level.getBlockEntity(pos) as? MahjongTileBlockEntity
            tiles?.codes?.forEach { popResource(level, pos, ItemRegistry.mahjongTile.withCode(it)) }
        }
        return super.playerWillDestroy(level, pos, state, player)
    }

    companion object {
        val WATERLOGGED = BlockStateProperties.WATERLOGGED
        private val EMPTY_TILE_SHAPE = box(7.0, 0.0, 7.0, 9.0, 2.5, 9.0)
    }
}
