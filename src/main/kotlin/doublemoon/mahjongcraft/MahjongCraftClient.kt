package doublemoon.mahjongcraft

import doublemoon.mahjongcraft.client.game.ClientMahjongState
import doublemoon.mahjongcraft.client.game.MahjongKeys
import doublemoon.mahjongcraft.client.gui.screen.GuideScreen
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.world.InteractionResult
import doublemoon.mahjongcraft.scheduler.client.OptionalBehaviorHandler
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import doublemoon.mahjongcraft.client.gui.hud.HandHud
import doublemoon.mahjongcraft.client.gui.hud.TableHud
import doublemoon.mahjongcraft.client.render.CodeItemModelProperty
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import doublemoon.mahjongcraft.client.ModConfig
import doublemoon.mahjongcraft.client.gui.screen.ConfigScreen
import doublemoon.mahjongcraft.client.gui.screen.HudPositionEditorScreen
import doublemoon.mahjongcraft.client.gui.screen.MahjongCraftHud
import doublemoon.mahjongcraft.client.gui.screen.yaku_overview.YakuOverviewScreen
import doublemoon.mahjongcraft.client.render.*
import doublemoon.mahjongcraft.network.mahjong_game.MahjongGamePayloadListener
import doublemoon.mahjongcraft.network.mahjong_table.MahjongTablePayloadListener
import doublemoon.mahjongcraft.network.mahjong_tile_code.MahjongTileCodePayloadListener
import doublemoon.mahjongcraft.registry.BlockEntityTypeRegistry
import doublemoon.mahjongcraft.registry.EntityTypeRegistry
import doublemoon.mahjongcraft.registry.ItemRegistry
import doublemoon.mahjongcraft.scheduler.client.ClientScheduler
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.KeyMapping
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier

@Environment(EnvType.CLIENT)
object MahjongCraftClient : ClientModInitializer {

    var playing = false //客戶端玩家是否在遊戲中
        set(value) {
            field = value
            hud?.refresh()
        }
    lateinit var config: ModConfig
        private set
    var hud: MahjongCraftHud? = null
        private set

    /** Shown in the controls screen as `key.category.mahjongcraft.main`. Declared before the keys that use it. */
    private val keyCategory: KeyMapping.Category = KeyMapping.Category.register(id("main"))

    //KeyBinding
    val configKey: KeyMapping = registerKeyBinding(
        translationKey = "key.$MOD_ID.open_config_gui",
        code = InputConstants.KEY_SEMICOLON,
    )
    val hudPositionEditorKey: KeyMapping = registerKeyBinding(
        translationKey = "key.$MOD_ID.open_hud_position_editor",
    )
    val yakuOverviewKey: KeyMapping = registerKeyBinding(
        translationKey = "key.$MOD_ID.open_yaku_overview",
    )

    override fun onInitializeClient() {
        logger.info("Initializing client")
        ClientTickEvents.END_CLIENT_TICK.register(this::tick)
        // Gameplay keys run before vanilla reads input, so a discard click isn't also an item use.
        MahjongKeys.register(keyCategory)
        ClientTickEvents.START_CLIENT_TICK.register(MahjongKeys::handleInput)
        // The Mahjong Guide opens its screen on the client; the server just lets the use pass.
        UseItemCallback.EVENT.register { player, level, hand ->
            if (level.isClientSide && player.getItemInHand(hand).`is`(ItemRegistry.mahjongGuide)) {
                Minecraft.getInstance().gui.setScreen(GuideScreen())
                InteractionResult.SUCCESS
            } else InteractionResult.PASS
        }
        HudElementRegistry.addLast(id("hand")) { graphics, delta -> HandHud.extract(graphics, delta) }
        HudElementRegistry.addLast(id("table")) { graphics, delta -> TableHud.extract(graphics, delta) }
        ClientLifecycleEvents.CLIENT_STOPPING.register { ClientScheduler.onStopping() }
        // Leaving a world mid-game never delivers GAME_OVER, so clear the table state here.
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            playing = false
            OptionalBehaviorHandler.cancel()
            ClientMahjongState.reset()
        }

        // Entity Renderer
        EntityRendererRegistry.register(EntityTypeRegistry.dice, ::DiceEntityRenderer)
        EntityRendererRegistry.register(EntityTypeRegistry.seat, ::SeatEntityRenderer)
        EntityRendererRegistry.register(EntityTypeRegistry.mahjongBot, ::MahjongBotEntityRenderer)
        EntityRendererRegistry.register(EntityTypeRegistry.mahjongScoringStick, ::MahjongScoringStickEntityRenderer)
        EntityRendererRegistry.register(EntityTypeRegistry.mahjongTile, ::MahjongTileEntityRenderer)

        // BlockEntity Renderer
        BlockEntityRenderers.register(BlockEntityTypeRegistry.mahjongTable, ::MahjongTableBlockEntityRenderer)

        // Item models pick the tile / scoring stick by its code (see assets/mahjongcraft/items/)
        CodeItemModelProperty.register()

        // Packet
        MahjongTablePayloadListener.registerClient()
        MahjongGamePayloadListener.registerClient()
        MahjongTileCodePayloadListener.registerClient()

        // Config
        AutoConfig.register(ModConfig::class.java, ::GsonConfigSerializer)
        config = AutoConfig.getConfigHolder(ModConfig::class.java).config

        // HUD
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen is TitleScreen && hud == null) hud = MahjongCraftHud() //在第 1 次標題畫面初始化後, 再將 hud 初始化
        }
        ScreenEvents.BEFORE_INIT.register { _, _, _, _ -> hud?.reposition() } //在畫面 resize 的時候刷新 hud, 以適配螢幕大小
    }

    fun saveConfig() = AutoConfig.getConfigHolder(ModConfig::class.java).save()

    private fun tick(client: Minecraft) {
        if (configKey.consumeClick()) client.gui.setScreen(ConfigScreen.build(null))
        if (hudPositionEditorKey.consumeClick()) hud?.also { client.gui.setScreen(HudPositionEditorScreen(it)) }
        if (yakuOverviewKey.consumeClick()) client.gui.setScreen(YakuOverviewScreen())
        ClientScheduler.tick(client)
    }

    private fun registerKeyBinding(
        translationKey: String,
        type: InputConstants.Type = InputConstants.Type.KEYBOARD,
        code: Int = InputConstants.UNKNOWN.value,
    ): KeyMapping = KeyMappingHelper.registerKeyMapping(KeyMapping(translationKey, type, code, keyCategory))

}