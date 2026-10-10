package io.github.suunxh.housingteleporthelper.core;

import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

public class CommandAndKeybindTest {
    private final TeleportCommandFormatter formatter = new TeleportCommandFormatter();
    @Test public void positiveDecimalCoordinates() { assertEquals("/tp 100.5 65 200.5", formatter.format("/tp {x} {y} {z}", new Vec3d(100.5, 65, 200.5), 5, false).command); }
    @Test public void negativeDecimalCoordinates() { assertEquals("/tp -100.5 -65 -200.5", formatter.format("/tp {x} {y} {z}", new Vec3d(-100.5, -65, -200.5), 5, false).command); }
    @Test public void negativeIntegerCoordinatesUseFloor() { assertEquals("/tp -2 64 -3", formatter.format("/tp {x} {y} {z}", new Vec3d(-1.5, 64, -2.5), 5, true).command); }
    @Test public void precisionAndNoScientificNotation() { assertEquals("/tp 0.00001 65 10000000", formatter.format("/tp {x} {y} {z}", new Vec3d(0.00001, 65, 10000000), 5, false).command); }
    @Test public void localeIndependent() {
        Locale before = Locale.getDefault();
        try { Locale.setDefault(Locale.GERMANY); assertEquals("/tp 1.25 64 0", formatter.format("/tp {x} {y} {z}", new Vec3d(1.25, 64, 0), 5, false).command); }
        finally { Locale.setDefault(before); }
    }
    @Test public void invalidTemplates() {
        for (String s : new String[] {"tp {x} {y} {z}", "/tp {x} {y}", "/tp {x} {y} {z} {x}", "/tp {x} {y} {unknown}", "/tp {x} {y} {z}\n/op me", "/tp {x}{y}{z}"})
            assertFalse(s, TeleportCommandFormatter.validTemplate(s));
    }
    @Test(expected = IllegalArgumentException.class) public void badPrecisionRejected() { formatter.format("/tp {x} {y} {z}", new Vec3d(0, 64, 0), 9, false); }
    @Test(expected = IllegalArgumentException.class) public void nonFiniteRejected() { formatter.format("/tp {x} {y} {z}", new Vec3d(Double.NaN, 64, 0), 5, false); }
    @Test public void hypixelHostVariantsBlockedWithoutSeparateOptIn() {
        for (String host : new String[] {"hypixel.net", "MC.HYPIXEL.NET:25565", "mc.hypixel.net.", "alpha.hypixel.net", "hypixel.io"})
            assertFalse(host, ExecutionPolicy.directAllowed(true, false, host, new String[] {host}));
    }
    @Test public void directModeHintUsesCurrentServerContext() {
        assertEquals("/caport direct", ExecutionPolicy.directModeCommand(true, ""));
        assertEquals("/caport direct hypixel-risk", ExecutionPolicy.directModeCommand(false, "MC.HYPIXEL.NET.:25565"));
        assertEquals("/caport direct authorized", ExecutionPolicy.directModeCommand(false, "private.example"));
        assertEquals("/caport direct authorized", ExecutionPolicy.directModeCommand(false, "mc.hypixel.net.example.org"));
    }
    @Test public void hypixelOptInStillRequiresGeneralConsentAndExactAllowlist() {
        String[] hosts = {"mc.hypixel.net"};
        assertTrue(ExecutionPolicy.directAllowed(true, false, "MC.HYPIXEL.NET:25565", hosts, true));
        assertFalse(ExecutionPolicy.directAllowed(true, false, "mc.hypixel.net", hosts, false));
        assertFalse(ExecutionPolicy.directAllowed(false, false, "mc.hypixel.net", hosts, true));
        assertFalse(ExecutionPolicy.directAllowed(true, false, "mc.hypixel.net", new String[0], true));
        assertFalse(ExecutionPolicy.directAllowed(true, false, "alpha.hypixel.net", hosts, true));
    }
    @Test public void authorizationRequiredEvenInSingleplayer() { assertFalse(ExecutionPolicy.directAllowed(false, true, "", new String[0])); }
    @Test public void authorizedSingleplayerAllowed() { assertTrue(ExecutionPolicy.directAllowed(true, true, "", new String[0])); }
    @Test public void exactPrivateHostRequired() {
        assertTrue(ExecutionPolicy.directAllowed(true, false, "private.example:25565", new String[] {"private.example"}));
        assertFalse(ExecutionPolicy.directAllowed(true, false, "other.private.example", new String[] {"private.example"}));
        assertFalse(ExecutionPolicy.directAllowed(true, false, "private.example.evil.org", new String[] {"private.example"}));
        assertFalse(ExecutionPolicy.directAllowed(true, false, "", new String[] {""}));
    }
    @Test public void ambiguousAddressDenied() { assertFalse(ExecutionPolicy.directAllowed(true, false, "mc.hypixel.net:25565:extra", new String[] {"mc.hypixel.net:25565:extra"})); }
    @Test public void wildcardDoesNotAuthorizeServer() { assertFalse(ExecutionPolicy.directAllowed(true, false, "private.example", new String[] {"*"})); }
    @Test public void oneActionPerPhysicalPress() {
        PressGate g = new PressGate();
        assertTrue(g.update(34, 35, true, false, true).target);
        assertFalse(g.update(34, 35, true, false, true).target);
        g.update(34, 35, false, false, true);
        assertTrue(g.update(34, 35, true, false, true).target);
    }
    @Test public void heldWhileTypingDoesNotFireWhenGuiCloses() {
        PressGate g = new PressGate();
        assertFalse(g.update(34, 35, false, true, false).plate);
        assertFalse(g.update(34, 35, false, true, true).plate);
        g.update(34, 35, false, false, true);
        assertTrue(g.update(34, 35, false, true, true).plate);
    }
    @Test public void missingWorldOrPlayerDisablesBothActions() {
        PressGate.Presses p = new PressGate().update(34, 35, true, true, false);
        assertFalse(p.target); assertFalse(p.plate);
    }
    @Test public void conflictingAssignmentsCauseNeitherAction() {
        PressGate.Presses p = new PressGate().update(34, 34, true, true, true);
        assertTrue(p.conflict); assertFalse(p.target); assertFalse(p.plate);
    }
    @Test public void customizedKeyboardOrMouseBindsWork() {
        assertTrue(new PressGate().update(-100, 42, true, false, true).target);
        assertTrue(new PressGate().update(-100, 42, false, true, true).plate);
    }
    @Test public void rebindingHeldKeyDoesNotTriggerAction() {
        PressGate g = new PressGate(); g.update(34, 35, false, false, true);
        assertFalse(g.update(42, 35, true, false, true).target);
    }
    private static final class RecordingSink implements CommandDelivery.Sink {
        int prepared, sent;
        String last;
        @Override public void prepare(String text) { prepared++; last = text; }
        @Override public void send(String text) { sent++; last = text; }
    }
    @Test public void defaultManualModeNeverSendsEvenOnAuthorizedServer() {
        RecordingSink sink = new RecordingSink();
        assertEquals(CommandDelivery.Outcome.PREPARED, new CommandDelivery().deliver(ExecutionPolicy.DEFAULT_MODE,
                true, "/tp 1 64 2", 1, 0, sink));
        assertEquals(1, sink.prepared); assertEquals(0, sink.sent); assertEquals("/tp 1 64 2", sink.last);
    }
    @Test public void restrictedDirectModeFallsBackToChat() {
        RecordingSink sink = new RecordingSink();
        assertEquals(CommandDelivery.Outcome.RESTRICTED, new CommandDelivery().deliver(ExecutionPolicy.Mode.DIRECT_COMMAND,
                false, "/tp 1 64 2", 1, 0, sink));
        assertEquals(1, sink.prepared); assertEquals(0, sink.sent);
    }
    @Test public void directModeSendsExactlyOneOrdinaryCommand() {
        RecordingSink sink = new RecordingSink();
        assertEquals(CommandDelivery.Outcome.SENT, new CommandDelivery().deliver(ExecutionPolicy.Mode.DIRECT_COMMAND,
                true, "/tp 1 64 2", 1, 0, sink));
        assertEquals(0, sink.prepared); assertEquals(1, sink.sent);
    }
    @Test public void directCooldownAndReset() {
        RecordingSink sink = new RecordingSink(); CommandDelivery delivery = new CommandDelivery();
        delivery.deliver(ExecutionPolicy.Mode.DIRECT_COMMAND, true, "/tp 1 64 2", 10, 20, sink);
        assertEquals(CommandDelivery.Outcome.COOLDOWN, delivery.deliver(ExecutionPolicy.Mode.DIRECT_COMMAND, true, "/tp 1 64 2", 29, 20, sink));
        assertEquals(1, sink.sent);
        assertEquals(CommandDelivery.Outcome.SENT, delivery.deliver(ExecutionPolicy.Mode.DIRECT_COMMAND, true, "/tp 1 64 2", 30, 20, sink));
        delivery.reset();
        assertEquals(CommandDelivery.Outcome.SENT, delivery.deliver(ExecutionPolicy.Mode.DIRECT_COMMAND, true, "/tp 1 64 2", 31, 20, sink));
        assertEquals(3, sink.sent);
    }
}
