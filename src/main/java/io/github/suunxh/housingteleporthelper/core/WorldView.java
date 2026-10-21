package io.github.suunxh.housingteleporthelper.core;

import java.util.List;

/** All methods run on the client thread; implementations must never load chunks. */
public interface WorldView {
    default int minY() { return 0; }
    default int maxY() { return 256; }
    boolean isLoaded(int x, int y, int z);
    default boolean isGeometryLoaded(int x, int y, int z) { return isLoaded(x, y, z); }
    boolean isSectionEmpty(int x, int y, int z);
    boolean isPressurePlate(int x, int y, int z);
    default boolean isClimbable(int x, int y, int z) { return false; }
    default boolean isLadder(int x, int y, int z) { return false; }
    default boolean isLowerDoor(int x, int y, int z) { return false; }
    default boolean isCobweb(int x, int y, int z) { return false; }
    default boolean isTargetDecoration(int x, int y, int z) { return false; }
    List<Box> collisionBoxes(int x, int y, int z);
    boolean hasClearance(Box playerBounds);
    // Returns the first ray intersection with this block, or null.
    Vec3d rayHit(int x, int y, int z, Vec3d start, Vec3d end);
}
