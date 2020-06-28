package ttftcuts.pioneer.map;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.registries.ForgeRegistries;
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

    public final World world;
    public final BiomeProvider provider;
    public final String filename;

    protected File file;
    protected ZipOutputStream zip;

    public int jobsize;
    protected final long startTime;

    public MapJob(World world, int x, int z, int radius, int skip) {
        this.world = world;
        this.provider = world.getServer().getWorld(DimensionType.OVERWORLD).getChunkProvider().getChunkGenerator().getBiomeProvider();

        this.tileQueue = new LinkedList<>();

        int tilerange = (int)Math.ceil(((radius*2) / (double)skip) / (double)Pioneer.TILE_SIZE);

        int tileworldsize = Pioneer.TILE_SIZE * skip;
        int offset = (int)Math.floor(tileworldsize * (tilerange / 2.0));
        this.jobsize = tilerange * tilerange;

        this.filename = this.world.getWorldInfo().getWorldName() +"_"+ LocalDateTime.now().format(DATE_FORMAT) + ".pioneer"; //.zip

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
        Minecraft.getInstance().player.sendMessage(new TranslationTextComponent("commands.pioneer.start", this.jobsize));
    }

    public void process() {
        Pioneer.logger.info("Pioneer: "+ this.filename +": "+ this.getCompletionPercent(false) +"%");
        try {
            if (!this.tileQueue.isEmpty()) {
                MapTile t = this.tileQueue.poll();

                t.generate(this.provider);

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

            Minecraft.getInstance().player.sendMessage(new TranslationTextComponent("commands.pioneer.finish", this.jobsize, time));
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
        mapinfo.addProperty("worldname", this.world.getWorldInfo().getWorldName());
        mapinfo.addProperty("dimensiontype", this.world.getDimension().getType().getRegistryName().toString());
        mapinfo.addProperty("dimension", this.world.getDimension().getType().getId()+"");
        mapinfo.addProperty("seed", this.world.getWorldInfo().getSeed()+"");

        JsonObject generator = new JsonObject();
        generator.addProperty("name", this.world.getWorldType().getTranslationKey());
        generator.addProperty("version", this.world.getWorldType().getVersion());
        generator.addProperty("options", this.world.getWorldInfo().getGeneratorOptions().toString());
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

        for (Biome biome : ForgeRegistries.BIOMES) {

            if (biome == null) {
                continue;
            }

            JsonObject bson = new JsonObject();

            bson.addProperty("name", biome.getDisplayName().getFormattedText());
            bson.addProperty("temperature", biome.getDefaultTemperature());
            bson.addProperty("moisture", biome.getDownfall());
            bson.addProperty("precipitation", biome.getPrecipitation().getName());
            bson.addProperty("height", biome.getDepth());
            bson.addProperty("heightvariation", 0);
            bson.addProperty("ismutation", biome.isMutation());

            if (biome.isMutation()) {
                // bson.addProperty("mutationof", Biome.MUTATION_TO_BASE_ID_MAP.get(biome));
                String bp = biome.getParent();

                if (bp != null) {
                    ResourceLocation locBiome = ForgeRegistries.BIOMES.getKey(biome);
                    Biome parent = ForgeRegistries.BIOMES.getValue(new ResourceLocation(locBiome.getNamespace() + ":" + bp));

                    if (parent != null) {
                        bson.addProperty("mutationof", Registry.BIOME.getId(parent));
                    } else {
                        Biome parent2 = ForgeRegistries.BIOMES.getValue(new ResourceLocation("minecraft:" + bp));
                        if (parent2 != null) {
                            bson.addProperty("mutationof", Registry.BIOME.getId(parent2));
                        } else {
                            bson.addProperty("mutationof", 0);
                        }
                    }
                } else {
                    bson.addProperty("mutationof", 0);
                }
            }

            bson.addProperty("colour", "#" + Integer.toHexString(Pioneer.mapColours.getBiomeMapColour(biome)).substring(2));

            biomes.add(Registry.BIOME.getId(biome) + "", bson);
        }

        this.zip.putNextEntry(new ZipEntry("biomes.json"));
        bytes = biomes.toString().getBytes();
        this.zip.write(bytes, 0, bytes.length);
        this.zip.closeEntry();
    }


}
