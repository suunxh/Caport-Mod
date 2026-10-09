package io.github.suunxh.housingteleporthelper.core;

/** Bounded incremental scan with immutable position/yaw snapshot. No per-tick idle scan. */
public final class PressurePlateSearchService {
    private final WorldView world;
    private final DestinationValidator validator;
    private final Vec3d origin;
    private final double forwardX, forwardZ, cosine, radiusSquared;
    private final int minX, maxX, minZ, maxZ;
    private int x, z, y, maxY;
    private boolean columnReady, done;
    private Destination best;
    private double bestDistance = Double.POSITIVE_INFINITY, bestDot = -1;

    public PressurePlateSearchService(WorldView world, DestinationValidator validator, Vec3d origin,
                                      double yawDegrees, double radius, double halfAngle) {
        this.world = world; this.validator = validator; this.origin = origin;
        double yaw = Math.toRadians(yawDegrees);
        forwardX = -Math.sin(yaw); forwardZ = Math.cos(yaw);
        cosine = Math.cos(Math.toRadians(halfAngle)); radiusSquared = radius * radius;
        minX = Vec3d.floor(origin.x - radius); maxX = Vec3d.floor(origin.x + radius);
        minZ = Vec3d.floor(origin.z - radius); maxZ = Vec3d.floor(origin.z + radius);
        x = minX; z = minZ;
    }

    public void advance(int positionBudget, long nanosecondBudget) {
        if (done) return;
        long start = System.nanoTime();
        int work = 0;
        while (!done && work < positionBudget) {
            // Check time every 32 positions rather than querying the clock for every voxel.
            if ((work & 31) == 0 && System.nanoTime() - start >= nanosecondBudget) return;
            work++;
            if (!columnReady) {
                double dx = x + 0.5 - origin.x, dz = z + 0.5 - origin.z;
                double horizontalSquared = dx * dx + dz * dz;
                if (horizontalSquared < 1e-12 || horizontalSquared > radiusSquared
                        || !inCone(dx, dz, horizontalSquared)
                        || !world.isLoaded(x, Math.max(0, Math.min(255, Vec3d.floor(origin.y))), z)) {
                    nextColumn(); continue;
                }
                double verticalReach = Math.sqrt(radiusSquared - horizontalSquared);
                y = Math.max(1, (int) Math.ceil(origin.y - verticalReach - 0.25));
                maxY = Math.min(255, Vec3d.floor(origin.y + verticalReach));
                columnReady = true;
            }
            if (y > maxY) { nextColumn(); continue; }
            if (!world.isLoaded(x, y, z)) { nextColumn(); continue; }
            if (world.isSectionEmpty(x, y, z)) { y = (y / 16 + 1) * 16; continue; }
            int candidateY = y++;
            if (!world.isPressurePlate(x, candidateY, z)) continue;
            Destination candidate = validator.standingOn(world, x, candidateY, z, true);
            if (candidate == null) continue;
            double distance = origin.distanceSquared(candidate.feet);
            if (distance > radiusSquared + 1e-9) continue;
            double dx = candidate.feet.x - origin.x, dz = candidate.feet.z - origin.z;
            double dot = (dx * forwardX + dz * forwardZ) / Math.sqrt(dx * dx + dz * dz);
            if (better(candidate, distance, dot)) {
                best = candidate; bestDistance = distance; bestDot = dot;
            }
        }
    }
    private boolean inCone(double dx, double dz, double squared) {
        double dot = dx * forwardX + dz * forwardZ;
        return dot > 0 && dot + 1e-9 >= cosine * Math.sqrt(squared);
    }
    private boolean better(Destination d, double distance, double dot) {
        if (best == null || distance < bestDistance - 1e-9) return true;
        if (Math.abs(distance - bestDistance) > 1e-9) return false;
        if (dot > bestDot + 1e-9) return true;
        if (Math.abs(dot - bestDot) > 1e-9) return false;
        if (d.blockX != best.blockX) return d.blockX < best.blockX;
        if (d.blockY != best.blockY) return d.blockY < best.blockY;
        return d.blockZ < best.blockZ;
    }
    private void nextColumn() {
        columnReady = false;
        if (++z > maxZ) { z = minZ; if (++x > maxX) done = true; }
    }
    public boolean isDone() { return done; }
    public Destination result() { return done ? best : null; }
}
