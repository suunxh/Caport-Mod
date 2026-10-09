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
}
