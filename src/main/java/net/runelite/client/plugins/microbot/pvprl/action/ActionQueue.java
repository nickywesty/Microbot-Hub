package net.runelite.client.plugins.microbot.pvprl.action;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pvprl.model.ActionPriority;
import net.runelite.client.plugins.microbot.pvprl.model.PvpAction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Priority-based action queue
 * Handles the challenge of executing >12 actions per game tick
 * by prioritizing critical actions and batching/queueing others
 */
@Slf4j
public class ActionQueue {
    // Separate queues for each priority level
    private final PriorityQueue<PvpAction> criticalQueue = new PriorityQueue<>(Comparator.comparing(PvpAction::getPriority));
    private final PriorityQueue<PvpAction> highQueue = new PriorityQueue<>(Comparator.comparing(PvpAction::getPriority));
    private final PriorityQueue<PvpAction> mediumQueue = new PriorityQueue<>(Comparator.comparing(PvpAction::getPriority));
    private final PriorityQueue<PvpAction> lowQueue = new PriorityQueue<>(Comparator.comparing(PvpAction::getPriority));

    private final ActionExecutor executor;
    private int actionsExecutedThisTick = 0;
    private int totalActionsExecuted = 0;
    private int totalActionsQueued = 0;
    private int totalActionsDropped = 0;

    public ActionQueue(ActionExecutor executor) {
        this.executor = executor;
    }

    /**
     * Add an action to the queue
     */
    public void enqueue(PvpAction action) {
        if (action == null) return;

        switch (action.getPriority()) {
            case CRITICAL:
                criticalQueue.offer(action);
                break;
            case HIGH:
                highQueue.offer(action);
                break;
            case MEDIUM:
                mediumQueue.offer(action);
                break;
            case LOW:
                lowQueue.offer(action);
                break;
        }

        totalActionsQueued++;
    }

    /**
     * Add multiple actions to the queue
     */
    public void enqueueAll(List<PvpAction> actions) {
        if (actions == null) return;
        actions.forEach(this::enqueue);
    }

    /**
     * Execute actions up to the specified budget
     * Prioritizes CRITICAL > HIGH > MEDIUM > LOW
     *
     * @param maxActions Maximum number of actions to execute
     * @return Number of actions executed
     */
    public int executeUpToBudget(int maxActions) {
        actionsExecutedThisTick = 0;
        List<PvpAction> toExecute = new ArrayList<>();

        // Collect actions up to budget, prioritizing by level
        collectActions(criticalQueue, toExecute, maxActions);
        collectActions(highQueue, toExecute, maxActions);
        collectActions(mediumQueue, toExecute, maxActions);
        collectActions(lowQueue, toExecute, maxActions);

        // Execute collected actions
        for (PvpAction action : toExecute) {
            boolean success = executor.execute(action);
            if (success) {
                actionsExecutedThisTick++;
                totalActionsExecuted++;
                log.debug("Executed action: {}", action);
            } else {
                log.warn("Failed to execute action: {}", action);
            }
        }

        return actionsExecutedThisTick;
    }

    /**
     * Collect actions from a queue up to the remaining budget
     */
    private void collectActions(PriorityQueue<PvpAction> queue, List<PvpAction> target, int maxActions) {
        while (!queue.isEmpty() && target.size() < maxActions) {
            target.add(queue.poll());
        }
    }

    /**
     * Clear all queues
     */
    public void clear() {
        int dropped = size();
        criticalQueue.clear();
        highQueue.clear();
        mediumQueue.clear();
        lowQueue.clear();
        totalActionsDropped += dropped;
        log.debug("Cleared action queue, dropped {} actions", dropped);
    }

    /**
     * Get total number of queued actions
     */
    public int size() {
        return criticalQueue.size() + highQueue.size() + mediumQueue.size() + lowQueue.size();
    }

    /**
     * Get number of actions executed this tick
     */
    public int getActionsExecutedThisTick() {
        return actionsExecutedThisTick;
    }

    /**
     * Get total actions executed since start
     */
    public int getTotalActionsExecuted() {
        return totalActionsExecuted;
    }

    /**
     * Get total actions queued since start
     */
    public int getTotalActionsQueued() {
        return totalActionsQueued;
    }

    /**
     * Get total actions dropped (cleared without executing)
     */
    public int getTotalActionsDropped() {
        return totalActionsDropped;
    }

    /**
     * Get queue status by priority
     */
    public Map<ActionPriority, Integer> getQueueSizes() {
        Map<ActionPriority, Integer> sizes = new HashMap<>();
        sizes.put(ActionPriority.CRITICAL, criticalQueue.size());
        sizes.put(ActionPriority.HIGH, highQueue.size());
        sizes.put(ActionPriority.MEDIUM, mediumQueue.size());
        sizes.put(ActionPriority.LOW, lowQueue.size());
        return sizes;
    }

    /**
     * Get snapshot of queued actions for debug overlay
     */
    public List<PvpAction> getQueueSnapshot() {
        List<PvpAction> snapshot = new ArrayList<>();
        snapshot.addAll(criticalQueue);
        snapshot.addAll(highQueue);
        snapshot.addAll(mediumQueue);
        snapshot.addAll(lowQueue);
        return snapshot;
    }

    /**
     * Reset tick counter (call at start of each game tick)
     */
    public void resetTickCounter() {
        actionsExecutedThisTick = 0;
    }

    /**
     * Check if queue is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Get statistics summary
     */
    public String getStatsSummary() {
        return String.format("Queue: %d actions (C:%d H:%d M:%d L:%d) | Executed: %d | Dropped: %d",
            size(),
            criticalQueue.size(),
            highQueue.size(),
            mediumQueue.size(),
            lowQueue.size(),
            totalActionsExecuted,
            totalActionsDropped
        );
    }
}
