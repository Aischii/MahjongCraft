package doublemoon.mahjongcraft.item

import doublemoon.mahjongcraft.entity.MahjongScoringStickEntity
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.Level

/**
 * 麻將點棒物品
 *
 * 使用 NBT 之中的 "code" 來控制牌的外觀
 * */
class MahjongScoringStick(settings: Properties) : Item(settings) {
    /**
     * 使用右鍵就會觸發,
     * 不會與 [useOnBlock] 一起觸發 (應該吧?),
     * 右鍵改變外觀
     * */
    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        val itemStack = user.getItemInHand(hand)
        // 僅允許伺服端執行，如果是客戶端就直接返回
        if (world.isClientSide) return InteractionResult.SUCCESS

        if (!user.isShiftKeyDown) { // 沒有蹲下
            val nbt = itemStack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: CompoundTag()
            val stickCode = nbt.getIntOr("code", 0)
            nbt.putInt("code", (stickCode + 1) % 4)
            itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    /**
     * 對方塊互動觸發,
     * 蹲下右鍵放置, 右鍵改變外觀
     * */
    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        // 僅允許伺服端執行，如果是客戶端就直接返回
        if (context.level.isClientSide) return InteractionResult.SUCCESS

        val itemStack = context.itemInHand
        val nbt = itemStack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: CompoundTag()
        val stickCode = nbt.getIntOr("code", 0)

        return if (player.isShiftKeyDown) { //有蹲下才能放置
            val world = context.level as ServerLevel
            MahjongScoringStickEntity(world = world).apply {
                code = stickCode
                val pos = context.clickLocation
                snapTo(pos.x, pos.y, pos.z, context.rotation, 0f)
                world.addFreshEntity(this)
            }
            if (!player.abilities.instabuild) itemStack.shrink(1)
            InteractionResult.CONSUME
        } else {  //沒蹲下, 改變牌的外觀
            nbt.putInt("code", (stickCode + 1) % 4)
            itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
            InteractionResult.SUCCESS
        }
    }
}