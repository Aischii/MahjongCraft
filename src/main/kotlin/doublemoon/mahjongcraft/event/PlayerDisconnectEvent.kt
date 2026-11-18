package doublemoon.mahjongcraft.event

import doublemoon.mahjongcraft.game.GameManager
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongGame
import doublemoon.mahjongcraft.network.mahjong_table.MahjongTablePayloadListener
import net.minecraft.server.level.ServerPlayer

/**
 * 玩家登出事件,
 * 用來控制登出後對遊戲進行的處理
 * */
fun onPlayerDisconnect(player: ServerPlayer) {
    if (!player.level().isClientSide && GameManager.isInAnyGame(player)) {
        when (val game = GameManager.getGameBy(player) ?: return) {
            is MahjongGame -> {
                MahjongTablePayloadListener.syncBlockEntityWithGame(game = game) {
                    onPlayerDisconnect(player)
                }
            }
        }
    }
}