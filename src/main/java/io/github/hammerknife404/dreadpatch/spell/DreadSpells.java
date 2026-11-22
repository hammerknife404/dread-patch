package io.github.hammerknife404.dreadpatch.spell;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.resources.ResourceLocation;

/**
 * Her spells' school, Rimefrost (0.51.0). Each of her spells names it as its default and returns it
 * from {@code getSchoolType} whatever Iron's per-world spell config says: that config stores each
 * spell's school, so worlds made before 0.51.0 hold "ice" for them, and would keep it.
 */
public final class DreadSpells {
    public static final ResourceLocation RIMEFROST = DreadPatch.id("rimefrost");

    private DreadSpells() {
    }

    public static SchoolType school() {
        return ModRegistries.RIMEFROST_SCHOOL.get();
    }
}
