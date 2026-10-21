package io.github.suunxh.housingteleporthelper.core;

import java.util.*;

class FakeWorld implements WorldView {
    final Map<String, List<Box>> blocks = new HashMap<String, List<Box>>();
    final Set<String> plates = new HashSet<String>();
    final Set<String> climbables = new HashSet<String>();
    final Set<String> decorations = new HashSet<String>();
    final Map<String, List<Box>> selections = new HashMap<String, List<Box>>();
    final Set<String> unloadedChunks = new HashSet<String>();
    int plateLookups, loadChecks;
    String key(int x, int y, int z) { return x + "," + y + "," + z; }
    FakeWorld solid(int x, int y, int z) { return shape(x, y, z, new Box(x, y, z, x + 1, y + 1, z + 1)); }
    FakeWorld shape(int x, int y, int z, Box... boxes) { blocks.put(key(x, y, z), Arrays.asList(boxes)); return this; }
    FakeWorld plate(int x, int y, int z) { plates.add(key(x, y, z)); return solid(x, y - 1, z); }
    FakeWorld unload(int chunkX, int chunkZ) { unloadedChunks.add(chunkX + "," + chunkZ); return this; }
    FakeWorld climbable(int x, int y, int z, Box selection) {
        climbables.add(key(x, y, z)); selections.put(key(x, y, z), Arrays.asList(selection)); return this;
    }
    FakeWorld decoration(int x, int y, int z) {
        decorations.add(key(x, y, z));
        selections.put(key(x, y, z), Arrays.asList(new Box(x, y, z, x + 1, y + 1, z + 1))); return this;
    }
    @Override public boolean isClimbable(int x, int y, int z) { return climbables.contains(key(x, y, z)); }
    @Override public boolean isTargetDecoration(int x, int y, int z) { return decorations.contains(key(x, y, z)); }
    @Override public boolean isLoaded(int x, int y, int z) {
        loadChecks++; return y >= minY() && y < maxY() && !unloadedChunks.contains((x >> 4) + "," + (z >> 4));
    }
    @Override public boolean isSectionEmpty(int x, int y, int z) { return false; }
    @Override public boolean isPressurePlate(int x, int y, int z) {
        if (!isLoaded(x, y, z)) throw new AssertionError("Unloaded lookup");
        plateLookups++; return plates.contains(key(x, y, z));
    }
    @Override public List<Box> collisionBoxes(int x, int y, int z) {
        if (!isLoaded(x, y, z)) throw new AssertionError("Unloaded collision lookup");
        List<Box> boxes = blocks.get(key(x, y, z));
        return boxes == null ? Collections.<Box>emptyList() : boxes;
    }
    @Override public boolean hasClearance(Box playerBounds) {
        for (List<Box> boxes : blocks.values()) for (Box box : boxes) if (box.intersects(playerBounds)) return false;
        return true;
    }
    @Override public Vec3d rayHit(int x, int y, int z, Vec3d start, Vec3d end) {
        List<Box> boxes = selections.get(key(x, y, z));
        for (Box box : boxes == null ? collisionBoxes(x, y, z) : boxes) {
            double enter = 0, exit = 1;
            double[] a = {start.x, start.y, start.z}, b = {end.x, end.y, end.z};
            double[] min = {box.minX, box.minY, box.minZ}, max = {box.maxX, box.maxY, box.maxZ};
            boolean hit = true;
            for (int axis = 0; axis < 3; axis++) {
                double direction = b[axis] - a[axis];
                if (direction == 0) { if (a[axis] < min[axis] || a[axis] > max[axis]) hit = false; }
                else {
                    double near = (min[axis] - a[axis]) / direction, far = (max[axis] - a[axis]) / direction;
                    enter = Math.max(enter, Math.min(near, far)); exit = Math.min(exit, Math.max(near, far));
                }
            }
            if (hit && enter <= exit) return new Vec3d(start.x + (end.x - start.x) * enter,
                    start.y + (end.y - start.y) * enter, start.z + (end.z - start.z) * enter);
        }
        return null;
    }
}
