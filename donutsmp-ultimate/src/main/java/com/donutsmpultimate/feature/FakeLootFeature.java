package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Toolkit: Fake Loot
 * Makes your held item appear as a different (configurable) item to yourself —
 * useful for faking loot drops on stream. Purely client-side; does not affect
 * what other players see or what's actually in your inventory.
 *
 * The swap is applied in ItemStackMixin when rendering the hotbar slot.
 */
public class FakeLootFeature {

    private static final FakeLootFeature INSTANCE = new FakeLootFeature();
    public static FakeLootFeature getInstance() { return INSTANCE; }

    private boolean enabled = false;

    public void toggle() {
        enabled = !enabled;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                net.minecraft.text.Text.literal(enabled ? "§aFake Loot ON" : "§cFake Loot OFF"), true);
        }
    }

    public boolean isEnabled() {
        return enabled && DonutConfig.getInstance().enableFakeLoot;
    }

    /**
     * Returns the fake item stack to render instead of the real one.
     * Only used for the player's main hand slot.
     */
    public ItemStack getFakeStack(ItemStack real) {
        if (!isEnabled() || real.isEmpty()) return real;
        String fakeId = DonutConfig.getInstance().fakeLootItem;
        Item fakeItem = Registries.ITEM.get(Identifier.tryParse(fakeId));
        if (fakeItem == null) return real;
        return new ItemStack(fakeItem, real.getCount());
    }
}
