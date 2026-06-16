# Placeholders

## Event State

- `%pulseevents_current_event%`
- `%pulseevents_current_event_time_remaining%`
- `%pulseevents_leading_event%`
- `%pulseevents_leading_event_votes%`
- `%pulseevents_event_active%`
- `%pulseevents_events_enabled%`

## Queue And Registry

- `%pulseevents_queue_size%`
- `%pulseevents_registered_events%`

## Streaks

- `%pulseevents_player_streak%`
- `%pulseevents_top_streak%`
- `%pulseevents_next_streak_milestone%`

## Notes

- `%pulseevents_current_event%` returns `none` when no event is running.
- `%pulseevents_leading_event%` returns `none` when no votes are active.
- Numeric placeholders return `0` when there is no active value.
