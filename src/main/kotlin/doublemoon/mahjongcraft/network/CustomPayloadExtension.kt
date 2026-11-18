package doublemoon.mahjongcraft.network

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

@Environment(EnvType.CLIENT)
fun CustomPacketPayload.sendToServer() {
    ClientPlayNetworking.send(this)
}

@Environment(EnvType.CLIENT)
fun sendPayloadToServer(payload: CustomPacketPayload) = payload.sendToServer()

fun CustomPacketPayload.sendToPlayer(player: ServerPlayer) {
    ServerPlayNetworking.send(player, this)
}

fun sendPayloadToPlayer(player: ServerPlayer, payload: CustomPacketPayload) = payload.sendToPlayer(player)
