package net.runelite.client.plugins.microbot.wildernessnicky;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.Projectile;
import net.runelite.api.ChatMessageType;
import net.runelite.client.util.Text;
import net.runelite.client.eventbus.Subscribe;

@PluginDescriptor(
    name = PluginConstants.CRANNY + "Wilderness Agility",
    description = "Automated wilderness agility training with banking and ticket collection",
    version = WildernessNickyPlugin.version,
    authors = { "Cranny" },
    minClientVersion = "2.0.21",
    tags = {"agility", "skilling", "solo", "mass", "MoneyMaking"},
    iconUrl = "https://chsami.github.io/Microbot-Hub/WildernessNickyPlugin/assets/icon.png",
    cardUrl = "httpa://chsami.github.io/Microbot-Hub/WildernessNickyPlugin/assets/card.png"
)
public class WildernessNickyPlugin extends Plugin {

    static final String version = "1.6.0";
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private WildernessNickyOverlay overlay;
    @Inject
    private WildernessNickyConfig config;
    @Inject
    private WildernessNickyScript script;

    @Provides
    WildernessNickyConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(WildernessNickyConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        Microbot.log("WildernessNickyPlugin: startUp called");
        if (overlayManager != null) {
            overlayManager.add(overlay);
        }
        overlay.setScript(script);
        overlay.setActive(true);
        script.setPlugin(this);
        script.run(config);
    }

    @Override
    protected void shutDown() throws Exception {
        Microbot.log("WildernessNickyPlugin: shutDown called");
        if (script != null) {
            script.shutdown();
        }
        if (overlayManager != null) {
            overlayManager.remove(overlay);
        }
        overlay.setActive(false);
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        String msg = chatMessage.getMessage(); // Keep raw message for regex matching
        String cleanMsg = Text.removeTags(msg); // Clean version for exact string matching
        
        // Detect friends chat join
        if (chatMessage.getType() == ChatMessageType.FRIENDSCHATNOTIFICATION && cleanMsg.startsWith("Now talking in chat-channel")) {
            script.setLastFcJoinMessageTime(System.currentTimeMillis());
        }
        
        // Detect player death
        if (chatMessage.getType() == ChatMessageType.GAMEMESSAGE && cleanMsg.equals("Oh dear, you are dead!")) {
            Microbot.log("[WildernessNicky] Death detected via chat message");
            script.triggerDeathHandling();
        }
        
        // Detect dispenser loot (use raw message for regex matching)
        if (chatMessage.getType() == ChatMessageType.GAMEMESSAGE) {
            script.handleDispenserChatMessage(msg);
        }
    }
    
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        // Forward looting bag container changes to script for value tracking
        script.handleItemContainerChanged(event);
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event) {
        Projectile projectile = event.getProjectile();

        // Only track projectiles targeting the local player
        if (Microbot.getClient().getLocalPlayer() == null) {
            return;
        }

        // Check if projectile is targeting us (either direct target or area effect at our location)
        boolean isTargetingPlayer = projectile.getTargetActor() == Microbot.getClient().getLocalPlayer();

        if (isTargetingPlayer) {
            // Store projectile by end cycle for the script to handle
            int hitCycle = projectile.getEndCycle();
            script.trackIncomingProjectile(hitCycle, projectile);
        }
    }
} 