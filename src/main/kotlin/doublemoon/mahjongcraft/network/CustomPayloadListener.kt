package doublemoon.mahjongcraft.network

import doublemoon.mahjongcraft.logger
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

interface CustomPayloadListener<T : CustomPacketPayload> {

    val id: CustomPacketPayload.Type<T>

    val codec: StreamCodec<RegistryFriendlyByteBuf, T>

    val channelType: ChannelType

    @Environment(EnvType.CLIENT)
    fun onClientReceive(
        payload: T,
        context: ClientPlayNetworking.Context,
    ) {
    }

    fun onServerReceive(
        payload: T,
        context: ServerPlayNetworking.Context,
    ) {
    }

    fun registerCommon() {
        if (channelType.s2c) PayloadTypeRegistry.clientboundPlay().register(id, codec)
        if (channelType.c2s) PayloadTypeRegistry.serverboundPlay().register(id, codec)
    }

    @Environment(EnvType.CLIENT)
    fun registerClient() {
        if (!channelType.s2c) return
        ClientPlayNetworking.registerGlobalReceiver(id) { payload, context ->
            try {
                onClientReceive(payload, context)
            } catch (e: Exception) {
                logger.warn("Ignored a bad ${id.id()} packet", e)
            }
        }
    }

    fun registerServer() {
        if (!channelType.c2s) return
        ServerPlayNetworking.registerGlobalReceiver(id) { payload, context ->
            try {
                onServerReceive(payload, context)
            } catch (e: Exception) {
                logger.warn("Ignored a bad ${id.id()} packet", e)
            }
        }
    }
}