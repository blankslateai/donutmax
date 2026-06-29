package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Toolkit: Ender Chest Viewer
 * Saves EC contents when the player opens their ender chest (27 slots).
 * The saved snapshot can be viewed any time via a GUI screen (even away from an EC).
 *
 * Data is saved as NBT to .minecraft/config/donutsmp-ultimate-ec.nbt
 */
public class EnderChestViewerFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger("DonutSMPUltimate/EC");
    private static final EnderChestViewerFeature INSTANCE = new EnderChestViewerFeature();
    public static EnderChestViewerFeature getInstance() { return INSTANCE; }

    /** Last saved ender chest contents (27 slots). Null = never captured. */
    private List<ItemStack> savedContents = null;

    /**
     * Called from ScreenHandlerMixin when an ender chest screen opens.
     * Copies the slot contents into savedContents.
     */
    public void captureEnderChest(List<ItemStack> slots) {
        if (!DonutConfig.getInstance().enableEnderChestViewer) return;
        savedContents = new ArrayList<>();
        for (ItemStack stack : slots) {
            savedContents.add(stack.copy());
        }
        save();
        LOGGER.info("[DonutSMP Ultimate] Ender chest contents captured ({} slots).", savedContents.size());
    }

    public List<ItemStack> getSavedContents() {
        if (savedContents == null) load();
        return savedContents != null ? List.copyOf(savedContents) : List.of();
    }

    public boolean hasSavedContents() {
        if (savedContents == null) load();
        return savedContents != null && !savedContents.isEmpty();
    }

    // ── NBT persistence ──────────────────────────────────────────────────

    private Path savePath() {
        return FabricLoader.getInstance().getConfigDir().resolve("donutsmp-ultimate-ec.nbt");
    }

    private void save() {
        if (savedContents == null) return;
        NbtCompound root = new NbtCompound();
        NbtList list = new NbtList();
        for (int i = 0; i < savedContents.size(); i++) {
            ItemStack stack = savedContents.get(i);
            if (!stack.isEmpty()) {
                NbtCompound entry = new NbtCompound();
                entry.putByte("Slot", (byte) i);
                stack.encode(MinecraftClient.getInstance().world.getRegistryManager(), entry);
                list.add(entry);
            }
        }
        root.put("Items", list);
        try {
            NbtIo.write(root, savePath());
        } catch (Exception e) {
            LOGGER.error("Failed to save EC data", e);
        }
    }

    private void load() {
        File file = savePath().toFile();
        if (!file.exists()) return;
        try {
            NbtCompound root = NbtIo.read(savePath());
            if (root == null) return;
            NbtList list = root.getList("Items", 10);
            savedContents = new ArrayList<>();
            for (int i = 0; i < 27; i++) savedContents.add(ItemStack.EMPTY);
            var rm = MinecraftClient.getInstance().world != null
                    ? MinecraftClient.getInstance().world.getRegistryManager()
                    : null;
            if (rm == null) return;
            for (int i = 0; i < list.size(); i++) {
                NbtCompound entry = list.getCompound(i);
                int slot = entry.getByte("Slot") & 0xFF;
                if (slot < 27) {
                    ItemStack.fromNbt(rm, entry).ifPresent(stack -> savedContents.set(slot, stack));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load EC data: {}", e.getMessage());
        }
    }
}
