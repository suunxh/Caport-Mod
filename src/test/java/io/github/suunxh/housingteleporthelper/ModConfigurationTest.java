package io.github.suunxh.housingteleporthelper;

import io.github.suunxh.housingteleporthelper.core.ExecutionPolicy;
import net.minecraftforge.common.config.Configuration;
import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.lang.reflect.Method;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import static org.junit.Assert.*;

public class ModConfigurationTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    @Before public void initializeForgeTestHome() throws Exception {
        // Forge normally supplies this during LaunchWrapper startup. A plain JUnit
        // JVM needs the same initialization before constructing Configuration.
        Method bootstrap = FMLInjectionData.class.getDeclaredMethod("build", File.class, LaunchClassLoader.class);
        bootstrap.setAccessible(true);
        bootstrap.invoke(null, temp.getRoot(), null);
    }
    @Test public void safeDefaultsPersistAcrossLoads() throws Exception {
        File file = new File(temp.getRoot(), "helper.cfg");
        ModConfiguration first = new ModConfiguration(); first.load(file);
        assertTrue(file.isFile()); assertEquals(ExecutionPolicy.Mode.MANUAL_CONFIRMATION, first.executionMode);
        assertFalse(first.directAuthorized); assertEquals(0, first.directAllowedServers.length);
        assertFalse(first.hypixelRiskAcknowledged);
        assertEquals(64, first.plateRange, 0); assertEquals(100, first.targetRange, 0);
        ModConfiguration second = new ModConfiguration(); second.load(file);
        assertEquals(first.commandTemplate, second.commandTemplate); assertFalse(second.corrected);
    }
    @Test public void invalidSettingsRestoreSafeDefaults() throws Exception {
        File file = new File(temp.getRoot(), "helper.cfg");
        Configuration raw = new Configuration(file);
        raw.get("general", "pressurePlateRange", 64.0).set(1000.0);
        raw.get("general", "targetBlockRange", 100.0).set(Double.NaN);
        raw.get("general", "coordinatePrecision", 5.0).set(1.5);
        raw.get("general", "commandTemplate", "").set("/tp {x} {y}");
        raw.get("execution", "mode", "").set("unknown"); raw.save();
        ModConfiguration config = new ModConfiguration(); config.load(file);
        assertTrue(config.corrected); assertEquals(64, config.plateRange, 0); assertEquals(100, config.targetRange, 0);
        assertEquals(5, config.precision); assertEquals("/tp {x} {y} {z}", config.commandTemplate);
        assertEquals(ExecutionPolicy.Mode.MANUAL_CONFIRMATION, config.executionMode);
        ModConfiguration second = new ModConfiguration(); second.load(file); assertFalse(second.corrected);
    }
    @Test public void singleplayerDirectTogglePersistsAndManualRestoresConfirmation() {
        File file = new File(temp.getRoot(), "caport.cfg");
        ModConfiguration config = new ModConfiguration(); config.load(file);
        assertTrue(config.enableDirectTesting(true, "", false));
        ModConfiguration loaded = new ModConfiguration(); loaded.load(file);
        assertEquals(ExecutionPolicy.Mode.DIRECT_COMMAND, loaded.executionMode); assertTrue(loaded.directAuthorized);
        assertFalse(loaded.hypixelRiskAcknowledged);
        loaded.useManualConfirmation();
        ModConfiguration manual = new ModConfiguration(); manual.load(file);
        assertEquals(ExecutionPolicy.Mode.MANUAL_CONFIRMATION, manual.executionMode);
    }
    @Test public void privateServerNeedsExplicitAuthorizationAndExactHost() {
        File file = new File(temp.getRoot(), "caport.cfg");
        ModConfiguration config = new ModConfiguration(); config.load(file);
        assertFalse(config.enableDirectTesting(false, "private.example", false));
        assertEquals(ExecutionPolicy.Mode.MANUAL_CONFIRMATION, config.executionMode);
        assertTrue(config.enableDirectTesting(false, "PRIVATE.EXAMPLE:25565", true));
        assertArrayEquals(new String[] {"private.example"}, config.directAllowedServers);
        assertTrue(config.enableDirectTesting(false, "private.example", true));
        assertEquals(1, config.directAllowedServers.length);
        ModConfiguration loaded = new ModConfiguration(); loaded.load(file);
        assertTrue(ExecutionPolicy.directAllowed(loaded.directAuthorized, false, "private.example", loaded.directAllowedServers));
        assertFalse(ExecutionPolicy.directAllowed(loaded.directAuthorized, false, "other.example", loaded.directAllowedServers));
    }
    @Test public void hypixelNeedsSeparateOptInWhichManualModeRevokes() {
        File file = new File(temp.getRoot(), "caport.cfg");
        ModConfiguration config = new ModConfiguration(); config.load(file);
        assertFalse(config.enableDirectTesting(false, "mc.hypixel.net", true));
        assertFalse(config.hypixelRiskAcknowledged); assertFalse(config.directAuthorized);
        assertTrue(config.enableDirectTesting(false, "mc.hypixel.net", true, true));
        ModConfiguration loaded = new ModConfiguration(); loaded.load(file);
        assertTrue(loaded.hypixelRiskAcknowledged);
        assertTrue(ExecutionPolicy.directAllowed(loaded.directAuthorized, false, "mc.hypixel.net", loaded.directAllowedServers, loaded.hypixelRiskAcknowledged));
        loaded.useManualConfirmation();
        ModConfiguration manual = new ModConfiguration(); manual.load(file);
        assertFalse(manual.hypixelRiskAcknowledged); assertEquals(ExecutionPolicy.Mode.MANUAL_CONFIRMATION, manual.executionMode);
    }
    @Test public void blankOrMalformedHostsCannotBeAuthorized() {
        ModConfiguration config = new ModConfiguration(); config.load(new File(temp.getRoot(), "caport.cfg"));
        assertFalse(config.enableDirectTesting(false, "", true));
        assertFalse(config.enableDirectTesting(false, "mc.hypixel.net:25565:extra", true, true));
        assertFalse(config.directAuthorized);
    }
    @Test public void firstUseHintSurvivesRestartAndModeChanges() {
        File file = new File(temp.getRoot(), "caport.cfg");
        ModConfiguration config = new ModConfiguration(); config.load(file);
        assertFalse(config.manualModeHintShown);
        config.markManualModeHintShown();
        ModConfiguration restarted = new ModConfiguration(); restarted.load(file);
        assertTrue(restarted.manualModeHintShown);
        assertEquals(ExecutionPolicy.Mode.MANUAL_CONFIRMATION, restarted.executionMode);
        restarted.enableDirectTesting(true, "", false);
        restarted.useManualConfirmation();
        ModConfiguration manual = new ModConfiguration(); manual.load(file);
        assertTrue(manual.manualModeHintShown);
    }
    @Test public void addingHintToExistingConfigPreservesDirectAndCommandSettings() {
        File file = new File(temp.getRoot(), "caport.cfg");
        Configuration old = new Configuration(file);
        old.get("execution", "mode", "").set("DIRECT_COMMAND");
        old.get("execution", "privateTestingAuthorized", false).set(true);
        old.get("execution", "hypixelDirectRiskAcknowledged", false).set(true);
        old.get("execution", "directAllowedServers", new String[0]).set(new String[] {"mc.hypixel.net"});
        old.get("general", "commandTemplate", "").set("/teleport {x} {y} {z}");
        old.get("general", "targetBlockRange", 100.0).set(80.0);
        old.save();
        ModConfiguration upgraded = new ModConfiguration(); upgraded.load(file);
        assertFalse(upgraded.manualModeHintShown);
        upgraded.markManualModeHintShown();
        ModConfiguration loaded = new ModConfiguration(); loaded.load(file);
        assertEquals("/teleport {x} {y} {z}", loaded.commandTemplate);
        assertEquals(80, loaded.targetRange, 0);
        assertEquals(ExecutionPolicy.Mode.DIRECT_COMMAND, loaded.executionMode);
        assertTrue(loaded.directAuthorized); assertTrue(loaded.hypixelRiskAcknowledged);
        assertArrayEquals(new String[] {"mc.hypixel.net"}, loaded.directAllowedServers);
        assertTrue(loaded.manualModeHintShown);
    }
}
