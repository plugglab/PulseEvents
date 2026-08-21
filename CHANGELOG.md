# Changelog

## 3.3

### Changed

- Ported to Minecraft 26.x: now targets the Paper API (26.2) instead of Spigot API, since Spigot no longer publishes mappings for the 26.x line. Requires Java 25 and a Paper 26.2+ server.
- Action bar messages now use Paper's native Adventure API instead of the deprecated Bungee `ChatMessageType` path.

### Added

- Seasonal/weekend Pulse Points multiplier: configure `progression.multiplier` to run 2x (or any multiplier) points on chosen days of the week, with an in-game announcement when it's active.
- Per-event cooldowns: `event-cooldowns.*` lets you set a minimum time between runs of a specific event (or a global default), separate from the `/pe start` command cooldown.
- Smarter random event weighting: `weighting.recency-bias` biases event selection away from whatever ran most recently, decaying back to normal chance over a configurable window (enabled by default).

## 3.2

### Added

- Persistent Pulse Points and level progression system, saved to `playerdata.yml` and awarded for participating in, surviving, and voting on events, plus streak-milestone and combo bonuses.
- Level-up rewards using the same vault/item/command modes as streak rewards.
- `/pe profile [player]` command showing points, level, next level threshold, and current streak.
- `/pe leaderboard points` mode alongside the existing streak leaderboard.
- Event combos: two events can now randomly run at the same time, configurable via `combos.*` (disabled by default).
- Action bar countdown alongside the boss bar, toggleable per player with `/pe actionbar`.
- New placeholders: `%pulseevents_points%`, `%pulseevents_level%`, `%pulseevents_points_next_level%`, `%pulseevents_points_to_next_level%`.

## 3.1

### Fixed

- Voting GUI clicks now always select the displayed votable event, even when some events have voting disabled.
- Clicking the voting summary item no longer casts a vote.
- A player can no longer pay repeatedly or inflate statistics by voting again for the same event.
- Changing a vote is now free after the initial paid vote, matching the voting GUI description.

### Added

- Added feedback when a player selects the event they have already voted for.

## 3.0

### Added

- Separate `/pe vote` command for player voting.
- `/pe votes` command for vote inspection and reset.
- `/pe streak` command for streak inspection and reset.
- `/pe stats` command for event and vote statistics.
- `/pe leaderboard` command for the streak leaderboard.
- `/pe validate` command for custom event validation.
- Placeholder support for current-event time remaining, leading event, leading votes, player streak, top streak, and next milestone.
- Bossbar title support for `%time%`, `%leading%`, and `%votes%` placeholders.
- Per-event voting enable and vote-cost override support for custom events.
- Custom event category support.
- Event and vote statistics tracking for reporting.

### Changed

- `/pe events` is now the admin event editor surface; regular players use `/pe vote` for voting.
- Voting UI now uses per-event vote costs when a custom event overrides the base cost.
- Event GUI now shows custom event categories and voting state in the lore.
- `next_upd.md` now tracks the next remaining feature work after this implementation pass.
- Vote handling charges the configured cost before the vote is recorded.
- Changelog entries are organized by plugin version instead of by date.

### Previous

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
- You need to delete the old `pulseevents` folder because of the new events location and changed `config.yml`.
