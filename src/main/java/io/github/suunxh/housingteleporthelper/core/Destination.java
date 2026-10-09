package io.github.suunxh.housingteleporthelper.core;

public final class Destination {
    public final int blockX, blockY, blockZ;
    public final boolean pressurePlate;
    public final Vec3d feet;
    public Destination(int x, int y, int z, boolean plate, Vec3d feet) {
        blockX = x; blockY = y; blockZ = z; pressurePlate = plate; this.feet = feet;
    }
}
