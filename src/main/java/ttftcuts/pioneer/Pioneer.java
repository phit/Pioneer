package ttftcuts.pioneer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ttftcuts.pioneer.commands.CommandPioneer;
import ttftcuts.pioneer.map.MapColours;
import ttftcuts.pioneer.map.MapJob;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.nio.file.Paths;

@Mod(Pioneer.MODID)
public class Pioneer
{
    public static final String MODID = "pioneer";

    public static final int TILE_SIZE = 128;
    public static File SAVE_PATH;

    public static final Logger logger = LogManager.getLogger("cwl");

    public static MapJob currentJob = null;
    public static MapColours mapColours;

    public Pioneer() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        mapColours = new MapColours();
    }

    public void preInit(final FMLCommonSetupEvent event)
    {
        SAVE_PATH = FMLPaths.GAMEDIR.get().resolve(Paths.get("pioneer")).toFile();
    }

    @SubscribeEvent
    public void serverStarting(FMLServerStartingEvent event) {
        new CommandPioneer(event.getCommandDispatcher());
    }

    @SubscribeEvent
    public void serverStopping(FMLServerStoppingEvent event) {
        if (currentJob != null) {
            currentJob.endJob(true);
        }
        mapColours.biomeColours.clear();
    }
}
