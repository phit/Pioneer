package ttftcuts.pioneer.map;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.ServerWorldInfo;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import ttftcuts.pioneer.Pioneer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MapJob {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    protected final Queue<MapTile> tileQueue;

    public final ServerWorld world;
    public final BiomeProvider provider;
    public final String filename;

    protected File file;
    protected ZipOutputStream zip;

    public int jobsize;
    protected final long startTime;

    public MapJob(ServerWorld world, int x, int z, int radius, int skip) {
        this.world = world;
        this.provider = world.getChunkProvider().getChunkGenerator().getBiomeProvider();

        this.tileQueue = new LinkedList<>();

        int tilerange = (int)Math.ceil(((radius*2) / (double)skip) / (double)Pioneer.TILE_SIZE);

        int tileworldsize = Pioneer.TILE_SIZE * skip;
        int offset = (int)Math.floor(tileworldsize * (tilerange / 2.0));
        this.jobsize = tilerange * tilerange;

        ServerWorldInfo info = (ServerWorldInfo) this.world.getWorldInfo();
        this.filename = info.getWorldName() +"_"+ LocalDateTime.now().format(DATE_FORMAT) + ".pioneer"; //.zip

        Pioneer.logger.info("Pioneer: new mapping job: "+radius+" radius, "+x+","+z+" at scale "+skip+". "+this.jobsize+" tiles");

        for (int ix = 0; ix<tilerange; ix++) {
            for (int iz = 0; iz<tilerange; iz++) {

                MapTile t = new MapTile(ix+"_"+iz, x + tileworldsize * ix - offset, z + tileworldsize * iz - offset, skip);
                this.tileQueue.add(t);
            }
        }

        try {
            if (!Pioneer.SAVE_PATH.exists()) {
                Pioneer.SAVE_PATH.mkdir();
            }

            this.file = new File(Pioneer.SAVE_PATH, this.filename);
            this.zip = new ZipOutputStream(new FileOutputStream(this.file));

            this.buildJsons(x,z,radius,skip,tilerange);

        } catch (Exception e) {
            e.printStackTrace();
            this.endJob(true);
        }

        this.startTime = new Date().getTime();
        Minecraft.getInstance().player.sendMessage(new TranslationTextComponent("commands.pioneer.start", this.jobsize),
                Minecraft.getInstance().player.getUniqueID());
    }

    public void process() {
        Pioneer.logger.info("Pioneer: "+ this.filename +": "+ this.getCompletionPercent(false) +"%");
        try {
            if (!this.tileQueue.isEmpty()) {
                MapTile t = this.tileQueue.poll();

                t.generate(this.world);

                t.save(this.zip);
            } else {
                if (zip != null) {
                    zip.close();
                }
                this.endJob(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.endJob(true);
        }
    }


    public void endJob(boolean cancel) {
        if (Pioneer.currentJob == this) {
            Pioneer.currentJob = null;
        }
        if(cancel) {
            this.file.delete();
        } else {
            long time = (new Date().getTime() - this.startTime) / 1000;

            Minecraft.getInstance().player.sendMessage(new TranslationTextComponent("commands.pioneer.finish", this.jobsize, time),
                    Minecraft.getInstance().player.getUniqueID());
        }
    }

    public double getCompletion(boolean offset) {
        return ((this.jobsize - this.tileQueue.size()) - (offset? 1 : 0)) / (double)this.jobsize;
    }

    public int getCompletionPercent(boolean offset) {
        return (int)Math.round(this.getCompletion(offset) * 100);
    }

    public void buildJsons(int x, int z, int radius, int skip, int tilerange) throws IOException {
        JsonObject mapinfo = new JsonObject();

        ServerWorldInfo info = (ServerWorldInfo) this.world.getWorldInfo();
        final Registry<DimensionType> reg = this.world.func_241828_r().getRegistry(Registry.DIMENSION_TYPE_KEY);

        mapinfo.addProperty("worldname", info.getWorldName());
        mapinfo.addProperty("dimensiontype", reg.getKey(this.world.getDimensionType()).toString());
        mapinfo.addProperty("dimension", this.world.getDimensionKey().getRegistryName()+"");
        mapinfo.addProperty("seed", this.world.getSeed()+"");

        JsonObject generator = new JsonObject();

        // FIXME: these are probably gone in 1.16.3?
        generator.addProperty("name", "n/a");
        generator.addProperty("version", "n/a");
        generator.addProperty("options", "");

        mapinfo.add("generator", generator);

        mapinfo.addProperty("x", x);
        mapinfo.addProperty("z", z);
        mapinfo.addProperty("radius", radius);
        mapinfo.addProperty("skip", skip);
        mapinfo.addProperty("jobsize", jobsize);
        mapinfo.addProperty("tilerange", tilerange);

        this.zip.putNextEntry(new ZipEntry("map.json"));
        byte[] bytes = mapinfo.toString().getBytes();
        this.zip.write(bytes, 0, bytes.length);
        this.zip.closeEntry();

        JsonObject biomes = new JsonObject();

        ForgeRegistry<Biome> biomereg = ((ForgeRegistry<Biome>) ForgeRegistries.BIOMES);
        for (Biome biome : ForgeRegistries.BIOMES) {

            if (biome == null) {
                continue;
            }

            JsonObject bson = new JsonObject();

            bson.addProperty("name", biome.getRegistryName().toString());
            bson.addProperty("temperature", biome.getTemperature());
            bson.addProperty("moisture", biome.getDownfall());
            bson.addProperty("precipitation", biome.getPrecipitation().getName());
            bson.addProperty("height", biome.getDepth());
            bson.addProperty("heightvariation", 0);
            bson.addProperty("ismutation", false);
            bson.addProperty("colour", "#" + Integer.toHexString(Pioneer.mapColours.getBiomeMapColour(biome)).substring(2));

            biomes.add(biomereg.getID(biome) + "", bson);
        }

        this.zip.putNextEntry(new ZipEntry("biomes.json"));
        bytes = biomes.toString().getBytes();
        this.zip.write(bytes, 0, bytes.length);
        this.zip.closeEntry();
    }


}
