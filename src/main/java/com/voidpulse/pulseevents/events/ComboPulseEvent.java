package com.voidpulse.pulseevents.events;

/**
 * Wraps two independent events so they run at the same time as a single "combo" event.
 * The combo takes the longer of the two durations and starts/stops both sub-events together.
 */
public class ComboPulseEvent implements PulseEvent {

    private final PulseEvent first;
    private final PulseEvent second;

    public ComboPulseEvent(PulseEvent first, PulseEvent second) {
        this.first = first;
        this.second = second;
    }

    public PulseEvent getFirst() {
        return first;
    }

    public PulseEvent getSecond() {
        return second;
    }

    @Override
    public String getName() {
        return first.getName() + " + " + second.getName();
    }

    @Override
    public void start() {
        first.start();
        second.start();
    }

    @Override
    public void stop() {
        first.stop();
        second.stop();
    }

    @Override
    public int getDuration() {
        return Math.max(first.getDuration(), second.getDuration());
    }
}
