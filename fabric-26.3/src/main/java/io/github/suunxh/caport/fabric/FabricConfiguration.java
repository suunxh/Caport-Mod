package io.github.suunxh.caport.fabric;

import io.github.suunxh.housingteleporthelper.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class FabricConfiguration {
    public double targetRange, plateRange, coneHalfAngle;
    public int precision, cooldownTicks;
    public String commandTemplate;
    public boolean integerOnly, feedback, directAuthorized, hypixelRiskAcknowledged, manualModeHintShown;
    public String[] directAllowedServers;
    public ExecutionPolicy.Mode executionMode;
    private final Path file;
    private final Properties properties = new Properties();
    public FabricConfiguration(Path file) {
        this.file = file;
        try { if (Files.exists(file)) try (InputStream in = Files.newInputStream(file)) { properties.load(in); } }
        catch (IOException e) { throw new UncheckedIOException(e); }
        targetRange = number("targetBlockRange",100,1,256,false);
        plateRange = number("pressurePlateRange",64,1,64,false);
        coneHalfAngle = number("pressurePlateConeHalfAngle",30,1,89,false);
        precision = (int)number("coordinatePrecision",5,0,8,true);
        cooldownTicks = (int)number("directCommandCooldownTicks",20,0,1200,true);
        integerOnly = flag("integerCoordinates",false); feedback = flag("feedbackMessages",true);
        directAuthorized = flag("directAuthorized",false);
        hypixelRiskAcknowledged = flag("hypixelDirectRiskAcknowledged",false);
        manualModeHintShown = flag("manualModeHintShown",false);
        commandTemplate = properties.getProperty("commandTemplate",TeleportCommandFormatter.DEFAULT_TEMPLATE);
        if (!TeleportCommandFormatter.validTemplate(commandTemplate)) commandTemplate = TeleportCommandFormatter.DEFAULT_TEMPLATE;
        try { executionMode = ExecutionPolicy.Mode.valueOf(properties.getProperty("mode","MANUAL_CONFIRMATION")); }
        catch (IllegalArgumentException e) { executionMode = ExecutionPolicy.DEFAULT_MODE; }
        directAllowedServers = Arrays.stream(properties.getProperty("directAllowedServers","").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        save();
    }
    private double number(String key,double fallback,double min,double max,boolean integer) {
        try { double n=Double.parseDouble(properties.getProperty(key,Double.toString(fallback)));
            return Double.isFinite(n) && n>=min && n<=max && (!integer || n==Math.floor(n)) ? n : fallback;
        } catch (NumberFormatException e) { return fallback; }
    }
    private boolean flag(String key,boolean fallback) {
        String value=properties.getProperty(key,Boolean.toString(fallback));
        return value.equalsIgnoreCase("true") ? true : value.equalsIgnoreCase("false") ? false : fallback;
    }
    public boolean enableDirect(boolean singleplayer,String address,boolean acknowledged,boolean risk) {
        String host=ExecutionPolicy.hostname(address);
        if (!singleplayer && (!acknowledged || !ExecutionPolicy.directAllowed(true,false,address,new String[]{host},risk))) return false;
        if (!singleplayer) {
            LinkedHashSet<String> hosts=new LinkedHashSet<>(Arrays.asList(directAllowedServers));hosts.add(host);
            directAllowedServers=hosts.toArray(new String[0]);
            if (ExecutionPolicy.recognizedHypixel(address)) hypixelRiskAcknowledged=risk;
        }
        executionMode=ExecutionPolicy.Mode.DIRECT_COMMAND;directAuthorized=true;save();return true;
    }
    public void manual() { executionMode=ExecutionPolicy.DEFAULT_MODE;hypixelRiskAcknowledged=false;save(); }
    public void save() {
        properties.setProperty("targetBlockRange",Double.toString(targetRange));
        properties.setProperty("pressurePlateRange",Double.toString(plateRange));
        properties.setProperty("pressurePlateConeHalfAngle",Double.toString(coneHalfAngle));
        properties.setProperty("coordinatePrecision",Integer.toString(precision));
        properties.setProperty("directCommandCooldownTicks",Integer.toString(cooldownTicks));
        properties.setProperty("integerCoordinates",Boolean.toString(integerOnly));
        properties.setProperty("feedbackMessages",Boolean.toString(feedback));
        properties.setProperty("commandTemplate",commandTemplate);
        properties.setProperty("mode",executionMode.name());
        properties.setProperty("directAuthorized",Boolean.toString(directAuthorized));
        properties.setProperty("hypixelDirectRiskAcknowledged",Boolean.toString(hypixelRiskAcknowledged));
        properties.setProperty("manualModeHintShown",Boolean.toString(manualModeHintShown));
        properties.setProperty("directAllowedServers",String.join(",",directAllowedServers));
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp=Files.createTempFile(file.toAbsolutePath().getParent(),"caport-",".tmp");
            try {
                try (OutputStream out=Files.newOutputStream(tmp)) { properties.store(out,"caport settings"); }
                try { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException e) { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(tmp); }
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
