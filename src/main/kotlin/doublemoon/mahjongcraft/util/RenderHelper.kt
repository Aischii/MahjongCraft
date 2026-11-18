package doublemoon.mahjongcraft.util

import com.mojang.authlib.GameProfile
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.Font.DisplayMode
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.client.gui.components.PlayerTabOverlay
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.world.item.ItemDisplayContext
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.core.BlockPos
import com.mojang.math.Axis
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.Level

@Environment(EnvType.CLIENT)
object RenderHelper {
    /**
     * 渲染物品用
     * */
    fun renderItem(
        itemRenderer: ItemRenderer,
        matrices: PoseStack,
        offsetX: Double,
        offsetY: Double,
        offsetZ: Double,
        stack: ItemStack,
        mode: ItemDisplayContext = ItemDisplayContext.GROUND,
        overlay: Int = OverlayTexture.NO_OVERLAY,
        light: Int,
        vertexConsumer: MultiBufferSource,
        world: Level? = null,
        seed: Int = 0,
    ) {
        with(matrices) {
            pushPose()
            translate(offsetX, offsetY, offsetZ)
            itemRenderer.renderStatic(
                stack,
                mode,
                light,
                overlay,
                matrices,
                vertexConsumer,
                world,
                seed
            )
            popPose()
        }
    }

    /**
     * 渲染文字用
     * */
    fun renderLabel(
        textRenderer: Font,
        matrices: PoseStack,
        offsetX: Double,
        offsetY: Double,
        offsetZ: Double,
        text: Component,
        color: Int,
        scale: Float = 0.02f,
        backgroundColor: Int = (.4f * 255f).toInt() shl 24,
        light: Int,
        vertexConsumers: MultiBufferSource,
        shadow: Boolean = false,
        textLayerType: DisplayMode = DisplayMode.NORMAL,
    ) {
        with(matrices) {
            pushPose()
            val client = Minecraft.getInstance()
            val offset = -textRenderer.width(text) / 2f
            val matrix4f = matrices.last().pose()
            translate(offsetX, offsetY, offsetZ)
            scale(scale, scale, scale)
            mulPose(client.entityRenderDispatcher.cameraOrientation())
            mulPose(Axis.ZP.rotationDegrees(180f))
            textRenderer.drawInBatch(
                text,
                offset,
                0f,
                color,
                shadow,
                matrix4f,
                vertexConsumers,
                textLayerType,
                backgroundColor,
                light
            )
            popPose()
        }
    }

    /**
     * 渲染玩家的臉, 參考自 [PlayerListHud]
     * */
    fun renderPlayerFace(
        context: GuiGraphics,
        gameProfile: GameProfile,
        x: Int,
        y: Int,
        size: Int,
    ) {
        val client = Minecraft.getInstance()
        val playerEntity = client.level?.getPlayerByUUID(gameProfile.id)
        val hatVisible = playerEntity != null && playerEntity.isModelPartShown(PlayerModelPart.HAT)
        val upsideDown = playerEntity != null && LivingEntityRenderer.isEntityUpsideDown(playerEntity)

        val skinTextures = client.skinManager.getInsecureSkin(gameProfile)
        val texture = skinTextures.texture

        PlayerFaceRenderer.draw(context, texture, x, y, size, hatVisible, upsideDown)
    }

    fun getLightLevel(world: Level, pos: BlockPos): Int {
        val bLight = world.getBrightness(LightLayer.BLOCK, pos)
        val sLight = world.getBrightness(LightLayer.SKY, pos)
        return LightTexture.pack(bLight, sLight)
    }
}