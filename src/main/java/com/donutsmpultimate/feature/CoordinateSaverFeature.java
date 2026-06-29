package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Toolkit: Coordinate Saver
 * Press keybind to save current XYZ with a label (timestamp) to a JSON file.
 * Saved coords persist between sessions.
 */
public class CoordinateSaverFeature {

    private static final CoordinateSaverFeature INSTANCE = new CoordinateSaverFeature();
    public static CoordinateSaverFeature getInstance() { return INSTANCE; }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private List<SavedCoord> saves = new ArrayList<>();

    public record SavedCoord(String label, int x, int y, int z, String dimension) {}

    public CoordinateSaverFeature() {
        load();
    }

    /** Called when the keybind is pressed. Saves current position. */
    public void saveCurrentPosition() {
        if (!DonutConfig.getInstance().enableCoordSaver) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        PlayerEntity p = client.player;
        BlockPos pos = p.getBlockPos();
        String dim = client.world.getRegistryKey().getValue().toString();
        String label = FMT.format(LocalDateTime.now());

        SavedCoord coord = new SavedCoord(label, pos.getX(), pos.getY(), pos.getZ(), dim);
        saves.add(coord);
        persist();

        String msg = String.format("§aSaved: §f%s §7@ §f%d, %d, %d §7[%s]",
                label, pos.getX(), pos.getY(), pos.getZ(), dim);
        client.player.sendMessage(Text.literal(msg), false);
    }

    public List<SavedCoord> getSaves() {
        return List.copyOf(saves);
    }

    public void deleteAt(int index) {
        if (index >= 0 && index < saves.size()) {
            saves.remove(index);
            persist();
        }
    }

    private Path savePath() {
        return FabricLoader.getInstance().getConfigDir().resolve("donutsmp-ultimate-coords.json");
    }

    private void load() {
        File file = savePath().toFile();
        if (!file.exists()) return;
        try (Reader r = new FileReader(file)) {
            JsonArray arr = JsonParser.parseReader(r).getAsJsonArray();
            saves = new ArrayList<>();
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                saves.add(new SavedCoord(
                    o.get("label").getAsString(),
                    o.get("x").getAsInt(),
                    o.get("y").getAsInt(),
                    o.get("z").getAsInt(),
                    o.has("dimension") ? o.get("dimension").getAsString() : "?"
                ));
            }
        } catch (Exception ignored) {}
    }

    private void persist() {
        JsonArray arr = new JsonArray();
        for (SavedCoord c : saves) {
            JsonObject o = new JsonObject();
            o.addProperty("label", c.label());
            o.addProperty("x", c.x());
            o.addProperty("y", c.y());
            o.addProperty("z", c.z());
            o.addProperty("dimension", c.dimension());
            arr.add(o);
        }
        try (Writer w = new FileWriter(savePath().toFile())) {
            GSON.toJson(arr, w);
        } catch (Exception ignored) {}
    }
}
