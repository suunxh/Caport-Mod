package io.github.suunxh.housingteleporthelper;

import io.github.suunxh.housingteleporthelper.core.ExecutionPolicy;
import io.github.suunxh.housingteleporthelper.core.TeleportCommandFormatter;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import java.io.File;

public final class ModConfiguration {
    public double targetRange, plateRange, coneHalfAngle;
    public int precision, cooldownTicks;
    public String commandTemplate;
    public boolean integerOnly, feedback, directAuthorized, corrected;
    public String[] directAllowedServers;
    public ExecutionPolicy.Mode executionMode;

    public void load(File file) {
        Configuration config = new Configuration(file);
        config.load();
        targetRange = bounded(config, "targetBlockRange", 100, 1, 256, "Dedicated targeting ray length; does not change interaction reach.");
        plateRange = bounded(config, "pressurePlateRange", 64, 1, 64, "Maximum three-dimensional search distance (hard cap 64).");
        coneHalfAngle = bounded(config, "pressurePlateConeHalfAngle", 30, 1, 89, "Horizontal cone half-angle in degrees; never includes blocks behind you.");
        precision = (int) bounded(config, "coordinatePrecision", 5, 0, 8, "Decimal places, 0 through 8. Rounded destinations are revalidated.");
        cooldownTicks = (int) bounded(config, "directCommandCooldownTicks", 20, 0, 1200, "Optional cooldown for authorized direct testing only (20 ticks = 1 second).");
        integerOnly = config.get("general", "integerCoordinates", false, "Floor coordinates to integers; fractional surfaces may be rejected as unsafe.").getBoolean();
        feedback = config.get("general", "feedbackMessages", true, "Show concise action feedback.").getBoolean();
        Property template = config.get("general", "commandTemplate", TeleportCommandFormatter.DEFAULT_TEMPLATE,
                "One chat command with exactly one each of {x}, {y}, {z}. No newline or other placeholders.");
        commandTemplate = template.getString();
        if (!TeleportCommandFormatter.validTemplate(commandTemplate)) {
            commandTemplate = TeleportCommandFormatter.DEFAULT_TEMPLATE; template.set(commandTemplate); corrected = true;
        }
        Property mode = config.get("execution", "mode", "MANUAL_CONFIRMATION", "MANUAL_CONFIRMATION or DIRECT_COMMAND. Direct mode is private testing only.");
        try { executionMode = ExecutionPolicy.Mode.valueOf(mode.getString()); }
        catch (IllegalArgumentException e) { executionMode = ExecutionPolicy.Mode.MANUAL_CONFIRMATION; mode.set(executionMode.name()); corrected = true; }
        directAuthorized = config.get("execution", "privateTestingAuthorized", false,
                "Set true only with express authorization for automation. Required even in singleplayer.").getBoolean();
        directAllowedServers = config.get("execution", "directAllowedServers", new String[0],
                "Exact authorized private hostnames/IP addresses, no wildcards. Recognized Hypixel domains are always blocked.").getStringList();
        if (config.hasChanged()) config.save();
    }

    private double bounded(Configuration config, String name, double fallback, double min, double max, String description) {
        Property property = config.get("general", name, fallback, description);
        double value = property.getDouble(fallback);
        boolean integral = name.equals("coordinatePrecision") || name.equals("directCommandCooldownTicks");
        if (!Double.isFinite(value) || value < min || value > max || (integral && value != Math.floor(value))) {
            property.set(fallback); corrected = true; return fallback;
        }
        // getDouble falls back on malformed strings; persist a parseable value as well.
        try { Double.parseDouble(property.getString()); }
        catch (NumberFormatException e) { property.set(fallback); corrected = true; }
        return value;
    }
}
