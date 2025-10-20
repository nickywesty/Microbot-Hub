package net.runelite.client.plugins.microbot.pvprl;

import net.runelite.client.plugins.microbot.pvprl.action.ActionQueue;
import net.runelite.client.plugins.microbot.pvprl.model.GameState;
import net.runelite.client.plugins.microbot.pvprl.model.PvpAction;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

/**
 * Debug overlay for PVP RL bot
 * Shows connection status, game state, action queue, and performance metrics
 */
public class PvpRLOverlay extends OverlayPanel {

    private final PvpRLPlugin plugin;
    private final PvpRLConfig config;

    @Inject
    public PvpRLOverlay(PvpRLPlugin plugin, PvpRLConfig config) {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay()) {
            return null;
        }

        PvpRLScript script = plugin.getScript();
        if (script == null) {
            renderNotRunning(graphics);
            return super.render(graphics);
        }

        renderRunning(script, graphics);
        return super.render(graphics);
    }

    private void renderNotRunning(Graphics2D graphics) {
        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(TitleComponent.builder()
            .text("PVP RL Bot")
            .color(Color.RED)
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Status:")
            .right("NOT RUNNING")
            .rightColor(Color.RED)
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("")
            .right("Click 'Start' to begin")
            .rightColor(Color.GRAY)
            .build());
    }

    private void renderRunning(PvpRLScript script, Graphics2D graphics) {
        panelComponent.getChildren().clear();

        // Title
        Color titleColor = script.isApiConnected() ? Color.GREEN : Color.ORANGE;
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("PVP RL Bot")
            .color(titleColor)
            .build());

        // Connection status
        String connStatus = script.isApiConnected() ? "CONNECTED" : "DISCONNECTED";
        Color connColor = script.isApiConnected() ? Color.GREEN : Color.RED;
        panelComponent.getChildren().add(LineComponent.builder()
            .left("API:")
            .right(connStatus)
            .rightColor(connColor)
            .build());

        // Model name
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Model:")
            .right(config.modelName())
            .build());

        // API latency
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Latency:")
            .right(script.getApiLatency() + "ms")
            .rightColor(getLatencyColor(script.getApiLatency()))
            .build());

        // Separator
        panelComponent.getChildren().add(LineComponent.builder()
            .left("---")
            .right("---")
            .build());

        // Game state
        GameState state = script.getCurrentState();
        if (state != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("HP:")
                .right(String.format("%d/%d (%.0f%%)",
                    state.getHealth(),
                    state.getMaxHealth(),
                    state.getHealthPercent() * 100))
                .rightColor(getHealthColor(state.getHealthPercent()))
                .build());

            panelComponent.getChildren().add(LineComponent.builder()
                .left("Prayer:")
                .right(String.format("%d/%d (%.0f%%)",
                    state.getPrayer(),
                    state.getMaxPrayer(),
                    state.getPrayerPercent() * 100))
                .rightColor(getPrayerColor(state.getPrayerPercent()))
                .build());

            panelComponent.getChildren().add(LineComponent.builder()
                .left("Spec:")
                .right(String.format("%.0f%%", state.getSpecialPercent()))
                .build());

            String targetName = state.getTarget() != null ?
                state.getTarget().getName() : "None";
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Target:")
                .right(targetName)
                .rightColor(state.getTarget() != null ? Color.YELLOW : Color.GRAY)
                .build());

            if (state.getTarget() != null) {
                panelComponent.getChildren().add(LineComponent.builder()
                    .left("Distance:")
                    .right(state.getDistanceToTarget() + " tiles")
                    .build());
            }
        }

        // Separator
        panelComponent.getChildren().add(LineComponent.builder()
            .left("---")
            .right("---")
            .build());

        // Action queue stats
        ActionQueue queue = script.getActionQueue();
        if (queue != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Queue:")
                .right(queue.size() + " actions")
                .rightColor(queue.size() > 10 ? Color.ORANGE : Color.WHITE)
                .build());

            panelComponent.getChildren().add(LineComponent.builder()
                .left("This tick:")
                .right(queue.getActionsExecutedThisTick() + " exec")
                .build());

            // Show pending actions
            List<PvpAction> pending = queue.getQueueSnapshot();
            if (!pending.isEmpty() && pending.size() <= 5) {
                panelComponent.getChildren().add(LineComponent.builder()
                    .left("Pending:")
                    .right("")
                    .build());

                for (int i = 0; i < Math.min(5, pending.size()); i++) {
                    PvpAction action = pending.get(i);
                    panelComponent.getChildren().add(LineComponent.builder()
                        .left("  " + (i + 1) + ".")
                        .right(action.getDescription())
                        .rightColor(getPriorityColor(action.getPriority()))
                        .build());
                }
            }
        }

        // Separator
        panelComponent.getChildren().add(LineComponent.builder()
            .left("---")
            .right("---")
            .build());

        // Performance stats
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Ticks:")
            .right(String.valueOf(script.getTicksRun()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Requests:")
            .right(String.format("%d/%d",
                script.getSuccessfulRequests(),
                script.getSuccessfulRequests() + script.getFailedRequests()))
            .build());

        // Error message
        if (script.getErrorMessage() != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Error:")
                .right(script.getErrorMessage())
                .rightColor(Color.RED)
                .build());
        }
    }

    private Color getHealthColor(float percent) {
        if (percent < 0.3f) return Color.RED;
        if (percent < 0.6f) return Color.ORANGE;
        return Color.GREEN;
    }

    private Color getPrayerColor(float percent) {
        if (percent < 0.2f) return Color.RED;
        if (percent < 0.5f) return Color.ORANGE;
        return Color.CYAN;
    }

    private Color getLatencyColor(long ms) {
        if (ms > 500) return Color.RED;
        if (ms > 200) return Color.ORANGE;
        return Color.GREEN;
    }

    private Color getPriorityColor(net.runelite.client.plugins.microbot.pvprl.model.ActionPriority priority) {
        switch (priority) {
            case CRITICAL:
                return Color.RED;
            case HIGH:
                return Color.ORANGE;
            case MEDIUM:
                return Color.YELLOW;
            case LOW:
                return Color.GRAY;
            default:
                return Color.WHITE;
        }
    }
}
