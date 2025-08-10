package me.padej.arcadegames_svc.mixin.inject;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.padej.arcadegames_svc.client.ArcadeGamesClient.GAMES;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(
            method = "getName",
            at = @At("RETURN"),
            cancellable = true
    )
    private void modifyGameItemNames(CallbackInfoReturnable<Text> cir) {
        ItemStack itemStack = (ItemStack) (Object) this;

        if (itemStack.getItem() == Items.PLAYER_HEAD || itemStack.getItem() == Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
            Text originalName = cir.getReturnValue();

            if (GAMES.containsKey(originalName.getString())) {
                MutableText customName = Text.literal(originalName.getString())
                        .setStyle(Style.EMPTY
                                .withColor(TextColor.fromRgb(0x3bea62))
                                .withItalic(false));

                cir.setReturnValue(customName);
            }
        }
    }
}