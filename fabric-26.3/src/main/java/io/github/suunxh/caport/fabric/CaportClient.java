package io.github.suunxh.caport.fabric;

import io.github.suunxh.housingteleporthelper.core.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.sdl.SDLMouse;
import java.nio.FloatBuffer;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class CaportClient implements ClientModInitializer {
    public static CaportClient instance;
    private FabricConfiguration config;
    private KeyMapping targetKey,plateKey;
    private final PressGate gate=new PressGate();
    private final PressurePlateIndex index=new PressurePlateIndex();
    private final CommandDelivery delivery=new CommandDelivery();
    private final TeleportCommandFormatter formatter=new TeleportCommandFormatter();
    private ClientLevel previousWorld,searchWorld;
    private net.minecraft.client.player.LocalPlayer searchPlayer;
    private PlateSearch search;
    private FabricWorldView searchView;
    private float searchWidth,searchHeight;
    private Vec3d searchOrigin;
    private long ticks,searchStarted;
    private int smokeTicks;
    @Override public void onInitializeClient() {
        instance=this;
        config=new FabricConfiguration(FabricLoader.getInstance().getConfigDir().resolve("caport.properties"));
        KeyMapping.Category category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("caport","controls"));
        targetKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.caport.target",InputConstants.Type.KEYBOARD,InputConstants.KEY_G,category));
        plateKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.caport.plate",InputConstants.Type.KEYBOARD,InputConstants.KEY_H,category));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientChunkEvents.CHUNK_LOAD.register((world,chunk)-> { if(world==Minecraft.getInstance().level)index.invalidateChunk(chunk.getPos().x(),chunk.getPos().z()); });
        ClientChunkEvents.CHUNK_UNLOAD.register((world,chunk)-> { if(world==Minecraft.getInstance().level)index.invalidateChunk(chunk.getPos().x(),chunk.getPos().z()); });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,context)->dispatcher.register(literal("caport")
            .executes(c->status(c.getSource()))
            .then(literal("status").executes(c->status(c.getSource())))
            .then(literal("manual").executes(c->{config.manual();commandMessage(c.getSource(),"Manual confirmation enabled and saved.");return 1;}))
            .then(literal("direct").executes(c->direct(c.getSource(),false,false))
                .then(literal("authorized").executes(c->direct(c.getSource(),true,false)))
                .then(literal("hypixel-risk").executes(c->direct(c.getSource(),true,true))))));
        org.slf4j.LoggerFactory.getLogger("caport").info("caport 1.1.0 Fabric 26.3 initialized");
    }
    private String address(Minecraft mc) { return mc.getConnection()==null || mc.getConnection().getServerData()==null ? "" : mc.getConnection().getServerData().ip; }
    private int status(FabricClientCommandSource source) {
        commandMessage(source,"Mode: "+config.executionMode+". Hypixel risk opt-in: "+config.hypixelRiskAcknowledged+".");return 1;
    }
    private int direct(FabricClientCommandSource source,boolean acknowledged,boolean risk) {
        Minecraft mc=source.getClient();String host=address(mc);boolean single=mc.hasSingleplayerServer();
        if(mc.player==null || mc.level==null) {commandMessage(source,"Join a world first.");return 0;}
        boolean hypixel=!single && ExecutionPolicy.recognizedHypixel(host);
        if(hypixel && !risk) {commandMessage(source,"Use /caport direct hypixel-risk for Hypixel direct mode. Use at your own risk.");return 0;}
        if(risk && !hypixel) {commandMessage(source,"hypixel-risk is only for recognized Hypixel hosts.");return 0;}
        if(!single && !acknowledged) {commandMessage(source,"Use /caport direct authorized for this private server.");return 0;}
        if(!config.enableDirect(single,host,acknowledged,risk)) {commandMessage(source,"Direct mode unavailable on this server.");return 0;}
        commandMessage(source,"One-key commands enabled and saved. /caport manual restores confirmation. Use at your own risk.");return 1;
    }
    private void commandMessage(FabricClientCommandSource source,String text) { source.sendFeedback(Component.literal("[caport] "+text)); }
    private void message(String text) {
        Minecraft mc=Minecraft.getInstance();
        if(config.feedback && mc.player!=null)mc.gui.hud.getChat().addMessage(Component.literal("[caport] "+text));
    }
    public void blockChanged(ClientLevel world,int x,int y,int z) {
        if(world==Minecraft.getInstance().level)index.invalidate(x,y,z);
    }
    private Vec3d vector(Vec3 v) { return new Vec3d(v.x,v.y,v.z); }
    private int code(KeyMapping mapping) {
        InputConstants.Key key=KeyMappingHelper.getBoundKeyOf(mapping);
        return key.equals(InputConstants.UNKNOWN) ? 0 : key.getType()==InputConstants.Type.MOUSE ? -100-key.getValue() : key.getValue();
    }
    private boolean down(KeyMapping mapping) {
        InputConstants.Key key=KeyMappingHelper.getBoundKeyOf(mapping);
        if(key.equals(InputConstants.UNKNOWN))return false;
        if(key.getType()==InputConstants.Type.MOUSE)
            return (SDLMouse.SDL_GetMouseState((FloatBuffer)null,(FloatBuffer)null) & (1 << (key.getValue()-1)))!=0;
        return InputConstants.isKeyDown(key.getValue());
    }
    private void tick(Minecraft mc) {
        ticks++;
        if(Boolean.getBoolean("caport.smokeTest") && mc.gui.screen() instanceof net.minecraft.client.gui.screens.TitleScreen
                && mc.gui.overlay()==null && ++smokeTicks>=5) {
            org.slf4j.LoggerFactory.getLogger("caport").info("caport client smoke: title screen reached");mc.stop();return;
        }
        if(mc.level!=previousWorld) {search=null;index.clear();delivery.reset();previousWorld=mc.level;}
        boolean enabled=mc.player!=null && mc.level!=null && mc.gui.screen()==null && mc.isWindowActive();
        while(targetKey.consumeClick()) { } while(plateKey.consumeClick()) { }
        PressGate.Presses presses=gate.update(code(targetKey),code(plateKey),down(targetKey),down(plateKey),enabled);
        if(mc.player!=null && mc.level!=null && search==null) {
            FabricWorldView view=new FabricWorldView(mc.level,mc.player);
            index.prepare(view,vector(mc.player.position()),config.plateRange);index.advance(view,8192,2_000_000L);
        }
        if(presses.conflict) {search=null;message("Actions share a key. Assign different keys in Controls.");}
        if(presses.target || presses.plate) {
            search=null;FabricWorldView view=new FabricWorldView(mc.level,mc.player);
            DestinationValidator validator=new DestinationValidator(mc.player.getBbWidth(),mc.player.getBbHeight());
            if(presses.target) {
                BlockTargetService.Result result=new BlockTargetService().target(view,vector(mc.player.getEyePosition()),vector(mc.player.getViewVector(1)),config.targetRange,validator);
                if(result.destination!=null)execute(result.destination,view,validator,mc);
                else message(result.failure==BlockTargetService.Failure.UNSAFE ? "Unsafe destination." : "No loaded target block found within range.");
            } else {
                searchWorld=mc.level;searchPlayer=mc.player;searchView=view;searchOrigin=vector(mc.player.position());searchStarted=ticks;
                searchWidth=mc.player.getBbWidth();searchHeight=mc.player.getBbHeight();
                search=index.search(view,validator,searchOrigin,mc.player.getYRot(),config.plateRange,config.coneHalfAngle);
            }
        }
        if(search==null)return;
        if(!enabled || mc.level!=searchWorld || mc.player!=searchPlayer || ticks-searchStarted>200
                || searchOrigin.distanceSquared(vector(mc.player.position()))>.25
                || mc.player.getBbWidth()!=searchWidth || mc.player.getBbHeight()!=searchHeight) {search=null;return;}
        searchView.beginTick();
        search.advance(8192,2_000_000L);
        if(search.isDone()) {
            Destination d=search.result();search=null;
            if(d==null)message("No pressure plate found in range and facing cone.");
            else execute(d,new FabricWorldView(mc.level,mc.player),new DestinationValidator(mc.player.getBbWidth(),mc.player.getBbHeight()),mc);
        }
    }
    private void execute(Destination d,FabricWorldView view,DestinationValidator validator,Minecraft mc) {
        Destination current=validator.standingOn(view,d.blockX,d.blockY,d.blockZ,d.pressurePlate);
        if(current==null || current.feet.distanceSquared(d.feet)>1e-10) {message("Unsafe destination: the target changed.");return;}
        TeleportCommandFormatter.Formatted command;
        try {command=formatter.format(config.commandTemplate,d.feet,config.precision,config.integerOnly);}
        catch(IllegalArgumentException e) {message("Invalid command configuration.");return;}
        Vec3d p=command.coordinates;
        if(Vec3d.floor(p.x)!=d.blockX || Vec3d.floor(p.z)!=d.blockZ || !validator.isSafeAt(view,p)) {
            message("Unsafe destination after rounding. Use decimal coordinates or more precision.");return;
        }
        boolean allowed=ExecutionPolicy.directAllowed(config.directAuthorized,mc.hasSingleplayerServer(),address(mc),config.directAllowedServers,config.hypixelRiskAcknowledged);
        CommandDelivery.Outcome outcome=delivery.deliver(config.executionMode,allowed,command.command,ticks,config.cooldownTicks,new CommandDelivery.Sink() {
            @Override public void prepare(String text) {mc.gui.setScreen(new ChatScreen(text,false));}
            @Override public void send(String text) {mc.getConnection().sendCommand(text.substring(1));}
        });
        switch(outcome) {
            case SENT -> message("Teleport command sent once.");
            case COOLDOWN -> message("Direct command cooldown active.");
            case RESTRICTED -> message("Direct mode unavailable on this server; press Enter to send.");
            default -> message("Command prepared. Press Enter to send.");
        }
        if((outcome==CommandDelivery.Outcome.PREPARED || outcome==CommandDelivery.Outcome.RESTRICTED) && config.feedback && !config.manualModeHintShown) {
            message("Tip: Skip Enter with "+ExecutionPolicy.directModeCommand(mc.hasSingleplayerServer(),address(mc))+". Saved across restarts. Use at your own risk.");
            config.manualModeHintShown=true;config.save();
        }
    }
}
