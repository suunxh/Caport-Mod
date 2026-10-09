package io.github.suunxh.housingteleporthelper.core;

import java.net.IDN;
import java.util.Locale;

public final class ExecutionPolicy {
    public enum Mode { MANUAL_CONFIRMATION, DIRECT_COMMAND }
    public static final Mode DEFAULT_MODE = Mode.MANUAL_CONFIRMATION;
    private ExecutionPolicy() { }
    public static String hostname(String address) {
        if (address == null) return "";
        String host = address.trim().toLowerCase(Locale.ROOT);
        // Bracketed IPv6 is an exact address; an unbracketed IPv6 host is not accepted.
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            if (end < 0 || !host.substring(end + 1).matches("(?::[0-9]{1,5})?")) return "";
            return host.substring(0, end + 1);
        }
        int colon = host.indexOf(':');
        if (colon >= 0) {
            if (!host.substring(colon).matches(":[0-9]{1,5}")) return "";
            host = host.substring(0, colon);
        }
        while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        try { host = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES); }
        catch (IllegalArgumentException e) { return ""; }
        return host;
    }
    public static boolean recognizedHypixel(String address) {
        String host = hostname(address);
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net")
                || host.equals("hypixel.io") || host.endsWith(".hypixel.io");
    }
    public static boolean directAllowed(boolean authorized, boolean singleplayer, String address, String[] allowedHosts) {
        return directAllowed(authorized, singleplayer, address, allowedHosts, false);
    }
    public static boolean directAllowed(boolean authorized, boolean singleplayer, String address, String[] allowedHosts,
                                        boolean hypixelRiskAcknowledged) {
        if (!authorized) return false;
        if (singleplayer) return true;
        String host = hostname(address);
        if (host.isEmpty() || (recognizedHypixel(address) && !hypixelRiskAcknowledged)) return false;
        for (String allowed : allowedHosts) {
            if (!host.isEmpty() && host.equals(hostname(allowed))) return true;
        }
        return false;
    }
}
