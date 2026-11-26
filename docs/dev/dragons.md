# Rimefrost dragons, Syndrigossa and the dragonforge

Part of the notes for agents working here: [CLAUDE.md](../../CLAUDE.md) has the rules and the index.

## Rimefrost dragons and Syndrigossa

**The split (0.37.0):** `entity/RimefrostDragon` (`dreadpatch:rimefrost_dragon`) is the subtype; `entity/Syndrigossa` (`dreadpatch:syndrigossa`, its id kept so saved ones load) extends it.
**Wild ones** (0.84.0): nesting on the Glacial Plains (`world/RimefrostNests`; `dreadlands.md`, The Glacial Plains): stage 2-5 or ancient, aging off, their home the nest; untamed, so Ice and Fire's wild-dragon AI with ours (players hunted, the Dread spared).
- **Every Rimefrost dragon** (`RimefrostDragon`):
  - the breath, the bite's rime, its own Shatter-Spine, takeoff and air-attack weighting;
  - the player-hunting goal, the Dread corpse (`corpseLoot`/`bonesLoot`/`heartLoot`, `entities/rimefrost_dragon/*`), the tag-made health fix, the animation layers;
  - Ice and Fire's health for its age (config `dragon.maxHealth`), its own `Variant` (Ice and Fire's random ice colour, or a summon's), `getCommander` null.
  - `aiStep` (server, alive): drop a bad target, `beforeAttacks()` (a hook), take off, Shatter-Spine. `carriesOthers()` is any passenger.
- **Only Syndrigossa** (`Syndrigossa`): white (`getVariant`), Sovereign's Eclipse, the boss bar, health by difficulty, ancient spawns, the queen's seat and tether (`positionRider`, `carriesOthers` not counting her queen, `isAllowedToTriggerFlight`, `bringQueenDown` and `followDifficulty` in `beforeAttacks`), her own tables (`entities/syndrigossa/*`, her heart with her egg).
- **Shared code** names `RimefrostDragon`: `SyndrigossaEvents` (breath events, charges, siphon, bite, Wither), `ShatterSpineCore.launch`, `ShatterSpineShard`. Tags: the four entity type tags list both.

**`entity/RimefrostDragon`**
- **An `IceDragonEntity` subclass:** Ice and Fire's model, animations, growth, flight, AI and riding all come with it.
- **An `IDreadMob`:**
  - no Withering Rime;
  - allied with the Dread through `isAlliedTo`, `canAttack` and `shouldTarget`;
  - `getCommander` is none (Syndrigossa's is her queen).
- **Colour:** a Rimefrost dragon keeps Ice and Fire's `Variant`, so every colour lookup works and its corpse's scales come out in its colour (`iceandfire:dragon_loot` swaps any scales for `DragonColor.getById(getVariant()).getScaleItem()`). Syndrigossa's `getVariant` is always `white`.
- **Boss bar:** a `ServerBossEvent` (white, progress), as vanilla's Wither keeps one:
  - players are added in `startSeenByPlayer` and removed in `stopSeenByPlayer`;
  - `aiStep` sets its progress and its visibility (alive, not `isModelDead`, not tame);
  - `setCustomName` renames it.
  - Ice and Fire gives its dragons none. `/bossbar` only lists command-made bars, so the smoke test can't see it.
- **Players:** her own `DragonAITargetNonTamedGoal` at 5 takes them, without the `neutralToPlayer` check.

**The skin** (`client/RimefrostDragonRenderer`, `SyndrigossaRenderer` adding `SaddleLayer`, `UndeadDragonSkins`, `UndeadSkin`)
- **Made:** on the render thread, the first time each is drawn, as a `DynamicTexture` named `dreadpatch:dynamic/rimefrost_<colour>_<stage>[_sleeping]`.
- **From:** Ice and Fire's `<colour>_<stage>` (the dragon's `getVariant`) and `ice_skeleton_<stage>`, which share a layout.
- **Rotted by noise:** soot scales, bone where the skeleton has it, torn membrane.
- **Consequences:**
  - nothing of Ice and Fire's ships with us;
  - resource packs that retexture ice dragons retexture her;
  - it's remade on a resource reload;
  - she wears Ice and Fire's skin if the textures can't be read.
- **Dead:** her corpse keeps her skin until it's skeletal. 0.15.0 left it to the white ice dragon's, and the owner saw her "lose her texture".
- **Male overlay:** Ice and Fire's (a frost shimmer on the wing membranes) still draws over a male's torn wings.

**A tag-made dragon** (`/summon` with NBT)
- Ice and Fire reads its health while its max is still a hatchling's 20, then grows the max afterwards.
- So after `super.readAdditionalSaveData` she re-reads the tag's `Health`, or fills to max if it has none.
- In 0.15.0 she came out at 20 of 419 health, and one warden hit killed her.

**Age** (`magic/DragonAge`)
- **Ancient:** for spawns by the world (`NATURAL`, `CHUNK_GENERATION`, `STRUCTURE`, `EVENT`) or by a mob (`MOB_SUMMONED`). `makeAncient` sets 125 days and heals her whole.
- **By hand** (egg, `/summon`, dispenser, spawner): Ice and Fire's random age.
- **Saving:** saved as `Ancient`. A tag with `Ancient:1b` makes her ancient in `readAdditionalSaveData`. A spawn egg's `entity_data` merges into the dragon the egg already made.
- **The creative tab's Ancient egg:** her egg with that `entity_data`, named by `item_name` (a custom name would name the dragon).

**Health by difficulty**
- Her `refreshDirtyAttributes` sets max health to `DragonAge.maxHealth(fullHealth(difficulty), days)`, after Ice and Fire's, whose range sits in `public final` fields.
- Ice and Fire calls it so rarely that her `aiStep` calls it again whenever the difficulty differs from the one last used, keeping her share of health.
- That difficulty is `healthDifficulty`, saved as `HealthDifficulty`. It has no field initializer, because Ice and Fire may call `refreshDirtyAttributes` from its constructor.

**Undead**
- `#minecraft:undead`: Smite applies, Healing and Harming are inverted, plus `#ignores_poison_and_regen` and `#wither_friends`.
- Wither is refused through `MobEffectEvent.Applicable` (NeoForge deprecates overriding `canBeAffected`).
- She also has the ice dragon's three tags.

**Corpse and egg**
- **Loot table:** `getDeadLootTable` gives `dreadpatch:entities/syndrigossa/corpse` (scales 60 : dread shards 40), or, once skeletal, `.../skeleton` (bones).
- **Harvest** (`mobInteract`):
  - it refuses a glass bottle, and flags the harvest;
  - during the harvest, `spawnAtLocation` drops nothing for a `DragonEggItem`;
  - the heart's drop rolls `.../heart` instead: shards and witherbones, and her theme's disc at 25% (0.72.0, [dreadlands.md](dreadlands.md)).
- **Her egg:**
  - `dreadpatch:syndrigossa_egg`, a plain item for now;
  - a pool in `.../heart` under `entity_properties` `nbt {Ancient:1b}`, so one from every ancient corpse, of either gender;
  - none if `loot.heart` is off.

**Attacks** (`entity/SyndrigossaEvents`, `ShatterSpineCore`, `ShatterSpineShard`, `magic/DragonBreath`)
- **Breath:** `performNormalBreathAttack` is hers.
  - **The cone:**
    - ±20° from her head, padded by each victim's half-width;
    - to 2 blocks past the aim (at most `CONE_MAX_LENGTH`, 32 blocks), grown over 10 ticks;
    - cut short by blocks.
  - **Who it hits:** everyone she may harm, can see and isn't carrying, in the cone or near where it lands, and within 32 blocks of her head (nearest point of their box): the splash stops at her max distance too (`DragonBreath.reach`).
  - **Damage:**
    - stage × `attackDamageIce` (Ice and Fire's config, so it matches the pack's ice dragons), as `dreadpatch:withering_breath`;
    - plus 60 ticks of rime;
    - and a 0.5 siphon past armor into her.
  - **Fire:** fire near the landing is drunk, and nothing freezes.
  - **Ice and Fire's breath:** `DragonFireDamageWorldEvent` is cancelled for her.
- **Her bite** (0.33.0): a `mob_attack` hit of hers on `LivingDamageEvent.Post` while `biting()` (Ice and Fire's `ANIMATION_BITE` or `ANIMATION_SHAKEPREY`) leaves Withering Rime II, 90 ticks.
  - Ice and Fire's `IceDragonEntity.doHurtTarget` only picks the attack's animation (bite, tail whack, prey shake, wing blast) and returns false; the hit lands later on the animation's tick, so the animation then tells a bite from the rest.
  - A rider's bite (`IafDragonLogic.attackTarget`) is also `mobAttack` from the dragon.
  - Her rime feeds no one (only a queen's does).
- **Flight:**
  - `randomizeAttacks` weights the air pick: Scorch Stream 50%, Hover Blast 35%, Tackle 15%.
  - With a live target, on the ground, she takes off with a 1-in-60 chance a tick.
- **Her own Shatter-Spine:**
  - Airborne, with her target in sight 6–32 blocks off, every 200 ticks she calls `performChargeAttack` for 24 ticks, whatever air attack is picked.
  - That's because Ice and Fire swaps `airAttack` in many places: Hover Blast alone gave 1–2 charges in 40 s.
- **Replacing ice charges:**
  - Any `IceDragonChargeEntity` she owns is cancelled in `EntityJoinLevelEvent`.
  - It's replaced by a `ShatterSpineCore` a tick later (`TickTask`), since adding an entity during another's join isn't safe.
- **The core:**
  - It bursts near its aim (9 blocks over the target) into 5–7 shards.
  - Each shard hits everyone within 1.5 blocks of its landing, measured to the nearest point of their box (a shard on a golem's head is 2.7 blocks from its feet).
- **Drawing:** `client/ShatterSpineRenderer`, with block-atlas `IceSpike`s.

**Sovereign's Eclipse** (`entity/SovereignsEclipse`)
- **What it is:** a `RimefireCrown` subclass, radius 8, lasting 200 ticks.
- **Effects:** Slowness IV and Withering Rime, not Rimebound. `shatterAround` doesn't break it.
- **Placement:**
  - every 600 ticks, from stage 3, within 24 blocks;
  - on the ground under her target, since a target she's carrying would float it.

**Animation (0.19.0)**
- **Why:** the owner saw her "posed still in a sitting idle animation except the mouth and head sway".
- **Three controllers** instead of Ice and Fire's one:
  - `flyby_attack`, first so the body's flight wins on shared bones: plays the stream breath and charge in flight;
  - `body`: picks as Ice and Fire does;
  - `attack`, last: plays the blast breath and charge when hovering or standing, and the ground actions.
- **Particles:**
  - `rime_flame`/`rime_smoke` (and `_small` for the bolt), our own `TextureSheetParticle`, with exact velocities, swelling;
  - the breath sends particles at `DragonBreath.particleSpeed` and seeds the cone itself, so even a short burst shows it whole.
  - **Their reach matches the breath's** (0.35.1). Vanilla's `Particle.tick` moves a particle, then keeps `friction` of its speed, for `lifetime` ticks: `DragonBreath.travel`. The numbers are shared with `RimeBreathParticle` (`FLAME_*`, `SMOKE_*`).
    - From her mouth, each carries `sentDistance`: half to all of the cone, capped so the longest-lived (`PARTICLE_OVERSHOOT`, 1.103) still ends within `reach`: the cone plus its splash (`breathRadius`), never past 32.
    - Set in the cone, each drifts `driftDistance`: 1.5–4 blocks, capped the same way.
    - Vanilla's snowflake (friction 1.0, then ×0.95 sideways in its own tick, 16–80 ticks) is sent at `snowSpeed`, the speed for its longest life, so none passes its mark.
    - `DragonBreathTest` runs that motion over every lifetime, cone length and stage, and checks the far end is still reached.

## Riders: her in the saddle, and any dragon's player rider (`client/SaddleLayer`)

- **Who:** a queen riding Syndrigossa (0.24.1), and since 0.49.0 the player steering any `DragonBaseEntity` (its `getControllingPassenger`): Ice and Fire's fire, ice and lightning dragons and our Rimefrost ones. Any other passenger (prey) is Ice and Fire's.
- **On every dragon:** added to each `DragonBaseEntityRenderer` (Ice and Fire's and ours, which extend it) on `EntityRenderersEvent.AddLayers`, every time the renderers are built; no mixin. (Syndrigossa's renderer used to add it itself.)
- **Where:**
  - at `SaddleSeat`, the top of `BodyUpper`'s cube, the same in all three of Ice and Fire's dragon models (as `SaddleSeatTest` checks, each);
  - measured from the model origin, in the bone-turned pose GeckoLib hands the layer.
- **How:**
  - turned by the body yaw − 180, with the dragon's scale undone, and 0.6 down;
  - their shadow is off while they ride.
- **Elsewhere:** while it draws a rider it lists them in Ice and Fire's `RENDERING_RIDERS` (so Ice and Fire's player check lets the draw through, still hiding the local player in first person) and its own `DRAWING`; a `RenderLivingEvent.Pre` listener cancels any other draw of a seated rider (Ice and Fire's layer's, or at their own position). Its listeners are registered with their event class (`addListener(Class, Consumer)`): a method reference to a static method of the generic `SaddleLayer` doesn't infer.

## Her egg (`item/SyndrigossaEggItem`, `entity/SyndrigossaEggEntity`, `client/SyndrigossaEggRenderer`; 0.39.0)

- **Laying:** `useOn` the top face of a `DreadGateBlock.FRAME` block (dreadstone) with room above: a `dreadpatch:syndrigossa_egg` entity (0.45 × 0.55, Ice and Fire's egg's size) owned by the player (`Owner`), named if the egg is. Anywhere else, an action-bar hint.
- **The egg entity:** a plain `Entity`, with gravity, and pickable. A player's hit drops the item back (its storms lost); nothing else harms it, though `/kill` removes it. Saved: `Owner`, `Soaked`. Drawn as its item sprite (`ItemDisplayContext.FIXED`), billboarded, 0.7 blocks.
- **Soaking** (`WhiteoutDominion`):
  - on a storm's first tick it records its `fullLifetime` and the eggs inside it;
  - every 20 ticks it drops eggs no longer inside;
  - only if it ends by running out a lifetime still equal to `fullLifetime` (hers is cut short when she falls; a recast or `/kill` discards it early, never reaching that), each egg still inside gets `soak()`.
  - That counts only on dreadstone: +1, with particles, a sound, and an action-bar count to its owner if they're online.
- **Hatching** (at `STORMS` 3), as Ice and Fire's `DragonEggEntity` hatches: a `RimefrostDragon`, variant white, a random gender, hunger 50, `refreshDirtyAttributes` and whole, the egg's name, `setTame(true, true)` and `setOwnerUUID`, persistent, with Ice and Fire's `EGG_HATCH` sound.

## Dragon corpses (`entity/DragonCorpses`)

- **The bug:** `DragonBaseEntity.isAlive()` is `!isRemoved()` while `isModelDead()`, so a corpse counts as alive until it's removed. A mob whose target died kept it (`TargetGoal.canContinueToUse` saw it alive). Ice and Fire's `DreadAITargetNonDreadGoal` only skips corpses (`DragonUtils.isAlive`, which is `!isMobDead`) when picking a new one. In a probe a dread thrall still targeted the corpse 6 s on.
- **The corpse itself** (the owner's suggestion, for other mods' sake): each tick a corpse that isn't yet is made invulnerable (saved as `Invulnerable`).
  - `LivingEntity.canBeSeenAsEnemy` is `!isInvulnerable() && canBeSeenByAnyone()`, which `Mob.canAttack` and `TargetingConditions` use, so a running target goal drops it and nothing picks it.
  - Nothing else changes: Ice and Fire's `hurt` on a corpse takes only `fellOutOfWorld`, which bypasses invulnerability, and harvesting is `mobInteract`.
- **Any mob's target:** `LivingChangeTargetEvent` turns a corpse into no target, and every 10 ticks a mob holding one drops it. That covers targets handed over outside the goals (the queen's rally to her court).

## The Rimefrost dragonforge (`registry/DreadForge`, `block/RimefrostForge*`, `client/RimefrostForgeScreen`, `compat/jei/*`, `magic/ForgeRules`; 0.59.0)

- **Blocks:** `rimefrost_forge_core` (one block, `lit` while whole, light 10: under what melts snow and ice), `rimefrost_forge_brick` (`grill`, set by the core on the 4 beside it), `rimefrost_forge_input` (the aperture, `active` 40 ticks after a breath hit: Ice and Fire has the texture and never sets it), `rimefrost_dragonscale_block`. Ice and Fire's numbers (forge: 40/500, metal; scale block: 30/500, stone, needs a pickaxe), all `#mineable/pickaxe`, each drops itself; the forge's are Ice and Fire's `DragonProof` (its dragons' griefing spares them). Creative: forge in Functional Blocks, scale block in Building Blocks.
- **The core** (`RimefrostForgeBlockEntity`): Ice and Fire's three slots and sides (top: what goes in; sides: blood, only a Rimefrost recipe's; bottom: out), its shape (`ForgeRules.assembled`: our bricks, Ice and Fire's `DRAGON_BONE_BLOCK`), its cooking (`ForgeRules.advance`/`heat`: held while unlit), the recipe found by walking `IafRecipes.DRAGON_FORGE_TYPE` for `dragonType` `rimefrost` with `isValidInput`/`isValidBlood`. Nothing else is made. Saved: `Items`, `CookTime` (a double), `Lit`. Hoppers and pipes: an item-handler capability on the core's entity and on the brick block (the core it touches; no brick entity), invalidated round the core when it comes or goes.
- **The aperture** (`RimefrostForgeInputBlockEntity`): `breathedOn(dragon)` lights the core beside it (horizontal, as Ice and Fire's), using the dragon's `dragon_forge_speed` if it has it, else the default. Every 10 ticks (Ice and Fire's: every tick) it lures one tame or chained `RimefrostDragon` within 50 that sees it (Ice and Fire's test) by `burningTarget`, and releases any other pointed at it.
- **The dragon** (`RimefrostDragon`): `performNormalBreathAttack` calls `fuelForges` every tick on the landing (`ForgeRules.breathReach`), and lights no rimefire where it reached a forge; `updateBurnTarget` breathes on our aperture by Ice and Fire's rules, and drops Ice and Fire's (only the new forge); `isFuelingForge` is ours only. `DreadPatch` gives Rimefrost dragons and Syndrigossa Ice and Fire's `DRAGON_FORGE_SPEED` (Ice and Fire adds it to its own types only).
- **Looks:** block sprites from Ice and Fire's ice forge and white scale block through `dread_recolor` (the animated core and grill keep their `.mcmeta`; unseen in a client yet); the screen is Ice and Fire's `textures/gui/dragonforge_ice.png` recoloured into a `DynamicTexture` (`DreadRecolors.forgeScreen`, which armour layers share as `recolored(key, source)`), labels in soot. `RimefrostForgeMenu` keeps Ice and Fire's slot positions; shift-click sends blood to the blood slot.
- **Recipes:** `rimefrost_dragonscale_block` / `rimefrost_dragon_scale_from_block`; `rimefrost_forge_brick` (`SBS/BSB/SBS`, scale blocks and `iceandfire:dread_stone_bricks` → 4), `_core` (8 bricks, `iceandfire:ice_dragon_heart`), `_input` (bricks, `#c:ingots/iron`); unlocked by holding the scale, the scale block or a brick. Forge: `recipe/dragonforge/*` with `"dragonType": "rimefrost"` (`ice` until 0.58.0).
- **JEI** (`compat/jei/DreadJeiPlugin`, `@JeiPlugin`; compiled against the pack's JEI, never required): a "Rimefrost Dragonforge" page (`dreadpatch:rimefrost_dragonforge`), laid out as Ice and Fire's forge pages, its core the catalyst.
- **Tooltips:** the core and aperture: built as Ice and Fire's forges are, lit only by a Rimefrost dragon's breath.
- **Ruins** (0.78.0): about one spire in seven keeps a half-built one on its summit, its core there 60% of the time ([dreadlands.md](dreadlands.md), the spires).
