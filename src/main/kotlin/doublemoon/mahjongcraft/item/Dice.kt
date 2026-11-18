package doublemoon.mahjongcraft.item

import doublemoon.mahjongcraft.entity.DiceEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.util.Mth
import net.minecraft.world.level.Level

class Dice(settings: Properties) : Item(settings) {

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemStack = user.getItemInHand(hand)
        if (world.isClientSide) return InteractionResultHolder.success(itemStack)
        val diceEntity = DiceEntity(
            world = world,
            pos = user.position().add(0.0, 1.1, 0.0),
            yaw = user.yHeadRot
        ).apply {
            val sin = Mth.sin(yRot * 0.017453292F - 11)
            val cos = Mth.cos(yRot * 0.017453292F - 11)
            setDeltaMovement(0.25 * cos, 0.2, 0.25 * sin)
        }
        world.addFreshEntity(diceEntity)
        if (!user.abilities.instabuild) itemStack.shrink(1)
        return InteractionResultHolder.success(itemStack)
    }
}