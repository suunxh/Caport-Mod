package io.github.suunxh.housingteleporthelper.core;

/** Announce each client world once, waiting until its player can receive chat. */
public final class WorldJoinNotice {
    private Object previousWorld;
    private boolean pending;

    public boolean shouldShow(Object world, boolean playerReady) {
        if (world != previousWorld) {
            previousWorld = world;
            pending = world != null;
        }
        if (!pending || !playerReady) return false;
        pending = false;
        return true;
    }
}
