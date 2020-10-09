package ttftcuts.pioneer.map;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.Direction;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.DimensionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MapColours {
    public final Map<Biome, Integer> biomeColours = new HashMap<>();

    public SharedSeedRandom rand = new SharedSeedRandom(50);

    public MapColours() {}

    public int getBiomeMapColour(Biome biome) {
        if (biomeColours.containsKey(biome)) {
            return biomeColours.get(biome);
        }

        int colour = getBiomeMapColourRaw(biome);
        biomeColours.put(biome, colour);
        return colour;
    }

    public int getBiomeMapColourRaw(Biome biome) {

        boolean treebased = false;
        int colour = this.getTopColour(biome);

        if (biome.getCategory() == Biome.Category.FOREST) {
            colour = blend(biome.getFoliageColor(), 0xff0b7000, 0.35);
            treebased = true;
        }


        if (biome.getCategory() == Biome.Category.RIVER
                || biome.getCategory() == Biome.Category.OCEAN) {
            colour = blend(colour, 0xff4582ff, 0.7); // sea blue
        }

        if (biome.getDepth() > 0.0) {
            double mod = Math.min(biome.getDepth() * 0.2 + 1.0, 1.35);
            colour = brightness(colour, mod);
        } else if (biome.getDepth() <= -1.2) {
            colour = brightness(colour, 0.9);
        }

        if (treebased) {
            colour = temptint(colour, biome.getTemperature());
        }

        if (biome.getPrecipitation() == Biome.RainType.SNOW) {
            colour = blend(colour, 0xffffffff, 0.5); // icy pale cyan
            //colour = blend(colour, 0xffc9e4ff, 0.25);
            colour = brightness(colour, 1.2);
        }

        return colour | 0xFF000000;
    }

    public int getTopColour(Biome biome) {

        BlockPos.Mutable pos = new BlockPos.Mutable(0,64,0);

        return this.getBiomeBlockColourForCoords(biome, pos);
    }

    public int getBiomeBlockColourForCoords(Biome biome, BlockPos pos) {
        int colour;

        if (biome.getGenerationSettings().getSurfaceBuilderConfig().getTop() == Blocks.GRASS_BLOCK.getDefaultState()) { // uuuugh
            IBlockReader overworld = Minecraft.getInstance().getIntegratedServer().getWorld(World.OVERWORLD);
            colour = biome.getGenerationSettings().getSurfaceBuilderConfig().getTop().getMaterialColor(overworld, pos).colorValue | 0xFF000000;
            int tint = biome.getGrassColor(pos.getX(), pos.getZ()) | 0xFF000000;
            colour = blend(colour,tint, 0.75);
        } else {
            colour = this.getBlockColourRaw(biome.getGenerationSettings().getSurfaceBuilderConfig().getTop());
        }

        return colour;
    }

    public int getBlockColourRaw(BlockState block) {
        Minecraft mc = Minecraft.getInstance();
        BlockRendererDispatcher brd = mc.getBlockRendererDispatcher();
        BlockModelShapes shapes = brd.getBlockModelShapes();
        BlockColors colours = mc.getBlockColors();

        IBlockReader overworld = Minecraft.getInstance().getIntegratedServer().getWorld(World.OVERWORLD);
        int colour = block.getMaterialColor(overworld, new BlockPos(0, 255, 0)).colorValue | 0xFF000000;
        int fallback = colour;

        if (block == Blocks.GRASS.getDefaultState()) {
            // ugh
        } else {

            try {
                IBakedModel topmodel = shapes.getModel(block);
                List<BakedQuad> topquads = topmodel.getQuads(block, Direction.UP, new Random(0));

                for (BakedQuad quad : topquads) {
                    colour = block.getMaterialColor(overworld, new BlockPos(0, 255, 0)).colorValue | 0xFF000000;
                    if (quad.hasTintIndex()) {
                        int tint = colours.getColor(block, null, null, quad.getTintIndex()) | 0xFF000000;
                        //colour = intAverage(colour, tint);
                        //colour = intAverage(colour, tint);
                        //colour = intAverage(colour, tint);

                        colour = blend(colour, tint, 0.75);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                colour = fallback;
            }
        }

        return colour;
    }

    public static int intAverage(int a, int b) {
        return (int)( ((((a) ^ (b)) & 0xfffefefeL) >> 1) + ((a) & (b)) );
    }

    public static int blend(int a, int b, double mix) {
        if (mix == 0) {
            return a;
        } else if (mix == 1) {
            return b;
        } else if (mix == 0.5) {
            return intAverage(a,b);
        }

        int ar = (a & 0x00FF0000) >> 16;
        int ag = (a & 0x0000FF00) >> 8;
        int ab = (a & 0x000000FF);

        int br = (b & 0x00FF0000) >> 16;
        int bg = (b & 0x0000FF00) >> 8;
        int bb = (b & 0x000000FF);

        int mr = (int)Math.min(255,Math.max(0,Math.floor(ar * (1.0-mix) + br * mix)));
        int mg = (int)Math.min(255,Math.max(0,Math.floor(ag * (1.0-mix) + bg * mix)));
        int mb = (int)Math.min(255,Math.max(0,Math.floor(ab * (1.0-mix) + bb * mix)));

        return (mr << 16) | (mg << 8) | (mb) | 0xFF000000;
    }

    public static int brightness(int col, double light) {
        int r = (col & 0x00FF0000) >> 16;
        int g = (col & 0x0000FF00) >> 8;
        int b = (col & 0x000000FF);

        r = (int)Math.min(255,Math.floor(r * light));
        g = (int)Math.min(255,Math.floor(g * light));
        b = (int)Math.min(255,Math.floor(b * light));

        return (r << 16) | (g << 8) | (b) | 0xFF000000;
    }

    public static int temptint(int col, double temp) {
        int r = (col & 0x00FF0000) >> 16;
        int g = (col & 0x0000FF00) >> 8;
        int b = (col & 0x000000FF);

        double limit = 0.25;
        double factor = Math.max(-limit, Math.min(limit, (temp - 0.4) * 0.75));

        r = (int)Math.min(255,Math.floor(r * (1+factor)));
        g = (int)Math.min(255,Math.floor(g * (1+factor * 0.5)));
        b = (int)Math.min(255,Math.floor(b * (1-factor * 2.5)));

        return (r << 16) | (g << 8) | (b) | 0xFF000000;
    }
}
