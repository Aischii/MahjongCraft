package doublemoon.mahjongcraft.event

import doublemoon.mahjongcraft.game.GameManager
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongGame
import doublemoon.mahjongcraft.network.mahjong_table.MahjongTablePayloadListener
import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel

/**
 * 玩家切換維度(世界)事件,
 * 用來切換世界後對遊戲進行的處理
 * */
fun onPlayerChangedWorld(player: Player, origin: ServerLevel, destination: ServerLevel) {
    if (origin == destination) return
    if (!player.level().isClientSide && GameManager.isInAnyGame(player as ServerPlayer)) {
        when (val game = GameManager.getGameBy(player) ?: return) {
            is MahjongGame -> {
                MahjongTablePayloadListener.syncBlockEntityWithGame(game = game) {
                    onPlayerChangedWorld(player)
                }
            }
        }
    }
}