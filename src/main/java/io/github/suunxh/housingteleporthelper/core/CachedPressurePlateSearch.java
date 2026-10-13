package io.github.suunxh.housingteleporthelper.core;

import java.util.*;

/** Same nearest/cone rules as the cold scan, but visit cached plate positions only. */
public final class CachedPressurePlateSearch implements PlateSearch {
    private final WorldView world;
    private final DestinationValidator validator;
    private final Vec3d origin;
    private final double fx, fz, cosine, radiusSquared;
    private final Iterator<List<PressurePlateIndex.Position>> sections;
    private Iterator<PressurePlateIndex.Position> positions = Collections.<PressurePlateIndex.Position>emptyList().iterator();
    private boolean done;
    private Destination best;
    private double bestDistance = Double.POSITIVE_INFINITY, bestDot = -1;
    CachedPressurePlateSearch(WorldView world, DestinationValidator validator, Vec3d origin, double yaw,
                              double radius, double cone, List<List<PressurePlateIndex.Position>> snapshot) {
        this.world = world; this.validator = validator; this.origin = origin;
        double radians = Math.toRadians(yaw); fx = -Math.sin(radians); fz = Math.cos(radians);
        cosine = Math.cos(Math.toRadians(cone)); radiusSquared = radius * radius; sections = snapshot.iterator();
    }
    @Override public void advance(int budget, long nanos) {
        long start = System.nanoTime(); int work = 0;
        while (!done && work < budget) {
            if ((work & 31) == 0 && System.nanoTime() - start >= nanos) return;
            if (!positions.hasNext()) {
                if (!sections.hasNext()) { done = true; break; }
                positions = sections.next().iterator(); continue;
            }
            PressurePlateIndex.Position p = positions.next(); work++;
            double dx = p.x + 0.5 - origin.x, dz = p.z + 0.5 - origin.z, horizontal = dx * dx + dz * dz;
            double dot = dx * fx + dz * fz;
            if (horizontal < 1e-12 || horizontal > radiusSquared || dot <= 0 || dot + 1e-9 < cosine * Math.sqrt(horizontal)) continue;
            double dy = origin.y < p.y ? p.y - origin.y : Math.max(0, origin.y - p.y - 0.25);
            if (horizontal + dy * dy > Math.min(radiusSquared, bestDistance) + 1e-9) continue;
            Destination d = validator.standingOn(world, p.x, p.y, p.z, true);
            if (d == null) continue;
            double distance = origin.distanceSquared(d.feet), angle = dot / Math.sqrt(horizontal);
            if (distance > radiusSquared + 1e-9) continue;
            if (best == null || distance < bestDistance - 1e-9 || (Math.abs(distance - bestDistance) <= 1e-9
                    && (angle > bestDot + 1e-9 || (Math.abs(angle - bestDot) <= 1e-9 && lexicographic(d, best))))) {
                best = d; bestDistance = distance; bestDot = angle;
            }
        }
    }
    private boolean lexicographic(Destination a, Destination b) {
        if (a.blockX != b.blockX) return a.blockX < b.blockX;
        if (a.blockY != b.blockY) return a.blockY < b.blockY;
        return a.blockZ < b.blockZ;
    }
    @Override public boolean isDone() { return done; }
    @Override public Destination result() { return done ? best : null; }
}
