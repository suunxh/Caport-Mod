package io.github.suunxh.housingteleporthelper;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public final class ChatFeedback {
    public static final String PREFIX = "\u00a77[caport] \u00a7r";
    private final ModConfiguration config;
    public ChatFeedback(ModConfiguration config) { this.config = config; }
    public void show(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (config.feedback && mc.thePlayer != null)
            mc.thePlayer.addChatMessage(new ChatComponentText(PREFIX + message));
    }
}
