package io.github.hammerknife404.dreadpatch.registry;

/**
 * Every dread light at most {@link #LIGHT} (0.64.0, the owner: "assure all sources of dread lights do not exceed
 * light level 6"): the dread lantern (10 before), a Dread Gate's face (11), the Rimefrost dragonforge's lit core
 * (10); rimefire was 6 already, and Ice and Fire's dread torches are 5. Give any new dread light this.
 */
public final class DreadLights {
    public static final int LIGHT = 6;

    private DreadLights() {
    }
}
