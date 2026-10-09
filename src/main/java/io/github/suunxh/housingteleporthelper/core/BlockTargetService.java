package io.github.suunxh.housingteleporthelper.core;

/** Dedicated voxel ray traversal, independent of vanilla interaction reach. */
public final class BlockTargetService {
    public enum Failure { NO_TARGET, UNLOADED, UNSAFE }
    public static final class Result {
        public final Destination destination;
        public final Failure failure;
        private Result(Destination d, Failure f) { destination = d; failure = f; }
    }
    public Result target(WorldView world, Vec3d eye, Vec3d look, double range, DestinationValidator validator) {
        double length = Math.sqrt(look.x * look.x + look.y * look.y + look.z * look.z);
        if (!Double.isFinite(length) || length < 1e-12 || range <= 0) return new Result(null, Failure.NO_TARGET);
        double dx = look.x / length, dy = look.y / length, dz = look.z / length;
        Vec3d end = new Vec3d(eye.x + dx * range, eye.y + dy * range, eye.z + dz * range);
        int x = Vec3d.floor(eye.x), y = Vec3d.floor(eye.y), z = Vec3d.floor(eye.z);
        int sx = sign(dx), sy = sign(dy), sz = sign(dz);
        double tx = boundary(eye.x, x, dx), ty = boundary(eye.y, y, dy), tz = boundary(eye.z, z, dz);
        double stepX = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dx);
        double stepY = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dy);
        double stepZ = dz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dz);
        double entered = 0;
        while (entered <= range + 1e-9) {
            if (y < 0 || y >= 256) return new Result(null, Failure.NO_TARGET);
            if (!world.isGeometryLoaded(x, y, z)) return new Result(null, Failure.UNLOADED);
            Vec3d hit = world.rayHit(x, y, z, eye, end);
            if (hit != null && eye.distanceSquared(hit) <= range * range + 1e-7) {
                Destination destination = validator.standingOn(world, x, y, z, world.isPressurePlate(x, y, z));
                return new Result(destination, destination == null ? Failure.UNSAFE : null);
            }
            entered = Math.min(tx, Math.min(ty, tz));
            if (tx <= entered) { x += sx; tx += stepX; }
            if (ty <= entered) { y += sy; ty += stepY; }
            if (tz <= entered) { z += sz; tz += stepZ; }
        }
        return new Result(null, Failure.NO_TARGET);
    }
    private static int sign(double n) { return n > 0 ? 1 : n < 0 ? -1 : 0; }
    private static double boundary(double value, int voxel, double direction) {
        return direction == 0 ? Double.POSITIVE_INFINITY
                : ((direction > 0 ? voxel + 1 : voxel) - value) / direction;
    }
}
