package com.donutsmpultimate.mixin;

import com.donutsmpultimate.feature.ChestFilterFeature;
import com.donutsmpultimate.feature.EnderChestViewerFeature;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * When a GenericContainerScreen (including Ender Chest) is initialised,
 * capture its contents for the ender chest viewer and run the chest filter.
 */
@Mixin(GenericContainerScreen.class)
public abstract class ScreenHandlerMixin extends Screen {

    protected ScreenHandlerMixin(Text title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        GenericContainerScreen self = (GenericContainerScreen)(Object)this;
        GenericContainerScreenHandler handler = self.getScreenHandler();

        List<ItemStack> items = new ArrayList<>();
        // Collect only the container slots (not the player inventory below)
        int containerSlots = handler.getRows() * 9;
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = handler.slots.get(i);
            items.add(slot.getStack().copy());
        }

        // Chest filter sound/highlight trigger
        ChestFilterFeature.getInstance().processContainerItems(items);

        // Ender chest: a 3-row (27 slot) container titled "Ender Chest"
        if (containerSlots == 27 && self.getTitle().getString().contains("Ender Chest")) {
            EnderChestViewerFeature.getInstance().captureEnderChest(items);
        }
    }
}
