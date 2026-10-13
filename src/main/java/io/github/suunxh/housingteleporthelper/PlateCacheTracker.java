package io.github.suunxh.housingteleporthelper;

import io.github.suunxh.housingteleporthelper.core.PressurePlateIndex;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IWorldAccess;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Observe client world notifications; never request or modify server/chunk data. */
public final class PlateCacheTracker implements IWorldAccess {
    private final WorldClient world;
    private final PressurePlateIndex index;
    public PlateCacheTracker(WorldClient world, PressurePlateIndex index) {
        this.world = world; this.index = index;
        world.addWorldAccess(this); MinecraftForge.EVENT_BUS.register(this);
    }
    public void close() { world.removeWorldAccess(this); MinecraftForge.EVENT_BUS.unregister(this); }
    @SubscribeEvent public void load(ChunkEvent.Load event) {
        if (event.world == world) index.invalidateChunk(event.getChunk().xPosition, event.getChunk().zPosition);
    }
    @SubscribeEvent public void unload(ChunkEvent.Unload event) {
        if (event.world == world) index.invalidateChunk(event.getChunk().xPosition, event.getChunk().zPosition);
    }
    @Override public void markBlockForUpdate(BlockPos pos) { index.invalidate(pos.getX(), pos.getY(), pos.getZ()); }
    @Override public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = x1 >> 4; x <= x2 >> 4; x++) for (int z = z1 >> 4; z <= z2 >> 4; z++)
            for (int y = Math.max(0, y1 >> 4); y <= Math.min(15, y2 >> 4); y++) index.invalidate(x << 4, y << 4, z << 4);
    }
    @Override public void notifyLightSet(BlockPos pos) { }
    @Override public void playSound(String name, double x, double y, double z, float volume, float pitch) { }
    @Override public void playSoundToNearExcept(EntityPlayer player, String name, double x, double y, double z, float volume, float pitch) { }
    @Override public void spawnParticle(int id, boolean ignore, double x, double y, double z, double dx, double dy, double dz, int... parameters) { }
    @Override public void onEntityAdded(Entity entity) { }
    @Override public void onEntityRemoved(Entity entity) { }
    @Override public void playRecord(String name, BlockPos pos) { }
    @Override public void broadcastSound(int id, BlockPos pos, int data) { }
    @Override public void playAuxSFX(EntityPlayer player, int type, BlockPos pos, int data) { }
    @Override public void sendBlockBreakProgress(int id, BlockPos pos, int progress) { }
}
