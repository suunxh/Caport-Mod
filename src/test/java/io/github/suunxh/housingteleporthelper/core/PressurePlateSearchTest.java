package io.github.suunxh.housingteleporthelper.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class PressurePlateSearchTest {
    private final DestinationValidator validator = new DestinationValidator(0.6, 1.8);
    private final Vec3d origin = new Vec3d(0.5, 64, 0.5);
    private Destination search(FakeWorld world, double yaw) {
        PressurePlateSearchService s = new PressurePlateSearchService(world, validator, origin, yaw, 64, 30);
        int iterations = 0;
        while (!s.isDone() && iterations++ < 10000) s.advance(8192, Long.MAX_VALUE);
        assertTrue("Search must terminate", s.isDone()); return s.result();
    }
    @Test public void directlyAhead() { assertEquals(5, search(new FakeWorld().plate(0, 64, 5), 0).blockZ); }
    @Test public void behindIsExcluded() { assertNull(search(new FakeWorld().plate(0, 64, -5), 0)); }
    @Test public void outsideConeIsExcluded() { assertNull(search(new FakeWorld().plate(10, 64, 5), 0)); }
    @Test public void nearestThreeDimensionalDistanceWins() {
        FakeWorld w = new FakeWorld().plate(0, 64, 10).plate(0, 64, 5).plate(0, 80, 2);
        assertEquals(5, search(w, 0).blockZ);
    }
    @Test public void equalDistanceUsesAngularTieBreak() {
        FakeWorld w = new FakeWorld().plate(0, 65, 3).plate(1, 64, 3);
        assertEquals(0, search(w, 0).blockX);
    }
    @Test public void remainingTiesUseCoordinates() {
        FakeWorld w = new FakeWorld().plate(1, 64, 3).plate(-1, 64, 3);
        assertEquals(-1, search(w, 0).blockX);
    }
    @Test public void includesRadiusBoundary() { assertNotNull(search(new FakeWorld().plate(0, 64, 64), 0)); }
    @Test public void excludesBeyondRadius() { assertNull(search(new FakeWorld().plate(0, 64, 65), 0)); }
    @Test public void excludesBeyondThreeDimensionalRadius() { assertNull(search(new FakeWorld().plate(0, 65, 64), 0)); }
    @Test public void plateAbovePlayer() { assertEquals(70, search(new FakeWorld().plate(0, 70, 5), 0).blockY); }
    @Test public void plateBelowPlayer() { assertEquals(60, search(new FakeWorld().plate(0, 60, 5), 0).blockY); }
    @Test public void unloadedChunkSkipped() { assertNull(search(new FakeWorld().plate(0, 64, 20).unload(0, 1), 0)); }
    @Test public void unsafeNearestDoesNotHideSafePlate() {
        FakeWorld w = new FakeWorld().plate(0, 64, 3).solid(0, 65, 3).plate(0, 64, 5);
        assertEquals(5, search(w, 0).blockZ);
    }
    @Test public void noPlates() { assertNull(search(new FakeWorld(), 0)); }
    @Test public void zeroHorizontalVectorExcluded() { assertNull(search(new FakeWorld().plate(0, 70, 0), 0)); }
    @Test public void north() { assertNotNull(search(new FakeWorld().plate(0, 64, -5), 180)); }
    @Test public void south() { assertNotNull(search(new FakeWorld().plate(0, 64, 5), 0)); }
    @Test public void east() { assertNotNull(search(new FakeWorld().plate(5, 64, 0), -90)); }
    @Test public void west() { assertNotNull(search(new FakeWorld().plate(-5, 64, 0), 90)); }
    @Test public void negativeOriginAndChunks() {
        PressurePlateSearchService s = new PressurePlateSearchService(new FakeWorld().plate(-2, 64, -3), validator,
                new Vec3d(-1.5, 64, -10.5), 0, 64, 30);
        while (!s.isDone()) s.advance(8192, Long.MAX_VALUE);
        assertEquals(-2, s.result().blockX);
    }
    @Test public void workIsBoundedAndPartialResultsAreHidden() {
        FakeWorld w = new FakeWorld().plate(0, 64, 5);
        PressurePlateSearchService s = new PressurePlateSearchService(w, validator, origin, 0, 64, 30);
        s.advance(20, Long.MAX_VALUE);
        assertFalse(s.isDone()); assertNull(s.result()); assertTrue(w.plateLookups <= 20);
    }
    @Test public void zeroTimeBudgetDoesNoWork() {
        FakeWorld w = new FakeWorld();
        PressurePlateSearchService s = new PressurePlateSearchService(w, validator, origin, 0, 64, 30);
        s.advance(8192, 0); assertEquals(0, w.loadChecks);
    }
}
