package net.runelite.client.plugins.microbot.smartminer;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginConstants.DEFAULT_PREFIX + "Smart Miner",
        description = "Advanced mining bot with webwalker, preset locations, and smart features",
        tags = {"mining", "ore", "microbot", "skilling"},
        authors = {"Community"},
        version = SmartMinerPlugin.version,
        minClientVersion = "1.9.8",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class SmartMinerPlugin extends Plugin {
    static final String version = "1.0.0";

    @Inject
    private SmartMinerConfig config;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SmartMinerOverlay overlay;

    @Inject
    private SmartMinerDebugOverlay debugOverlay;

    @Provides
    SmartMinerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SmartMinerConfig.class);
    }

    private SmartMinerScript script;

    public SmartMinerScript getScript() {
        return script;
    }

    @Override
    protected void startUp() throws AWTException {
        log.info("Smart Miner plugin started");
        if (overlayManager != null) {
            overlayManager.add(overlay);
            overlayManager.add(debugOverlay);
        }
        script = new SmartMinerScript();
        script.run(config);
    }

    @Override
    protected void shutDown() {
        log.info("Smart Miner plugin stopped");
        if (overlayManager != null) {
            overlayManager.remove(overlay);
            overlayManager.remove(debugOverlay);
        }
        if (script != null) {
            script.shutdown();
        }
    }
}
