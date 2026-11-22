package doublemoon.mahjongcraft.game.mahjong.riichi.player

import doublemoon.mahjongcraft.entity.toMahjongTileList
import doublemoon.mahjongcraft.game.mahjong.riichi.model.ClaimTarget
import doublemoon.mahjongcraft.game.mahjong.riichi.model.DiscardRequest
import doublemoon.mahjongcraft.game.mahjong.riichi.model.TableState
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongGameBehavior
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongRule
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import doublemoon.mahjongcraft.network.mahjong_game.MahjongGamePayload
import doublemoon.mahjongcraft.network.sendPayloadToPlayer
import doublemoon.mahjongcraft.scheduler.server.ServerScheduler
import doublemoon.mahjongcraft.util.delayOnServer
import kotlinx.serialization.encodeToString
import doublemoon.mahjongcraft.network.PayloadJson
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.Component

/** How long riichi autoplay shows the drawn tile before discarding it. */
private const val AUTO_DISCARD_MS = 800L

/**
 * 麻將玩家
 * */
class MahjongPlayer(
    override val entity: ServerPlayer,
) : MahjongPlayerBase() {

    init {
        // 初始化的時候就發數據包給玩家, 用來取得玩家要不要自動理牌
        sendPayloadToPlayer(
            player = entity,
            payload = MahjongGamePayload(behavior = MahjongGameBehavior.AUTO_ARRANGE)
        )
    }

    fun sendMessage(text: Component, overlay: Boolean = true) {
        if (overlay) entity.sendOverlayMessage(text) else entity.sendSystemMessage(text)
    }

    /**
     * 正在等待客戶端執行的動作
     * */
    val waitingBehavior = mutableListOf<MahjongGameBehavior>()

    /**
     * 取消等待 [waitingBehavior], 用在遊戲結束的時候, 避免遊戲結束卻還在倒數
     * */
    var cancelWaitingBehavior = false

    /**
     * 在發送數據包給玩家後,
     * 用來等待玩家回傳的數據,
     * 後面的 [String] 是拿來裝額外資料的,
     * 只有玩家有, 機器人沒有
     * */
    var behaviorResult: Pair<MahjongGameBehavior, String>? = null

    /**
     * 玩家不能丟的牌
     * */
    var cannotDiscardTiles = listOf<MahjongTile>()
        private set

    /** The hand last sent with [MahjongGameBehavior.HAND_UPDATE]; null forces the next sync. */
    private var lastSyncedHand: List<MahjongTile>? = null

    /** Sends the player's hand to their client when it changed, for the hand HUD. */
    fun syncHandIfChanged() {
        val hand = hands.toMahjongTileList()
        if (hand == lastSyncedHand) return
        lastSyncedHand = hand
        sendPayloadToPlayer(
            player = entity,
            payload = MahjongGamePayload(behavior = MahjongGameBehavior.HAND_UPDATE, hands = hand)
        )
    }

    private var lastSyncedTable: TableState? = null
    private var waitsHand: List<MahjongTile>? = null
    private var waitsCache: List<MahjongTile> = emptyList()

    /** [waits], recalculated only when the hand changed. */
    fun cachedWaits(): List<MahjongTile> {
        val hand = hands.toMahjongTileList()
        if (hand != waitsHand) {
            waitsHand = hand
            waitsCache = waits
        }
        return waitsCache
    }

    /** Sends [state] with [MahjongGameBehavior.TABLE_STATE] when it differs from the last one sent. */
    fun syncTableIfChanged(state: TableState) {
        if (state == lastSyncedTable) return
        lastSyncedTable = state
        sendPayloadToPlayer(
            player = entity,
            payload = MahjongGamePayload(behavior = MahjongGameBehavior.TABLE_STATE, extraData = PayloadJson.encodeToString(state))
        )
    }

    /**
     * Whether discarding [tileCode] is allowed right now: the tile is in the hand and not one of
     * [cannotDiscardTiles]. Clients only offer allowed tiles, but the server checks too.
     */
    fun canDiscard(tileCode: Int): Boolean {
        val tile = MahjongTile.entries.getOrNull(tileCode) ?: return false
        return tile in hands.toMahjongTileList() && tile !in cannotDiscardTiles
    }

    override fun teleport(targetWorld: ServerLevel, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        this.entity.teleportTo(targetWorld, x, y, z, emptySet(), yaw, pitch, true)
    }

    /**
     * 要求並讓玩家丟牌的功能,
     * 傳過去的額外資料是 能不能自動摸切 (不能丟的牌的 [MahjongTile] 編號列表, 交給 [MahjongPlayer.cannotDiscardTiles] 處理),
     * 傳回來的額外資料是 要丟的牌的 [MahjongTile] 編號
     * */
    override suspend fun askToDiscardTile(
        timeoutTile: MahjongTile,
        cannotDiscardTiles: List<MahjongTile>,
        skippable: Boolean,
        auto: Boolean,
    ): MahjongTile {
        if (auto) return autoDiscard(timeoutTile, cannotDiscardTiles)
        this.cannotDiscardTiles = cannotDiscardTiles
        return waitForBehaviorResult(
            behavior = MahjongGameBehavior.DISCARD,
            extraData = PayloadJson.encodeToString(DiscardRequest(skippable, cannotDiscardTiles)),
            target = ClaimTarget.SELF,
            skippable = skippable
        ) { behavior, data ->
            this.cannotDiscardTiles = listOf()
            val tileCode = data.toIntOrNull() ?: return@waitForBehaviorResult timeoutTile
            if (behavior == MahjongGameBehavior.DISCARD && tileCode in MahjongTile.entries.toTypedArray().indices) {
                MahjongTile.entries[tileCode]
            } else timeoutTile
        }
    }

    /**
     * Riichi autoplay: shows the drawn tile for [AUTO_DISCARD_MS], then discards it. Tsumo and
     * Kan were already asked before this, so there is nothing left to choose.
     */
    private suspend fun autoDiscard(tile: MahjongTile, cannotDiscardTiles: List<MahjongTile>): MahjongTile {
        sendPayloadToPlayer(
            player = entity,
            payload = MahjongGamePayload(
                behavior = MahjongGameBehavior.DISCARD,
                hands = hands.toMahjongTileList(),
                target = ClaimTarget.SELF,
                extraData = PayloadJson.encodeToString(DiscardRequest(false, cannotDiscardTiles, auto = true))
            )
        )
        delayOnServer(AUTO_DISCARD_MS)
        // Two nulls end the request on the client, as after a normal countdown.
        sendPayloadToPlayer(
            player = entity,
            payload = MahjongGamePayload(
                behavior = MahjongGameBehavior.COUNTDOWN_TIME,
                extraData = PayloadJson.encodeToString<Pair<Int?, Int?>>(null to null)
            )
        )
        return tile
    }

    /**
     * 詢問玩家是否要吃的功能,
     * 傳過去的額外資料是 要進行吃的牌的編號,以及可以與之組成順子的牌的編號對的列表,
     * 傳回來的額外資料是 要與之組成順子的牌的編號對,
     * */
    override suspend fun askToChii(
        tile: MahjongTile,
        tilePairs: List<Pair<MahjongTile, MahjongTile>>,
        target: ClaimTarget,
    ): Pair<MahjongTile, MahjongTile>? = waitForBehaviorResult(
        behavior = MahjongGameBehavior.CHII,
        extraData = PayloadJson.encodeToString( //轉成 Json 字串傳過去
            listOf(
                PayloadJson.encodeToString(tile),
                PayloadJson.encodeToString(tilePairs)
            )
        ),
        target = target
    ) { behavior, data ->
        val result = runCatching {
            PayloadJson.decodeFromString<Pair<MahjongTile, MahjongTile>>(data)
        }.getOrNull() ?: return@waitForBehaviorResult null
        if (behavior == MahjongGameBehavior.CHII && result in tilePairs) result
        else null
    }

    /**
     * 詢問玩家是否要碰或吃的功能,
     * 傳過去的額外資料是 要進行碰或吃的牌的編號,以及可以與之組成順子的牌對的列表,最後是可以與之組成刻子的牌對
     * 傳回來的額外資料是 如果是要吃->與 [tile] 組成順子的牌對,碰->原本的 [tile],
     * */
    override suspend fun askToPonOrChii(
        tile: MahjongTile,
        tilePairsForChii: List<Pair<MahjongTile, MahjongTile>>,
        tilePairForPon: Pair<MahjongTile, MahjongTile>,
        target: ClaimTarget,
    ): Pair<MahjongTile, MahjongTile>? = waitForBehaviorResult(
        behavior = MahjongGameBehavior.PON_OR_CHII,
        waitingBehavior = listOf(
            MahjongGameBehavior.CHII,
            MahjongGameBehavior.PON,
            MahjongGameBehavior.SKIP
        ),
        extraData = PayloadJson.encodeToString( //轉成 Json 字串傳過去
            listOf(
                PayloadJson.encodeToString(tile),
                PayloadJson.encodeToString(tilePairsForChii),
                PayloadJson.encodeToString(tilePairForPon)
            )
        ),
        target = target
    ) { behavior, data ->
        when (behavior) {
            MahjongGameBehavior.CHII -> {
                val result = runCatching {
                    PayloadJson.decodeFromString<Pair<MahjongTile, MahjongTile>>(data)
                }.getOrNull() ?: return@waitForBehaviorResult null
                if (result in tilePairsForChii) result else null
            }
            MahjongGameBehavior.PON -> tile to tile
            else -> null
        }
    }


    /**
     * 詢問玩家是否要碰的功能,
     * 傳過去的額外資料是 要進行碰的牌 和 可以與之組成刻子的牌對,
     * 傳回來的額外資料是 空白
     * */
    override suspend fun askToPon(
        tile: MahjongTile,
        tilePairForPon: Pair<MahjongTile, MahjongTile>,
        target: ClaimTarget,
    ): Boolean = waitForBehaviorResult(
        behavior = MahjongGameBehavior.PON,
        extraData = PayloadJson.encodeToString( //轉成 Json 字串傳過去
            listOf(
                PayloadJson.encodeToString(tile),
                PayloadJson.encodeToString(tilePairForPon)
            )
        ),
        target = target
    ) { behavior, _ ->
        behavior == MahjongGameBehavior.PON
    }

    /**
     * 詢問玩家是否要暗槓或加槓的功能,
     * 傳過去的額外資料是 可以進行暗槓 以及 可以進行加槓的牌的集合 和 規則,
     * 傳回來的額外資料是 要暗槓或加槓的 [MahjongTile], null 表示沒有要暗槓
     * */
    override suspend fun askToAnkanOrKakan(
        canAnkanTiles: Set<MahjongTile>,
        canKakanTiles: Set<Pair<MahjongTile, ClaimTarget>>,
        rule: MahjongRule,
    ): MahjongTile? = waitForBehaviorResult(
        behavior = MahjongGameBehavior.ANKAN_OR_KAKAN,
        extraData = PayloadJson.encodeToString(
            listOf(
                PayloadJson.encodeToString(canAnkanTiles),
                PayloadJson.encodeToString(canKakanTiles),
                rule.toJsonString()
            )
        ),
        target = ClaimTarget.SELF
    ) { behavior, data ->
        val result = runCatching {
            PayloadJson.decodeFromString<MahjongTile>(data)
        }.getOrNull() ?: return@waitForBehaviorResult null
        if (behavior == MahjongGameBehavior.ANKAN_OR_KAKAN) {
            if (result in canAnkanTiles || result in canKakanTiles.unzip().first) result else null
        } else null
    }

    /**
     * 詢問玩家是否要明槓的功能,
     * 傳過去的額外資料是 要進行明槓的牌 和 規則,
     * 傳回來的額外資料是 空白
     *
     * @return 由回傳的行為決定是否要碰還是要槓還是跳過
     * */
    override suspend fun askToMinkanOrPon(
        tile: MahjongTile,
        target: ClaimTarget,
        rule: MahjongRule,
    ): MahjongGameBehavior = waitForBehaviorResult(
        behavior = MahjongGameBehavior.MINKAN,
        waitingBehavior = listOf(MahjongGameBehavior.PON, MahjongGameBehavior.MINKAN),
        extraData = PayloadJson.encodeToString(
            listOf(
                PayloadJson.encodeToString(tile),
                rule.toJsonString()
            )
        ),
        target = target
    ) { behavior, _ ->
        when (behavior) {
            MahjongGameBehavior.PON -> behavior
            MahjongGameBehavior.MINKAN -> behavior
            else -> MahjongGameBehavior.SKIP
        }
    }

    /**
     * 詢問玩家是否要立直的功能,
     * 傳過去的額外資料是 要丟的牌和丟了之後對應會聽的牌,
     * 傳回來的額外資料是 空白
     * */
    override suspend fun askToRiichi(tilePairsForRiichi: List<Pair<MahjongTile, List<MahjongTile>>>): MahjongTile? =
        waitForBehaviorResult(
            behavior = MahjongGameBehavior.RIICHI,
            extraData = PayloadJson.encodeToString(tilePairsForRiichi),
            target = ClaimTarget.SELF
        ) { behavior, data ->
            val result = runCatching {
                PayloadJson.decodeFromString<MahjongTile>(data)
            }.getOrNull() ?: return@waitForBehaviorResult null
            if (behavior == MahjongGameBehavior.RIICHI && result in tilePairsForRiichi.unzip().first) result
            else null
        }

    /**
     * 詢問玩家是否要自摸的功能,
     * 傳過去的額外資料是 空白,
     * 傳回來的額外資料是 空白
     * */
    override suspend fun askToTsumo(): Boolean = waitForBehaviorResult(
        behavior = MahjongGameBehavior.TSUMO,
        extraData = "",
        target = ClaimTarget.SELF
    ) { behavior, _ ->
        behavior == MahjongGameBehavior.TSUMO
    }

    /**
     * 詢問玩家是否要榮和的功能,
     * 傳過去的額外資料是 要榮和的牌的編號,
     * 傳回來的額外資料是 空白
     * */
    override suspend fun askToRon(tile: MahjongTile, target: ClaimTarget): Boolean = waitForBehaviorResult(
        behavior = MahjongGameBehavior.RON,
        extraData = PayloadJson.encodeToString(tile),
        target = target
    ) { behavior, _ ->
        behavior == MahjongGameBehavior.RON
    }

    override suspend fun askToKita(): Boolean = waitForBehaviorResult(
        behavior = MahjongGameBehavior.KITA,
        extraData = "",
        target = ClaimTarget.SELF
    ) { behavior, _ ->
        behavior == MahjongGameBehavior.KITA
    }

    override suspend fun askToKyuushuKyuuhai(): Boolean = waitForBehaviorResult(
        behavior = MahjongGameBehavior.KYUUSHU_KYUUHAI,
        extraData = "",
        target = ClaimTarget.SELF
    ) { behavior, _ ->
        behavior == MahjongGameBehavior.KYUUSHU_KYUUHAI
    }

    /**
     * 發送給玩家行為 [behavior],
     * 傳過去的資料分別是 :
     * 1.手牌 [hands]
     * 2.額外資料 [extraData] (轉成 [PayloadJson] 字串列表後傳給玩家)
     * 3.鳴牌對象
     *
     * 最後收到的結果的時候會執行 [onResult], 會傳回對應的 [MahjongGameBehavior] 以及字串形式的資料 [String],
     * 超時的話會得到結果 [MahjongGameBehavior.SKIP]
     * */
    private suspend fun <T> waitForBehaviorResult(
        behavior: MahjongGameBehavior,
        waitingBehavior: List<MahjongGameBehavior> = listOf(behavior),
        extraData: String,
        hands: List<MahjongTile> = this.hands.toMahjongTileList(),
        target: ClaimTarget,
        skippable: Boolean = true,
        onResult: (MahjongGameBehavior, String) -> T,
    ): T {
        this.waitingBehavior += waitingBehavior
        if (skippable) this.waitingBehavior += MahjongGameBehavior.SKIP

        // 發送請求
        sendPayloadToPlayer(
            player = entity,
            payload = MahjongGamePayload(
                behavior = behavior,
                hands = hands,
                target = target,
                extraData = extraData
            )
        )

        // 倒數計時並等待回應
        var tBase = basicThinkingTime
        var tExtra = extraThinkingTime
        var count = 0
        var completed = false
        val action = ServerScheduler.scheduleRepeatAction(
            times = (tBase + tExtra) * 20 + 1, //間隔 1 tick (50ms) 的話, 1 sec = 20 ticks
            interval = 0,  // 1000 ms 響應時間太慢, 看起來卡卡的, 這裡給 0 ,表示每個 tick 都執行一次
        ) {
            count++
            if (cancelWaitingBehavior || behaviorResult != null || ((tBase + tExtra) <= 0 && count % 20 == 1)) {
                completed = true
            } else if ((tBase + tExtra) > 0 && count % 20 == 1) {
                sendPayloadToPlayer(
                    player = entity,
                    payload = MahjongGamePayload(
                        behavior = MahjongGameBehavior.COUNTDOWN_TIME,
                        extraData = PayloadJson.encodeToString<Pair<Int?, Int?>>(tBase to tExtra)
                    )
                )

                if (tBase > 0) {
                    tBase--
                } else if (tExtra > 0) {
                    tExtra--
                }
            }
        }
        while (!completed) delayOnServer(50)
        ServerScheduler.removeQueuedAction(action)

        // 傳兩個 null 過去表示倒數結束
        sendPayloadToPlayer(
            player = entity,
            payload = MahjongGamePayload(
                behavior = MahjongGameBehavior.COUNTDOWN_TIME,
                extraData = PayloadJson.encodeToString<Pair<Int?, Int?>>(null to null)
            )
        )

        val usedExtraTime = extraThinkingTime - tExtra //使用過的額外思考時間
        extraThinkingTime -= usedExtraTime //扣掉用掉的額外思考時間
        val result = behaviorResult ?: (MahjongGameBehavior.SKIP to "")
        behaviorResult = null //最後重置 behaviorResult
        this.waitingBehavior.clear() //重置等待中的動作
        return onResult.invoke(result.first, result.second)
    }
}