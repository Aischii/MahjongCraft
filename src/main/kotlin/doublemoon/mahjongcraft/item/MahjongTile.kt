package doublemoon.mahjongcraft.item

import net.minecraft.server.level.ServerPlayer
import doublemoon.mahjongcraft.game.progress.MahjongAdvancements
import doublemoon.mahjongcraft.block.MahjongTileBlock
import doublemoon.mahjongcraft.blockentity.MahjongTileBlockEntity
import doublemoon.mahjongcraft.registry.BlockRegistry
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.material.Fluids
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.Level

/**
 * 麻將牌物品
 *
 * 使用 NBT 之中的 "code" 來控制牌的外觀
 * */
class MahjongTile(settings: Properties) : Item(settings) {
    /**
     * 使用右鍵就會觸發,
     * 不會與 [useOnBlock] 一起觸發 (應該吧?),
     * 右鍵改變外觀
     * */
    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        val itemStack = user.getItemInHand(hand)
        // 僅允許伺服端執行，如果是客戶端就直接返回
        if (user.level().isClientSide) return InteractionResult.SUCCESS

        if (!user.isShiftKeyDown) { // 沒有蹲下
            val nbt = itemStack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: CompoundTag()
            val tileCode = nbt.getIntOr("code", 0)
            nbt.putInt("code", (tileCode + 1) % 38)
            itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        val level = context.level
        if (!player.isShiftKeyDown) return use(level, player, context.hand)

        val clicked = context.clickedPos
        val block = BlockRegistry.mahjongTileBlock
        val adding = level.getBlockState(clicked).`is`(block)
        val pos = if (adding) clicked else clicked.relative(context.clickedFace)
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.clickedFace, context.itemInHand)) {
            return InteractionResult.FAIL
        }
        if (adding) {
            val tiles = level.getBlockEntity(pos) as? MahjongTileBlockEntity ?: return InteractionResult.FAIL
            if (tiles.isFull) return InteractionResult.FAIL
        } else if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.FAIL
        }

        val code = context.itemInHand.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getIntOr("code", 0) ?: 0
        if (code !in 0..37) return InteractionResult.FAIL
        if (level.isClientSide) return InteractionResult.SUCCESS
        if (!adding) {
            val state = block.defaultBlockState().setValue(MahjongTileBlock.WATERLOGGED, level.getFluidState(pos).`is`(Fluids.WATER))
            if (!level.setBlock(pos, state, Block.UPDATE_ALL)) return InteractionResult.FAIL
        }
        val tiles = level.getBlockEntity(pos) as? MahjongTileBlockEntity ?: return InteractionResult.FAIL
        if (!tiles.addTile(code, player.direction.opposite)) return InteractionResult.FAIL
        if (tiles.isFull && player is ServerPlayer) MahjongAdvancements.award(player, "place_four_tiles")
        if (!player.abilities.instabuild) context.itemInHand.shrink(1)
        val sound = tiles.blockState.soundType
        level.playSound(null, pos, sound.placeSound, SoundSource.BLOCKS, (sound.volume + 1f) / 2f, sound.pitch * 0.8f)
        return InteractionResult.SUCCESS_SERVER
    }
}
