package io.github.suunxh.housingteleporthelper;

import io.github.suunxh.housingteleporthelper.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.multiplayer.ServerData;

public final class CommandExecutionService {
    private final ModConfiguration config;
    private final ChatFeedback feedback;
    private final TeleportCommandFormatter formatter = new TeleportCommandFormatter();
    private final CommandDelivery delivery = new CommandDelivery();
    public CommandExecutionService(ModConfiguration config, ChatFeedback feedback) { this.config = config; this.feedback = feedback; }
    public void reset() { delivery.reset(); }
    public void execute(Destination destination, MinecraftWorldView world, DestinationValidator validator, long tick) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        // Revalidate the block and body after an incremental search completes.
        Destination current = validator.standingOn(world, destination.blockX, destination.blockY,
                destination.blockZ, destination.pressurePlate);
        if (current == null || current.feet.distanceSquared(destination.feet) > 1e-10) {
            feedback.show("Unsafe destination: the target changed."); return;
        }
        TeleportCommandFormatter.Formatted command;
        try { command = formatter.format(config.commandTemplate, destination.feet, config.precision, config.integerOnly); }
        catch (IllegalArgumentException e) { feedback.show("Invalid command configuration."); return; }
        Vec3d coordinates = command.coordinates;
        if (Vec3d.floor(coordinates.x) != destination.blockX || Vec3d.floor(coordinates.z) != destination.blockZ
                || !validator.isSafeAt(world, coordinates)) {
            feedback.show("Unsafe destination after coordinate rounding. Use decimal coordinates or more precision."); return;
        }
        ServerData server = mc.getCurrentServerData();
        boolean allowed = ExecutionPolicy.directAllowed(config.directAuthorized, mc.isSingleplayer(),
                server == null ? "" : server.serverIP, config.directAllowedServers, config.hypixelRiskAcknowledged);
        CommandDelivery.Outcome outcome = delivery.deliver(config.executionMode, allowed, command.command,
                tick, config.cooldownTicks, new CommandDelivery.Sink() {
                    @Override public void prepare(String text) { mc.displayGuiScreen(new GuiChat(text)); }
                    @Override public void send(String text) { mc.thePlayer.sendChatMessage(text); }
                });
        switch (outcome) {
            case RESTRICTED: feedback.show("Direct mode unavailable on this server; command prepared for manual confirmation."); break;
            case COOLDOWN: feedback.show("Direct command cooldown active."); break;
            case SENT: feedback.show("Teleport command sent once."); break;
            default: feedback.show("Command prepared. Press Enter to send.");
        }
    }
}
