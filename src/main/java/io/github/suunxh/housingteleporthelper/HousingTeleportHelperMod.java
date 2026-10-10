package io.github.suunxh.housingteleporthelper;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.client.ClientCommandHandler;

@Mod(modid = HousingTeleportHelperMod.MOD_ID, name = "caport", version = "1.0.2",
        acceptedMinecraftVersions = "[1.8.9]", clientSideOnly = true, acceptableRemoteVersions = "*")
public final class HousingTeleportHelperMod {
    public static final String MOD_ID = "housingteleporthelper";
    private final ModConfiguration config = new ModConfiguration();
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide().isClient()) config.load(event.getSuggestedConfigurationFile());
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        if (event.getSide().isClient()) {
            FMLCommonHandler.instance().bus().register(new KeybindHandler(config));
            ClientCommandHandler.instance.registerCommand(new CaportCommand(config));
        }
    }
}
