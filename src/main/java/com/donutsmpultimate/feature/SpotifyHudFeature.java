package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Toolkit: Spotify HUD (Windows only)
 *
 * Listens on a local TCP socket (localhost:19234) for the currently playing song.
 * A companion helper executable (DonutSpotifyHelper.exe, source provided separately)
 * reads from Windows System Media Transport Controls and writes to this socket.
 *
 * Protocol: the helper sends a single UTF-8 line: "Artist - Title\n"
 * The helper only connects to localhost — no external network traffic.
 */
public class SpotifyHudFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger("DonutSMPUltimate/Spotify");
    private static final int PORT = 19234;

    private static final SpotifyHudFeature INSTANCE = new SpotifyHudFeature();
    public static SpotifyHudFeature getInstance() { return INSTANCE; }

    private String currentSong = "";
    private ServerSocket serverSocket;
    private Thread listenerThread;
    private volatile boolean running = false;

    public void start() {
        if (!DonutConfig.getInstance().enableSpotifyHUD) return;
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            LOGGER.info("[Spotify HUD] Windows only — skipping.");
            return;
        }
        running = true;
        listenerThread = new Thread(this::listenLoop, "DonutSMP-SpotifyListener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        LOGGER.info("[Spotify HUD] Listening on localhost:{}", PORT);
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
    }

    private void listenLoop() {
        try {
            serverSocket = new ServerSocket(PORT, 1,
                    java.net.InetAddress.getByName("127.0.0.1")); // localhost ONLY
            while (running) {
                try (Socket client = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (line != null) currentSong = line.trim();
                } catch (Exception e) {
                    if (running) LOGGER.debug("Spotify socket error: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            if (running) LOGGER.warn("Spotify HUD failed to start: {}", e.getMessage());
        }
    }

    public void render(DrawContext ctx, RenderTickCounter ticker) {
        if (!DonutConfig.getInstance().enableSpotifyHUD) return;
        if (currentSong.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        String label = "§2♫ §f" + currentSong;
        int tw = client.textRenderer.getWidth(label);
        int x = (sw - tw) / 2;
        int y = sh - 30;

        ctx.fill(x - 4, y - 2, x + tw + 4, y + 11, 0x99000000);
        ctx.drawText(client.textRenderer, label, x, y, 0xFFFFFF, true);
    }
}
