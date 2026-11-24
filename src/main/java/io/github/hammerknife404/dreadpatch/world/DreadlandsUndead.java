package io.github.hammerknife404.dreadpatch.world;

import io.github.hammerknife404.dreadpatch.magic.UndeadSun;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The Dreadlands' sun doesn't burn the undead (0.60.0): its ghost villages' zombie villagers, and
 * anything undead that strays in. Around each undead mob's own tick there: fire that rose during it
 * with no fire or lava touching it was the sun's, and is put back before it shows or hurts
 * ({@link UndeadSun}). A zombie's or skeleton's helmet still wears in the sun, as vanilla's does.
 */
public final class DreadlandsUndead {
    /** The undead mob whose tick is running, and its fire before it: entities tick one at a time, on the server thread. */
    private static Mob ticking;
    private static int fireBefore;

    private DreadlandsUndead() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadlandsUndead::onTickPre);
        gameBus.addListener(DreadlandsUndead::onTickPost);
    }

    private static void onTickPre(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && Dreadlands.is(mob.level())
                && mob.getType().is(EntityTypeTags.UNDEAD) && !mob.fireImmune()) {
            ticking = mob;
            fireBefore = mob.getRemainingFireTicks();
        }
    }

    private static void onTickPost(EntityTickEvent.Post event) {
        if (event.getEntity() != ticking) {
            return;
        }
        Mob mob = ticking;
        ticking = null;
        int after = mob.getRemainingFireTicks();
        int kept = UndeadSun.keptFire(fireBefore, after, touchingFire(mob));
        if (kept != after) {
            mob.setRemainingFireTicks(kept);
        }
    }

    /** In lava, or in a fire block (vanilla's own test for what's inside: its box, a hair in). */
    private static boolean touchingFire(Mob mob) {
        if (mob.isInLava()) {
            return true;
        }
        AABB box = mob.getBoundingBox().deflate(1.0E-5);
        return BlockPos.betweenClosedStream(box).anyMatch(pos -> mob.level().getBlockState(pos).is(BlockTags.FIRE));
    }
}
