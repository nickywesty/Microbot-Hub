package net.runelite.client.plugins.microbot.pvprl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.pvprl.action.ActionExecutor;
import net.runelite.client.plugins.microbot.pvprl.action.ActionParser;
import net.runelite.client.plugins.microbot.pvprl.action.ActionQueue;
import net.runelite.client.plugins.microbot.pvprl.api.RLApiClient;
import net.runelite.client.plugins.microbot.pvprl.model.GameState;
import net.runelite.client.plugins.microbot.pvprl.model.PvpAction;
import net.runelite.client.plugins.microbot.pvprl.state.ActionMaskGenerator;
import net.runelite.client.plugins.microbot.pvprl.state.GameStateExtractor;
import net.runelite.client.plugins.microbot.pvprl.state.LmsGearDetector;
import net.runelite.client.plugins.microbot.pvprl.state.ObservationBuilder;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Main script loop for PVP RL bot
 * Orchestrates the game loop: extract state → query AI → execute actions
 */
@Slf4j
public class PvpRLScript extends Script {
    public static final String VERSION = "1.0.0";

    private final PvpRLConfig config;

    // Components
    private RLApiClient apiClient;
    private GameStateExtractor stateExtractor;
    private ObservationBuilder obsBuilder;
    private ActionMaskGenerator maskGenerator;
    private ActionParser actionParser;
    private ActionExecutor actionExecutor;
    private ActionQueue actionQueue;

    // State
    @Getter
    private GameState currentState;
    @Getter
    private boolean connected = false;
    @Getter
    private long ticksRun = 0;
    @Getter
    private String errorMessage = null;
    @Getter
    private long lastPredictionMs = 0;

    public PvpRLScript(PvpRLConfig config) {
        this.config = config;
    }

    public boolean run() {
        // Call parent run for standard checks
        if (!super.run()) {
            return false;
        }

        try {
            // Initialize on first run
            if (!connected) {
                if (!initialize()) {
                    errorMessage = "Failed to connect to API";
                    return false;
                }
            }

            // Safety check: logout if HP too low
            if (config.emergencyLogoutHP() > 0) {
                int currentHP = Rs2Player.getBoostedSkillLevel(net.runelite.api.Skill.HITPOINTS);
                if (currentHP < config.emergencyLogoutHP()) {
                    log.warn("HP below emergency threshold ({}), logging out", config.emergencyLogoutHP());
                    Rs2Player.logout();
                    shutdown();
                    return false;
                }
            }

            // Main game loop - runs once per game tick
            gameLoop();

            // Apply tick delay multiplier
            // LIGHTNING FAST: 50ms for ultra-responsive PVP combat
            int baseSleepMs = 50; // Lightning fast for NH tribridding (was 100ms)
            int sleepMs = (int) (baseSleepMs * config.tickDelay());
            sleep(sleepMs);

            ticksRun++;
            return true;

        } catch (Exception e) {
            log.error("Error in PvpRLScript main loop", e);
            errorMessage = e.getMessage();
            return false;
        }
    }

    private boolean initialize() {
        log.info("Initializing PVP RL Script v{}", VERSION);

        try {
            // Detect LMS gear on startup
            LmsGearDetector.LmsGearInfo gearInfo = LmsGearDetector.detectGear();
            LmsGearDetector.printGearReport(gearInfo);
            log.info("Config Suggestion: {}", LmsGearDetector.getConfigSuggestion(gearInfo));

            // Warn if not tribrid (AI was likely trained on tribrid)
            if (!gearInfo.isTribrid()) {
                log.warn("═══════════════════════════════════════════════════════");
                log.warn("WARNING: You don't have tribrid gear!");
                log.warn("The AI model was trained with all 3 combat styles.");
                log.warn("Performance may be reduced with limited gear.");
                log.warn("For best results in LMS, select a tribrid loadout.");
                log.warn("═══════════════════════════════════════════════════════");
            }

            // Initialize components
            stateExtractor = new GameStateExtractor();
            obsBuilder = new ObservationBuilder();
            maskGenerator = new ActionMaskGenerator();
            actionParser = new ActionParser();
            actionExecutor = new ActionExecutor(config);
            actionQueue = new ActionQueue(actionExecutor);

            // Connect to API
            apiClient = new RLApiClient(
                config.apiHost(),
                config.apiPort(),
                config.connectionTimeout(),
                config.requestTimeout()
            );

            if (!apiClient.connect()) {
                log.error("Failed to connect to API at {}:{}", config.apiHost(), config.apiPort());
                return false;
            }

            connected = true;
            log.info("Successfully initialized and connected to API");
            return true;

        } catch (Exception e) {
            log.error("Error during initialization", e);
            return false;
        }
    }

    private void gameLoop() {
        // 1. Extract game state
        currentState = stateExtractor.extract();

        // Log state if enabled
        if (config.logObservations()) {
            log.debug("Game state: HP={}/{}, Prayer={}/{}, Target={}",
                currentState.getHealth(),
                currentState.getMaxHealth(),
                currentState.getPrayer(),
                currentState.getMaxPrayer(),
                currentState.getTarget() != null ? currentState.getTarget().getName() : "none"
            );
        }

        // Safety: Require target if configured
        if (config.requireTarget() && currentState.getTarget() == null) {
            log.debug("No target - skipping tick");
            return;
        }

        // 2. Build observation vector
        float[][] observation = obsBuilder.buildFrameStacked(currentState, 1);
        boolean[][] actionMasks = maskGenerator.generate(currentState);

        // 3. Query AI for action
        long startTime = System.currentTimeMillis();
        int[] actionVector = apiClient.predict(
            config.modelName(),
            observation,
            actionMasks,
            config.deterministicActions()
        );
        lastPredictionMs = System.currentTimeMillis() - startTime;

        if (actionVector == null) {
            log.warn("Failed to get prediction from API");
            errorMessage = "API prediction failed";
            return;
        }

        // 4. Parse action vector into PvpActions
        List<PvpAction> actions = actionParser.parse(actionVector);

        if (config.logActions()) {
            log.info("AI actions: {}", actions);
        }

        // 5. Update executor state
        actionExecutor.updateState(currentState);

        // 6. Enqueue actions
        actionQueue.enqueueAll(actions);

        // 7. Execute up to budget
        actionQueue.resetTickCounter();
        int executed = actionQueue.executeUpToBudget(config.maxActionsPerTick());

        if (config.logActions()) {
            log.info("Executed {}/{} actions this tick", executed, actions.size());
        }
    }

    /**
     * Start the script with a scheduled executor
     */
    public void start() {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!this.run()) {
                    log.warn("Script run() returned false, shutting down...");
                    this.shutdown();
                }
            } catch (Exception e) {
                log.error("Error in PvpRLScript execution", e);
                this.shutdown();
            }
        }, 0, 600, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        log.info("Shutting down PVP RL Script");

        if (apiClient != null) {
            apiClient.shutdown();
        }

        if (actionQueue != null) {
            actionQueue.clear();
        }

        connected = false;
        errorMessage = null;

        super.shutdown();
    }

    // Getters for overlay
    public ActionQueue getActionQueue() {
        return actionQueue;
    }

    public boolean isApiConnected() {
        return apiClient != null && apiClient.isConnected();
    }

    public long getApiLatency() {
        return apiClient != null ? apiClient.getLastRequestTime() : 0;
    }

    public int getSuccessfulRequests() {
        return apiClient != null ? apiClient.getSuccessfulRequests() : 0;
    }

    public int getFailedRequests() {
        return apiClient != null ? apiClient.getFailedRequests() : 0;
    }
}
