package io.github.suunxh.housingteleporthelper.core;

import java.util.List;

public final class DestinationValidator {
    private static final double EPSILON = 1e-7;
    private final double width, height;
    public DestinationValidator(double width, double height) { this.width = width; this.height = height; }

    public Destination standingOn(WorldView world, int x, int y, int z, boolean plate) {
        if (y < 0 || y >= 256 || !world.isGeometryLoaded(x, y, z)) return null;
        if (plate && !world.isPressurePlate(x, y, z)) return null;
        double centerX = x + 0.5, centerZ = z + 0.5;
        // Vanilla 1.8.9 plates have NO collision box. Use the supporting block's
        // real surface, not the plate's visual bounding box or y + 1.
        int supportY = plate ? y - 1 : y;
        if (!world.isGeometryLoaded(x, supportY, z)) return null;
        List<Box> shapes = world.collisionBoxes(x, supportY, z);
        double top = Double.NEGATIVE_INFINITY;
        for (Box shape : shapes) {
            if (shape.containsHorizontal(centerX, centerZ)) top = Math.max(top, shape.maxY);
        }
        if (!Double.isFinite(top) || top < 0 || top + height > 256) return null;
        // A raised fence surface above a plate's activation volume is unsuitable.
        if (plate && (top < y - EPSILON || top > y + 0.25 - EPSILON)) return null;
        Vec3d feet = new Vec3d(centerX, top, centerZ);
        return isSafeAt(world, feet) ? new Destination(x, y, z, plate, feet) : null;
    }

    public boolean isSafeAt(WorldView world, Vec3d feet) {
        if (feet.y < 0 || feet.y + height > 256) return false;
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
        int bottom = Math.max(0, Vec3d.floor(feet.y) - 2);
        int top = Math.min(255, Vec3d.floor(feet.y));
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
                for (int y = Math.max(0, Vec3d.floor(b.minY)); y <= Math.min(255, Vec3d.floor(b.maxY)); y++)
                    if (!world.isLoaded(x, y, z)) return false;
        return true;
    }
}
