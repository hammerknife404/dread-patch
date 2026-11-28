package io.github.hammerknife404.dreadpatch.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * How her regalia looks (0.98.7, a client config, {@code config/dreadpatch-client.toml}; on the mods list's config
 * screen too). The owner: "include toggles to enable the classic dread regalia armor, sword and staff"; "classic
 * textures are the default, but use the armor variant with the bodice bust"; then of the winged bodice's wings, "Toggle
 * for both elytra wing types, with new model the default". Classic is the look before 0.81.0: the armour her own
 * outfit's boxes (bust and all) in her Ice and Fire texture, on her as her own outfit; the sword and staff Ice and
 * Fire's models. Read each time something's drawn, so a change shows at once.
 */
public final class DreadPatchClientConfig {
    public enum Wings { DRAGON, ELYTRA }

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue CLASSIC_ARMOR;
    private static final ModConfigSpec.BooleanValue CLASSIC_SWORD;
    private static final ModConfigSpec.BooleanValue CLASSIC_STAFF;
    private static final ModConfigSpec.EnumValue<Wings> WINGS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("How the Dread Queen's regalia looks (on you, on her, in hand).").push("regalia");
        CLASSIC_ARMOR = builder.comment("Her regalia's armour in its classic look: her own outfit in Ice and Fire's texture, bust and all, as she wears it.",
                "Off: the redrawn dark steel and cyan regalia.").define("classic_regalia_armor", true);
        CLASSIC_SWORD = builder.comment("Her sword as Ice and Fire draws it. Off: the redrawn glowing blade.").define("classic_regalia_sword", true);
        CLASSIC_STAFF = builder.comment("Her staff as Ice and Fire draws it. Off: the redrawn glowing staff.").define("classic_regalia_staff", true);
        WINGS = builder.comment("The winged bodice's wings: DRAGON, its ice-bone dragon wings; ELYTRA, a vanilla elytra's.")
                .defineEnum("regalia_wings", Wings.DRAGON);
        builder.pop();
        SPEC = builder.build();
    }

    private DreadPatchClientConfig() {
    }

    public static boolean classicArmor() {
        return !SPEC.isLoaded() || CLASSIC_ARMOR.get();
    }

    public static boolean classicSword() {
        return !SPEC.isLoaded() || CLASSIC_SWORD.get();
    }

    public static boolean classicStaff() {
        return !SPEC.isLoaded() || CLASSIC_STAFF.get();
    }

    public static Wings wings() {
        return SPEC.isLoaded() ? WINGS.get() : Wings.DRAGON;
    }
}
