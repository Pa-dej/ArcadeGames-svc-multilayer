package me.padej.arcadegames_svc.mixin.accessor;

import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SkullBlockEntity.class)
public interface SkullBlockEntityAccessor {
    @Accessor("customName")
    Text getCustomName();
}
