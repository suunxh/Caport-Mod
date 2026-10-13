package io.github.suunxh.housingteleporthelper.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlateIndexTest {
    private final Vec3d origin = new Vec3d(0.5, 64, 0.5);
    private final DestinationValidator validator = new DestinationValidator(0.6, 1.8);
    private void warm(PressurePlateIndex index, FakeWorld world) {
        index.prepare(world, origin, 8);
        for (int i = 0; !index.isReady() && i < 100; i++) index.advance(world, 8192, Long.MAX_VALUE);
        assertTrue(index.isReady());
    }
    private Destination find(PressurePlateIndex index, FakeWorld world) {
        PlateSearch search = index.search(world, validator, origin, 0, 8, 30);
        for (int i = 0; !search.isDone() && i < 100; i++) search.advance(8192, Long.MAX_VALUE);
        assertTrue(search.isDone()); return search.result();
    }
    @Test public void warmLookupVisitsPlatesOnlyAndMatchesColdNearest() {
        FakeWorld world = new FakeWorld().plate(0, 64, 3).plate(0, 64, 6).plate(0, 64, -2);
        PressurePlateIndex index = new PressurePlateIndex(); warm(index, world); world.plateLookups = 0;
        assertEquals(3, find(index, world).blockZ); assertTrue(world.plateLookups <= 2);
        PlateSearch cold = new PressurePlateSearchService(world, validator, origin, 0, 8, 30);
        while (!cold.isDone()) cold.advance(8192, Long.MAX_VALUE);
        assertEquals(cold.result().feet.z, find(index, world).feet.z, 0);
    }
    @Test public void blockUpdatesInvalidateAndIncludeNewNearerPlate() {
        FakeWorld world = new FakeWorld().plate(0, 64, 6);
        PressurePlateIndex index = new PressurePlateIndex(); warm(index, world);
        world.plate(0, 64, 2); index.invalidate(0, 64, 2); assertFalse(index.isReady());
        assertEquals(2, find(index, world).blockZ); // cold fallback still finds the new plate
        warm(index, world); assertEquals(2, find(index, world).blockZ);
    }
    @Test public void cachedRemovedPlateAndChangedClearanceAreRevalidated() {
        FakeWorld world = new FakeWorld().plate(0, 64, 2).plate(0, 64, 6);
        PressurePlateIndex index = new PressurePlateIndex(); warm(index, world);
        world.plates.remove(world.key(0, 64, 2)); assertEquals(6, find(index, world).blockZ);
        world.solid(0, 65, 6); assertNull(find(index, world));
    }
    @Test public void unloadAndReloadCannotExecuteStalePlate() {
        FakeWorld world = new FakeWorld().plate(0, 64, 2);
        PressurePlateIndex index = new PressurePlateIndex(); warm(index, world);
        world.unload(0, 0); index.invalidateChunk(0, 0); warm(index, world); assertNull(find(index, world));
        world.unloadedChunks.clear(); index.invalidateChunk(0, 0); warm(index, world);
        assertEquals(2, find(index, world).blockZ);
    }
    @Test public void warmingHonorsBudgetAndClearDropsPreviousWorld() {
        FakeWorld world = new FakeWorld().plate(0, 64, 2);
        PressurePlateIndex index = new PressurePlateIndex(); index.prepare(world, origin, 8);
        index.advance(world, 32, Long.MAX_VALUE); assertTrue(world.plateLookups <= 32); assertFalse(index.isReady());
        warm(index, world); index.clear(); warm(index, new FakeWorld()); assertNull(find(index, new FakeWorld()));
    }
    @Test public void cachedTieBreakMatchesColdSearch() {
        FakeWorld world = new FakeWorld().plate(-1, 64, 3).plate(1, 64, 3).plate(4, 64, 1);
        PressurePlateIndex index = new PressurePlateIndex(); warm(index, world);
        assertEquals(-1, find(index, world).blockX);
    }
    @Test public void cachedLookupSupportsModernNegativeWorldHeight() {
        FakeWorld world = new FakeWorld() {
            @Override public int minY() { return -64; }
            @Override public int maxY() { return 320; }
        };
        world.plate(0,-40,3);
        Vec3d origin = new Vec3d(.5,-40.5,.5);
        PressurePlateIndex index = new PressurePlateIndex(); index.prepare(world,origin,8);
        for(int i=0;!index.isReady() && i<100;i++)index.advance(world,8192,Long.MAX_VALUE);
        assertTrue(index.isReady());
        PlateSearch search=index.search(world,validator,origin,0,8,30);
        assertTrue(search instanceof CachedPressurePlateSearch);
        search.advance(8192,Long.MAX_VALUE);assertTrue(search.isDone());
        assertEquals(-40,search.result().feet.y,0);
    }
}
