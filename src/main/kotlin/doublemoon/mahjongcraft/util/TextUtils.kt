package doublemoon.mahjongcraft.util

import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component

/**
 * 簡化 [MutableText.append] 用
 * */
operator fun <T : MutableComponent> T.plus(text: Component): MutableComponent =
    this.append(text)

operator fun <T : MutableComponent> T.plus(text: String): MutableComponent =
    this.append(text)


interface TextFormatting {
    fun toText(): Component
}

