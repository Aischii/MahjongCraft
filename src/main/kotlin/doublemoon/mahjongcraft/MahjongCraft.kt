package doublemoon.mahjongcraft

import doublemoon.mahjongcraft.game.GameManager
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongGame
import doublemoon.mahjongcraft.event.onPlayerChangedWorld
import doublemoon.mahjongcraft.event.onPlayerDisconnect
import doublemoon.mahjongcraft.network.mahjong_game.MahjongGamePayloadListener
import doublemoon.mahjongcraft.network.mahjong_table.MahjongTablePayloadListener
import doublemoon.mahjongcraft.network.mahjong_tile_code.MahjongTileCodePayloadListener
import doublemoon.mahjongcraft.registry.*
import doublemoon.mahjongcraft.scheduler.server.ServerScheduler
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.resources.Identifier
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

const val MOD_ID = "mahjongcraft"
val logger: Logger = LogManager.getLogger()  //取得 logger

fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

object MahjongCraft : ModInitializer {

    override fun onInitialize() {
        // Registry
        ItemRegistry.register()
        EntityTypeRegistry.register()
        BlockRegistry.register()
        BlockEntityTypeRegistry.register()
        SoundRegistry.register()

        // Event
        ServerTickEvents.END_SERVER_TICK.register(ServerScheduler::tick)
        ServerTickEvents.END_SERVER_TICK.register { GameManager.games.forEach { (it as? MahjongGame)?.syncHands() } }
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerScheduler::onStopping)
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(::onPlayerChangedWorld)
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> onPlayerDisconnect(handler.player) }

        // Packet
        MahjongTablePayloadListener.registerCommon()
        MahjongGamePayloadListener.registerCommon()
        MahjongTileCodePayloadListener.registerCommon()

        MahjongTablePayloadListener.registerServer()
        MahjongGamePayloadListener.registerServer()
        MahjongTileCodePayloadListener.registerServer()
    }
}