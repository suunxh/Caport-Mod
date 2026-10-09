package io.github.suunxh.housingteleporthelper.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class TargetAndSafetyTest {
    private final DestinationValidator validator = new DestinationValidator(0.6, 1.8);
    private final BlockTargetService target = new BlockTargetService();
    private BlockTargetService.Result aim(FakeWorld world, double range) {
        return target.target(world, new Vec3d(0.5, 64.5, 0.5), new Vec3d(0, 0, 1), range, validator);
    }
    @Test public void ordinaryFullBlock() {
        Destination d = aim(new FakeWorld().solid(0, 64, 5), 100).destination;
        assertNotNull(d); assertEquals(0.5, d.feet.x, 0); assertEquals(65, d.feet.y, 0); assertEquals(5.5, d.feet.z, 0);
    }
    @Test public void negativeCoordinatesUseFloor() {
        FakeWorld w = new FakeWorld().solid(-2, 64, -5);
        Destination d = target.target(w, new Vec3d(-1.5, 64.5, -0.5), new Vec3d(0, 0, -1), 100, validator).destination;
        assertNotNull(d); assertEquals(-1.5, d.feet.x, 0); assertEquals(-4.5, d.feet.z, 0);
    }
    @Test public void includesExactMaximumRayDistance() { assertNotNull(aim(new FakeWorld().solid(0, 64, 5), 4.5).destination); }
    @Test public void excludesBeyondRayDistance() { assertEquals(BlockTargetService.Failure.NO_TARGET, aim(new FakeWorld().solid(0, 64, 5), 4.49).failure); }
    @Test public void unloadedRayStops() { assertEquals(BlockTargetService.Failure.UNLOADED, aim(new FakeWorld().unload(0, 1), 100).failure); }
    @Test public void obstructedStandingSpaceRejected() { assertEquals(BlockTargetService.Failure.UNSAFE, aim(new FakeWorld().solid(0, 64, 5).solid(0, 66, 5), 100).failure); }
    @Test public void firstUnsafeHitDoesNotTargetThroughWall() {
        FakeWorld w = new FakeWorld().solid(0, 64, 2).solid(0, 65, 2).solid(0, 64, 5);
        assertEquals(BlockTargetService.Failure.UNSAFE, aim(w, 100).failure);
    }
    @Test public void noTarget() { assertEquals(BlockTargetService.Failure.NO_TARGET, aim(new FakeWorld(), 100).failure); }
    @Test public void zeroLookIsSafe() { assertNull(target.target(new FakeWorld(), new Vec3d(0, 64, 0), new Vec3d(0, 0, 0), 100, validator).destination); }
    @Test public void nonSolidBlockRejected() { assertNull(validator.standingOn(new FakeWorld(), 0, 64, 0, false)); }
    @Test public void bottomSlabUsesHalfHeight() {
        FakeWorld w = new FakeWorld().shape(0, 64, 0, new Box(0, 64, 0, 1, 64.5, 1));
        assertEquals(64.5, validator.standingOn(w, 0, 64, 0, false).feet.y, 0);
    }
    @Test public void stairsUseMultipartCollisionGeometry() {
        FakeWorld w = new FakeWorld().shape(0, 64, 0, new Box(0, 64, 0, 1, 64.5, 1), new Box(0, 64.5, 0.5, 1, 65, 1));
        assertEquals(65, validator.standingOn(w, 0, 64, 0, false).feet.y, 0);
    }
    @Test public void plateUsesSupportNotVisualHeight() { assertEquals(64, validator.standingOn(new FakeWorld().plate(0, 64, 0), 0, 64, 0, true).feet.y, 0); }
    @Test public void plateWithoutSupportRejected() {
        FakeWorld w = new FakeWorld().plate(0, 64, 0); w.blocks.clear();
        assertNull(validator.standingOn(w, 0, 64, 0, true));
    }
    @Test public void fencePlateAboveActivationVolumeRejected() {
        FakeWorld w = new FakeWorld().plate(0, 64, 0).shape(0, 63, 0, new Box(0.375, 63, 0.375, 0.625, 64.5, 0.625));
        assertNull(validator.standingOn(w, 0, 64, 0, true));
    }
    @Test public void worldCeilingRejected() { assertNull(validator.standingOn(new FakeWorld().solid(0, 255, 0), 0, 255, 0, false)); }
    @Test public void unloadedBodyRejected() {
        FakeWorld w = new FakeWorld().solid(15, 64, 0).unload(1, 0);
        assertFalse(validator.isSafeAt(w, new Vec3d(15.9, 65, 0.5)));
    }
    @Test public void roundedSlabPositionCannotEmbedPlayer() {
        FakeWorld w = new FakeWorld().shape(0, 64, 0, new Box(0, 64, 0, 1, 64.5, 1));
        assertFalse(validator.isSafeAt(w, new Vec3d(0.5, 64, 0.5)));
        assertFalse(validator.isSafeAt(w, new Vec3d(0.5, 65, 0.5)));
    }
}
