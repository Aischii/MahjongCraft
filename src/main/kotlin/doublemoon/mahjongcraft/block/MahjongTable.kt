package doublemoon.mahjongcraft.block

import doublemoon.mahjongcraft.game.progress.MahjongAdvancements
import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.block.enums.MahjongTablePart
import doublemoon.mahjongcraft.blockentity.MahjongTableBlockEntity
import doublemoon.mahjongcraft.game.GameManager
import doublemoon.mahjongcraft.game.GameStatus
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongGame
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTableBehavior
import doublemoon.mahjongcraft.network.mahjong_table.MahjongTablePayload
import doublemoon.mahjongcraft.network.sendPayloadToPlayer
import doublemoon.mahjongcraft.registry.BlockEntityTypeRegistry
import doublemoon.mahjongcraft.registry.ItemRegistry
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import doublemoon.mahjongcraft.util.boxBySize
import doublemoon.mahjongcraft.util.plus
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level

/**
 * 參考自 [BedBlock]
 * */
class MahjongTable(settings: Properties) : BaseEntityBlock(settings) {

    init {
        registerDefaultState(stateDefinition.any().setValue(PART, MahjongTablePart.BOTTOM_CENTER))
    }

    /**
     * 只有 blockstate 的 [PART] 為 [MahjongTablePart.BOTTOM_CENTER] 那個方塊才會渲染
     * */
    override fun getRenderShape(state: BlockState): RenderShape =
        if (state.getValue(PART) == MahjongTablePart.BOTTOM_CENTER) RenderShape.MODEL else RenderShape.INVISIBLE

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        val centerPos = ctx.clickedPos
        val canPlace = peripheryPos(centerPos).all { ctx.level.getBlockState(it.second).canBeReplaced(ctx) }
        return if (canPlace) defaultBlockState() else null
    }

    override fun setPlacedBy(
        world: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        itemStack: ItemStack,
    ) {
        super.setPlacedBy(world, pos, state, placer, itemStack)
        if (!world.isClientSide) {
            peripheryPos(pos).forEach { (part, position) -> world.setBlockAndUpdate(position, state.setValue(PART, part)) }
            world.updateNeighborsAt(pos, Blocks.AIR)
            state.updateNeighbourShapes(world, pos, 3)
        }
    }

    override fun playerWillDestroy(world: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        if (!world.isClientSide) {
            val part = state.getValue(PART) ?: return super.playerWillDestroy(world, pos, state, player)
            val centerPos = getCenterPosByPart(pos, part)
            //確保打掉桌子的其中一格方塊就能拆掉整張桌子
            allPos(centerPos).forEach { blockPos ->
                val blockState = world.getBlockState(blockPos)
                if (blockState.block != this) return@forEach
                val blockPart = blockState.getValue(PART) ?: return@forEach
                if (blockPart == MahjongTablePart.BOTTOM_CENTER) {  //center 被破壞時會離開遊戲
                    GameManager.getGame<MahjongGame>(world = world as ServerLevel, pos = centerPos)?.onBreak()
                }
                if (blockPart == MahjongTablePart.BOTTOM_CENTER && !player.isCreative) {  //讓玩家在非創造破壞時, 讓中間的桌子掉落
                    world.destroyBlock(blockPos, true)
                } else {
                    world.destroyBlock(blockPos, false)
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(PART)
    }

    /** Right-click with a Mahjong Tile Set loads it into the table (one per table). */
    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult,
    ): InteractionResult {
        if (!stack.`is`(ItemRegistry.mahjongTileSet)) return InteractionResult.TRY_WITH_EMPTY_HAND
        val centerPos = getCenterPosByPart(pos = pos, part = state.getValue(PART))
        val table = world.getBlockEntity(centerPos) as? MahjongTableBlockEntity ?: return InteractionResult.PASS
        if (table.tileSetLoaded) return InteractionResult.TRY_WITH_EMPTY_HAND
        if (!world.isClientSide) {
            table.tileSetLoaded = true
            if (player is ServerPlayer) MahjongAdvancements.award(player, "load_tile_set")
            table.setChanged()
            stack.consume(1, player)
            world.playSound(null, centerPos, SoundEvents.BAMBOO_WOOD_PLACE, SoundSource.BLOCKS, 1f, 1.2f)
            player.sendOverlayMessage(Component.translatable("$MOD_ID.game.message.tile_set_loaded").withStyle(ChatFormatting.GREEN))
        }
        return InteractionResult.SUCCESS
    }

    /** Sneak + right-click with an empty hand takes the loaded Tile Set back out, while no game is running. */
    private fun takeTileSet(world: ServerLevel, centerPos: BlockPos, player: ServerPlayer): Boolean {
        val table = world.getBlockEntity(centerPos) as? MahjongTableBlockEntity ?: return false
        if (!table.tileSetLoaded || table.playing) return false
        table.tileSetLoaded = false
        table.setChanged()
        val set = ItemRegistry.mahjongTileSet.defaultInstance
        if (!player.inventory.add(set)) net.minecraft.world.Containers.dropItemStack(world, player.x, player.y, player.z, set)
        world.playSound(null, centerPos, SoundEvents.BAMBOO_WOOD_BREAK, SoundSource.BLOCKS, 1f, 1.2f)
        return true
    }

    override fun useWithoutItem(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult,
    ): InteractionResult {
        if (!world.isClientSide) {
            val centerPos = getCenterPosByPart(pos = pos, part = state.getValue(PART))
            player as ServerPlayer
            world as ServerLevel
            if (player.isShiftKeyDown && player.mainHandItem.isEmpty && takeTileSet(world, centerPos, player)) {
                return InteractionResult.SUCCESS
            }
            val game = GameManager.getGame<MahjongGame>(world = world, pos = centerPos) ?: return InteractionResult.CONSUME
            if (!GameManager.isInAnyGame(player) || game.isInGame(player)) {
                //如果玩家沒有在任何遊戲中, 或者玩家在"這個"遊戲中
                if (game.status == GameStatus.PLAYING) { //如果遊戲已經開始,傳訊息->遊戲已經開始了
                    player.sendOverlayMessage(
                        Component.translatable("$MOD_ID.game.message.already_started").withStyle(ChatFormatting.YELLOW)
                    )
                } else {  // 如果遊戲尚未開始, 讓玩家開啟麻將桌 gui
                    sendPayloadToPlayer(
                        player = player,
                        payload = MahjongTablePayload(
                            behavior = MahjongTableBehavior.OPEN_TABLE_WAITING_GUI,
                            pos = centerPos
                        )
                    )
                }
            } else {  //玩家已經在某個遊戲中, 且不是"這個"遊戲
                val message = Component.translatable("$MOD_ID.game.message.already_in_a_game")
                    .withStyle(ChatFormatting.YELLOW) + " (" + game.name + ")"
                player.sendOverlayMessage(message)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        if (state.getValue(PART) == MahjongTablePart.BOTTOM_CENTER) MahjongTableBlockEntity(pos, state) else null

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>,
    ): BlockEntityTicker<T>? = createTickerHelper(
        type,
        BlockEntityTypeRegistry.mahjongTable
    ) { world1, pos, _, blockEntity -> MahjongTableBlockEntity.tick(world1, pos, blockEntity) }

    override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext,
    ): VoxelShape = when (state.getValue(PART) ?: MahjongTablePart.BOTTOM_CENTER) {
        MahjongTablePart.BOTTOM_CENTER -> BOTTOM_CENTER_SHAPE
        MahjongTablePart.BOTTOM_EAST -> BOTTOM_EAST_SHAPE
        MahjongTablePart.BOTTOM_WEST -> BOTTOM_WEST_SHAPE
        MahjongTablePart.BOTTOM_SOUTH -> BOTTOM_SOUTH_SHAPE
        MahjongTablePart.BOTTOM_NORTH -> BOTTOM_NORTH_SHAPE
        MahjongTablePart.BOTTOM_SOUTHEAST -> BOTTOM_SOUTHEAST_SHAPE
        MahjongTablePart.BOTTOM_SOUTHWEST -> BOTTOM_SOUTHWEST_SHAPE
        MahjongTablePart.BOTTOM_NORTHEAST -> BOTTOM_NORTHEAST_SHAPE
        MahjongTablePart.BOTTOM_NORTHWEST -> BOTTOM_NORTHWEST_SHAPE
        MahjongTablePart.TOP_CENTER -> TOP_CENTER_SHAPE
        MahjongTablePart.TOP_EAST -> TOP_EAST_SHAPE
        MahjongTablePart.TOP_WEST -> TOP_WEST_SHAPE
        MahjongTablePart.TOP_SOUTH -> TOP_SOUTH_SHAPE
        MahjongTablePart.TOP_NORTH -> TOP_NORTH_SHAPE
        MahjongTablePart.TOP_SOUTHEAST -> TOP_SOUTHEAST_SHAPE
        MahjongTablePart.TOP_SOUTHWEST -> TOP_SOUTHWEST_SHAPE
        MahjongTablePart.TOP_NORTHEAST -> TOP_NORTHEAST_SHAPE
        MahjongTablePart.TOP_NORTHWEST -> TOP_NORTHWEST_SHAPE
    }

    companion object {
        val PART: EnumProperty<MahjongTablePart> = EnumProperty.create("mahjong_table_part", MahjongTablePart::class.java)

        private val BOTTOM_CENTER_SHAPE: VoxelShape

        init { //計算出 BOTTOM_CENTER_SHAPE
            val bottom: VoxelShape = boxBySize(1.0, 0.0, 1.0, 14.0, 2.0, 14.0)
            val pillar: VoxelShape = boxBySize(4.0, 2.0, 4.0, 8.0, 12.0, 8.0)
            val top: VoxelShape = boxBySize(-15.0, 14.0, -15.0, 46.0, 2.0, 46.0)
            val border1: VoxelShape = boxBySize(-16, 14, -15, 1, 3, 47)
            val border2: VoxelShape = boxBySize(-15, 14, 31, 47, 3, 1)
            val border3: VoxelShape = boxBySize(31, 14, -16, 1, 3, 47)
            val border4: VoxelShape = boxBySize(-16, 14, -16, 47, 3, 1)
            BOTTOM_CENTER_SHAPE = Shapes.or(bottom, pillar, top, border1, border2, border3, border4)
        }

        //這裡的位置是指相對於 BOTTOM_CENTER_SHAPE 的
        private val BOTTOM_EAST_SHAPE: VoxelShape = BOTTOM_CENTER_SHAPE.move(-1.0, 0.0, 0.0)
        private val BOTTOM_WEST_SHAPE: VoxelShape = BOTTOM_CENTER_SHAPE.move(1.0, 0.0, 0.0)
        private val BOTTOM_SOUTH_SHAPE: VoxelShape = BOTTOM_CENTER_SHAPE.move(0.0, 0.0, -1.0)
        private val BOTTOM_NORTH_SHAPE: VoxelShape = BOTTOM_CENTER_SHAPE.move(0.0, 0.0, 1.0)
        private val BOTTOM_SOUTHEAST_SHAPE: VoxelShape = BOTTOM_SOUTH_SHAPE.move(-1.0, 0.0, 0.0)
        private val BOTTOM_SOUTHWEST_SHAPE: VoxelShape = BOTTOM_SOUTH_SHAPE.move(1.0, 0.0, 0.0)
        private val BOTTOM_NORTHEAST_SHAPE: VoxelShape = BOTTOM_NORTH_SHAPE.move(-1.0, 0.0, 0.0)
        private val BOTTOM_NORTHWEST_SHAPE: VoxelShape = BOTTOM_NORTH_SHAPE.move(1.0, 0.0, 0.0)
        private val TOP_CENTER_SHAPE: VoxelShape = BOTTOM_CENTER_SHAPE.move(0.0, -1.0, 0.0)
        private val TOP_EAST_SHAPE: VoxelShape = BOTTOM_EAST_SHAPE.move(0.0, -1.0, 0.0)
        private val TOP_WEST_SHAPE: VoxelShape = BOTTOM_WEST_SHAPE.move(0.0, -1.0, 0.0)
        private val TOP_SOUTH_SHAPE: VoxelShape = BOTTOM_SOUTH_SHAPE.move(0.0, -1.0, 0.0)
        private val TOP_NORTH_SHAPE: VoxelShape = BOTTOM_NORTH_SHAPE.move(0.0, -1.0, 0.0)
        private val TOP_SOUTHEAST_SHAPE: VoxelShape = BOTTOM_SOUTHEAST_SHAPE.move(0.0, -1.0, 0.0)
        private val TOP_SOUTHWEST_SHAPE: VoxelShape = BOTTOM_SOUTHWEST_SHAPE.move(0.0, -1.0, 0.0)
        private val TOP_NORTHEAST_SHAPE: VoxelShape = BOTTOM_NORTHEAST_SHAPE.move(0.0, -1.0, 0.0)
        private val TOP_NORTHWEST_SHAPE: VoxelShape = BOTTOM_NORTHWEST_SHAPE.move(0.0, -1.0, 0.0)

        /**
         * 取得以 [pos] 為中心, xz 平面周圍不含 [pos] 的 8 格方塊位置 + xz 平面往偏移 +y 的平面 9 個方塊的位置
         * */
        private fun peripheryPos(pos: BlockPos): List<Pair<MahjongTablePart, BlockPos>> = listOf(
            MahjongTablePart.BOTTOM_EAST to pos.east(),
            MahjongTablePart.BOTTOM_WEST to pos.west(),
            MahjongTablePart.BOTTOM_SOUTH to pos.south(),
            MahjongTablePart.BOTTOM_NORTH to pos.north(),
            MahjongTablePart.BOTTOM_NORTHEAST to pos.north().east(),
            MahjongTablePart.BOTTOM_NORTHWEST to pos.north().west(),
            MahjongTablePart.BOTTOM_SOUTHEAST to pos.south().east(),
            MahjongTablePart.BOTTOM_SOUTHWEST to pos.south().west(),
            MahjongTablePart.TOP_CENTER to pos.above(),
            MahjongTablePart.TOP_EAST to pos.east().above(),
            MahjongTablePart.TOP_WEST to pos.west().above(),
            MahjongTablePart.TOP_SOUTH to pos.south().above(),
            MahjongTablePart.TOP_NORTH to pos.north().above(),
            MahjongTablePart.TOP_NORTHEAST to pos.north().east().above(),
            MahjongTablePart.TOP_NORTHWEST to pos.north().west().above(),
            MahjongTablePart.TOP_SOUTHEAST to pos.south().east().above(),
            MahjongTablePart.TOP_SOUTHWEST to pos.south().west().above(),
        )

        /**
         * 取得以 [centerPos] 為中心, xz 平面含 [centerPos] 的 9 格方塊位置
         * */
        private fun allPos(centerPos: BlockPos): List<BlockPos> =
            buildList { add(centerPos); addAll(peripheryPos(centerPos).map { it.second }) }

        /**
         * 以 [pos] 加上 [part] 找到麻將桌中心的位置
         * */
        private fun getCenterPosByPart(pos: BlockPos, part: MahjongTablePart): BlockPos = when (part) {
            MahjongTablePart.BOTTOM_CENTER -> pos
            MahjongTablePart.BOTTOM_EAST -> pos.west()
            MahjongTablePart.BOTTOM_WEST -> pos.east()
            MahjongTablePart.BOTTOM_SOUTH -> pos.north()
            MahjongTablePart.BOTTOM_NORTH -> pos.south()
            MahjongTablePart.BOTTOM_SOUTHEAST -> pos.north().west()
            MahjongTablePart.BOTTOM_SOUTHWEST -> pos.north().east()
            MahjongTablePart.BOTTOM_NORTHEAST -> pos.south().west()
            MahjongTablePart.BOTTOM_NORTHWEST -> pos.south().east()
            MahjongTablePart.TOP_CENTER -> pos.below()
            MahjongTablePart.TOP_EAST -> pos.west().below()
            MahjongTablePart.TOP_WEST -> pos.east().below()
            MahjongTablePart.TOP_SOUTH -> pos.north().below()
            MahjongTablePart.TOP_NORTH -> pos.south().below()
            MahjongTablePart.TOP_SOUTHEAST -> pos.north().west().below()
            MahjongTablePart.TOP_SOUTHWEST -> pos.north().east().below()
            MahjongTablePart.TOP_NORTHEAST -> pos.south().west().below()
            MahjongTablePart.TOP_NORTHWEST -> pos.south().east().below()
        }
    }
}