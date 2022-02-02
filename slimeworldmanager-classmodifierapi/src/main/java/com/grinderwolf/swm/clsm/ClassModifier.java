package com.grinderwolf.swm.clsm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

/**
 * This class serves as a bridge between the SWM and the Minecraft server.
 *
 * As plugins are loaded using a different ClassLoader, their code cannot
 * be accessed from a NMS method. Because of this, it's impossible to make
 * any calls to any method when rewriting the bytecode of a NMS class.
 *
 * As a workaround, this bridge simply calls a method of the {@link CLSMBridge} interface,
 * which is implemented by the SWM plugin when loaded.
 */
public class ClassModifier {

    // Required for Paper 1.13 as javassist can't compile this class
    public static final BooleanSupplier BOOLEAN_SUPPLIER = () -> true;

    private static Set<CLSMBridge> customLoaders = new HashSet<>();

    public static CompletableFuture getFutureChunk(Object world, int x, int z) {

        return getBridgeForWorld(world).map(clsmBridge -> CompletableFuture.supplyAsync(() ->
                clsmBridge.getChunk(world, x, z)
        )).orElse(null);
    }

    public static boolean saveChunk(Object world, Object chunkAccess) {
        return getBridgeForWorld(world).map(clsmBridge -> clsmBridge.saveChunk(world, chunkAccess)).orElse(false);
    }

    private static Optional<CLSMBridge> getBridgeForWorld(Object world) {
        return customLoaders.stream().filter(clsmBridge -> clsmBridge.isCustomWorld(world)).findFirst();
    }

    public static boolean isCustomWorld(Object world) {
        return getBridgeForWorld(world).isPresent();
    }

    public static boolean skipWorldAdd(Object world) {
        return getBridgeForWorld(world).map(clsmBridge -> clsmBridge.skipWorldAdd(world)).orElse(false);
    }

    /**
     * @deprecated use {@link #registerLoader(CLSMBridge)} instead
     * @param loader
     */
    @Deprecated
    public static void setLoader(CLSMBridge loader) {
        customLoaders.add(loader);
    }

    public static void registerLoader(CLSMBridge loader) {
        customLoaders.add(loader);
    }

    public static Object[] getDefaultWorlds() {
        return customLoaders.stream().flatMap(clsmBridge -> Arrays.stream(clsmBridge.getDefaultWorlds())).toArray();
    }

    public static Object getDefaultGamemode() {
        return customLoaders.stream().findFirst().map(CLSMBridge::getDefaultGamemode).orElse(null);
    }
}
