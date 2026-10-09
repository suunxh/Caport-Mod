package io.github.suunxh.housingteleporthelper.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TeleportCommandFormatter {
    public static final String DEFAULT_TEMPLATE = "/tp {x} {y} {z}";
    public static final class Formatted {
        public final String command;
        public final Vec3d coordinates;
        private Formatted(String command, Vec3d coordinates) { this.command = command; this.coordinates = coordinates; }
    }
    public static boolean validTemplate(String template) {
        if (template == null || template.length() > 80
                || !template.matches("/[A-Za-z][A-Za-z0-9:_-]*(?: +(?:[A-Za-z0-9_@.:-]+|\\{[xyz]\\}))+")) return false;
        for (String axis : new String[] {"x", "y", "z"}) {
            String token = "{" + axis + "}";
            int at = template.indexOf(token);
            if (at < 0 || template.indexOf(token, at + token.length()) >= 0) return false;
        }
        return true;
    }
    public Formatted format(String template, Vec3d destination, int precision, boolean integerOnly) {
        if (!validTemplate(template) || precision < 0 || precision > 8) throw new IllegalArgumentException("Invalid command configuration");
        String x = number(destination.x, precision, integerOnly);
        String y = number(destination.y, precision, integerOnly);
        String z = number(destination.z, precision, integerOnly);
        String command = template.replace("{x}", x).replace("{y}", y).replace("{z}", z);
        if (command.length() > 100) throw new IllegalArgumentException("Command exceeds Minecraft 1.8.9 chat limit");
        return new Formatted(command, new Vec3d(Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z)));
    }
    private String number(double value, int precision, boolean integerOnly) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid coordinate");
        return BigDecimal.valueOf(value).setScale(integerOnly ? 0 : precision,
                integerOnly ? RoundingMode.FLOOR : RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
