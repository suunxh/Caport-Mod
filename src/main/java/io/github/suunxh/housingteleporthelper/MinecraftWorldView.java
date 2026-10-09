package io.github.suunxh.housingteleporthelper;

import io.github.suunxh.housingteleporthelper.core.Box;
import io.github.suunxh.housingteleporthelper.core.Vec3d;
import io.github.suunxh.housingteleporthelper.core.WorldView;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MinecraftWorldView implements WorldView {
    private final WorldClient world;
    private final EntityPlayerSP player;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private Chunk cachedChunk;
    private int cachedChunkX = Integer.MIN_VALUE, cachedChunkZ = Integer.MIN_VALUE;
    public MinecraftWorldView(WorldClient world, EntityPlayerSP player) { this.world = world; this.player = player; }
    public void beginTick() { cachedChunk = null; }

    @Override public boolean isLoaded(int x, int y, int z) {
        // isBlockLoaded(..., false) only consults the provider. Never provide/load a chunk first.
        return y >= 0 && y < 256 && world.isBlockLoaded(cursor.set(x, y, z), false);
    }
    @Override public boolean isGeometryLoaded(int x, int y, int z) {
        return isLoaded(x, y, z) && neighborChunksLoaded(x, z);
    }
    private Chunk loadedChunk(int x, int y, int z) {
        if (!isLoaded(x, y, z)) return null;
        int cx = x >> 4, cz = z >> 4;
        if (cachedChunk == null || cx != cachedChunkX || cz != cachedChunkZ) {
            cachedChunk = world.getChunkFromChunkCoords(cx, cz);
            cachedChunkX = cx; cachedChunkZ = cz;
        }
        return cachedChunk;
    }
    @Override public boolean isSectionEmpty(int x, int y, int z) {
        Chunk chunk = loadedChunk(x, y, z);
        if (chunk == null) return true;
        ExtendedBlockStorage section = chunk.getBlockStorageArray()[y >> 4];
        return section == null || section.isEmpty();
    }
    @Override public boolean isPressurePlate(int x, int y, int z) {
        Chunk chunk = loadedChunk(x, y, z);
        if (chunk == null) return false;
        ExtendedBlockStorage section = chunk.getBlockStorageArray()[y >> 4];
        if (section == null || section.isEmpty()) return false;
        Block block = section.get(x & 15, y & 15, z & 15).getBlock();
        return block == Blocks.stone_pressure_plate || block == Blocks.wooden_pressure_plate
                || block == Blocks.light_weighted_pressure_plate || block == Blocks.heavy_weighted_pressure_plate;
    }
    @Override public List<Box> collisionBoxes(int x, int y, int z) {
        if (!isLoaded(x, y, z) || !neighborChunksLoaded(x, z)) return Collections.emptyList();
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = world.getBlockState(pos);
        List<AxisAlignedBB> raw = new ArrayList<AxisAlignedBB>();
        // addCollisionBoxesToList preserves multipart stair/fence geometry.
        state.getBlock().addCollisionBoxesToList(world, pos, state,
                new AxisAlignedBB(x - 1, y - 1, z - 1, x + 2, y + 3, z + 2), raw, player);
        List<Box> boxes = new ArrayList<Box>(raw.size());
        for (AxisAlignedBB box : raw) boxes.add(convert(box));
        return boxes;
    }
    @Override public boolean hasClearance(Box bounds) {
        // A one-block border covers blocks whose collision geometry reads neighbors.
        int minX = Vec3d.floor(bounds.minX) - 1, maxX = Vec3d.floor(bounds.maxX) + 1;
        int minZ = Vec3d.floor(bounds.minZ) - 1, maxZ = Vec3d.floor(bounds.maxZ) + 1;
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
                if (!isLoaded(x, Math.max(0, Vec3d.floor(bounds.minY)), z)) return false;
        AxisAlignedBB box = convert(bounds);
        if (!world.getCollidingBoundingBoxes(player, box).isEmpty() || world.isAnyLiquid(box)) return false;
        for (int x = Vec3d.floor(bounds.minX); x <= Vec3d.floor(bounds.maxX); x++)
            for (int z = Vec3d.floor(bounds.minZ); z <= Vec3d.floor(bounds.maxZ); z++)
                for (int y = Vec3d.floor(bounds.minY); y <= Math.min(255, Vec3d.floor(bounds.maxY)); y++)
                    if (world.getBlockState(cursor.set(x, y, z)).getBlock() == Blocks.fire) return false;
        return true;
    }
    @Override public Vec3d rayHit(int x, int y, int z, Vec3d start, Vec3d end) {
        if (!isLoaded(x, y, z) || !neighborChunksLoaded(x, z)) return null;
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!block.canCollideCheck(state, false)) return null;
        MovingObjectPosition hit = block.collisionRayTrace(world, pos,
                new Vec3(start.x, start.y, start.z), new Vec3(end.x, end.y, end.z));
        return hit == null ? null : new Vec3d(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
    }
    private boolean neighborChunksLoaded(int x, int z) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (!isLoaded(x + dx, 0, z + dz)) return false;
        return true;
    }
    private static Box convert(AxisAlignedBB b) { return new Box(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ); }
    private static AxisAlignedBB convert(Box b) { return new AxisAlignedBB(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ); }
}
