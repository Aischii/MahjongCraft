package doublemoon.mahjongcraft.util

import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component


/**
 * 對玩家發送顯示標題的數據包, 不能控制時間
 *
 * @param title 要顯示的標題
 * @param subtitle 要顯示的副標題
 * */
fun ServerPlayer.sendTitles(
    title: Component = Component.nullToEmpty(""),
    subtitle: Component? = null,
) {
    with(connection) {
        send(ClientboundSetTitleTextPacket(title))
        subtitle?.also { send(ClientboundSetSubtitleTextPacket(it)) }
    }
}

/**
 * 對玩家發送顯示標題的數據包
 *
 * @param title 要顯示的標題
 * @param subtitle 要顯示的副標題
 * */
fun Collection<ServerPlayer>.sendTitles(
    title: Component = Component.nullToEmpty(""),
    subtitle: Component? = null
) {
    forEach { it.sendTitles(title, subtitle) }
}