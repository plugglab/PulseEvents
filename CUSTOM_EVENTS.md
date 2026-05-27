# Custom Events Reference

This file documents every custom-event option currently supported by PulseEvents `1.0.0`.

## Location

Custom events live as separate `.yml` files in the plugin `events/` folder.

## Minimal Example

```yml
event:
  key: my-event
  enabled: true
  name: "My Event"
  chance: 100
  duration: 20
  icon: NETHER_STAR
  min-players: 1
  allowed-worlds: []
  start-message: "&a%event% has started."
  end-message: "&7%event% has ended."
  bossbar-title: "&d%event%"
  actions:
    opening-title:
      type: title
      target: all-players
      title: "&d%event%"
      subtitle: "&7Good luck."
    first-wave:
      type: spawn-mob
      target: random-players
      target-count: 2
      chance: 75
      delay-seconds: 3
      entity: ZOMBIE
      amount: 2
```

## Top-Level Event Fields

- `enabled`
  - Type: `boolean`
  - Default: `true`
  - Disabled events are skipped on startup and reload.

- `name`
  - Type: `string`
  - Default: generated from the event key
  - Used in GUI, queue, broadcasts, and placeholders.

- `chance`
  - Type: `integer`
  - Default: `100`
  - Random selection weight.
  - `0` means the event stays queueable/manual only.

- `duration`
  - Type: `integer`
  - Required practical value: `> 0`
  - Used by the bossbar timer and automatic stop task.

- `icon`
  - Type: Bukkit `Material`
  - Example: `MAGMA_BLOCK`, `EMERALD`, `CHORUS_FRUIT`
  - Used in `/pe events`.

- `min-players`
  - Type: `integer`
  - Default: `1`
  - Counts only players eligible for that event.

- `allowed-worlds`
  - Type: `list of world names`
  - Default: `[]`
  - Empty means any world allowed by global `multiworld`.
  - Non-empty means players must satisfy both global and event-specific world rules.

- `start-message`
  - Type: `string`
  - Optional
  - Overrides the default event start broadcast.
  - Supports `%event%`.

- `end-message`
  - Type: `string`
  - Optional
  - Overrides the default event end broadcast.
  - Supports `%event%`.

- `bossbar-title`
  - Type: `string`
  - Optional
  - Overrides the default bossbar title for this event.
  - Supports `%event%`.

- `actions`
  - Type: configuration section
  - Required
  - Contains one or more named action entries.

## Common Action Fields

Every action supports these fields unless stated otherwise:

- `type`
  - Required
  - Supported values:
    - `message`
    - `sound`
    - `potion`
    - `teleport`
    - `spawn-mob`
    - `strike-lightning`
    - `spawn-tnt`
    - `velocity`
    - `ignite`
    - `economy-reward`
    - `title`
    - `console-command`
    - `player-command`

- `target`
  - Type: `string`
  - Default: `all-players`
  - Supported values:
    - `all-players`
    - `all`
    - `random-player`
    - `random-players`

- `target-count`
  - Type: `integer`
  - Default: `1`
  - Used with `random-players`.
  - Clamped to the number of eligible players online.

- `chance`
  - Type: `integer`
  - Default: `100`
  - Per-action chance gate from `0` to `100`.
  - If the roll fails, the action execution for that run is skipped.

- `delay-seconds`
  - Type: `integer`
  - Default: `0`
  - Delays the first run.

- `repeat-every-seconds`
  - Type: `integer`
  - Default: `0`
  - If greater than `0`, the action repeats.

- `repeat-times`
  - Type: `integer`
  - Default: `1`
  - Number of executions when repeating is enabled.

## Supported Action Types

### `message`

Sends chat text to the target players.

Fields:
- `message` (`string`)

Supports:
- `%event%`

```yml
warning:
  type: message
  target: all-players
  message: "&cThe sky is breaking apart."
```

### `sound`

Plays a Bukkit sound at each target player.

Fields:
- `sound` (`Sound`)
- `volume` (`double`, default `1.0`)
- `pitch` (`double`, default `1.0`)

```yml
alarm:
  type: sound
  target: all-players
  sound: ENTITY_ENDER_DRAGON_GROWL
  volume: 0.8
  pitch: 0.8
```

### `potion`

Applies a potion effect to each target player.

Fields:
- `effect` (`PotionEffectType`)
- `effect-duration-seconds` (`integer`, default `5`)
- `amplifier` (`integer`, default `0`)

```yml
speed-boost:
  type: potion
  target: random-player
  effect: SPEED
  effect-duration-seconds: 3
  amplifier: 1
```

### `teleport`

Teleports each target player by a random offset from their current location.

Fields:
- `random-offset.x.min` (`double`)
- `random-offset.x.max` (`double`)
- `random-offset.y.min` (`double`)
- `random-offset.y.max` (`double`)
- `random-offset.z.min` (`double`)
- `random-offset.z.max` (`double`)

```yml
blink:
  type: teleport
  target: all-players
  random-offset:
    x:
      min: -8.0
      max: 8.0
    y:
      min: 0.0
      max: 2.0
    z:
      min: -8.0
      max: 8.0
```

### `spawn-mob`

Spawns entities near each target player.

Fields:
- `entity` (`EntityType`, default `ZOMBIE`)
- `amount` (`integer`, default `1`)
- `offset.x` (`double`, default `0.0`)
- `offset.y` (`double`, default `0.0`)
- `offset.z` (`double`, default `0.0`)

```yml
swarm:
  type: spawn-mob
  target: random-players
  target-count: 2
  entity: ZOMBIE
  amount: 2
```

### `strike-lightning`

Strikes lightning at or near each target player.

Fields:
- `effect-only` (`boolean`, default `false`)
- `offset.x` (`double`, default `0.0`)
- `offset.y` (`double`, default `0.0`)
- `offset.z` (`double`, default `0.0`)

```yml
shock:
  type: strike-lightning
  target: random-player
  chance: 60
  effect-only: true
```

### `spawn-tnt`

Spawns primed TNT near each target player.

Fields:
- `amount` (`integer`, default `1`)
- `fuse-ticks` (`integer`, default `40`)
- `explosion-power` (`double`, default `4.0`)
- `offset.x` (`double`, default `0.0`)
- `offset.y` (`double`, default `0.0`)
- `offset.z` (`double`, default `0.0`)

```yml
impact:
  type: spawn-tnt
  target: all-players
  amount: 2
  fuse-ticks: 50
  explosion-power: 3.0
  offset:
    y: 18.0
```

### `velocity`

Sets player velocity directly.

Fields:
- `velocity.x` (`double`, default `0.0`)
- `velocity.y` (`double`, default `0.0`)
- `velocity.z` (`double`, default `0.0`)

```yml
pull:
  type: velocity
  target: all-players
  velocity:
    x: 0.0
    y: -0.2
    z: 0.0
```

### `ignite`

Sets players on fire.

Fields:
- `fire-ticks` (`integer`, default `60`)

```yml
burn:
  type: ignite
  target: random-players
  target-count: 2
  fire-ticks: 60
```

### `economy-reward`

Pays players using Vault economy.

Fields:
- `money.min` (`double`)
- `money.max` (`double`)

Notes:
- Requires Vault and a working economy provider.

```yml
payout:
  type: economy-reward
  target: all-players
  money:
    min: 5.0
    max: 15.0
```

### `title`

Shows a title and subtitle to the target players.

Fields:
- `title` (`string`)
- `subtitle` (`string`)
- `fade-in-ticks` (`integer`, default `10`)
- `stay-ticks` (`integer`, default `40`)
- `fade-out-ticks` (`integer`, default `10`)

Supports:
- `%event%`

```yml
opening-title:
  type: title
  target: all-players
  title: "&d%event%"
  subtitle: "&7Reality is unstable."
```

### `console-command`

Runs a command as console once per selected target player.

Fields:
- `command` (`string`)

Supports:
- `%event%`
- `%player%`
- `%world%`

```yml
bonus-command:
  type: console-command
  target: random-player
  command: "say %player% triggered %event% in %world%."
```

### `player-command`

Runs a command as the selected player.

Fields:
- `command` (`string`)

Supports:
- `%event%`
- `%player%`
- `%world%`

```yml
self-action:
  type: player-command
  target: random-player
  command: "me is trying to survive %event%."
```

## Validation Rules

Custom events are skipped if:

- `duration <= 0`
- `icon` is not a valid `Material`
- no `actions` section exists
- an action has no `type`
- `sound` uses an invalid `Sound`
- `potion` uses an invalid potion effect name
- `spawn-mob` uses an invalid `EntityType`
- `console-command` or `player-command` has an empty `command`
- `economy-reward` is configured without a Vault economy provider
- an action uses an unsupported `type`

## Runtime Notes

- Custom events are registered on startup and `/pe reload`.
- GUI chance editing works for custom events.
- Queueing works for custom events.
- GUI queue input supports:
  - middle click
  - `Q`
  - `Ctrl+Q`
- `min-players` counts only players eligible under both global and event-specific world rules.
- Action chance is rolled separately for each execution.
- `random-players` selects unique players up to `target-count`.

## Shipped Presets

- `meteor-shower`
- `gravity-well`
- `hot-potato`
- `jackpot-rush`
- `blink-surge`
