package doublemoon.mahjongcraft.game

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.util.TextFormatting
import net.minecraft.network.chat.Component


/**
 * 遊戲的狀態
 * */
enum class GameStatus : TextFormatting {
    WAITING,
    PLAYING;

    override fun toText(): Component = Component.translatable("$MOD_ID.game.status.${name.lowercase()}")
}