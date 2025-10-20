package net.runelite.client.plugins.microbot.pvprl;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

/**
 * PVP RL Plugin - AI-powered PvP bot using reinforcement learning
 *
 * Connects to Python serve-api to get action predictions from trained RL model
 * Handles the challenge of executing 12+ actions per game tick via priority queue
 *
 * @author Claude/Elite
 * @version 1.0.0
 */
@PluginDescriptor(
    name = "PVP RL",
    description = "AI-powered PvP bot using reinforcement learning from trained models",
    tags = {"pvp", "ai", "combat", "reinforcement learning", "nh"},
    enabledByDefault = false,
    minClientVersion = "2.0.0"
)
@Slf4j
public class PvpRLPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private PvpRLConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PvpRLOverlay overlay;

    private PvpRLScript script;

    @Provides
    PvpRLConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PvpRLConfig.class);
    }

    /**
     * GameTick event handler - auto-starts bot when player is attacked in LMS
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        // Only auto-start if enabled in config and bot isn't already running
        if (!config.autoStart() || isBotRunning()) {
            return;
        }

        // Check if player is being attacked
        if (isPlayerBeingAttacked()) {
            log.info("Player is being attacked - auto-starting PVP RL bot!");
            startBot();
        }
    }

    /**
     * Check if the player is currently being attacked by another player
     */
    private boolean isPlayerBeingAttacked() {
        if (!Microbot.isLoggedIn()) {
            return false;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return false;
        }

        // Check if we're interacting with someone (being attacked)
        Actor interacting = localPlayer.getInteracting();
        if (interacting instanceof Player) {
            // We're in combat with another player
            return true;
        }

        // Check if anyone is attacking us
        for (Player player : client.getPlayers()) {
            if (player == null || player == localPlayer) {
                continue;
            }

            Actor theirTarget = player.getInteracting();
            if (theirTarget == localPlayer) {
                // This player is targeting us
                return true;
            }
        }

        return false;
    }

    @Override
    protected void startUp() throws Exception {
        log.info("PVP RL Plugin starting up...");

        if (config.showOverlay() && overlay != null) {
            overlayManager.add(overlay);
        }

        if (config.autoStart()) {
            log.info("PVP RL Plugin started - bot will auto-start when attacked");
        } else {
            log.info("PVP RL Plugin started - auto-start disabled");
        }
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("PVP RL Plugin shutting down...");

        if (script != null && script.isRunning()) {
            stopBot();
        }

        if (overlay != null) {
            overlayManager.remove(overlay);
        }

        log.info("PVP RL Plugin shut down");
    }

    /**
     * Start the bot
     * Called from UI panel button
     */
    public void startBot() {
        if (script != null && script.isRunning()) {
            log.warn("Bot is already running");
            return;
        }

        log.info("Starting PVP RL bot...");

        // Create new script instance
        script = new PvpRLScript(config);

        // Start the script (it will manage its own scheduler)
        script.start();

        log.info("PVP RL bot started successfully");
    }

    /**
     * Stop the bot
     * Called from UI panel button or on error
     */
    public void stopBot() {
        if (script == null) {
            return;
        }

        log.info("Stopping PVP RL bot...");

        script.shutdown();
        script = null;

        log.info("PVP RL bot stopped");
    }

    /**
     * Check if bot is running
     */
    public boolean isBotRunning() {
        return script != null && script.isRunning();
    }

    /**
     * Get current script instance (for overlay)
     */
    public PvpRLScript getScript() {
        return script;
    }
}
