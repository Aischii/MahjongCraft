package doublemoon.mahjongcraft.item

import doublemoon.mahjongcraft.entity.MahjongTileEntity
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
import net.minecraft.world.InteractionResultHolder
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
    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemStack = user.getItemInHand(hand)
        // 僅允許伺服端執行，如果是客戶端就直接返回
        if (user.level().isClientSide) return InteractionResultHolder.success(itemStack)

        if (!user.isShiftKeyDown) { // 沒有蹲下
            val nbt = itemStack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: CompoundTag()
            val tileCode = nbt.getInt("code")
            nbt.putInt("code", (tileCode + 1) % 38)
            itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
            return InteractionResultHolder.success(itemStack)
        }
        return InteractionResultHolder.pass(itemStack)
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
        val tileCode = nbt.getInt("code")

        return if (player.isShiftKeyDown) { // 有蹲下才能放置
            val world = context.level as ServerLevel
            MahjongTileEntity(world = world, code = tileCode).apply {
                val pos = context.clickLocation
                moveTo(pos.x, pos.y, pos.z, (context.rotation + 180f), 0f)
                world.addFreshEntity(this)
            }
            if (!player.abilities.instabuild) itemStack.shrink(1)
            InteractionResult.CONSUME
        } else {  //沒蹲下, 改變牌的外觀
            nbt.putInt("code", (tileCode + 1) % 38)
            itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
            InteractionResult.SUCCESS
        }
    }
}