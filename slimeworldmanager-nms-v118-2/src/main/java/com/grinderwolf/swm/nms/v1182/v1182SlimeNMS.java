package com.grinderwolf.swm.nms.v1182;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.*;
import com.grinderwolf.swm.api.world.properties.*;
import com.grinderwolf.swm.nms.*;
import com.mojang.serialization.*;
import lombok.*;
import net.minecraft.*;
import net.minecraft.core.Registry;
import net.minecraft.core.*;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.*;
import net.minecraft.server.dedicated.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.util.datafix.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.dimension.*;
import net.minecraft.world.level.storage.*;
import org.apache.commons.io.*;
import org.apache.logging.log4j.*;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_18_R2.*;
import org.bukkit.event.world.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Getter
public class v1182SlimeNMS implements SlimeNMS {

    private static final Logger LOGGER = LogManager.getLogger("SWM");
    private static final File UNIVERSE_DIR;
    public static LevelStorageSource CONVERTABLE;
    public static boolean isPaperMC;

    static {
        Path path;

        try {
            path = Files.createTempDirectory("swm-" + UUID.randomUUID().toString().substring(0, 5) + "-");
        } catch (IOException ex) {
//            LOGGER.log(Level.FATAL, "Failed to create temp directory", ex);
            path = null;
            System.exit(1);
        }

        UNIVERSE_DIR = path.toFile();
        CONVERTABLE = LevelStorageSource.createDefault(path);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            try {
                FileUtils.deleteDirectory(UNIVERSE_DIR);
            } catch (IOException ex) {
//                LOGGER.log(Level.FATAL, "Failed to delete temp directory", ex);
            }

        }));
    }

    private final byte worldVersion = 0x08;

    private boolean loadingDefaultWorlds = true; // If true, the addWorld method will not be skipped

    private CustomWorldServer defaultWorld;
    private CustomWorldServer defaultNetherWorld;
    private CustomWorldServer defaultEndWorld;

    public v1182SlimeNMS(boolean isPaper) {
        try {
            isPaperMC = isPaper;
            CraftCLSMBridge.initialize(this);
        } catch (NoClassDefFoundError ex) {
            LOGGER.error("Failed to find ClassModifier classes. Are you sure you installed it correctly?", ex);
            Bukkit.getServer().shutdown();
        }
    }

    @Override
    public void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) {
        if (normalWorld != null) {
            defaultWorld = createDefaultWorld(normalWorld, LevelStem.OVERWORLD, ServerLevel.OVERWORLD);
        }

        if (netherWorld != null) {
            defaultNetherWorld = createDefaultWorld(netherWorld, LevelStem.NETHER, ServerLevel.NETHER);
        }

        if (endWorld != null) {
            defaultEndWorld = createDefaultWorld(endWorld, LevelStem.END, ServerLevel.END);
        }

        loadingDefaultWorlds = false;
    }

    private CustomWorldServer createDefaultWorld(SlimeWorld world, ResourceKey<LevelStem> dimensionKey,
                                                 ResourceKey<Level> worldKey) {
        PrimaryLevelData worldDataServer = createWorldData(world);

        Registry<LevelStem> registryMaterials = worldDataServer.worldGenSettings().dimensions();
        LevelStem worldDimension = registryMaterials.get(dimensionKey);
        Holder<DimensionType> dimensionManager = worldDimension.typeHolder();
        ChunkGenerator chunkGenerator = worldDimension.generator();

        World.Environment environment = getEnvironment(world);

        if (dimensionKey == LevelStem.NETHER && environment != World.Environment.NORMAL) {
//            LOGGER.warn("The environment for the default world should always be 'NORMAL'.");
        }

        try {
            return new CustomWorldServer((CraftSlimeWorld) world, worldDataServer,
                    worldKey, dimensionKey, dimensionManager, chunkGenerator, environment);
        } catch (IOException ex) {
            throw new RuntimeException(ex); // TODO do something better with this?
        }
    }

    @Override
    public void generateWorld(SlimeWorld world) {
        String worldName = world.getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        PrimaryLevelData worldDataServer = createWorldData(world);
        World.Environment environment = getEnvironment(world);
        ResourceKey<LevelStem> dimension;

        switch (environment) {
            case NORMAL:
                dimension = LevelStem.OVERWORLD;
                break;
            case NETHER:
                dimension = LevelStem.NETHER;
                break;
            case THE_END:
                dimension = LevelStem.END;
                break;
            default:
                throw new IllegalArgumentException("Unknown dimension supplied");
        }

        Registry<LevelStem> materials = worldDataServer.worldGenSettings().dimensions();
        LevelStem worldDimension = materials.get(dimension);
        Holder<DimensionType> dimensionManager = worldDimension.typeHolder();
        ChunkGenerator chunkGenerator = worldDimension.generator();

        ResourceKey<Level> worldKey = ResourceKey.create(Registry.DIMENSION_REGISTRY,
                new ResourceLocation(worldName.toLowerCase(java.util.Locale.ENGLISH)));

        Holder<DimensionType> type = null;
        {
            DimensionType predefinedType = worldDimension.typeHolder().value();

            OptionalLong fixedTime = switch (environment) {
                case NORMAL -> OptionalLong.empty();
                case NETHER -> OptionalLong.of(18000L);
                case THE_END -> OptionalLong.of(6000L);
                case CUSTOM -> throw new UnsupportedOperationException();
            };
            double light = switch (environment) {
                case NORMAL, THE_END -> 0;
                case NETHER -> 0.1;
                case CUSTOM -> throw new UnsupportedOperationException();
            };

            TagKey<Block> infiniburn = switch (environment) {
                case NORMAL -> BlockTags.INFINIBURN_OVERWORLD;
                case NETHER -> BlockTags.INFINIBURN_NETHER;
                case THE_END -> BlockTags.INFINIBURN_END;
                case CUSTOM -> throw new UnsupportedOperationException();
            };


            type = Holder.direct(DimensionType.create(fixedTime, predefinedType.hasSkyLight(), predefinedType.hasCeiling(),
                    predefinedType.ultraWarm(), predefinedType.natural(), predefinedType.coordinateScale(),
                    world.getPropertyMap().getValue(SlimeProperties.DRAGON_BATTLE), predefinedType.piglinSafe(), predefinedType.bedWorks(),
                    predefinedType.respawnAnchorWorks(), predefinedType.hasRaids(),
                    predefinedType.minY(), predefinedType.height(), predefinedType.logicalHeight(),
                    infiniburn,
                    predefinedType.effectsLocation(),
                    (float) light));
        }


        CustomWorldServer server;

        try {
            server = new CustomWorldServer((CraftSlimeWorld) world, worldDataServer,
                    worldKey, dimension, type, chunkGenerator, environment);
        } catch (IOException ex) {
            throw new RuntimeException(ex); // TODO do something better with this?
        }

        server.setReady(true);

        MinecraftServer mcServer = MinecraftServer.getServer();
        mcServer.initWorld(server, worldDataServer, mcServer.getWorldData(), worldDataServer.worldGenSettings());

        mcServer.levels.put(worldKey, server);

        server.setSpawnSettings(world.getPropertyMap().getValue(SlimeProperties.ALLOW_MONSTERS), world.getPropertyMap().getValue(SlimeProperties.ALLOW_ANIMALS));

        Bukkit.getPluginManager().callEvent(new WorldInitEvent(server.getWorld()));
//        try {
//            world.getLoader().loadWorld(worldName, world.isReadOnly());
//        } catch(UnknownWorldException | WorldInUseException | IOException e) {
//            e.printStackTrace();
//        }
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(server.getWorld()));
    }

    private World.Environment getEnvironment(SlimeWorld world) {
        return World.Environment.valueOf(world.getPropertyMap().getValue(SlimeProperties.ENVIRONMENT).toUpperCase());
    }

    private PrimaryLevelData createWorldData(SlimeWorld world) {
        String worldName = world.getName();
        CompoundTag extraData = world.getExtraData();
        PrimaryLevelData worldDataServer;
        net.minecraft.nbt.CompoundTag extraTag = (net.minecraft.nbt.CompoundTag) Converter.convertTag(extraData);
        MinecraftServer mcServer = MinecraftServer.getServer();
        DedicatedServerProperties serverProps = ((DedicatedServer) mcServer).getProperties();

        if (extraTag.getTagType("LevelData") == Tag.TAG_COMPOUND) {
            net.minecraft.nbt.CompoundTag levelData = extraTag.getCompound("LevelData");
            int dataVersion = levelData.getTagType("DataVersion") == Tag.TAG_INT ? levelData.getInt("DataVersion") : -1;
            Dynamic<Tag> dynamic = mcServer.getFixerUpper().update(DataFixTypes.LEVEL.getType(),
                    new Dynamic<>(NbtOps.INSTANCE, levelData), dataVersion, SharedConstants.getCurrentVersion()
                            .getWorldVersion());

            LevelVersion levelVersion = LevelVersion.parse(dynamic);
            LevelSettings worldSettings = LevelSettings.parse(dynamic, mcServer.datapackconfiguration);

            worldDataServer = PrimaryLevelData.parse(dynamic, mcServer.getFixerUpper(), dataVersion, null,
                    worldSettings, levelVersion, serverProps.getWorldGenSettings(mcServer.registryHolder), Lifecycle.stable());
        } else {

            // Game rules
            Optional<CompoundTag> gameRules = extraData.getAsCompoundTag("gamerules");
            GameRules rules = new GameRules();

            gameRules.ifPresent(compoundTag -> {
                net.minecraft.nbt.CompoundTag compound = ((net.minecraft.nbt.CompoundTag) Converter.convertTag(compoundTag));
                Map<String, GameRules.Key<?>> gameRuleKeys = CraftWorld.getGameRulesNMS();

                compound.getAllKeys().forEach(gameRule -> {
                    if (gameRuleKeys.containsKey(gameRule)) {
                        GameRules.Value<?> gameRuleValue = rules.getRule(gameRuleKeys.get(gameRule));
                        String theValue = compound.getString(gameRule);
                        gameRuleValue.deserialize(theValue);
                        gameRuleValue.onChanged(mcServer);
                    }
                });
            });

            LevelSettings worldSettings = new LevelSettings(worldName, serverProps.gamemode, false,
                    serverProps.difficulty, false, rules, mcServer.datapackconfiguration);

            worldDataServer = new PrimaryLevelData(worldSettings, serverProps.getWorldGenSettings(mcServer.registryHolder), Lifecycle.stable());
        }

        worldDataServer.checkName(worldName);
        worldDataServer.setModdedInfo(mcServer.getServerModName(), mcServer.getModdedStatus().shouldReportAsModified());
        worldDataServer.setInitialized(true);

        return worldDataServer;
    }

    @Override
    public SlimeWorld getSlimeWorld(World world) {
        CraftWorld craftWorld = (CraftWorld) world;

        if (!(craftWorld.getHandle() instanceof CustomWorldServer)) {
            return null;
        }

        CustomWorldServer worldServer = (CustomWorldServer) craftWorld.getHandle();
        return worldServer.getSlimeWorld();
    }

    @Override
    public CompoundTag convertChunk(CompoundTag tag) {
        net.minecraft.nbt.CompoundTag nmsTag = (net.minecraft.nbt.CompoundTag) Converter.convertTag(tag);
        int version = nmsTag.getInt("DataVersion");

        net.minecraft.nbt.CompoundTag newNmsTag = NbtUtils.update(DataFixers.getDataFixer(), DataFixTypes.CHUNK, nmsTag, version);

        return (CompoundTag) Converter.convertTag("", newNmsTag);
    }
}