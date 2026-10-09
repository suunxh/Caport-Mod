package io.github.suunxh.housingteleporthelper.core;

import java.util.*;

final class FakeWorld implements WorldView {
    final Map<String, List<Box>> blocks = new HashMap<String, List<Box>>();
    final Set<String> plates = new HashSet<String>();
    final Set<String> unloadedChunks = new HashSet<String>();
    int plateLookups, loadChecks;
    String key(int x, int y, int z) { return x + "," + y + "," + z; }
    FakeWorld solid(int x, int y, int z) { return shape(x, y, z, new Box(x, y, z, x + 1, y + 1, z + 1)); }
    FakeWorld shape(int x, int y, int z, Box... boxes) { blocks.put(key(x, y, z), Arrays.asList(boxes)); return this; }
    FakeWorld plate(int x, int y, int z) { plates.add(key(x, y, z)); return solid(x, y - 1, z); }
    FakeWorld unload(int chunkX, int chunkZ) { unloadedChunks.add(chunkX + "," + chunkZ); return this; }
    @Override public boolean isLoaded(int x, int y, int z) {
        loadChecks++; return y >= 0 && y < 256 && !unloadedChunks.contains((x >> 4) + "," + (z >> 4));
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
        for (Box box : collisionBoxes(x, y, z)) {
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
