package com.grinderwolf.swm.nms.v1181;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.flowpowered.nbt.*;
import com.grinderwolf.swm.api.utils.*;
import com.grinderwolf.swm.api.world.*;
import com.grinderwolf.swm.nms.*;
import com.mojang.serialization.*;
import lombok.*;
import net.minecraft.core.*;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.lighting.*;
import org.bukkit.craftbukkit.v1_18_R1.entity.*;

import java.util.*;

public class NMSSlimeChunk implements SlimeChunk {

    private LevelChunk chunk;

    public NMSSlimeChunk(LevelChunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public String getWorldName() {
        return chunk.getLevel().getMinecraftWorld().serverLevelData.getLevelName();
    }

    @Override
    public int getX() {
        return chunk.getPos().x;
    }

    @Override
    public int getZ() {
        return chunk.getPos().z;
    }

    @Override
    public SlimeChunkSection[] getSections() {
        SlimeChunkSection[] sections = new SlimeChunkSection[this.chunk.getMaxSection() - this.chunk.getMinSection() + 1];
        LevelLightEngine lightEngine = chunk.getLevel().getChunkSource().getLightEngine();

        Registry<Biome> biomeRegistry = chunk.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);

        // Ignore deprecation, spigot only method
        Codec<PalettedContainer<Biome>> codec = PalettedContainer.codec(biomeRegistry, biomeRegistry.byNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, (Biome) biomeRegistry.getOrThrow(Biomes.PLAINS));

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            LevelChunkSection section = chunk.getSections()[sectionId];
            // Sections CANNOT be null in 1.18

            // Block Light Nibble Array
            NibbleArray blockLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            // Sky light Nibble Array
            NibbleArray skyLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            // Tile/Entity Data

            // Block Data
            Tag blockStateData = ChunkSerializer.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow(false, System.err::println); // todo error handling
            Tag biomeData = codec.encodeStart(NbtOps.INSTANCE, section.getBiomes()).getOrThrow(false, System.err::println); // todo error handling

            CompoundTag blockStateTag = (CompoundTag) Converter.convertTag("", blockStateData);
            CompoundTag biomeTag = (CompoundTag) Converter.convertTag("", biomeData);

            sections[sectionId] = new CraftSlimeChunkSection(null, null, null, null, blockStateTag, biomeTag, blockLightArray, skyLightArray);
        }

        return sections;
    }

    @Override
    public int getMinSection() {
        return this.chunk.getMinSection();
    }

    @Override
    public int getMaxSection() {
        return this.chunk.getMaxSection();
    }

    @Override
    public CompoundTag getHeightMaps() {
        // HeightMap
        CompoundMap heightMaps = new CompoundMap();

        for (Map.Entry<Heightmap.Types, Heightmap> entry : chunk.heightmaps.entrySet()) {
            if (!entry.getKey().keepAfterWorldgen()) {
                continue;
            }

            Heightmap.Types type = entry.getKey();
            Heightmap map = entry.getValue();

            heightMaps.put(type.name(), new LongArrayTag(type.name(), map.getRawData()));
        }

        return new CompoundTag("", heightMaps);
    }

    @Override
    public int[] getBiomes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CompoundTag> getTileEntities() {
        List<CompoundTag> tileEntities = new ArrayList<>();

        for (BlockEntity entity : chunk.blockEntities.values()) {
            final net.minecraft.nbt.CompoundTag entityNbt = entity.saveWithFullMetadata();
            tileEntities.add((CompoundTag) Converter.convertTag(entityNbt.getString("id"), entityNbt));
        }

        return tileEntities;
    }

    @Override
    public List<CompoundTag> getEntities() {
        List<CompoundTag> entities = new ArrayList<>();

        for (var bukkitEntity : chunk.getBukkitChunk().getEntities()) { // Use api method for performance gains
            Entity entity = ((CraftEntity) bukkitEntity).getHandle();

            net.minecraft.nbt.CompoundTag entityNbt = new net.minecraft.nbt.CompoundTag();
            if (entity.save(entityNbt)) {
                entities.add((CompoundTag) Converter.convertTag("", entityNbt));
            }
        }
        return entities;
    }

    public LevelChunk getChunk() {
        return chunk;
    }

    public void setChunk(LevelChunk chunk) {
        this.chunk = chunk;
    }
}
