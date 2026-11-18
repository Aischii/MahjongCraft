package doublemoon.mahjongcraft.scheduler.client

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

/**
 * 儲存由數據包收到的時間, 沒有任何功能
 * */
object ClientCountdownTimeHandler {

    private val client = Minecraft.getInstance()

    /**
     * null to null 表示沒在倒數
     * */
    var basicAndExtraTime: Pair<Int?, Int?> = null to null
        set(value) {
            // The hand HUD shows the remaining time; only clean up when the countdown ends.
            if (value.first == null && value.second == null) {  //兩個都為 null, 清除倒數計時, 關閉所有該關的處理程序
                if (OptionalBehaviorHandler.waiting) OptionalBehaviorHandler.cancel()
            }
            field = value
        }
}