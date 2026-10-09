package io.github.suunxh.housingteleporthelper.core;

public final class Vec3d {
    public final double x, y, z;
    public Vec3d(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    public double distanceSquared(Vec3d v) {
        double dx = x - v.x, dy = y - v.y, dz = z - v.z;
        return dx * dx + dy * dy + dz * dz;
    }
    public static int floor(double value) { return (int) Math.floor(value); }
}
