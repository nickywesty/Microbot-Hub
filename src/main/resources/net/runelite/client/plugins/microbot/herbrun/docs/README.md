# Auto Bank Stander Plugin

The **Auto Bank Stander Plugin** is an all-in-one automation tool for Old School RuneScape, designed to efficiently perform a wide variety of bank-standing skilling and processing activities. Built for the Microbot RuneLite client, this plugin automates repetitive tasks such as alching, herblore, fletching, and more, allowing for hands-free and optimized experience gains while standing at a bank.

---

## Features

- **Automated Skilling and Processing:**  
  Supports a range of bank-standing activities, including:
    - Magic (e.g., alching, enchanting)
    - Herblore (cleaning herbs, making unfinished/finished potions)
    - Fletching (stringing bows, making darts/bolts/arrows)
    - Bolt enchanting and other processing tasks

- **Configurable Skill Modes:**  
  Easily select your desired skill and processing method through the plugin panel or configuration.

- **Inventory and Resource Management:**  
  Automatically withdraws required items from the bank, manages inventory space, and ensures you always have the necessary supplies.

- **Overlay and Sidebar Panel:**  
  Provides a user-friendly sidebar panel for starting/stopping scripts, selecting modes, and monitoring status.

- **Failsafes and Error Handling:**  
  Handles running out of supplies, full inventory, or unexpected in-game events.

- **Extensible and Modular:**  
  Designed with modular actions and configuration, making it easy to add support for new bank-standing activities.

---

## How It Works

1. **Configuration:**  
   Select your desired skill and processing method in the plugin panel or configuration.

2. **Startup:**  
   The plugin prepares the required configuration and waits for you to start the script from the sidebar panel.

3. **Automation Loop:**  
   The script performs the selected activity, manages inventory, interacts with the bank, and repeats the process for continuous training or processing.

4. **Overlay/Panel:**  
   Displays real-time information such as:
    - Current skill and method
    - Status (Running/Stopped)
    - Actions performed

5. **Failsafes:**  
   Pauses or stops if requirements are not met, or if unexpected events occur.

---

## Configuration

The plugin provides a configuration panel where you can:

- Select the skill to train (Magic, Herblore, Fletching, etc.)
- Choose the specific method or item to process
- Enable or disable overlay features
- Adjust advanced options (e.g., use of Amulet of Chemistry, bolt type, potion type)

---

## Requirements

- Microbot RuneLite client
- Sufficient skill level for the selected activity
- Required items in the bank/inventory

---

## Usage

1. **Enable the Plugin:**  
   Open the Microbot sidebar, find the Auto Bank Stander Plugin, and enable it.

2. **Configure Settings:**  
   Select your desired skill and processing method.

3. **Start the Script:**  
   Use the sidebar panel to start the automation.

4. **Monitor Progress:**  
   Watch the panel for real-time updates on status and actions.

5. **Stop at Any Time:**  
   Use the panel to halt the automation.

---

## Limitations

- Only supports activities and items defined in the script logic.
- Requires the player to have the necessary items and skill levels.
- May not handle all random events or interruptions (e.g., player death, aggressive NPCs).

---

## Source Files

- `AutoBankStanderPlugin.java` – Main plugin class, manages lifecycle and integration.
- `AutoBankStanderScript.java` – Core automation logic for bank-standing activities.
- `AutoBankStanderConfig.java` – User configuration options.
- `AutoBankStanderPanel.java` – Sidebar panel for user interaction.
- `config/ConfigData.java` – Configuration data structure.

---

**Automate your bank-standing skilling and processing with the Auto Bank Stander Plugin for maximum efficiency!**