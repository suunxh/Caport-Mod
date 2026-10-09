package io.github.suunxh.housingteleporthelper.core;

public final class Box {
    public final double minX, minY, minZ, maxX, maxY, maxZ;
    public Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }
    public boolean containsHorizontal(double x, double z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
    public boolean intersects(Box b) {
        return maxX > b.minX && minX < b.maxX && maxY > b.minY && minY < b.maxY
                && maxZ > b.minZ && minZ < b.maxZ;
    }
}
