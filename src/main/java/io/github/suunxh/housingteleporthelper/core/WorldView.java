package io.github.suunxh.housingteleporthelper.core;

import java.util.List;

/** All methods run on the client thread; implementations must never load chunks. */
public interface WorldView {
    boolean isLoaded(int x, int y, int z);
    default boolean isGeometryLoaded(int x, int y, int z) { return isLoaded(x, y, z); }
    boolean isSectionEmpty(int x, int y, int z);
    boolean isPressurePlate(int x, int y, int z);
    List<Box> collisionBoxes(int x, int y, int z);
    boolean hasClearance(Box playerBounds);
    // Returns the first ray intersection with this block, or null.
    Vec3d rayHit(int x, int y, int z, Vec3d start, Vec3d end);
}
