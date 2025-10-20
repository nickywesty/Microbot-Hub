# PVP RL Plugin for Microbot

AI-powered PvP bot using reinforcement learning from trained models. Integrates the Python-based PVP RL training system with Microbot's RuneLite client.

## Overview

This plugin connects your Microbot client to a Python serve-api server that hosts trained reinforcement learning models. The AI observes game state, predicts optimal actions, and executes them via Microbot APIs.

### Key Features

- **AI-Powered Combat**: Uses trained RL models (FineTunedNh, GeneralizedNh, etc.)
- **Priority-Based Action Queue**: Handles 12+ actions per game tick via intelligent queueing
- **Full Action Set**: Prayer switching, gear switches, eating, attacking, movement
- **Real-time Debug Overlay**: See AI decisions, queue status, and performance metrics
- **Safety Features**: Emergency logout, PvP area restrictions, configurable limits

## Architecture

```
┌─────────────────────────────────────────┐
│  OSRS Client (Microbot)                 │
│  ┌───────────────────────────────────┐  │
│  │  PvpRLPlugin                      │  │
│  │  ├─ Extract game state            │  │
│  │  ├─ Build observation vector      │  │
│  │  ├─ Query AI via socket           │  │
│  │  ├─ Parse action vector           │  │
│  │  ├─ Queue & execute actions       │  │
│  │  └─ Repeat every game tick        │  │
│  └───────────────────────────────────┘  │
└──────────────┬──────────────────────────┘
               │ TCP Socket (127.0.0.1:8888)
    ┌──────────▼──────────┐
    │  Python serve-api   │
    │  - Loads RL model   │
    │  - Predicts actions │
    └─────────────────────┘
```

## Installation

### Prerequisites

1. **Microbot** - Latest version
2. **Python Environment** - From osrs-pvp-reinforcement-learning project
3. **Trained Model** - FineTunedNh.zip in models/ directory

### Setup

1. **Start Python API Server**:
   ```bash
   cd /path/to/osrs-pvp-reinforcement-learning/pvp-ml
   ./env/bin/serve-api --port 8888
   ```

2. **Enable Plugin in Microbot**:
   - Open Microbot/RuneLite
   - Navigate to Plugin Hub
   - Search for "PVP RL"
   - Enable the plugin

3. **Configure Plugin**:
   - Open plugin config panel
   - Set API Host: `127.0.0.1`
   - Set API Port: `8888`
   - Set Model Name: `FineTunedNh`
   - Configure feature toggles as needed

4. **Start Bot**:
   - Enter a PvP area (LMS, PvP world, etc.)
   - Find a target
   - Click "Start" in plugin panel
   - AI takes over!

## Configuration

### API Connection
- **API Host**: Python server host (default: 127.0.0.1)
- **API Port**: Python server port (default: 8888)
- **Model Name**: Model to use (FineTunedNh, GeneralizedNh, etc.)
- **Connection Timeout**: Timeout for initial connection (default: 5000ms)
- **Request Timeout**: Timeout for predictions (default: 2000ms)

### Feature Toggles
- **Enable Prayer Switching**: Allow AI to switch prayers
- **Enable Gear Switching**: Allow AI to change equipment
- **Enable Movement**: Allow AI to move player (disabled by default)
- **Enable Attacking**: Allow AI to attack targets
- **Enable Eating**: Allow AI to eat/drink
- **Enable Special Attack**: Allow AI to use special attacks

### Safety Settings
- **Emergency Logout HP**: Auto-logout if HP drops below threshold (default: 20)
- **Only in PvP Areas**: Only run in designated PvP zones (default: true)
- **Require Target**: Only run when target is present (default: true)
- **Max Actions Per Tick**: Budget for action execution (default: 12)

### Debug Options
- **Show Debug Overlay**: Display stats overlay (default: true)
- **Log Actions**: Log all executed actions to console
- **Log Observations**: Log observation vectors to console
- **Tick Delay Multiplier**: Slow down execution (1.0 = normal speed)
- **Deterministic Actions**: Use deterministic sampling (default: true)

## How It Works

### Game Loop (Every 600ms)

1. **Extract State** - Read game state from Microbot APIs:
   - Player HP, prayer, special attack
   - Equipment worn
   - Inventory items (food, potions)
   - Active prayers
   - Combat target and distance
   - Timers (freeze, attack cooldown, etc.)

2. **Build Observation** - Map to 130-value float array:
   - Normalized stats (0-1 range)
   - Binary flags (prayers, equipment)
   - Combat metrics

3. **Generate Action Masks** - Determine valid actions:
   - Can't attack without target
   - Can't eat without food
   - Can't use special if energy < 50%

4. **Query AI** - Send to Python API:
   ```json
   {
     "model": "FineTunedNh",
     "obs": [[...130 floats...]],
     "actionMasks": [[bools], [bools], ...],
     "deterministic": true
   }
   ```

5. **Receive Action** - AI returns 12-value action vector:
   ```
   [attack_style, melee_type, ranged_type, mage_spell,
    potion, food, karambwan, veng, gear, movement,
    farcast_dist, prayer]
   ```

6. **Parse Actions** - Convert to PvpAction objects with priorities:
   - CRITICAL: Prayer switches, emergency eating
   - HIGH: Offensive prayers, special attacks
   - MEDIUM: Gear switches, attacks
   - LOW: Movement

7. **Queue & Execute** - Add to priority queue, execute up to 12:
   - Execute CRITICAL first
   - Then HIGH, MEDIUM, LOW
   - Remaining actions queued for next tick

### Action Priority System

The core challenge is Microbot can't send packets, so we can't execute 12 actions instantly. The solution:

**Priority Queue + Batching + Merging**

Example scenario - AI wants to do simultaneously:
- Switch to Protect from Melee (CRITICAL)
- Eat food (CRITICAL)
- Switch to melee gear (MEDIUM)
- Attack with special (HIGH)

Execution:
- **Tick 1**: Prayer switch + Eat (2 actions)
- **Tick 2**: Start gear switches (assume 3 items)
- **Tick 3**: Complete gear + Special attack

This ensures critical actions (staying alive) happen first, while less important actions are delayed or queued.

## Components

### Core Classes

- **PvpRLPlugin** - Main plugin entry point
- **PvpRLScript** - Main game loop (extends Script)
- **PvpRLConfig** - Configuration interface
- **PvpRLOverlay** - Debug UI overlay

### API Layer
- **RLApiClient** - Socket client to Python server

### State Management
- **GameState** - Current game state data model
- **GameStateExtractor** - Extracts state from Microbot APIs
- **ObservationBuilder** - Builds observation vector
- **ActionMaskGenerator** - Generates valid action masks

### Action System
- **PvpAction** - Single action to execute
- **ActionType** - Enum of 12 action types
- **ActionPriority** - Priority levels (CRITICAL → LOW)
- **ActionParser** - Parses AI action vector
- **ActionQueue** - Priority-based queue
- **ActionExecutor** - Executes actions via Microbot APIs

## Troubleshooting

### Connection Failed
**Problem**: Can't connect to API server

**Solutions**:
- Check Python server is running: `./env/bin/serve-api --port 8888`
- Verify port in config matches server port
- Check firewall isn't blocking localhost:8888
- Look for errors in Python server console

### Actions Not Executing
**Problem**: Bot connects but doesn't do anything

**Solutions**:
- Verify you're in a PvP area
- Check you have a target (if "Require Target" is enabled)
- Enable "Log Actions" in config to see what's happening
- Check overlay - queue should show pending actions
- Review RuneLite console for Java errors

### Poor Performance
**Problem**: Laggy, slow predictions

**Solutions**:
- Check API latency in overlay (should be <100ms)
- Ensure Python server isn't under heavy load
- Consider using GPU-accelerated PyTorch
- Reduce observation complexity if modified

### Bot Dies Too Often
**Problem**: AI makes poor survival decisions

**Solutions**:
- Lower "Emergency Logout HP" threshold
- Disable risky features (movement, spec attacks)
- Use a different model (GeneralizedNh vs FineTunedNh)
- The model was trained in simulation - real game is harder!

## Limitations

1. **Simulation vs Reality**: Model trained in simulation may not transfer perfectly to live game
2. **No Enemy HP**: Can't always see opponent's exact HP in real OSRS (unlike simulation)
3. **Latency**: Real game has network latency that simulation doesn't
4. **Limited Observations**: Some advanced metrics (PID, pending damage) are unknown
5. **Gear Switching**: Simplified implementation - full gear sets need manual configuration

## Future Improvements

### Phase 2 Enhancements
- [ ] Full gear set management (loadout presets)
- [ ] Advanced timer tracking (freeze immunity, veng cooldown via varbits)
- [ ] Enemy HP estimation (overhead prayer reading)
- [ ] PID detection
- [ ] Pending damage calculation
- [ ] Attack confidence scoring

### Phase 3 Features
- [ ] Multi-target support
- [ ] Smarter movement (pathing, safespotting)
- [ ] Combo food detection
- [ ] Auto-resupply from bank
- [ ] Statistics tracking (kills, deaths, damage dealt)
- [ ] Model fine-tuning from real game data

## Development Notes

### File Structure
```
pvprl/
├── PvpRLPlugin.java          # Main plugin
├── PvpRLConfig.java          # Configuration
├── PvpRLScript.java          # Game loop
├── PvpRLOverlay.java         # Debug UI
├── api/
│   └── RLApiClient.java      # Socket client
├── state/
│   ├── GameStateExtractor.java
│   ├── ObservationBuilder.java
│   └── ActionMaskGenerator.java
├── action/
│   ├── ActionExecutor.java
│   ├── ActionQueue.java
│   └── ActionParser.java
└── model/
    ├── GameState.java
    ├── PvpAction.java
    ├── ActionType.java
    └── ActionPriority.java
```

### Testing

1. **API Connection Test**:
   ```bash
   # Terminal 1: Start API
   cd pvp-ml && ./env/bin/serve-api --port 8888

   # Terminal 2: Test with Python
   cd pvp-ml && ./env/bin/python test_api_visual.py
   ```

2. **In-Game Test**:
   - LMS (safest for testing)
   - PvP worlds with cheap gear
   - Duel Arena (for controlled tests)

### Debugging

Enable verbose logging:
- Set "Log Actions" = true
- Set "Log Observations" = true
- Check RuneLite console (View → Show Console)
- Monitor Python server console for API errors

## Credits

- **Original PVP RL Project**: osrs-pvp-reinforcement-learning
- **Microbot Framework**: Microbot RuneLite fork
- **Integration**: Claude + Elite

## License

Follows the license of the parent Microbot project.

---

**Version**: 1.0.0
**Created**: 2025-10-20
**Author**: Claude/Elite
