# DonutSpawn

A Paper plugin for random spawn in donut-shaped zones with GriefPrevention awareness.

## Features

- **Donut-shaped spawn zones** - Define an inner (exclusion) radius and outer radius; players spawn randomly in the ring between them
- **Multiple spawn zones** - Configure as many zones as you want with weighted random selection
- **GriefPrevention integration** - Automatically avoids spawning players inside claims they don't own
- **Safety checks** - Finds solid ground, avoids hazards (lava, cacti, magma, etc.), respects Y limits
- **Biome filtering** - Optionally block spawning in specific biomes (e.g., oceans)
- **Uniform distribution** - Uses proper math to ensure even distribution across the donut area
- **End portal support** - Unbedded players returning from the End also get a random spawn

## How It Works

DonutSpawn triggers in two scenarios for unbedded players (no bed or respawn anchor):

1. **Death respawn** - When a player dies and respawns
2. **End portal return** - When a player exits the End via the end portal

In either case, DonutSpawn selects a random spawn zone (weighted if multiple exist), then finds a random point within that zone's "donut" - the area between the inner and outer radius. It then verifies the location is:

1. Not inside another player's GriefPrevention claim
2. On solid ground (not water, lava, or air)
3. Not in a blocked biome
4. Within configured Y limits
5. Free of hazards (lava, fire, cacti, etc.)

If a valid location can't be found after the configured number of attempts, the player spawns at the world's default spawn point.

## Installation

1. Place `DonutSpawn-x.x.x.jar` in your server's `plugins/` folder
2. Restart the server
3. Configure spawn zones in `plugins/DonutSpawn/config.yml` or use in-game commands

## Commands

All commands require the `donutspawn.admin` permission (default: op).

| Command | Description |
|---------|-------------|
| `/donutspawn reload` | Reload configuration from disk |
| `/donutspawn list` | List all configured spawn zones |
| `/donutspawn addzone <name> <inner> <outer> [weight]` | Create a new zone centered at your current location |
| `/donutspawn removezone <name>` | Delete a spawn zone |
| `/donutspawn test [player]` | Teleport yourself or another player to a random spawn location |
| `/donutspawn info` | Show plugin status and settings |

**Aliases:** `/ds`, `/dspawn`

### Examples

```
# Create a zone where players spawn 100-500 blocks from your location
/ds addzone main 100 500

# Create a secondary zone with lower weight (chosen less often)
/ds addzone secondary 50 300 0.5

# Test the spawn system on yourself
/ds test

# Test spawn another player
/ds test PlayerName

# Remove a zone
/ds removezone secondary
```

## Configuration

Default config is generated at `plugins/DonutSpawn/config.yml`:

```yaml
# Maximum attempts to find a valid spawn location before falling back to world spawn
max-attempts: 50

# Safety checks for spawn locations
safety:
  # Check for solid ground (not air, water, lava)
  require-solid-ground: true
  # Minimum Y level for spawns
  min-y: 63
  # Maximum Y level for spawns (255 = no limit, uses world surface)
  max-y: 255
  # Avoid spawning in these biomes (empty = allow all)
  blocked-biomes: []
  # Example: blocked-biomes: [OCEAN, DEEP_OCEAN, FROZEN_OCEAN]

# Spawn zones - players will randomly spawn in one of these areas
zones:
  main:
    world: world
    center-x: 0
    center-z: 0
    inner-radius: 100    # No spawns within 100 blocks of center
    outer-radius: 500    # No spawns beyond 500 blocks from center
    weight: 1.0          # Selection weight (higher = more likely)

  # Example second zone:
  # north_continent:
  #   world: world
  #   center-x: 5000
  #   center-z: -3000
  #   inner-radius: 50
  #   outer-radius: 400
  #   weight: 0.5

# Messages (supports color codes with &)
messages:
  spawned: "&aWelcome! You've spawned in a random location."
  fallback: "&eNo valid spawn found, using world spawn."
  # Set to empty string to disable:
  # spawned: ""
```

### Configuration Options

#### General
| Option | Description | Default |
|--------|-------------|---------|
| `max-attempts` | Number of attempts to find valid spawn before using world spawn | `50` |

#### Safety
| Option | Description | Default |
|--------|-------------|---------|
| `require-solid-ground` | Ensure spawn location has solid block below | `true` |
| `min-y` | Minimum Y coordinate for spawns | `63` |
| `max-y` | Maximum Y coordinate (255 = surface only) | `255` |
| `blocked-biomes` | List of biome names to avoid | `[]` |

#### Zones
| Option | Description |
|--------|-------------|
| `world` | World name this zone applies to |
| `center-x` | X coordinate of zone center |
| `center-z` | Z coordinate of zone center |
| `inner-radius` | Exclusion radius - no spawns within this distance |
| `outer-radius` | Maximum radius - no spawns beyond this distance |
| `weight` | Selection probability weight (higher = more likely) |

#### Messages
| Option | Description |
|--------|-------------|
| `spawned` | Message shown after random spawn (supports `&` color codes) |
| `fallback` | Message shown when falling back to world spawn |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `donutspawn.admin` | Access to all `/donutspawn` commands | `op` |
| `donutspawn.bypass` | Bypass random spawn (use normal respawn behavior) | `false` |

## Soft Dependencies

- **GriefPrevention** - If installed, DonutSpawn will avoid spawning players in claims owned by other players. Players can spawn in their own claims or claims where they have trust.

## Building from Source

Requires Java 21 and Gradle.

```bash
cd /path/to/donut-spawn
./gradlew build
```

The compiled JAR will be at `build/libs/DonutSpawn-x.x.x.jar`.

## Technical Notes

### Uniform Distribution

The plugin uses the formula `r = sqrt(random * (R2² - R1²) + R1²)` to ensure uniform distribution across the donut area. Without this, points would cluster near the inner radius.

### Hazard Avoidance

The following blocks are considered dangerous and avoided:
- Lava, Fire, Soul Fire
- Campfire, Soul Campfire
- Magma Block
- Cactus
- Sweet Berry Bush
- Wither Rose
- Powder Snow

### GriefPrevention Integration

- Admin claims are always avoided
- Player claims are avoided unless the spawning player owns or has trust in the claim
- If GriefPrevention is not installed, claim checking is simply skipped

## License

MIT
