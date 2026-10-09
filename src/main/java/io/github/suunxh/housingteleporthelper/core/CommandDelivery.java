package io.github.suunxh.housingteleporthelper.core;

/** The only command delivery decision point; testable without a Minecraft client. */
public final class CommandDelivery {
    public interface Sink {
        void prepare(String command);
        void send(String command);
    }
    public enum Outcome { PREPARED, RESTRICTED, COOLDOWN, SENT }
    private long lastDirectTick = Long.MIN_VALUE;
    public void reset() { lastDirectTick = Long.MIN_VALUE; }
    public Outcome deliver(ExecutionPolicy.Mode mode, boolean authorizedServer, String command,
                           long tick, int cooldownTicks, Sink sink) {
        if (mode != ExecutionPolicy.Mode.DIRECT_COMMAND) { sink.prepare(command); return Outcome.PREPARED; }
        if (!authorizedServer) { sink.prepare(command); return Outcome.RESTRICTED; }
        if (lastDirectTick != Long.MIN_VALUE && tick - lastDirectTick < cooldownTicks) return Outcome.COOLDOWN;
        lastDirectTick = tick;
        sink.send(command);
        return Outcome.SENT;
    }
}
