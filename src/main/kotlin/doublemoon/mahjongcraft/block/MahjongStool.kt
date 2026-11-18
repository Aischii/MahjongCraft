package doublemoon.mahjongcraft.block

import doublemoon.mahjongcraft.entity.SeatEntity
import doublemoon.mahjongcraft.util.boxBySize
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level

class MahjongStool(settings: Properties) : Block(settings) {

    override fun useWithoutItem(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult,
    ): InteractionResult =
        if (!world.isClientSide && SeatEntity.canSpawnAt(world = world as ServerLevel, pos = pos)) {
            SeatEntity.spawnAt(world = world, pos = pos, entity = player, sitOffsetY = 0.4)
            InteractionResult.SUCCESS
        } else {
            InteractionResult.PASS
        }

    override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext,
    ): VoxelShape = SHAPE

    companion object {
        private val SHAPE: VoxelShape

        init {
            val bottom1: VoxelShape = boxBySize(3, 4, 5, 2, 2, 6)
            val bottom2: VoxelShape = boxBySize(11, 4, 5, 2, 2, 6)
            val bottom3: VoxelShape = boxBySize(5, 4, 3, 6, 2, 2)
            val bottom4: VoxelShape = boxBySize(5, 4, 11, 6, 2, 2)
            val pillar1: VoxelShape = boxBySize(3, 0, 3, 2, 8, 2)
            val pillar2: VoxelShape = boxBySize(11, 0, 11, 2, 8, 2)
            val pillar3: VoxelShape = boxBySize(3, 0, 11, 2, 8, 2)
            val pillar4: VoxelShape = boxBySize(11, 0, 3, 2, 8, 2)
            val top: VoxelShape = boxBySize(1.0, 8.0, 1.0, 14.0, 2.0, 14.0)
            SHAPE = Shapes.or(bottom1, bottom2, bottom3, bottom4, pillar1, pillar2, pillar3, pillar4, top)
        }
    }
}