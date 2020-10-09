package ttftcuts.pioneer;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.gui.GuiUtils;
import ttftcuts.pioneer.map.MapJob;

public class EventHandler {

    public static final ResourceLocation workingTexture = new ResourceLocation(Pioneer.MODID, "textures/gui/working.png");

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (Pioneer.currentJob != null) {
            Pioneer.currentJob.process();
        }
    }

    @SubscribeEvent
    public void onGameOverlay(RenderGameOverlayEvent event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        if (Pioneer.currentJob == null) {
            return;
        }

        MapJob job = Pioneer.currentJob;

        Minecraft mc = Minecraft.getInstance();

        mc.getTextureManager().bindTexture(workingTexture);
        GlStateManager.clearCurrentColor();

        int frame = (Minecraft.getInstance().getFrameTimer().getIndex() / 500) % 4;
        int offset = 32 * frame;

        GuiUtils.drawTexturedModalRect(5,5,offset,0,32,32,1.0f);

        FontRenderer font = mc.fontRenderer;
        String text = job.getCompletionPercent(true) + "%";

        font.drawString(event.getMatrixStack(), text, 5 + 16 - font.getStringWidth(text)/2, 39, 0xFFFFFF);

        if (mc.isGamePaused()) {
            text = I18n.format("commands.pioneer.paused");
            font.drawString(event.getMatrixStack(), text, 5 + 16 - font.getStringWidth(text)/2, 49, 0xFF3333);
        }
    }
}
