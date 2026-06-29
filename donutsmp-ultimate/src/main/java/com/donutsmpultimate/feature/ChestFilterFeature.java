package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Toolkit: Chest Filter
 * When a chest/container screen opens, scans the contents for valuable items.
 * Highlights them and plays a sound effect if any are found.
 */
public class ChestFilterFeature {

    private static final ChestFilterFeature INSTANCE = new ChestFilterFeature();
    public static ChestFilterFeature getInstance() { return INSTANCE; }

    // Items considered "valuable" — extend this list as needed
    private static final Set<String> VALUABLE_ITEMS = Set.of(
        "minecraft:diamond", "minecraft:diamond_block",
        "minecraft:netherite_ingot", "minecraft:netherite_block",
        "minecraft:netherite_scrap",
        "minecraft:emerald", "minecraft:emerald_block",
        "minecraft:gold_ingot", "minecraft:gold_block",
        "minecraft:ancient_debris",
        "minecraft:totem_of_undying",
        "minecraft:elytra",
        "minecraft:nether_star",
        "minecraft:end_crystal",
        "minecraft:beacon"
    );

    /**
     * Called when a container screen is opened with a list of its items.
     * Returns true and plays sound if any valuable items were found.
     */
    public boolean processContainerItems(Iterable<ItemStack> items) {
        if (!DonutConfig.getInstance().enableChestFilter) return false;

        boolean found = false;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            if (VALUABLE_ITEMS.contains(id) || hasValuableEnchantments(stack)) {
                found = true;
                break;
            }
        }

        if (found) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getSoundManager() != null) {
                client.getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f, 1.0f));
            }
        }
        return found;
    }

    /**
     * Returns true if the item stack has any high-tier enchantments.
     * Used by the screen rendering mixin to decide which slots to highlight.
     */
    public boolean isValuable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        return VALUABLE_ITEMS.contains(id) || hasValuableEnchantments(stack);
    }

    private boolean hasValuableEnchantments(ItemStack stack) {
        // Any item with enchantments is considered notable
        return !stack.getEnchantments().isEmpty();
    }
}
