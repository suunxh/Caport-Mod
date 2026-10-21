package io.github.suunxh.caport.fabric;

import io.github.suunxh.housingteleporthelper.core.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.*;

public final class FabricWorldView implements WorldView {
    private final ClientLevel world;
    private final LocalPlayer player;
    private LevelChunk cached;
    private int cachedX, cachedZ;
    public FabricWorldView(ClientLevel world,LocalPlayer player) { this.world=world;this.player=player; }
    public void beginTick() { cached=null; }
    @Override public int minY() { return world.getMinY(); }
    @Override public int maxY() { return world.getMinY()+world.getHeight(); }
    @Override public boolean isLoaded(int x,int y,int z) { return y>=minY() && y<maxY() && world.hasChunk(x>>4,z>>4); }
    @Override public boolean isGeometryLoaded(int x,int y,int z) { return isLoaded(x,y,z) && neighbors(x,z); }
    private boolean neighbors(int x,int z) {
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(!world.hasChunk((x+dx)>>4,(z+dz)>>4))return false;
        return true;
    }
    private LevelChunk chunk(int x,int y,int z) {
        if(!isLoaded(x,y,z))return null;
        if(cached==null || cachedX!=(x>>4) || cachedZ!=(z>>4)) {
            cached=world.getChunk(x>>4,z>>4);cachedX=x>>4;cachedZ=z>>4;
        }
        return cached;
    }
    @Override public boolean isSectionEmpty(int x,int y,int z) {
        LevelChunk c=chunk(x,y,z);return c==null || c.getSection(world.getSectionIndex(y)).hasOnlyAir();
    }
    @Override public boolean isPressurePlate(int x,int y,int z) {
        LevelChunk c=chunk(x,y,z);
        return c!=null && c.getSection(world.getSectionIndex(y)).getBlockState(x&15,y&15,z&15).getBlock() instanceof BasePressurePlateBlock;
    }
    @Override public boolean isClimbable(int x,int y,int z) {
        return isGeometryLoaded(x,y,z) && world.getBlockState(new BlockPos(x,y,z)).is(BlockTags.CLIMBABLE);
    }
    @Override public boolean isLadder(int x,int y,int z) {
        return isGeometryLoaded(x,y,z) && world.getBlockState(new BlockPos(x,y,z)).getBlock() instanceof LadderBlock;
    }
    @Override public boolean isLowerDoor(int x,int y,int z) {
        if(!isGeometryLoaded(x,y,z))return false;
        BlockState state=world.getBlockState(new BlockPos(x,y,z));
        return state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER;
    }
    @Override public boolean isCobweb(int x,int y,int z) {
        return isGeometryLoaded(x,y,z) && world.getBlockState(new BlockPos(x,y,z)).is(Blocks.COBWEB);
    }
    @Override public boolean isTargetDecoration(int x,int y,int z) {
        if(!isGeometryLoaded(x,y,z))return false;
        BlockState state=world.getBlockState(new BlockPos(x,y,z));Block block=state.getBlock();
        return state.is(BlockTags.FLOWERS) || block==Blocks.SHORT_GRASS || block==Blocks.TALL_GRASS
            || block==Blocks.FERN || block==Blocks.LARGE_FERN || block==Blocks.DEAD_BUSH
            || state.is(BlockTags.RAILS) || block==Blocks.TORCH || block==Blocks.WALL_TORCH
            || block==Blocks.REDSTONE_TORCH || block==Blocks.REDSTONE_WALL_TORCH
            || block==Blocks.SOUL_TORCH || block==Blocks.SOUL_WALL_TORCH || block instanceof SignBlock;
    }
    @Override public List<Box> collisionBoxes(int x,int y,int z) {
        if(!isGeometryLoaded(x,y,z))return Collections.emptyList();
        BlockPos pos=new BlockPos(x,y,z);List<Box> result=new ArrayList<>();
        for(AABB b:world.getBlockState(pos).getCollisionShape(world,pos,CollisionContext.of(player)).toAabbs())
            result.add(new Box(b.minX+x,b.minY+y,b.minZ+z,b.maxX+x,b.maxY+y,b.maxZ+z));
        return result;
    }
    @Override public boolean hasClearance(Box b) {
        for(int x=Vec3d.floor(b.minX)-1;x<=Vec3d.floor(b.maxX)+1;x++)
            for(int z=Vec3d.floor(b.minZ)-1;z<=Vec3d.floor(b.maxZ)+1;z++)if(!world.hasChunk(x>>4,z>>4))return false;
        AABB box=new AABB(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);
        if(world.getBlockCollisions(player,box).iterator().hasNext() || world.containsAnyLiquid(box))return false;
        for(int x=Vec3d.floor(b.minX);x<=Vec3d.floor(b.maxX);x++)for(int z=Vec3d.floor(b.minZ);z<=Vec3d.floor(b.maxZ);z++)
            for(int y=Math.max(minY(),Vec3d.floor(b.minY));y<=Math.min(maxY()-1,Vec3d.floor(b.maxY));y++) {
                Block block=world.getBlockState(new BlockPos(x,y,z)).getBlock();
                if(block==Blocks.FIRE || block==Blocks.SOUL_FIRE)return false;
            }
        return true;
    }
    @Override public Vec3d rayHit(int x,int y,int z,Vec3d start,Vec3d end) {
        if(!isGeometryLoaded(x,y,z))return null;
        BlockPos pos=new BlockPos(x,y,z);BlockState state=world.getBlockState(pos);
        BlockHitResult hit=state.getShape(world,pos,CollisionContext.of(player)).clip(
            new Vec3(start.x,start.y,start.z),new Vec3(end.x,end.y,end.z),pos);
        if(hit==null)return null;Vec3 v=hit.getLocation();return new Vec3d(v.x,v.y,v.z);
    }
}
