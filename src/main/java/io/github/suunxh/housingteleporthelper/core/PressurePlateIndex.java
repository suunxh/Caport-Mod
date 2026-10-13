package io.github.suunxh.housingteleporthelper.core;

import java.util.*;

/** Bounded, client-local section cache. Publish immutable lists only after a full section scan. */
public final class PressurePlateIndex {
    public static final class Position {
        public final int x, y, z;
        public Position(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    }
    private static final class Key {
        final int x, y, z;
        Key(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        @Override public int hashCode() { return (x * 31 + y) * 31 + z; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key k = (Key)o; return x == k.x && y == k.y && z == k.z;
        }
    }
    private static final class Section {
        int cursor;
        boolean dirty = true;
        List<Position> building = new ArrayList<Position>();
        List<Position> plates = Collections.emptyList();
    }
    private final Map<Key, Section> sections = new HashMap<Key, Section>();
    private final Deque<Key> pending = new ArrayDeque<Key>();
    private int anchorX = Integer.MIN_VALUE, anchorY, anchorZ, reach;
    public void clear() { sections.clear(); pending.clear(); anchorX = Integer.MIN_VALUE; }
    public void prepare(WorldView world, Vec3d origin, double radius) {
        int cx = Vec3d.floor(origin.x) >> 4, cy = Vec3d.floor(origin.y) >> 4, cz = Vec3d.floor(origin.z) >> 4;
        int r = (int)Math.ceil(radius / 16);
        if (cx == anchorX && cy == anchorY && cz == anchorZ && r == reach) return;
        anchorX = cx; anchorY = cy; anchorZ = cz; reach = r;
        Set<Key> wanted = new HashSet<Key>();
        for (int x = cx - r; x <= cx + r; x++) for (int z = cz - r; z <= cz + r; z++)
            for (int y = Math.max(world.minY() >> 4, cy - r); y <= Math.min((world.maxY() - 1) >> 4, cy + r); y++) {
                Key key = new Key(x, y, z); wanted.add(key);
                if (!sections.containsKey(key)) sections.put(key, new Section());
            }
        sections.keySet().retainAll(wanted);
        pending.clear();
        for (Map.Entry<Key, Section> e : sections.entrySet()) if (e.getValue().dirty) pending.add(e.getKey());
    }
    public void invalidate(int x, int y, int z) {
        Key key = new Key(x >> 4, y >> 4, z >> 4);
        Section section = sections.get(key);
        if (section == null) return;
        if (!section.dirty) pending.add(key);
        section.dirty = true; section.cursor = 0; section.building = new ArrayList<Position>();
    }
    public void invalidateChunk(int cx, int cz) {
        for (Key key : new ArrayList<Key>(sections.keySet()))
            if (key.x == cx && key.z == cz) invalidate(cx << 4, key.y << 4, cz << 4);
    }
    public void advance(WorldView world, int budget, long nanos) {
        long start = System.nanoTime(); int work = 0;
        while (!pending.isEmpty() && work < budget) {
            if ((work & 31) == 0 && System.nanoTime() - start >= nanos) return;
            Key key = pending.peek(); Section s = sections.get(key);
            int bx = key.x << 4, by = key.y << 4, bz = key.z << 4;
            if (s.cursor == 0 && (!world.isLoaded(bx, Math.max(world.minY(), by), bz)
                    || world.isSectionEmpty(bx, Math.max(world.minY(), by), bz))) s.cursor = 4096;
            if (s.cursor < 4096) {
                int i = s.cursor++, x = bx + (i & 15), y = by + (i >> 8), z = bz + ((i >> 4) & 15);
                if (y >= world.minY() && y < world.maxY() && world.isPressurePlate(x, y, z))
                    s.building.add(new Position(x, y, z));
            }
            work++;
            if (s.cursor == 4096) {
                s.plates = Collections.unmodifiableList(s.building); s.dirty = false; pending.remove();
            }
        }
    }
    public boolean isReady() { return anchorX != Integer.MIN_VALUE && pending.isEmpty(); }
    public PlateSearch search(WorldView world, DestinationValidator validator, Vec3d origin, double yaw, double radius, double cone) {
        prepare(world, origin, radius);
        if (!isReady()) return new PressurePlateSearchService(world, validator, origin, yaw, radius, cone);
        List<List<Position>> snapshot = new ArrayList<List<Position>>();
        for (Section section : sections.values()) if (!section.plates.isEmpty()) snapshot.add(section.plates);
        return new CachedPressurePlateSearch(world, validator, origin, yaw, radius, cone, snapshot);
    }
}
