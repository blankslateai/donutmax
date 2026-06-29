package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Mod 2: Look-Pay
 * When keybind is pressed, loads /pay <targeted player> <amount> into chat.
 * Amount is configurable in config.
 */
public class LookPayFeature {

    private static final LookPayFeature INSTANCE = new LookPayFeature();
    public static LookPayFeature getInstance() { return INSTANCE; }

    public void execute() {
        DonutConfig cfg = DonutConfig.getInstance();
        if (!cfg.enableLookPay) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.crosshairTarget instanceof EntityHitResult ehr) {
            Entity target = ehr.getEntity();
            if (target instanceof PlayerEntity player) {
                String name   = player.getName().getString();
                long   amount = cfg.lookPayAmount;
                // Suggest the command — player reviews and presses Enter
                client.inGameHud.getChatHud().addToMessageHistory(
                        "/pay " + name + " " + amount);
                // Pre-fill the chat box
                client.setScreen(new net.minecraft.client.gui.screen.ChatScreen(
                        "/pay " + name + " " + amount));
            }
        } else {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§cNot looking at a player."), true);
            }
        }
    }
}
