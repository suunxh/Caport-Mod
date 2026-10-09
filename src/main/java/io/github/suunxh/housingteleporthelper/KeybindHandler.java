package io.github.suunxh.housingteleporthelper;

import io.github.suunxh.housingteleporthelper.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class KeybindHandler {
    private final KeyBinding targetKey = new KeyBinding("key.housingteleporthelper.target", Keyboard.KEY_G, "key.categories.housingteleporthelper");
    private final KeyBinding plateKey = new KeyBinding("key.housingteleporthelper.plate", Keyboard.KEY_H, "key.categories.housingteleporthelper");
    private final ModConfiguration config;
    private final ChatFeedback feedback;
    private final CommandExecutionService execution;
    private final PressGate gate = new PressGate();
    private final BlockTargetService targetService = new BlockTargetService();
    private PressurePlateSearchService search;
    private WorldClient searchWorld, previousWorld;
    private EntityPlayerSP searchPlayer;
    private MinecraftWorldView searchView;
    private DestinationValidator searchValidator;
    private Vec3d searchOrigin;
    private long ticks, searchStarted;
    private boolean configWarningShown;

    public KeybindHandler(ModConfiguration config) {
        this.config = config; feedback = new ChatFeedback(config); execution = new CommandExecutionService(config, feedback);
        ClientRegistry.registerKeyBinding(targetKey); ClientRegistry.registerKeyBinding(plateKey);
    }
    @SubscribeEvent public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != previousWorld) { cancelSearch(); execution.reset(); previousWorld = mc.theWorld; }
        boolean enabled = mc.thePlayer != null && mc.theWorld != null && mc.currentScreen == null && mc.inGameHasFocus;
        // Drain vanilla press counters, then use physical rising edges to prevent held-key repeat.
        while (targetKey.isPressed()) { }
        while (plateKey.isPressed()) { }
        PressGate.Presses presses = gate.update(targetKey.getKeyCode(), plateKey.getKeyCode(),
                down(targetKey.getKeyCode()), down(plateKey.getKeyCode()), enabled);
        if (enabled && config.corrected && !configWarningShown) {
            feedback.show("Invalid command/configuration values were restored to safe defaults."); configWarningShown = true;
        }
        if (presses.conflict) { cancelSearch(); feedback.show("G/H actions share a key. Assign different keys in Controls."); }
        if (presses.target || presses.plate) {
            cancelSearch();
            MinecraftWorldView view = new MinecraftWorldView(mc.theWorld, mc.thePlayer);
            DestinationValidator validator = new DestinationValidator(mc.thePlayer.width, mc.thePlayer.height);
            if (presses.target) {
                Vec3 eye = mc.thePlayer.getPositionEyes(1.0F), look = mc.thePlayer.getLook(1.0F);
                BlockTargetService.Result result = targetService.target(view, vector(eye), vector(look), config.targetRange, validator);
                if (result.destination != null) execution.execute(result.destination, view, validator, ticks);
                else if (result.failure == BlockTargetService.Failure.UNLOADED) feedback.show("Destination in unloaded chunk.");
                else if (result.failure == BlockTargetService.Failure.UNSAFE) feedback.show("Unsafe destination.");
                else feedback.show("No target block found within range.");
            } else {
                searchWorld = mc.theWorld; searchPlayer = mc.thePlayer; searchView = view; searchValidator = validator;
                searchOrigin = new Vec3d(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                searchStarted = ticks;
                search = new PressurePlateSearchService(view, validator, searchOrigin, mc.thePlayer.rotationYaw,
                        config.plateRange, config.coneHalfAngle);
            }
        }
        if (search == null) return;
        // Never complete a stale action after a GUI, respawn, world change, movement, or timeout.
        if (!enabled || mc.theWorld != searchWorld || mc.thePlayer != searchPlayer || ticks - searchStarted > 200
                || searchOrigin.distanceSquared(new Vec3d(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)) > 0.25
                || Math.abs(mc.thePlayer.width - 0.6F) > 0.01 || Math.abs(mc.thePlayer.height - 1.8F) > 0.01) {
            cancelSearch(); if (enabled) feedback.show("Pressure plate search cancelled. Stand still and press again."); return;
        }
        searchView.beginTick();
        search.advance(8192, 2_000_000L);
        if (search.isDone()) {
            Destination destination = search.result();
            MinecraftWorldView view = searchView;
            DestinationValidator validator = searchValidator;
            cancelSearch();
            if (destination == null) feedback.show("No pressure plate found in range and facing cone.");
            else execution.execute(destination, view, validator, ticks);
        }
    }
    private void cancelSearch() { search = null; searchWorld = null; searchPlayer = null; searchView = null; searchValidator = null; searchOrigin = null; }
    private static Vec3d vector(Vec3 v) { return new Vec3d(v.xCoord, v.yCoord, v.zCoord); }
    private static boolean down(int code) {
        if (code == 0) return false;
        if (code < 0) { int button = code + 100; return Mouse.isCreated() && button >= 0 && button < Mouse.getButtonCount() && Mouse.isButtonDown(button); }
        return Keyboard.isCreated() && code < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(code);
    }
}
