package io.github.suunxh.housingteleporthelper.core;

public final class Destination {
    public enum Kind { STANDING, CLIMBABLE, EMBEDDED }
    public final int blockX, blockY, blockZ;
    public final boolean pressurePlate;
    public final Vec3d feet;
    public final Kind kind;
    public Destination(int x, int y, int z, boolean plate, Vec3d feet) {
        this(x, y, z, plate, feet, Kind.STANDING);
    }
    public Destination(int x, int y, int z, boolean plate, Vec3d feet, Kind kind) {
        blockX = x; blockY = y; blockZ = z; pressurePlate = plate; this.feet = feet; this.kind = kind;
    }
}
