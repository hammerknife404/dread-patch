# What Ice and Fire already has (2.1-beta.1)

Part of the notes for agents working here: [CLAUDE.md](../../CLAUDE.md) has the rules and the index.

Read from its source and the pack's jar (where and how: CLAUDE.md, Ice and Fire). The traps that shape our code are listed there too; this is the rest.

## The queen

**Her class**
- `com.iafenvoy.iceandfire.entity.DreadQueenEntity extends DreadMobEntity` is a complete class:
  - a blue boss bar;
  - attributes in `bakeAttributes()`:
    - max health from config `misc.dreadQueenMaxHealth` (default 750);
    - armor 30, attack damage 5, speed 0.3, follow range 256;
    - we override health, armor and toughness;
  - no regeneration, and melee and target goals;
  - a 40-tick `ANIMATION_SPAWN` (Uranus `IAnimatedEntity`);
  - she spawns holding her sword and staff, and she's persistent.
- Ice and Fire never registers her. `IafEntities` registers eight dread types (`dread_beast` … `dread_thrall`, `dread_lich_skull`) and no queen: no `EntityType`, attributes, spawn placement, renderer, model or spawn egg.
- **Her animation never advances.** Unlike the other dread mobs, she never calls `AnimationHandler.INSTANCE.updateAnimations(this)`, so `ANIMATION_SPAWN` stays at tick 0 and never reaches a client.
  - Uranus' `updateAnimations` counts the tick on both sides.
  - It sends an `AnimationPayload` (entity id plus index into `getAnimations()`) on the animation's first server tick, and resets to `NO_ANIMATION` at its duration.
  - The animation isn't saved, so a queen loaded from disk doesn't rise again.

**Assets and items**
- **Textures:** 128×64, the same size as the dread knight's and lich's:
  - `iceandfire:textures/entity/dread/dread_queen.png`, `_eyes`, `_inactive`, `_injured`;
  - lang `entity.iceandfire.dread_queen` in 15 languages (ours is `entity.dreadpatch.dread_queen`).
- **Items:**
  - `iceandfire:dread_queen_sword`, "Blade of the Dreaded Queen":
    - a plain `SwordItem`, tier `IafTiers.DREAD_QUEEN`;
    - 4,000 uses, enchantability 21, repair ingredient air;
    - 8 attack damage, 1.6 speed.
  - `iceandfire:dread_queen_staff`, "Staff of the Dreaded Queen": a bare `Item`, stack size 1, no use, recipe or tags.

**Hooks and models**
- **Dragon hooks:** `DragonBaseEntity` (riding tick), `IafDragonLogic` (hovering) and `DragonRiderFeatureRenderer` treat a `DreadQueenEntity` controlling passenger specially. They test with `instanceof`, so our subclass counts. "Original logic involves riding" there suggests she was meant to ride one.
- **The other dread mobs' models can't be reused from here.** They're hand-built on `DreadBaseModel` (Uranus `HideableModelRenderer` parts), and:
  - `DreadBaseModel` is package-private;
  - its public parent `BipedBaseModel` declares a package-private abstract `animate(...)`;
  - so nothing outside `com.iafenvoy.iceandfire.render.model` can subclass either;
  - and NeoForge won't load two mods that share a package.
- **Tags:** Ice and Fire tags its dread mobs `#minecraft:undead` and `#minecraft:freeze_immune_entity_types`.

## Dragons (for Syndrigossa)

**Classes and rendering**
- **Classes:**
  - `DragonBaseEntity` is a public abstract `TamableAnimal` and GeckoLib `GeoEntity`, with a public constructor. Every abstract method is public or protected.
  - `IceDragonEntity` is public, not final, with a public `(EntityType, Level)` constructor.
  - Anything Ice and Fire checks with `instanceof IceDragonEntity` (the dragon forge, the model) counts a subclass.
  - Its private `shootIceAtMob` (from `aiStep`) and its `riderShootFire` spawn `IceDragonChargeEntity` directly, not through `createCharge`.
- **Rendering:**
  - `DragonBaseEntityRenderer<T>` is a public `GeoEntityRenderer` with an overridable `getTextureLocation`, plus Ice and Fire's eyes/overlay and saddle/armor layers.
  - `DragonGeoModel` picks the geo and animation file by `instanceof`, and the texture through `DragonColor.getById(getVariant())`:
    - alive: `iceandfire:textures/entity/<type>dragon/<color>_<stage>[_sleeping|_eyes].png` (sleeping also while blinking);
    - dead: `<type>_skeleton_<stage>.png`;
    - 512×256, stages 1–5; the ice colours are white, sapphire, silver and blue.
  - Don't add a `DragonColor`: its constructor adds it to every dragon of its type, and armour gets made for it.

**Stats and summons**
- **Stats** (`refreshDirtyAttributes`, public, from the age capped at 125 days):
  - health from 4% of config `dragon.maxHealth` to all of it; the range sits in `public final` fields set by `IceDragonEntity`'s constructor;
  - bite from 1 to 1 + `dragon.attackDamage`, speed 0.15–0.4;
  - armor 1–20 on an uncapped age, plus worn dragon armor.
- **When stats update:** only on load, in `growDragon`, and on equipment changes, not as a dragon ages.
- **Age:**
  - stages 2–5 at 25/50/75/100 days;
  - `getAgeInDays` clamps at 128 for a wild dragon;
  - `finalizeSpawn` adds a random 1–80 days (`growDragon`) and heals to that age's health.
- **Summons:**
  - A summon with any NBT skips `finalizeSpawn`:
    - with no age, it's a hatchling (stage 1, 20 health);
    - with `AgeTicks`, its max health grows to that age (`readAdditionalSaveData` calls `refreshDirtyAttributes`) but its health stays at 20.
  - A plain summon gets a random age.
  - Summoning any ice dragon logs `Not a list: null` from Ice and Fire.
  - Saved keys include `Variant`, `AgeTicks`, `Gender`, `ModelDead`, `DeathStage` and `AgingDisabled`.
  - `/kill` removes one outright.

**Targets, riders and drawing**
- **Sleep** is `setInSittingPose`: Ice and Fire's `SLEEPING` flag, saved as `Sleeping` (sitting is `setOrderedToSit`). `IafDragonLogic.updateDragonServer` sets it at night (config `dragon.sleep`; only with no passengers, not when its command is 2, near home if it has one) unless `isTimeToWake()` (day, or its command).
- **Small dragons on a player:** a tame stage-1 dragon right-clicked by its owner with an empty hand, not sneaking, rides them (`startRiding(player)`, up to three). It gets down when its control state is 16, which Ice and Fire's client sets while its rider holds sneak (`ClientEvents` → `dismount(keyShift.isDown())`), or when they start gliding, unless they're riding something themselves.
- **Targeting players:**
  - The wild-dragon target goals (`DragonAITargetNonTamedGoal` at 5, `DragonAITargetGoal` at 6) take a player only if config `dragon.neutralToPlayer` is false, and never one in creative. **The owner's pack sets it true.**
  - The non-tamed goal stands down when the dragon is tame or roosting. Asleep, it only takes a player within 4 blocks.
- **Riding:**
  - `getControllingPassenger` is the first living passenger, but only if the dragon is tame and that passenger is its owner.
  - Every other passenger is prey: `positionRider` hands it to `updatePreyInMouth`, which shakes it, bites it for twice its bite, and calls `stopRiding`.
  - Ice and Fire's queen branch in `updateRider` only runs for a controlling `DreadQueenEntity`, so never on a wild dragon. Nothing steers for a mob.
  - With any passenger aboard, a dragon won't take off (`isAllowedToTriggerFlight`, public), sleep or sit, and its corpse ejects every passenger.
  - `getRiderPosition()` is where a rider sits.
- **Drawing riders:**
  - `DragonGeoAttachmentLayer` draws every passenger on the `BodyUpper` bone through the entity render dispatcher, listing them in `DragonRiderRenderState.RENDERING_RIDERS` meanwhile.
  - GeckoLib 4.9 hands `renderForBone` the pose at the model's origin turned with the bone, not the pose at the bone. So Ice and Fire's rider transform (a near-zero offset, a half turn about Z) puts a rider upside down at the dragon's feet: our queen hung under Syndrigossa in 0.24.0, and players did too in the owner's pack (they said so on 2026-11-16). Our `SaddleLayer` draws both since 0.49.0 ([dragons.md](dragons.md), Riders).
  - Only players are kept from also drawing at their own position (`ClientEvents.disablePlayerRenderWhenNeed`, on `RenderPlayerEvent.Pre`: a player riding a dragon draws only while listed in `RENDERING_RIDERS`, and the local player never in first person).
  - A controlling rider's entity is set at `getRiderPosition()` plus its own height (`positionRider`): its feet a body-height over the seat, for the camera; nothing is drawn there.
  - Fire, ice and lightning dragons' models (`firedragon`, `icedragon`, `lightningdragon.geo.json`) share the same `BodyUpper` cube.
  - GeckoLib's `getRenderLayers()` is the live list; `addRenderLayer` adds to it.

**Breath and attacks**
- **Breath:**
  - `IceDragonEntity.breathFireAtPos` is protected and called every tick while breathing. It calls `breathAttack` (public final).
  - `breathAttack` posts a cancellable `DragonFireEvent`, then calls `performChargeAttack` or `performNormalBreathAttack` (both protected).
  - The normal breath streams from `getHeadPosition()` and calls `IafDragonDestructionManager.destroyAreaBreath`.
    - Like `destroyAreaCharge`, that first posts a cancellable `DragonFireDamageWorldEvent`.
    - Damage is stage × `dragon.attackDamage<Fire|Ice|Lightning>`, and each victim is hit at most every 10 ticks.
    - It also sets the public `burnParticleX/Y/Z`, and grows its stream over `burnProgress` / 40.
  - The charge plays `ANIMATION_FIRECHARGE`, and at its tick 20 adds `createCharge(...)`.
- **Attacks and flight:**
  - `randomizeAttacks()` (public; called at construction and after many events) picks two public fields:
    - `airAttack`: `SCORCH_STREAM`, `HOVER_BLAST` or `TACKLE`;
    - `groundAttack`: `BITE`, `SHAKE_PREY`, `TAIL_WHIP`, `WING_BLAST` or `FIRE`.
  - In `IafDragonFlightManager.update()`:
    - Tackle dives;
    - Hover Blast hovers 4–30 blocks off and charges, the only attack that does;
    - Scorch Stream is the strafing fly-by.
  - Takeoff (`IafDragonLogic.updateDragonServer`) needs a 1-in-1,500 roll a tick, and then a target more than 5 blocks above or below, or water. So wild dragons almost never fly on even ground.

**Animation, corpse and tags**
- **Animation:**
  - One GeckoLib controller, `"main"`, picks from a private pool:
    - dead;
    - swimming;
    - airborne: charge, breath, else `flight`;
    - on the ground: sleeping, the Uranus action, sitting, `walk`, else `ground`.
  - Names are `animation.icedragon.<name>`. The file also has `attack_stream_breath`, `attack_stream_charge`, `hovering`, `flying`, `diving` and `grab`, which are never played.
  - The attack animations move only the head, neck, jaw, claws and wing bones, so during one the body sits in its rest pose.
- **Corpse:**
  - A dead dragon (`isModelDead`) stays until its death stage reaches age / 5 (at most 25).
  - Each empty-handed right-click (`mobInteract`) takes one stage:
    - a glass bottle gives blood (first half; `loot.blood`);
    - at stage last / 2 − 1, the heart (`loot.heart`), plus a random colour's egg from a female over stage 3;
    - the last stage gives the skull;
    - otherwise, one roll of `getDeadLootTable()`: for an ice dragon scales 60 : flesh 40, then bones once skeletal (death stage ≥ age / 10).
  - Everything drops through `spawnAtLocation`, sized by age through `iceandfire:dragon_loot`. Nothing drops at death itself.
- **Tags:** `iceandfire:ice_dragon` is in `#minecraft:freeze_immune_entity_types`, `#minecraft:can_breathe_under_water`, and `#iceandfire:ice_dragon` (lightning-dragon-blood weapons get +4 against it). A subclass's own entity type gets none of them.

## Its dragonforge (read for 0.59.0)

- **Shape** (`DragonForgeBlockEntity.assembled`): a 3×3×3 cube, not a ring. Above and below the core, a layer of its bricks with `dragon_bone_block` on the 4 corners; round the core, bricks on the 4 corners and on at least 3 of the 4 sides (the aperture, `DragonForgeInputBlock`, usually the fourth). Its core block swaps to the "activated" one (light 15) whenever assembly changes, either way.
- **Only its own blocks:** its recipe input's `getTypeID` comes from `getFireType`, a hard-coded check for its six core blocks (anything else reads "fire"); its block entities are bound to `IafBlockEntities` types; its menu reads a `DragonType` from `IafRegistries.DRAGON_TYPE`. So a forge of another kind can't reuse its classes.
- **Recipes** (`DragonForgeRecipe`, `iceandfire:dragonforge`): `dragonType` is any string; `matches` needs its own block entity's input, but `isValidInput`/`isValidBlood`/`getResultItem()` are public. With no recipe it makes dragon ice (ice) or ash (others) of anything, in 100 ticks.
- **Cooking:** a breath hit (`transferPower`, the dragon's `iceandfire:generic.dragon_forge_speed`, default 0.025) sets `lastDragonFlameTimer` 40 and adds the speed; while the timer runs the cook goes +1 a tick; once it's out, `cookTime--` then `++`, so it **holds** (the branch that would cool it 2 a tick needs `!isBurning() && cookTime > 0` inside `isBurning()`'s else: it never runs). Nothing to make, the cook is lost.
- **Breath:** `IafDragonDestructionManager.destroyAreaBreath` calls `onHitWithFlame(dragon)` on any input within ±1 (stage ≤ 3), ±2 (4) or ±3 (5) of where it lands.
- **Luring:** each tick an assembled input with something to make sets `burningTarget` on one tame or chained dragon **of its `dragonType`** within 50 (a cube) whose head has a clear line reaching within 10 + 2 × width of it. `DragonBaseEntity.updateBurnTarget` (public, from `IafDragonLogic.updateDragonServer`) then has it look and `breathFireAtPos` while not asleep, dead or a baby, within 115 × stage (squared) and `canPositionBeSeen`, else drops it. `isFuelingForge` stands its wander and idle-look goals down. **A Rimefrost dragon is an ice dragon to it**, so Ice and Fire's ice forge drew ours (to no effect: our breath never reaches its code).
- **The attribute is on its dragons only:** `dragon_forge_speed` is added to Ice and Fire's own dragon types, not in `bakeAttributes()`; reading it on one without it throws (`Can't find attribute`), which crashed the dev server's tick once (0.59.0, before ours got it).
- **JEI:** its plugin sorts forge recipes by `dragonType` into fire, ice and lightning pages only.

## The Dread Lich

`DreadLichEntity`, registered as `IafEntities.DREAD_LICH`:
- **Stats:** 50 health, 2 armor, speed 0.3, follow range 128.
- **Combat:** ranged (`DreadLichAIStrifeGoal`), with skulls.
- **Minions:**
  - In `performRangedAttack`, while its `MinionCount` is under 5 and its 100-tick cooldown is up, it raises a minion instead of shooting.
  - The odds: thrall 50%, ghoul 35%, dread beast 15%, else scuttler.
  - Each minion gets the lich's own `CommanderUUID`.
- **Commander:** `getCommander()` always returns null for a lich, so a `CommanderUUID` on it changes nothing in Ice and Fire.
- **Spawning:** its `finalizeSpawn` sets `ANIMATION_SPAWN`.
- **Loot:** bones and dread shards, plus a Dread Key at 10% (+5% per looting level) if a player killed it.

## The Mausoleum and its key

**The structure**
- `iceandfire:mausoleum`: one 21×15×30 template, `structure/mausoleum/building.nbt`.
- **Placement:** in snowy biomes (`#iceandfire:structure_gen/mausoleum`), spacing 32, separation 12, and further gated by config `worldGen.generateMausoleumChance`.
- **Inside:**
  - dread stone, bricks, tiles and chiseled skulls, most of them `unbreakable=true`;
  - 13 `dread_spawner`s, and 17 chests of `chest/mausoleum_chest` (no key among them);
  - an ice dragonforge in the far hall.
- **Entrance:** a wall of unbreakable `dreadwood_planks` with one `dreadwood_planks_lock` in it (`DreadWoodLockBlock`, unbreakable unless `PLAYER_PLACED`).

**The key**
- Right-clicking the lock with `iceandfire:dread_key` (**Gravemaster's Key**, a plain item) uses the key up (not in creative) and removes the planks within `distSqr` 32.
- The key had no recipe; its only source was the lich's drop.
- There's no Dread portal block in CE.

## Necromancy, never called

`DreadMobEntity.necromancyEntity(LivingEntity)` (public static) makes the dread mob a dead thing would rise as:
- an arthropod → a scuttler;
- a `Zombie` or an `IHumanoid` → a ghoul, sized by the dead's width;
- an undead, an `AbstractSkeleton` or a `Player` → a thrall wearing the dead's equipment (the same `ItemStack`s, not copies);
- an `AbstractHorse` → a dread horse (a `SkeletonHorse`, with no commander);
- an `Animal` → a dread beast;
- anything else → null.

It calls `finalizeSpawn` itself (as `MOB_SUMMONED`, not through `EventHooks`), except for the horse. `DreadMobEntity.onKillEntity` would raise whatever a lich kills, but nothing calls it in CE.
