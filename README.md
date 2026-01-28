# FF-Spawn

**Finite Frontier Spawn** - A Paper plugin for random spawn in donut-shaped zones with player-craftable spawn blocks and GriefPrevention awareness.

## Features

- **Donut-shaped spawn zones** - Define an inner (exclusion) radius and outer radius; players spawn randomly in the ring between them
- **Player-craftable spawn blocks** - Players can craft expensive spawn points that join the random spawn rotation
- **Multiple spawn zones** - Configure as many zones as you want with weighted random selection
- **GriefPrevention integration** - Automatically avoids spawning players inside claims they don't own (except at spawn blocks)
- **Safety checks** - Finds solid ground, avoids hazards (lava, cacti, magma, etc.), respects Y limits
- **Biome filtering** - Optionally block spawning in specific biomes (e.g., oceans)
- **Uniform distribution** - Uses proper math to ensure even distribution across donut areas
- **End portal support** - Unbedded players returning from the End also get a random spawn

## Spawn Blocks

Players can craft **Spawn Blocks** - expensive lodestone variants that register as spawn points. When a player respawns, they may randomly spawn at any registered spawn block instead of in a donut zone.

### Recipe

```
[Gold Block]  [Iron Block]  [Gold Block]
[Iron Block]  [Lodestone]   [Iron Block]
[Redstone Block] [Nether Star] [Redstone Block]
```

### How Spawn Blocks Work

- Place a spawn block anywhere - it faces the direction you were facing when placed
- The block joins the weighted random spawn selection alongside donut zones
- Players spawn 1 block above the spawn block, facing the block's direction
- Spawn blocks work even inside claims (players can spawn at spawn blocks on claimed land)
- Breaking a spawn block drops the custom item back
- Spawn blocks retain normal lodestone functionality (compass binding)

### Spawn Block Weight

Each spawn block has equal weight to each donut zone by default. With 1 zone and 3 spawn blocks at default weight (1.0 each), players have a 25% chance of spawning in the zone and 25% chance at each spawn block.

## How It Works

FF-Spawn triggers in two scenarios for unbedded players (no bed or respawn anchor):

1. **Death respawn** - When a player dies and respawns
2. **End portal return** - When a player exits the End via the end portal

The plugin builds a weighted list of all spawn points (donut zones + spawn blocks), picks one randomly, then finds a valid location. It verifies the location is:

1. Not inside another player's GriefPrevention claim (skipped for spawn blocks)
2. On solid ground (not water, lava, or air)
3. Not in a blocked biome
4. Within configured Y limits
5. Free of hazards (lava, fire, cacti, etc.)
6. Has 2 blocks of air above for the player

If a valid location can't be found after the configured number of attempts, the player spawns at the world's default spawn point.

## Installation

1. Place `ff-spawn-x.x.x.jar` in your server's `plugins/` folder
2. Restart the server
3. Configure spawn zones in `plugins/ff-spawn/config.yml` or use in-game commands

## Commands

All commands require the `ffspawn.admin` permission (default: op).

| Command | Description |
|---------|-------------|
| `/ffspawn reload` | Reload configuration from disk |
| `/ffspawn list` | List all configured spawn zones |
| `/ffspawn addzone <name> <inner> <outer> [weight]` | Create a new zone centered at your current location |
| `/ffspawn removezone <name>` | Delete a spawn zone |
| `/ffspawn test [player]` | Teleport yourself or another player to a random spawn location |
| `/ffspawn info` | Show plugin status and settings |
| `/ffspawn listblocks` | List all registered spawn blocks |
| `/ffspawn removeblock <x> <y> <z> [world]` | Admin-remove a spawn block |
| `/ffspawn blockinfo` | Show info about the spawn block you're looking at |
| `/ffspawn giveblock [player]` | Give a spawn block item to yourself or a player |

**Aliases:** `/ffs`, `/spawn`

### Examples

```
# Create a zone where players spawn 100-500 blocks from your location
/ffs addzone main 100 500

# Create a secondary zone with lower weight (chosen less often)
/ffs addzone secondary 50 300 0.5

# Test the spawn system on yourself
/ffs test

# List all spawn blocks on the server
/ffs listblocks

# Give yourself a spawn block
/ffs giveblock

# Remove a spawn block at specific coordinates
/ffs removeblock 100 64 200
```

## Configuration

Default config is generated at `plugins/ff-spawn/config.yml`:

```yaml
# Maximum attempts to find a valid spawn location before falling back to world spawn
max-attempts: 50

# Safety checks for spawn locations
safety:
  require-solid-ground: true
  min-y: 63
  max-y: 255
  blocked-biomes: []

# Spawn zones - players will randomly spawn in one of these areas
zones:
  main:
    world: world
    center-x: 0
    center-z: 0
    inner-radius: 100
    outer-radius: 500
    weight: 1.0

# Spawn Blocks - player-placed spawn points
spawn-blocks:
  enabled: true
  weight: 1.0  # Weight per spawn block in random selection
  recipe:
    enabled: true
    shape:
      - "GIG"
      - "ILI"
      - "RNR"
    ingredients:
      G: GOLD_BLOCK
      I: IRON_BLOCK
      L: LODESTONE
      N: NETHER_STAR
      R: REDSTONE_BLOCK

# Messages (supports color codes with &)
messages:
  spawned: "&aWelcome! You've spawned in a random location."
  fallback: "&eNo valid spawn found, using world spawn."
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `ffspawn.admin` | Access to all `/ffspawn` commands | `op` |
| `ffspawn.bypass` | Bypass random spawn (use normal respawn behavior) | `false` |
| `ffspawn.place` | Allows placing spawn blocks | `true` |

## Soft Dependencies

- **GriefPrevention** - If installed, FF-Spawn will avoid spawning players in claims owned by other players. Players can spawn in their own claims or claims where they have trust. Spawn blocks bypass claim checks entirely.

## Building from Source

Requires Java 21 and Gradle.

```bash
cd /path/to/ff-spawn
./gradlew build
```

The compiled JAR will be at `build/libs/ff-spawn-x.x.x.jar`.

## Technical Notes

### Uniform Distribution

The plugin uses the formula `r = sqrt(random * (R2² - R1²) + R1²)` to ensure uniform distribution across the donut area.

### Hazard Avoidance

The following blocks are considered dangerous and avoided:
- Lava, Fire, Soul Fire
- Campfire, Soul Campfire
- Magma Block, Cactus
- Sweet Berry Bush, Wither Rose
- Powder Snow

### Spawn Block Data

Spawn blocks are stored in `plugins/ff-spawn/spawn-blocks.yml` and tracked by:
- Location (world, x, y, z)
- Facing direction (N/E/S/W)
- Owner UUID and name
- Placement timestamp

Spawn blocks use CustomModelData 1002 for resource pack textures.

## License

MIT
