package io.github.suunxh.caport.fabric.mixin;

import io.github.suunxh.caport.fabric.CaportClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class BlockUpdatesMixin {
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("RETURN"))
    private void caport$changed(BlockPos pos,BlockState state,int flags,int depth,CallbackInfoReturnable<Boolean> callback) {
        if(callback.getReturnValue() && (Object)this instanceof ClientLevel world && CaportClient.instance!=null)
            CaportClient.instance.blockChanged(world,pos.getX(),pos.getY(),pos.getZ());
    }
}
