package io.github.hammerknife404.dreadpatch.magic;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Game-free: her sword's Glass-Rime Shatter, kept per wielder and counted per target. Every third
 * full strike on a target shatters its frost, if it's chilled or frozen by then (if not, the next
 * full strike that finds it chilled does). Strikes on others don't break a target's count (a Better
 * Combat swing hits everyone in its arc, one after another), but a count lapses {@link #WINDOW} ticks
 * after its last strike. A blade charged by its guard shatters on its next full strike, whatever the
 * count. Weak swings neither count nor break a count.
 */
public final class RimeCombo {
    public static final int STRIKES = 3;
    /** Ticks after a target's last full strike that its count lapses. */
    public static final long WINDOW = 100;
    /** The most targets one wielder keeps counts for; the longest unstruck goes first. */
    static final int MAX_TARGETS = 16;

    /** By target, the least recently struck first. */
    private final Map<UUID, Count> counts = new LinkedHashMap<>();

    private static final class Count {
        int strikes;
        long last;
    }

    /** Records a strike at game time {@code time}; whether it shatters. */
    public boolean strike(UUID target, long time, boolean full, boolean chilled, boolean charged) {
        if (!full) {
            return false;
        }
        this.counts.values().removeIf(c -> time - c.last > WINDOW);
        Count count = this.counts.remove(target);
        if (count == null) {
            count = new Count();
        }
        count.strikes++;
        count.last = time;
        boolean shatter = charged || (count.strikes >= STRIKES && chilled);
        if (shatter) {
            count.strikes = 0;
        }
        this.counts.put(target, count);
        if (this.counts.size() > MAX_TARGETS) {
            Iterator<UUID> oldest = this.counts.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        return shatter;
    }
}
