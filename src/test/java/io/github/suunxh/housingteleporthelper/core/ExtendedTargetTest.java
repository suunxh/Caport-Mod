package io.github.suunxh.housingteleporthelper.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExtendedTargetTest {
    // Actual vanilla player width is a float, which matters near an attached wall.
    private final DestinationValidator validator = new DestinationValidator((double)0.6F, (double)1.8F);
    private final TeleportCommandFormatter formatter = new TeleportCommandFormatter();

    private Destination aim(FakeWorld world, Vec3d eye, Vec3d look) {
        return new BlockTargetService().target(world, eye, look, 100, validator).destination;
    }
    private Vec3d formatted(Destination d) {
        return formatter.format(TeleportCommandFormatter.DEFAULT_TEMPLATE, d, 5, false).coordinates;
    }

    @Test public void lilyPadRetainsExactStandingHeightAfterFormatting() {
        FakeWorld world = new FakeWorld().shape(0,64,0,new Box(0,64,0,1,64.015625,1));
        Destination d = aim(world,new Vec3d(.5,67,.5),new Vec3d(0,-1,0));
        assertNotNull(d); assertEquals(Destination.Kind.STANDING,d.kind);
        assertEquals(64.015625,formatted(d).y,0);
        assertTrue(validator.unchanged(world,d)); assertTrue(validator.acceptsCoordinates(world,d,formatted(d)));
    }
    @Test public void negativeLilyPadHeightRetainsPrecision() {
        FakeWorld world = new FakeWorld() { @Override public int minY() { return -64; } };
        world.shape(-2,-40,-3,new Box(-2,-40,-3,-1,-39.984375,-2));
        Destination d=validator.targetBlock(world,-2,-40,-3);
        assertNotNull(d); assertEquals(-39.984375,formatted(d).y,0);
        assertTrue(validator.acceptsCoordinates(world,d,formatted(d)));
    }
    @Test public void integerOnlyDoesNotBypassLilyPadSafety() {
        FakeWorld world=new FakeWorld().shape(0,64,0,new Box(0,64,0,1,64.015625,1));
        Destination d=validator.targetBlock(world,0,64,0);
        Vec3d p=formatter.format(TeleportCommandFormatter.DEFAULT_TEMPLATE,d,5,true).coordinates;
        assertFalse(validator.acceptsCoordinates(world,d,p));
    }
    @Test public void grassAndFlowersDoNotHideUnderlyingGround() {
        FakeWorld world=new FakeWorld().solid(0,63,0).decoration(0,64,0).decoration(0,65,0);
        Destination d=aim(world,new Vec3d(.5,68,.5),new Vec3d(0,-1,0));
        assertNotNull(d); assertEquals(63,d.blockY); assertEquals(64,d.feet.y,0);
    }
    @Test public void decorativePlantDoesNotHideBlockBehindIt() {
        FakeWorld world=new FakeWorld().decoration(0,64,2).solid(0,64,4);
        Destination d=aim(world,new Vec3d(.5,64.5,.5),new Vec3d(0,0,1));
        assertNotNull(d); assertEquals(4,d.blockZ);
    }
    @Test public void horizontalAimAtGrassUsesItsGroundRatherThanDistantBlock() {
        FakeWorld world=new FakeWorld().solid(0,63,2).decoration(0,64,2).solid(0,64,6);
        Destination d=aim(world,new Vec3d(.5,64.5,.5),new Vec3d(0,0,1));
        assertNotNull(d); assertEquals(2,d.blockZ); assertEquals(63,d.blockY);
    }
    @Test public void skippingDecorationStillStopsAtUnloadedChunk() {
        FakeWorld world=new FakeWorld().decoration(0,64,2).unload(0,1);
        BlockTargetService.Result r=new BlockTargetService().target(world,new Vec3d(.5,64.5,.5),new Vec3d(0,0,1),100,validator);
        assertNull(r.destination); assertEquals(BlockTargetService.Failure.UNLOADED,r.failure);
    }
    @Test public void attachedLadderTargetsItsStandingEdgeRatherThanClimbingSpace() {
        FakeWorld world=new FakeWorld().solid(0,64,1).solid(0,65,1);
        Box ladder=new Box(0,64,.875,1,65,1);
        world.climbable(0,64,0,ladder).shape(0,64,0,ladder);
        world.ladders.add(world.key(0,64,0));
        Destination d=aim(world,new Vec3d(.5,64.5,-3),new Vec3d(0,0,1));
        assertNotNull(d); assertEquals(Destination.Kind.STANDING,d.kind);
        assertEquals(65,d.feet.y,0); assertTrue(d.feet.z > .5);
        assertTrue(validator.isSafeAt(world,d.feet));
        assertTrue(validator.unchanged(world,d)); assertTrue(validator.acceptsCoordinates(world,d,formatted(d)));
    }
    @Test public void vineTargetsItsOwnVoxelBesideSupportingWall() {
        FakeWorld world=new FakeWorld().solid(0,64,1).solid(0,65,1)
                .climbable(0,64,0,new Box(0,64,.9375,1,65,1));
        Destination d=aim(world,new Vec3d(.5,64.5,-3),new Vec3d(0,0,1));
        assertNotNull(d); assertEquals(Destination.Kind.CLIMBABLE,d.kind); assertEquals(0,d.blockZ);
        assertTrue(validator.acceptsCoordinates(world,d,formatted(d)));
    }
    @Test public void climbableStillRequiresClearBodySpace() {
        FakeWorld world=new FakeWorld().climbable(0,64,0,new Box(0,64,.9375,1,65,1)).solid(0,65,0);
        assertNull(validator.targetBlock(world,0,64,0));
    }
    @Test public void removedClimbableCannotBeSentAsAnOldDestination() {
        FakeWorld world=new FakeWorld().climbable(0,64,0,new Box(0,64,.9375,1,65,1));
        Destination d=validator.targetBlock(world,0,64,0); world.climbables.clear();
        assertFalse(validator.unchanged(world,d)); assertFalse(validator.acceptsCoordinates(world,d,d.feet));
    }
    @Test public void allAttachedOpenTrapdoorsHaveAClearSupportedOffset() {
        Box[] shapes={new Box(0,64,0,.1875,65,1),new Box(.8125,64,0,1,65,1),
                new Box(0,64,0,1,65,.1875),new Box(0,64,.8125,1,65,1)};
        int[][] walls={{-1,0},{1,0},{0,-1},{0,1}};
        for(int i=0;i<shapes.length;i++) {
            FakeWorld world=new FakeWorld().shape(0,64,0,shapes[i]);
            for(int y=64;y<=66;y++)world.solid(walls[i][0],y,walls[i][1]);
            Destination d=validator.targetBlock(world,0,64,0);
            assertNotNull("orientation "+i,d); assertEquals(Destination.Kind.STANDING,d.kind);
            assertEquals(65,d.feet.y,0); assertTrue(validator.acceptsCoordinates(world,d,formatted(d)));
        }
    }
    @Test public void attachedClosedTrapdoorKeepsActualTopHeight() {
        FakeWorld world=new FakeWorld().shape(0,64,0,new Box(0,64,0,1,64.1875,1)).solid(-1,64,0).solid(-1,65,0);
        Destination d=validator.targetBlock(world,0,64,0);
        assertNotNull(d); assertEquals(64.1875,d.feet.y,0); assertTrue(validator.acceptsCoordinates(world,d,formatted(d)));
    }
    @Test public void middleOfThreeByThreeWallUsesAimedBlockNotWallTopOrBlockBehind() {
        FakeWorld world=new FakeWorld();
        for(int x=-1;x<=1;x++)for(int y=63;y<=65;y++)world.solid(x,y,3);
        world.solid(0,64,6);
        Destination d=aim(world,new Vec3d(.5,64.5,.5),new Vec3d(0,0,1));
        assertNotNull(d); assertEquals(Destination.Kind.EMBEDDED,d.kind);
        assertEquals(.5,d.feet.x,0); assertEquals(64,d.feet.y,0); assertEquals(3.5,d.feet.z,0);
        assertFalse(validator.isSafeAt(world,d.feet));
        assertTrue(validator.unchanged(world,d)); assertTrue(validator.acceptsCoordinates(world,d,formatted(d)));
    }
    @Test public void clearWallTopStillUsesStandingDestination() {
        Destination d=validator.targetBlock(new FakeWorld().solid(0,64,0),0,64,0);
        assertEquals(Destination.Kind.STANDING,d.kind); assertEquals(65,d.feet.y,0);
    }
    @Test public void embeddedDestinationInvalidatedWhenWallChanges() {
        FakeWorld world=new FakeWorld().solid(0,64,0).solid(0,65,0);
        Destination d=validator.targetBlock(world,0,64,0);
        world.blocks.remove(world.key(0,65,0)); assertFalse(validator.unchanged(world,d));
        world.blocks.remove(world.key(0,64,0)); assertFalse(validator.acceptsCoordinates(world,d,d.feet));
    }
    @Test public void embeddedDestinationCannotMoveIntoAnotherVoxel() {
        FakeWorld world=new FakeWorld().solid(0,64,0).solid(0,65,0);
        Destination d=validator.targetBlock(world,0,64,0);
        assertFalse(validator.acceptsCoordinates(world,d,new Vec3d(1.5,64,.5)));
        assertFalse(validator.acceptsCoordinates(world,d,new Vec3d(.5,65,.5)));
    }
    @Test public void blockedPlateNeverFallsBackToEmbeddedWallOrClimbableRules() {
        FakeWorld world=new FakeWorld().plate(0,64,0).solid(0,65,0);
        assertNull(validator.targetBlock(world,0,64,0));
        assertNull(validator.standingOn(world,0,64,0,true));
    }
    @Test public void obstructedPartialBlockDoesNotFallBackToEmbedding() {
        FakeWorld world=new FakeWorld().shape(0,64,0,new Box(0,64,0,1,64.5,1)).solid(0,65,0);
        assertNull(validator.targetBlock(world,0,64,0));
    }
    @Test public void unloadedEmbeddedBodyStillRejected() {
        FakeWorld world=new FakeWorld().solid(15,64,0).solid(15,65,0).unload(1,0);
        Destination d=new Destination(15,64,0,false,new Vec3d(15.9,64,.5),Destination.Kind.EMBEDDED);
        assertFalse(validator.acceptsCoordinates(world,d,d.feet));
    }
    @Test public void emptyVoxelNeverBecomesAnEmbeddedTarget() {
        assertNull(validator.targetBlock(new FakeWorld(),0,64,0));
    }
}
