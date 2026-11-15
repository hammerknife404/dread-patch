package io.github.hammerknife404.dreadpatch.magic;

import java.util.UUID;

/**
 * Game-free: her sword's Glass-Rime Shatter, kept per wielder. Every third full strike in a row on
 * the same target shatters its frost, if it's chilled or frozen by then (if not, the next full
 * strike that finds it chilled does). A blade charged by its guard shatters on its next full strike,
 * whatever the count. Weak swings neither count nor break the count; a strike on someone else
 * starts it over.
 */
public final class RimeCombo {
    public static final int STRIKES = 3;

    private UUID target;
    private int strikes;

    /** Records a strike; whether it shatters. */
    public boolean strike(UUID target, boolean full, boolean chilled, boolean charged) {
        if (!full) {
            return false;
        }
        if (!target.equals(this.target)) {
            this.target = target;
            this.strikes = 0;
        }
        this.strikes++;
        if (charged || (this.strikes >= STRIKES && chilled)) {
            this.strikes = 0;
            return true;
        }
        return false;
    }
}
