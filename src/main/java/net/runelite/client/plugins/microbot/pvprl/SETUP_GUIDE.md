# PVP RL Plugin - Setup & Testing Guide

## Quick Start (5 Minutes)

### Step 1: Start the Python API Server

```bash
# Navigate to pvp-ml directory
cd "/mnt/c/Users/Elite/Desktop/Microbot Practice/osrs-pvp-reinforcement-learning/pvp-ml"

# Activate environment and start API
./env/bin/serve-api --port 8888
```

You should see:
```
INFO:__main__:Starting agent server on 127.0.0.1:8888
INFO:__main__:Preloading 1 models on 1 workers
INFO:__main__:Preloaded 1 models on 1 workers
INFO:__main__:Serving agents on ('127.0.0.1', 8888): ['FineTunedNh']
```

### Step 2: Compile Microbot (if needed)

```bash
cd "/mnt/c/Users/Elite/Desktop/Coding/Microbot"

# Build the project (if you made any changes)
mvn clean install -DskipTests
```

### Step 3: Launch Microbot

1. Start Microbot/RuneLite
2. Navigate to plugins panel (wrench icon)
3. Search for "PVP RL"
4. Click the checkbox to enable

### Step 4: Configure Plugin

1. Click the gear icon next to "PVP RL" plugin
2. Verify settings:
   - **API Host**: 127.0.0.1
   - **API Port**: 8888
   - **Model Name**: FineTunedNh
   - **Enable Prayer Switching**: ✓
   - **Enable Eating**: ✓
   - **Enable Attacking**: ✓
   - **Emergency Logout HP**: 20
   - **Show Debug Overlay**: ✓

### Step 5: Test in LMS

1. **Enter LMS** (safest for testing)
2. **Wait for match start**
3. **Find an opponent**
4. **Open PVP RL panel** (microbot sidebar)
5. **Click "Start Bot"**

You should see:
- Overlay appears in top-left
- "Status: CONNECTED" in green
- AI starts making decisions!

---

## Detailed Testing Plan

### Phase 1: Connection Test (5 min)

**Goal**: Verify plugin connects to Python API

1. Start Python API server
2. Enable plugin in Microbot
3. Check overlay shows "API: CONNECTED"
4. Watch console for errors

**Expected Behavior**:
- Green "CONNECTED" status
- Latency < 100ms
- No errors in console

**Common Issues**:
- Red "DISCONNECTED": Check Python server is running
- High latency (>500ms): Close other programs
- Java errors: Check Microbot compilation

### Phase 2: State Extraction Test (10 min)

**Goal**: Verify game state is being read correctly

1. Enable "Log Observations" in config
2. Start bot
3. Check console for observation logs

**Expected Behavior**:
```
DEBUG: Game state: HP=99/99, Prayer=99/99, Target=none
DEBUG: Game state: HP=95/99, Prayer=87/99, Target=PlayerName
```

**What to Check**:
- HP values are correct
- Prayer values are correct
- Target detected when in combat
- Equipment state matches what you're wearing

### Phase 3: Action Execution Test (15 min)

**Goal**: Verify AI actions are being executed

1. Enable "Log Actions" in config
2. Enter LMS
3. Start bot when you have opponent
4. Watch overlay "Queue" section

**Expected Behavior**:
```
INFO: AI actions: [Prayer switch, Eat food, Attack]
INFO: Executed 3/3 actions this tick
```

**What to Check**:
- Prayers switch correctly
- Character eats food when low HP
- Character attacks opponent
- Actions appear in overlay queue

### Phase 4: Combat Test (30 min)

**Goal**: See AI in actual combat

**Setup**:
- LMS with basic NH gear
- Anglerfish (food)
- Saradomin brews
- Super restores
- Prayer potions

**Monitor**:
- Does AI switch prayers appropriately?
- Does AI eat when HP is low?
- Does AI attack consistently?
- Does AI survive or die quickly?

**Take Notes**:
- How many fights won/lost
- Common mistakes AI makes
- Any crashes or errors

---

## Common Issues & Solutions

### Issue: "Failed to connect to API"

**Symptoms**: Red DISCONNECTED, can't start bot

**Solutions**:
1. Check Python server is running:
   ```bash
   ps aux | grep serve-api
   ```

2. Check correct port:
   ```bash
   netstat -an | grep 8888
   ```

3. Try restarting API:
   ```bash
   # Kill old process
   pkill -f serve-api

   # Start fresh
   ./env/bin/serve-api --port 8888
   ```

### Issue: "Actions not executing"

**Symptoms**: Bot connects but doesn't do anything

**Solutions**:
1. Check you have a target:
   - Overlay should show "Target: PlayerName"
   - If "None", find an opponent

2. Check feature toggles:
   - "Enable Attacking" should be ON
   - "Enable Prayer Switching" should be ON

3. Check action queue:
   - Overlay should show "Queue: X actions"
   - If 0, AI might think no actions are needed

4. Enable logging:
   - Turn on "Log Actions"
   - Check console for errors

### Issue: "Bot dies immediately"

**Symptoms**: AI doesn't defend itself

**Solutions**:
1. Lower emergency logout threshold:
   - Set "Emergency Logout HP" to 40 or 50

2. Check prayer switching:
   - Watch overlay - prayers should change
   - If not, check prayer points

3. Check food:
   - Make sure you have food in inventory
   - AI should eat when HP < 60

4. Model might not be trained for this scenario:
   - Try different opponent
   - Use better gear
   - Consider fine-tuning model

### Issue: "High API latency (>500ms)"

**Symptoms**: Slow, laggy gameplay

**Solutions**:
1. Check Python server load:
   - Close other Python processes
   - Restart serve-api

2. Check system resources:
   - Close other heavy programs
   - Use GPU if available

3. Simplify observation:
   - Reduce frame stacking (already at 1)
   - Use smaller model if available

### Issue: "Queue keeps growing"

**Symptoms**: "Queue: 20+ actions", never executes all

**Solutions**:
1. Increase max actions per tick:
   - Set "Max Actions Per Tick" to 15-20

2. This is actually normal:
   - AI generates actions faster than execution
   - Priority system ensures critical actions happen

3. If queue > 50:
   - Something is wrong
   - Check action execution is working
   - Check for errors in executor

---

## Performance Benchmarks

### Expected Performance

| Metric | Good | Acceptable | Bad |
|--------|------|------------|-----|
| API Latency | <50ms | <200ms | >500ms |
| Actions/Tick | 8-12 | 5-8 | <5 |
| Queue Size | 0-10 | 10-20 | >30 |
| Ticks/Second | ~1.6 | ~1.2 | <1 |

### Logging Stats

Enable "Log Actions" and run for 100 ticks:

**Good Session**:
```
Requests: 95/100 successful (95% success rate)
Average latency: 45ms
Actions executed: 850 total
Queue depth: avg 5, max 15
```

**Bad Session**:
```
Requests: 60/100 successful (60% - API issues)
Average latency: 350ms
Actions executed: 400 total (too low)
Queue depth: avg 25, max 80 (too high)
```

---

## Debug Checklist

Before reporting issues, verify:

- [ ] Python API server is running
- [ ] Model file exists in models/ directory
- [ ] Plugin is enabled in Microbot
- [ ] Configuration is correct (host, port, model name)
- [ ] You're in a PvP area
- [ ] You have a combat target
- [ ] You have food/potions in inventory
- [ ] Prayers are unlocked
- [ ] No Java errors in console
- [ ] No Python errors in server console

---

## Next Steps After Testing

### If Everything Works:
1. Test in actual PvP scenarios (PvP worlds, wilderness)
2. Fine-tune configuration for your playstyle
3. Experiment with different models
4. Consider implementing Phase 2 features (gear sets, etc.)

### If Issues Persist:
1. Check logs thoroughly (Java + Python)
2. Try simplified config (disable features one by one)
3. Test with different model
4. Consider simulation vs reality differences

---

## Advanced Configuration

### Custom Gear Sets

To add custom gear switching, edit `ActionExecutor.java`:

```java
private boolean executeGear(PvpAction action) {
    if (action.getValue() == 1) {
        // Define your tank gear
        Rs2Equipment.wear("Bandos chestplate");
        Rs2Equipment.wear("Bandos tassets");
        Rs2Equipment.wear("Amulet of fury");
        return true;
    }
    return false;
}
```

### Custom Food Priority

To prioritize certain foods, edit `ActionExecutor.java`:

```java
private boolean executeFood(PvpAction action) {
    // Prioritize anglerfish over other food
    if (Rs2Inventory.hasItem("Anglerfish")) {
        return Rs2Inventory.interact("Anglerfish", "Eat");
    }
    // Fall back to other foods
    // ...
}
```

### Tick Rate Adjustment

For slower, more controlled gameplay:
- Set "Tick Delay Multiplier" to 2.0 (half speed)
- Good for debugging and watching AI decisions

---

## Support

If you encounter issues:
1. Check console logs (Java + Python)
2. Review this guide's troubleshooting section
3. Verify your setup matches the Quick Start
4. Test with minimal config (all features off except attacking)

**Remember**: The AI was trained in simulation. Real OSRS is more unpredictable. Performance may vary!

---

**Last Updated**: 2025-10-20
**Plugin Version**: 1.0.0
