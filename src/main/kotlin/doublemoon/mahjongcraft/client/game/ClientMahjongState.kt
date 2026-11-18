package doublemoon.mahjongcraft.client.game

import doublemoon.mahjongcraft.MahjongCraftClient
import doublemoon.mahjongcraft.game.mahjong.riichi.model.ActionGroup
import doublemoon.mahjongcraft.game.mahjong.riichi.model.ActionOption
import doublemoon.mahjongcraft.game.mahjong.riichi.model.ActionOptions
import doublemoon.mahjongcraft.game.mahjong.riichi.model.ClaimTarget
import doublemoon.mahjongcraft.game.mahjong.riichi.model.DiscardRequest
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongGameBehavior
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import doublemoon.mahjongcraft.network.mahjong_game.MahjongGamePayload
import doublemoon.mahjongcraft.network.sendPayloadToServer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

/**
 * What the local player sees and can do at the mahjong table: their hand, the selected tile,
 * the request the game is waiting on, and the thinking time. Drives the hand HUD and the
 * gameplay keys; everything runs on the client thread.
 */
@Environment(EnvType.CLIENT)
object ClientMahjongState {

    /** The player's hand, in table order (a freshly drawn tile is last). */
    var hand: List<MahjongTile> = emptyList()
        private set

    /** Index into [hand] of the selected tile. */
    var selected: Int = 0
        private set

    /** Set while the game waits for this player to discard. */
    var discardRequest: DiscardRequest? = null
        private set

    /** Options for the current optional request (calls, riichi, win, ...), empty when none. */
    var options: List<ActionOption> = emptyList()
        private set

    /** The action group the player has picked and is choosing a variant of, if any. */
    var activeGroup: ActionGroup? = null
        private set

    /** Which variant of [activeGroup] is highlighted. */
    var variantIndex: Int = 0
        private set

    /** Remaining base and extra thinking seconds; nulls when no countdown is running. */
    var time: Pair<Int?, Int?> = null to null
        private set

    /** True while the hand HUD and the gameplay keys are in use. */
    val active: Boolean get() = MahjongCraftClient.playing && hand.isNotEmpty()

    /** Whether the drawn tile should be drawn apart from the rest of the hand. */
    val hasDrawnTile: Boolean get() = hand.size % 3 == 2

    val availableGroups: List<ActionGroup>
        get() = options.map { it.group }.distinct()

    fun optionsIn(group: ActionGroup): List<ActionOption> = options.filter { it.group == group }

    /** Whether [index] may be picked: riichi limits it to the tiles riichi can discard. */
    fun isSelectable(index: Int): Boolean {
        val tile = hand.getOrNull(index) ?: return false
        if (activeGroup == ActionGroup.RIICHI) return optionsIn(ActionGroup.RIICHI).any { it.tiles.first() == tile }
        val request = discardRequest ?: return true
        return tile !in request.cannotDiscard
    }

    // ---- updates from the server ----

    fun onHand(newHand: List<MahjongTile>) {
        val grew = newHand.size > hand.size
        hand = newHand
        selected = if (grew && hasDrawnTile) hand.lastIndex else selected.coerceIn(0, (hand.size - 1).coerceAtLeast(0))
    }

    fun onDiscardRequest(hands: List<MahjongTile>, request: DiscardRequest) {
        hand = hands
        discardRequest = request
        selected = hand.lastIndex
        if (!isSelectable(selected)) moveSelection(-1)
    }

    fun onRequest(behavior: MahjongGameBehavior, hands: List<MahjongTile>, target: ClaimTarget, data: String) {
        if (hands.isNotEmpty()) hand = hands
        options = ActionOptions.build(behavior, hands, target, data)
        activeGroup = null
        variantIndex = 0
    }

    /** A null-to-null time means the game stopped waiting, so any open request is over. */
    fun onTime(newTime: Pair<Int?, Int?>) {
        time = newTime
        if (newTime.first == null && newTime.second == null) clearRequests()
    }

    fun reset() {
        hand = emptyList()
        selected = 0
        clearRequests()
        time = null to null
    }

    private fun clearRequests() {
        discardRequest = null
        options = emptyList()
        activeGroup = null
        variantIndex = 0
    }

    // ---- player input ----

    /** Scroll or arrow keys: cycle variants while choosing one, otherwise move along the hand. */
    fun scroll(direction: Int) {
        val group = activeGroup
        if (group != null && group != ActionGroup.RIICHI) {
            val count = optionsIn(group).size
            if (count > 0) variantIndex = Math.floorMod(variantIndex + direction, count)
            return
        }
        moveSelection(direction)
    }

    private fun moveSelection(direction: Int) {
        if (hand.isEmpty()) return
        var index = selected
        repeat(hand.size) {
            index = Math.floorMod(index + direction, hand.size)
            if (isSelectable(index)) {
                selected = index
                return
            }
        }
    }

    /** Right-click / Enter: confirm the highlighted choice, or discard the selected tile. */
    fun confirm() {
        when (val group = activeGroup) {
            ActionGroup.RIICHI -> {
                val tile = hand.getOrNull(selected) ?: return
                optionsIn(group).firstOrNull { it.tiles.first() == tile }?.let { send(it.response()) }
            }
            null -> {
                if (discardRequest == null || !isSelectable(selected)) return
                send(MahjongGameBehavior.DISCARD to MahjongTile.entries.indexOf(hand[selected]).toString())
            }
            else -> optionsIn(group).getOrNull(variantIndex)?.let { send(it.response()) }
        }
    }

    /** An action key: act at once when there's one option, otherwise start choosing a variant. */
    fun press(group: ActionGroup) {
        val choices = optionsIn(group)
        if (choices.isEmpty()) return
        when {
            group == ActionGroup.RIICHI -> {
                if (activeGroup == ActionGroup.RIICHI) return confirm()
                activeGroup = group
                if (!isSelectable(selected)) moveSelection(1)
            }
            choices.size == 1 -> send(choices.single().response())
            activeGroup == group -> confirm()
            else -> {
                activeGroup = group
                variantIndex = 0
            }
        }
    }

    /** Skip: back out of a variant choice first, then decline the request. */
    fun skip() {
        when {
            activeGroup != null -> activeGroup = null
            options.isNotEmpty() -> send(MahjongGameBehavior.SKIP to "")
        }
    }

    private fun send(response: Pair<MahjongGameBehavior, String>) {
        sendPayloadToServer(MahjongGamePayload(behavior = response.first, extraData = response.second))
        clearRequests()
    }
}
