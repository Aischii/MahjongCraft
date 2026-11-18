package doublemoon.mahjongcraft.util

import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.Font.DisplayMode
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.PlayerFaceExtractor
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import org.joml.Quaternionfc

@Environment(EnvType.CLIENT)
object RenderHelper {

    /**
     * Submits a camera-facing text label at ([offsetX], [offsetY], [offsetZ]) from the pose origin.
     *
     * [color] is RGB; it is drawn fully opaque.
     */
    fun submitLabel(
        font: Font,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraOrientation: Quaternionfc,
        offsetX: Double,
        offsetY: Double,
        offsetZ: Double,
        text: Component,
        color: Int,
        light: Int,
        scale: Float = 0.02f,
        backgroundColor: Int = (.4f * 255f).toInt() shl 24,
        shadow: Boolean = false,
        displayMode: DisplayMode = DisplayMode.NORMAL,
    ) {
        poseStack.pushPose()
        poseStack.translate(offsetX, offsetY, offsetZ)
        // Face the camera; text is drawn with y pointing down, so flip y (as vanilla does for name tags).
        poseStack.rotate(cameraOrientation)
        poseStack.scale(scale, -scale, scale)
        collector.submitText(
            poseStack,
            -font.width(text) / 2f,
            0f,
            text.visualOrderText,
            shadow,
            displayMode,
            light,
            OPAQUE or color,
            backgroundColor,
            0,
        )
        poseStack.popPose()
    }

    /**
     * Draws a player's face in a GUI. The skin is looked up in the background and cached by
     * Minecraft, so the default face shows until it arrives.
     */
    fun renderPlayerFace(graphics: GuiGraphicsExtractor, profile: ResolvableProfile, x: Int, y: Int, size: Int) {
        PlayerFaceExtractor.extractRenderState(graphics, profile, x, y, size)
    }

    fun getLightLevel(world: Level, pos: BlockPos): Int {
        val bLight = world.getBrightness(LightLayer.BLOCK, pos)
        val sLight = world.getBrightness(LightLayer.SKY, pos)
        return LightCoordsUtil.pack(bLight, sLight)
    }

    private const val OPAQUE = 0xFF000000.toInt()
}
