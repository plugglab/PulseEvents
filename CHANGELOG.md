# Changelog

## Latest Changes

### Added

- Survival streak rewards for players who finish events alive.
- Configurable streak reward milestones under `streak-rewards.milestones`.
- Support for streak reward modes:
  - `vault`
  - `item`
  - `custom-item`
  - `command`
- Player streak reset when they die or leave during an active event.
- New listener for tracking player survival state during events.
- File-based custom event loading from the plugin `events/` folder.
- Example custom event files:
  - `meteor-shower.yml`
  - `gravity-well.yml`
  - `hot-potato.yml`
- Saving custom event chance changes back into each event file.
- Player voting GUI for upcoming events.
- Vote-based event priority when the next random event is selected.
- Configurable vote cost under `voting.cost`.
- Automatic free voting fallback when Vault is unavailable.
- New language messages for streak rewards and voting feedback.

### Changed

- Removed embedded custom events from the main `config.yml`.
- Moved custom event configuration into separate `.yml` files inside `events/`.
- Updated `/pe events` behavior:
  - admins open the event management GUI
  - regular players open the voting GUI
- Changed streak reward config format from plain numeric values to structured entries with `mode` and `value`.
- Updated custom event documentation to reflect the new file-based format.

### Notes

- Voting resets when an event starts.
- The first vote in a round costs money only if Vault economy is available.
- Tied top-voted events are resolved through the existing weighted random selection.
- you need to delete old folder called pulseevents because of new place for events and changed config.yml
