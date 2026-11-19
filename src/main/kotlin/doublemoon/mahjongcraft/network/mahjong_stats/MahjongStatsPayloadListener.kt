package doublemoon.mahjongcraft.network.mahjong_stats

import doublemoon.mahjongcraft.client.gui.screen.GuideScreen
import doublemoon.mahjongcraft.game.progress.MahjongStatsData
import doublemoon.mahjongcraft.game.progress.PlayerStats
import doublemoon.mahjongcraft.network.ChannelType
import doublemoon.mahjongcraft.network.CustomPayloadListener
import doublemoon.mahjongcraft.network.sendPayloadToPlayer
import kotlinx.serialization.json.Json
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

object MahjongStatsPayloadListener : CustomPayloadListener<MahjongStatsPayload> {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override val id: CustomPacketPayload.Type<MahjongStatsPayload> = MahjongStatsPayload.ID
    override val codec: StreamCodec<RegistryFriendlyByteBuf, MahjongStatsPayload> = MahjongStatsPayload.CODEC
    override val channelType: ChannelType = ChannelType.Both

    /** Asks the server for this player's stats; the Guide shows them when they arrive. */
    @Environment(EnvType.CLIENT)
    fun request() = ClientPlayNetworking.send(MahjongStatsPayload())

    @Environment(EnvType.CLIENT)
    override fun onClientReceive(payload: MahjongStatsPayload, context: ClientPlayNetworking.Context) {
        val stats = runCatching { json.decodeFromString<PlayerStats>(payload.json) }.getOrNull() ?: return
        context.client().execute {
            (context.client().gui.screen() as? GuideScreen)?.showStats(stats)
        }
    }

    override fun onServerReceive(payload: MahjongStatsPayload, context: ServerPlayNetworking.Context) {
        val player = context.player()
        val book = MahjongStatsData.of(player.level().server).book
        val stats = book.players[player.stringUUID] ?: PlayerStats(name = player.name.string)
        sendPayloadToPlayer(player, MahjongStatsPayload(json.encodeToString(PlayerStats.serializer(), stats)))
    }
}
