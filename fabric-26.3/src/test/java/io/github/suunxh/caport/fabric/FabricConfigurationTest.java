package io.github.suunxh.caport.fabric;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import io.github.suunxh.housingteleporthelper.core.ExecutionPolicy;
import static org.junit.Assert.*;
public class FabricConfigurationTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    @Test public void persistedModesHostsAndHintSurviveRestart() {
        Path file=temp.getRoot().toPath().resolve("caport.properties");
        FabricConfiguration first=new FabricConfiguration(file);
        assertEquals(ExecutionPolicy.DEFAULT_MODE,first.executionMode);assertFalse(first.directAuthorized);
        assertFalse(first.enableDirect(false,"mc.hypixel.net",true,false));
        assertTrue(first.enableDirect(false,"mc.hypixel.net",true,true));
        first.manualModeHintShown=true;first.commandTemplate="/teleport {x} {y} {z}";first.save();
        FabricConfiguration loaded=new FabricConfiguration(file);
        assertEquals(ExecutionPolicy.Mode.DIRECT_COMMAND,loaded.executionMode);assertTrue(loaded.manualModeHintShown);
        assertEquals(first.commandTemplate,loaded.commandTemplate);
        assertTrue(ExecutionPolicy.directAllowed(loaded.directAuthorized,false,"mc.hypixel.net",loaded.directAllowedServers,loaded.hypixelRiskAcknowledged));
        loaded.manual();FabricConfiguration manual=new FabricConfiguration(file);
        assertEquals(ExecutionPolicy.DEFAULT_MODE,manual.executionMode);assertFalse(manual.hypixelRiskAcknowledged);
    }
    @Test public void invalidValuesAreCorrectedAndSaved() throws Exception {
        Path file=temp.getRoot().toPath().resolve("caport.properties");
        Files.writeString(file,"pressurePlateRange=1000\ncoordinatePrecision=2.5\ncommandTemplate=/tp {x} {z}\nmode=wrong\n");
        FabricConfiguration c=new FabricConfiguration(file);
        assertEquals(64,c.plateRange,0);assertEquals(5,c.precision);assertEquals("/tp {x} {y} {z}",c.commandTemplate);
        assertEquals(ExecutionPolicy.DEFAULT_MODE,c.executionMode);
        FabricConfiguration restarted=new FabricConfiguration(file);assertEquals(64,restarted.plateRange,0);
    }
}
