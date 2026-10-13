package io.github.suunxh.housingteleporthelper.core;

import java.util.*;

public final class DestinationValidator {
    private static final double EPSILON = 1e-7;
    private final double width, height;
    public DestinationValidator(double width, double height) { this.width = width; this.height = height; }

    public Destination standingOn(WorldView world, int x, int y, int z, boolean plate) {
        if (y < world.minY() || y >= world.maxY() || !world.isGeometryLoaded(x, y, z)) return null;
        if (plate && !world.isPressurePlate(x, y, z)) return null;
        double centerX = x + 0.5, centerZ = z + 0.5;
        // Vanilla 1.8.9 plates have NO collision box. Use the supporting block's
        // real surface, not the plate's visual bounding box or y + 1.
        int supportY = plate ? y - 1 : y;
        if (!world.isGeometryLoaded(x, supportY, z)) return null;
        List<Box> shapes = world.collisionBoxes(x, supportY, z);
        List<Vec3d> candidates = new ArrayList<Vec3d>();
        for (Box shape : shapes) {
            if (shape.containsHorizontal(centerX, centerZ)) candidates.add(new Vec3d(centerX, shape.maxY, centerZ));
            // Open trapdoors and other narrow/off-center surfaces have no support
            // below the voxel center. Try the actual surface center instead.
            if (!plate) candidates.add(new Vec3d((shape.minX + shape.maxX) / 2, shape.maxY, (shape.minZ + shape.maxZ) / 2));
        }
        final double cx = centerX, cz = centerZ;
        Collections.sort(candidates, new Comparator<Vec3d>() {
            @Override public int compare(Vec3d a, Vec3d b) {
                int top = Double.compare(b.y, a.y); if (top != 0) return top;
                double da = (a.x - cx) * (a.x - cx) + (a.z - cz) * (a.z - cz);
                double db = (b.x - cx) * (b.x - cx) + (b.z - cz) * (b.z - cz);
                int distance = Double.compare(da, db); if (distance != 0) return distance;
                int horizontal = Double.compare(a.x, b.x); return horizontal != 0 ? horizontal : Double.compare(a.z, b.z);
            }
        });
        for (Vec3d feet : candidates) {
            if (!Double.isFinite(feet.y) || feet.y < world.minY() || feet.y + height > world.maxY()) continue;
            // A raised fence surface above a plate's activation volume is unsuitable.
            if (plate && (feet.y < y - EPSILON || feet.y > y + 0.25 - EPSILON)) continue;
            if (isSafeAt(world, feet)) return new Destination(x, y, z, plate, feet);
        }
        return null;
    }

    public boolean isSafeAt(WorldView world, Vec3d feet) {
        if (feet.y < world.minY() || feet.y + height > world.maxY()) return false;
        double half = width / 2;
        Box body = new Box(feet.x - half, feet.y + EPSILON, feet.z - half,
                feet.x + half, feet.y + height, feet.z + half);
        if (!loaded(world, new Box(body.minX, feet.y - EPSILON, body.minZ,
                body.maxX, body.maxY, body.maxZ))) return false;
        if (!world.hasClearance(body)) return false;
        // Require actual support just below the feet, including adjacent shapes
        // such as stairs and fences whose boxes extend beyond their voxel.
        Box sole = new Box(body.minX, feet.y - EPSILON, body.minZ,
                body.maxX, feet.y + EPSILON, body.maxZ);
        int bottom = Math.max(world.minY(), Vec3d.floor(feet.y) - 2);
        int top = Math.min(world.maxY() - 1, Vec3d.floor(feet.y));
        for (int x = Vec3d.floor(body.minX); x <= Vec3d.floor(body.maxX); x++) {
            for (int z = Vec3d.floor(body.minZ); z <= Vec3d.floor(body.maxZ); z++) {
                for (int y = bottom; y <= top; y++) {
                    if (!world.isLoaded(x, y, z)) return false;
                    for (Box shape : world.collisionBoxes(x, y, z)) {
                        if (Math.abs(shape.maxY - feet.y) <= EPSILON && shape.intersects(sole)) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean loaded(WorldView world, Box b) {
        for (int x = Vec3d.floor(b.minX); x <= Vec3d.floor(b.maxX); x++)
            for (int z = Vec3d.floor(b.minZ); z <= Vec3d.floor(b.maxZ); z++)
                for (int y = Math.max(world.minY(), Vec3d.floor(b.minY)); y <= Math.min(world.maxY() - 1, Vec3d.floor(b.maxY)); y++)
                    if (!world.isLoaded(x, y, z)) return false;
        return true;
    }
}
