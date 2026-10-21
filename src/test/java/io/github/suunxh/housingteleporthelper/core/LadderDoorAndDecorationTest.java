package io.github.suunxh.housingteleporthelper.core;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class LadderDoorAndDecorationTest {
    private final DestinationValidator validator = new DestinationValidator((double)0.6F, (double)1.8F);
    private final TeleportCommandFormatter formatter = new TeleportCommandFormatter();

    private Destination aim(FakeWorld world, Vec3d eye, Vec3d look) {
        return new BlockTargetService().target(world, eye, look, 100, validator).destination;
    }
    private void accepted(FakeWorld world, Destination d) {
        assertNotNull(d);
        Vec3d p = formatter.format(TeleportCommandFormatter.DEFAULT_TEMPLATE, d, 5, false).coordinates;
        assertTrue(validator.unchanged(world, d));
        assertTrue(validator.acceptsCoordinates(world, d, p));
    }
    private FakeWorld ladder(Box shape) {
        FakeWorld world = new FakeWorld().shape(0,64,0,shape).climbable(0,64,0,shape);
        world.ladders.add(world.key(0,64,0));
        return world;
    }

    @Test public void ladderEdgesWorkInAllDirectionsWithAnAttachedWall() {
        for (double thickness : new double[] {.125, .1875}) {
            Box[] shapes = {new Box(0,64,0,thickness,65,1),new Box(1-thickness,64,0,1,65,1),
                    new Box(0,64,0,1,65,thickness),new Box(0,64,1-thickness,1,65,1)};
            int[][] walls = {{-1,0},{1,0},{0,-1},{0,1}};
            for(int i=0;i<shapes.length;i++) {
                FakeWorld world=ladder(shapes[i]);
                for(int y=64;y<=66;y++)world.solid(walls[i][0],y,walls[i][1]);
                Destination d=validator.targetBlock(world,0,64,0);
                accepted(world,d); assertEquals(Destination.Kind.STANDING,d.kind);
                assertEquals(65,d.feet.y,0); assertTrue(validator.isSafeAt(world,d.feet));
            }
        }
    }
    @Test public void blockedLadderTopDoesNotFallBackToVinePosition() {
        FakeWorld world=ladder(new Box(0,64,.875,1,65,1)).solid(0,66,0);
        assertNull(validator.targetBlock(world,0,64,0));
    }
    @Test public void removedLadderInvalidatesItsStandingDestination() {
        FakeWorld world=ladder(new Box(0,64,.875,1,65,1));
        Destination d=validator.targetBlock(world,0,64,0); accepted(world,d);
        world.blocks.clear();world.ladders.clear();world.climbables.clear();
        assertFalse(validator.unchanged(world,d)); assertFalse(validator.acceptsCoordinates(world,d,d.feet));
    }
    @Test public void negativeCoordinateLadderKeepsItsStandingEdgeAfterRounding() {
        FakeWorld world=new FakeWorld().shape(-2,64,-3,new Box(-2,64,-3,-1.875,65,-2)).solid(-3,65,-3);
        world.ladders.add(world.key(-2,64,-3));
        Destination d=validator.targetBlock(world,-2,64,-3); accepted(world,d);
        assertEquals(65,d.feet.y,0); assertEquals(-2,Vec3d.floor(d.feet.x));
    }
    @Test public void bothDoorHalvesResolveToTheSameTopForEveryOrientation() {
        Box[] planes={new Box(0,64,0,.1875,65,1),new Box(.8125,64,0,1,65,1),
                new Box(0,64,0,1,65,.1875),new Box(0,64,.8125,1,65,1)};
        for(Box lower:planes) {
            Box upper=new Box(lower.minX,65,lower.minZ,lower.maxX,66,lower.maxZ);
            FakeWorld world=new FakeWorld().shape(0,64,0,lower).shape(0,65,0,upper);
            world.lowerDoors.add(world.key(0,64,0));
            Destination bottom=aim(world,new Vec3d(-3,64.5,-3),new Vec3d(3.5,0,3.5));
            Destination top=aim(world,new Vec3d(-3,65.5,-3),new Vec3d(3.5,0,3.5));
            accepted(world,bottom); accepted(world,top);
            assertEquals(65,bottom.blockY); assertEquals(66,bottom.feet.y,0);
            assertEquals(0,bottom.feet.distanceSquared(top.feet),0);
        }
    }
    @Test public void doorTopStillRequiresHeadroom() {
        FakeWorld world=new FakeWorld().shape(0,64,0,new Box(0,64,0,.1875,65,1))
                .shape(0,65,0,new Box(0,65,0,.1875,66,1)).solid(0,67,0);
        world.lowerDoors.add(world.key(0,64,0));
        assertNull(validator.targetBlock(world,0,64,0));
    }
    @Test public void missingUpperDoorDoesNotProduceAFloatingDestination() {
        FakeWorld world=new FakeWorld().shape(0,64,0,new Box(0,64,0,.1875,65,1));
        world.lowerDoors.add(world.key(0,64,0));
        assertNull(validator.targetBlock(world,0,64,0));
    }
    @Test public void removedUpperDoorInvalidatesDestinationSelectedFromLowerHalf() {
        FakeWorld world=new FakeWorld().shape(0,64,0,new Box(0,64,0,.1875,65,1))
                .shape(0,65,0,new Box(0,65,0,.1875,66,1));
        world.lowerDoors.add(world.key(0,64,0));
        Destination d=validator.targetBlock(world,0,64,0);accepted(world,d);
        world.blocks.remove(world.key(0,65,0)); assertFalse(validator.unchanged(world,d));
    }
    @Test public void railSelectionUsesItsSupportingSurface() {
        FakeWorld world=new FakeWorld().solid(0,63,0).decoration(0,64,0);
        world.selections.put(world.key(0,64,0),Arrays.asList(new Box(0,64,0,1,64.125,1)));
        Destination d=aim(world,new Vec3d(.5,67,.5),new Vec3d(0,-1,0));
        accepted(world,d); assertEquals(63,d.blockY); assertEquals(64,d.feet.y,0);
    }
    @Test public void torchSelectionUsesItsSupportingSurface() {
        FakeWorld world=new FakeWorld().solid(0,63,0).decoration(0,64,0);
        world.selections.put(world.key(0,64,0),Arrays.asList(new Box(.4,64,.4,.6,64.6,.6)));
        Destination d=aim(world,new Vec3d(.5,64.3,-3),new Vec3d(0,0,1));
        accepted(world,d); assertEquals(63,d.blockY); assertEquals(64,d.feet.y,0);
    }
    @Test public void groundDecorationAboveUnsupportedAirDoesNotInventSupport() {
        FakeWorld world=new FakeWorld().decoration(0,64,0);
        assertNull(aim(world,new Vec3d(.5,67,.5),new Vec3d(0,-1,0)));
    }
    @Test public void standingSignUsesGroundBelowInsteadOfItsNonCollidingBoard() {
        FakeWorld world=new FakeWorld().solid(0,63,0).decoration(0,64,0);
        world.selections.put(world.key(0,64,0),Arrays.asList(new Box(.25,64,.25,.75,65,.75)));
        Destination d=aim(world,new Vec3d(.5,64.5,-3),new Vec3d(0,0,1));
        accepted(world,d);assertEquals(63,d.blockY);assertEquals(64,d.feet.y,0);
    }
    @Test public void wallSignWithoutGroundContinuesToTheSolidBlockBehindIt() {
        FakeWorld world=new FakeWorld().solid(0,64,1).decoration(0,64,0);
        world.selections.put(world.key(0,64,0),Arrays.asList(new Box(0,64.28125,.875,1,64.78125,1)));
        Destination d=aim(world,new Vec3d(.5,64.5,-3),new Vec3d(0,0,1));
        accepted(world,d);assertEquals(1,d.blockZ);assertEquals(65,d.feet.y,0);
    }
    private FakeWorld web() {
        FakeWorld world=new FakeWorld();world.cobwebs.add(world.key(0,64,0));
        world.selections.put(world.key(0,64,0),Arrays.asList(new Box(0,64,0,1,65,1)));
        return world;
    }
    @Test public void cobwebTargetsItsOwnSpaceWithoutInventingAFloor() {
        FakeWorld world=web();
        Destination d=aim(world,new Vec3d(.5,64.5,-3),new Vec3d(0,0,1));
        accepted(world,d); assertEquals(Destination.Kind.COBWEB,d.kind);
        assertEquals(.5,d.feet.x,0);assertEquals(64,d.feet.y,0);assertEquals(.5,d.feet.z,0);
        assertFalse(validator.isSafeAt(world,d.feet));
    }
    @Test public void cobwebWithFloorStillSelectsTheWebRatherThanTheFloor() {
        FakeWorld world=web().solid(0,63,0);
        Destination d=aim(world,new Vec3d(.5,67,.5),new Vec3d(0,-1,0));
        accepted(world,d);assertEquals(Destination.Kind.COBWEB,d.kind);assertEquals(64,d.blockY);
    }
    @Test public void cobwebDoesNotBypassSolidHeadroomChecks() {
        FakeWorld world=web().solid(0,65,0);assertNull(validator.targetBlock(world,0,64,0));
    }
    @Test public void removedCobwebInvalidatesOldDestination() {
        FakeWorld world=web();Destination d=validator.targetBlock(world,0,64,0);accepted(world,d);
        world.cobwebs.clear();assertFalse(validator.unchanged(world,d));assertFalse(validator.acceptsCoordinates(world,d,d.feet));
    }
    @Test public void roundedCobwebCannotMoveIntoAnotherVoxel() {
        FakeWorld world=web();Destination d=validator.targetBlock(world,0,64,0);
        assertFalse(validator.acceptsCoordinates(world,d,new Vec3d(.5,65,.5)));
        assertFalse(validator.acceptsCoordinates(world,d,new Vec3d(1.5,64,.5)));
    }
}
