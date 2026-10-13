package io.github.suunxh.housingteleporthelper.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class SurfaceTargetTest {
    private final DestinationValidator validator = new DestinationValidator(0.6, 1.8);
    @Test public void closedTrapdoorsUseBothActualHeights() {
        FakeWorld bottom = new FakeWorld().shape(0, 64, 0, new Box(0, 64, 0, 1, 64.1875, 1));
        assertEquals(64.1875, validator.standingOn(bottom, 0, 64, 0, false).feet.y, 0);
        FakeWorld top = new FakeWorld().shape(0, 64, 0, new Box(0, 64.8125, 0, 1, 65, 1));
        assertEquals(65, validator.standingOn(top, 0, 64, 0, false).feet.y, 0);
    }
    @Test public void allOpenTrapdoorOrientationsUseTheNarrowUpperEdge() {
        Box[] shapes = {new Box(0,64,0,0.1875,65,1), new Box(0.8125,64,0,1,65,1),
                new Box(0,64,0,1,65,0.1875), new Box(0,64,0.8125,1,65,1)};
        for (Box shape : shapes) {
            FakeWorld world = new FakeWorld().shape(0,64,0,shape);
            Destination d = validator.standingOn(world,0,64,0,false);
            assertNotNull(d); assertEquals(65,d.feet.y,0); assertTrue(shape.containsHorizontal(d.feet.x,d.feet.z));
        }
    }
    @Test public void fenceWallAndPaneUseCollisionSurface() {
        for (Box box : new Box[] {new Box(.375,64,.375,.625,65.5,.625),
                new Box(.25,64,.25,.75,65.5,.75),new Box(.4375,64,0,.5625,65,1)}) {
            Destination d = validator.standingOn(new FakeWorld().shape(0,64,0,box),0,64,0,false);
            assertNotNull(d);assertEquals(box.maxY,d.feet.y,0);
        }
    }
    @Test public void narrowSurfaceWithCeilingStillRejected() {
        FakeWorld world = new FakeWorld().shape(0,64,0,new Box(0,64,0,.1875,65,1)).solid(0,66,0);
        assertNull(validator.standingOn(world,0,64,0,false));
    }
    @Test public void modernWorldSupportsNegativeAndHighCoordinates() {
        FakeWorld world = new FakeWorld() {
            @Override public int minY() { return -64; }
            @Override public int maxY() { return 320; }
        };
        world.solid(0,-40,0).solid(0,300,0);
        assertEquals(-39,validator.standingOn(world,0,-40,0,false).feet.y,0);
        assertEquals(301,validator.standingOn(world,0,300,0,false).feet.y,0);
        assertNotNull(new BlockTargetService().target(world,new Vec3d(.5,-39.5,-3),new Vec3d(0,0,1),10,validator).destination);
    }
}
