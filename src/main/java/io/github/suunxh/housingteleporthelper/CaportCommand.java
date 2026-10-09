package io.github.suunxh.housingteleporthelper;

import io.github.suunxh.housingteleporthelper.core.ExecutionPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import java.util.List;

/** Client-local settings command; never sends its arguments to a server. */
public final class CaportCommand extends CommandBase {
    private final ModConfiguration config;
    public CaportCommand(ModConfiguration config) { this.config = config; }
    @Override public String getCommandName() { return "caport"; }
    @Override public String getCommandUsage(ICommandSender sender) {
        return "/caport <status|manual|direct [authorized|hypixel-risk]>";
    }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }

    @Override public void processCommand(ICommandSender sender, String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("status"))) {
            message(sender, "Mode: " + config.executionMode.name()
                    + ". /caport manual restores confirmation. Hypixel risk opt-in: " + config.hypixelRiskAcknowledged + ".");
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("manual")) {
            config.useManualConfirmation(); message(sender, "Manual confirmation enabled and saved."); return;
        }
        boolean direct = args[0].equalsIgnoreCase("direct");
        boolean acknowledged = args.length == 2 && args[1].equalsIgnoreCase("authorized");
        boolean hypixelRisk = args.length == 2 && args[1].equalsIgnoreCase("hypixel-risk");
        if (!direct || args.length > 2 || (args.length == 2 && !acknowledged && !hypixelRisk)) {
            message(sender, getCommandUsage(sender)); return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            message(sender, "Join a singleplayer world or an authorized private server first."); return;
        }
        ServerData server = mc.getCurrentServerData();
        String address = server == null ? "" : server.serverIP;
        if (!mc.isSingleplayer() && ExecutionPolicy.recognizedHypixel(address)) {
            if (!hypixelRisk) {
                message(sender, "Hypixel direct mode is blocked by default. One-key /tp is automation and may cause a ban.");
                message(sender, "Seek Hypixel staff approval. /caport direct hypixel-risk explicitly accepts this risk; it is not approval.");
                return;
            }
            message(sender, "WARNING: Hypixel direct mode is NOT approved. Automated /tp may violate rules and cause a ban.");
        } else if (hypixelRisk) {
            message(sender, "The hypixel-risk option is only for recognized Hypixel hosts. Use authorized for a permitted private server.");
            return;
        }
        if (!mc.isSingleplayer() && !acknowledged && !hypixelRisk) {
            message(sender, "Only with express private-server permission, use /caport direct authorized."); return;
        }
        if (!config.enableDirectTesting(mc.isSingleplayer(), address, acknowledged || hypixelRisk, hypixelRisk)) {
            message(sender, "Direct mode unavailable on this server."); return;
        }
        message(sender, "One-key direct commands enabled and saved for this server. G/H send once; cooldown applies. /caport manual turns this off.");
    }
    private void message(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(ChatFeedback.PREFIX + text));
    }
    @Override public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "status", "manual", "direct");
        if (args.length == 2 && args[0].equalsIgnoreCase("direct")) return getListOfStringsMatchingLastWord(args, "authorized", "hypixel-risk");
        return null;
    }
}
