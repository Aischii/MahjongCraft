package doublemoon.mahjongcraft.client.game

import com.mojang.blaze3d.platform.InputConstants
import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.game.mahjong.riichi.model.ActionGroup
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.HitResult

/**
 * Keys for playing at the table, listed under Controls → MahjongCraft. They only act while
 * [ClientMahjongState.active]; defaults avoid vanilla's default keys.
 */
@Environment(EnvType.CLIENT)
object MahjongKeys {
    private lateinit var category: KeyMapping.Category

    lateinit var selectLeft: KeyMapping
        private set
    lateinit var selectRight: KeyMapping
        private set
    lateinit var discard: KeyMapping
        private set
    lateinit var skip: KeyMapping
        private set

    /** One key per action group (Skip has its own key above). */
    val actions = linkedMapOf<ActionGroup, KeyMapping>()

    fun register(category: KeyMapping.Category) {
        this.category = category
        selectLeft = key("select_left", InputConstants.KEY_LEFT)
        selectRight = key("select_right", InputConstants.KEY_RIGHT)
        discard = key("discard", InputConstants.KEY_RETURN)
        actions[ActionGroup.CHII] = key("chii", InputConstants.KEY_J)
        actions[ActionGroup.PON] = key("pon", InputConstants.KEY_B)
        actions[ActionGroup.KAN] = key("kan", InputConstants.KEY_K)
        actions[ActionGroup.RIICHI] = key("riichi", InputConstants.KEY_R)
        actions[ActionGroup.WIN] = key("win", InputConstants.KEY_V)
        actions[ActionGroup.DRAW] = key("draw", InputConstants.KEY_N)
        skip = key("skip", InputConstants.KEY_Z)
    }

    /** The key shown for [group] in the HUD. */
    fun keyFor(group: ActionGroup): KeyMapping = if (group == ActionGroup.SKIP) skip else actions.getValue(group)

    /**
     * Runs at the start of each client tick, before vanilla reads input, so a right-click used to
     * discard doesn't also place or use the held item. Right-clicking a tile entity still goes to
     * vanilla, which lets you discard by clicking the 3D tile on the table.
     */
    fun handleInput(client: Minecraft) {
        val state = ClientMahjongState
        if (!state.active || client.gui.screen() != null) {
            drain()
            return
        }
        while (selectLeft.consumeClick()) state.scroll(-1)
        while (selectRight.consumeClick()) state.scroll(1)
        while (discard.consumeClick()) state.confirm()
        while (skip.consumeClick()) state.skip()
        actions.forEach { (group, key) -> while (key.consumeClick()) state.press(group) }

        val wantsChoice = state.discardRequest != null || state.activeGroup != null
        if (wantsChoice && client.hitResult?.type != HitResult.Type.ENTITY) {
            while (client.options.keyUse.consumeClick()) state.confirm()
        }
    }

    private fun drain() {
        (listOf(selectLeft, selectRight, discard, skip) + actions.values).forEach { while (it.consumeClick()) Unit }
    }

    private fun key(name: String, code: Int): KeyMapping = KeyMappingHelper.registerKeyMapping(
        KeyMapping("key.$MOD_ID.$name", InputConstants.Type.KEYBOARD, code, category)
    )
}
