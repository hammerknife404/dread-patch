# Dread Patch: notes for the next agent

NeoForge 1.21.1 mod (Java 21, ModDevGradle) that adds Ice and Fire CE's unfinished Dread Queen to the game, with the Rimefrost (undead frost dragons of the Dread) and her own, Syndrigossa, for the owner's modpack. Hard dependencies on Ice and Fire CE and Iron's Spells 'n Spellbooks. It's the owner's largest mod so far.

- **Version:** `mod_version` in `gradle.properties`.
- **README:** it describes everything as a player sees it. Keep it in step.
- **Still to come:** the Create Rimefrost Dragon spell, hatching Syndrigossa's egg (both decided, under the owner's decisions), and natural spawning. For a public release, also seat player riders properly on Ice and Fire dragons (deferred).

## The owner's decisions

The design comes from the owner's two chats with another assistant, `proposed_stuff_1.txt` and `proposed_stuff_2.txt` (local only). Their numbers are that assistant's proposals, and some of their "facts" about the game are invented. Only what the owner adopted, listed below, holds. The version says when each decision shipped.

**The queen and her items**
- **Stats (0.12.0):** 300 health, 20 armor and 12 toughness. Only she heals from her Withering Rime.
- **Movement:**
  - She keeps her distance like Iron's necromancer (0.8.0).
  - She Barrow-Steps away when cornered (0.11.0, from the 2nd notes).
- **Figure (0.31.0):** her body should "reflect the female player variant", so she got a bust. Her arms were already Alex's 3 px.
- **The Rime siphon (0.14.0):**
  - She heals a flat 1/2/3 per target by difficulty.
  - 0.13.0 had her leech 1–2% of the victim's max health instead, and she soloed areas full of wardens, about 10 s each.
  - Since then every share-of-max-health hit is capped at 20 against everything. Before, the cap applied only against bosses.
- **The staff (0.9.0):** the chat's numbers.
- **The sword (0.10.0):** the chat's profile and properties, with two gaps the owner filled:
  - A guard-blocked hit primes a shatter (the chat's "frost charge" did nothing defined).
  - The guard stops only fire and magic.
  - The shatter's 10% is ours.
- **Her drops (0.33.0):** with the craftable key letting players summon her at will, the sword and staff drop at 25% each (+5% per Looting level), with one ink and one scroll; until then all four were certain, with two inks and one or two scrolls.
- **Summon Court (0.33.0):** a court spell on her staff and as one of her scrolls. The owner's terms:
  - a player's court never fights the Dread (and the Dread leave it be);
  - it's "never hostile to other friendly mobs" and doesn't retaliate, with config options (`config/dreadpatch-server.toml`: `attack_friendly_mobs`, `retaliate`, `attack_players`, all off).
- **The Rimefire Crown (0.8.0):** it replaced the Barrow-Glass Tomb of 0.6.0–0.7.0, which the owner said looked like glass jars. Scrolls made before went blank.
- **Loot (0.4.0):** the owner's list (Data, below).

**Syndrigossa**
- **0.15.0:** an ice dragon of the Dread in a rotted skin.
- **0.16.0:** her Withering Rime breath and Shatter-Spine.
- **0.17.0:** Sovereign's Eclipse, with no closing in and no root, but a heavy slow.
- **0.18.0:** a wider breath cone, more air time, and strafing fly-bys.
- **0.20.0:** dread scuttlers spit bolts.
- **0.21.0:** Shatter-Spine finally seen (every 10 s in the air).
- **0.22.0:**
  - As the boss she's ancient and at full strength; otherwise she grows like an ice dragon.
  - She's undead, and her corpse is Dread.
- **0.23.0:** her egg comes only from the boss.
- **0.25.0:** her breath's particles, at normal size, for the queen's bolt too.
- **0.30.0:** health by difficulty: 500 Easy, 750 Normal, 1,000 Hard.
- **0.33.0:**
  - In the owner's taxonomy she's a unique, named dragon of the **Rimefrost**, a subtype of frost dragon (they offered "Rime" or "Rimefrost"). Lore only: no one-per-world limit. Her spawn eggs and egg say so in their tooltips.
  - Rimefrost dragons' bites inflict Withering Rime II: hers, 4.5 s.
- **0.37.0, the Rimefrost subtype** (the owner, from their notes: "Create the Rimefrost subtype"):
  - the plan's split, which they didn't change: every Rimefrost dragon has the breath, the bite, Shatter-Spine, undeath, Dread alliance, the rotted look in its own ice dragon colour and a Dread corpse;
  - only Syndrigossa has Sovereign's Eclipse, the boss bar, health by difficulty, the ancient boss, the queen's saddle and tether, and her egg;
  - a plain Rimefrost dragon's health is Ice and Fire's for its age (their pick wasn't given; ours, as "same tiered").
- **Next, decided (2026-11-21):**
  - **Create Rimefrost Dragon**, a spell (scroll only): it raises an Ice and Fire ice dragon corpse as a Rimefrost dragon of the same age, owned by the caster. 3 minutes' cooldown at least; 500 mana and all the rest a player has (the queen pays none). It rises with health of half (rounded down) the caster's missing health: the queen's, or the casting player's. A player has one at a time. "Recasting Create Rimefrost Dragon within its cooldown unsummons the current one and allows the new target".
  - **Hatching her egg, B:** placed on dreadstone, it hatches after soaking up three full Whiteout Dominion storms cast over it, into a Rimefrost hatchling (white) tamed to whoever placed it.
- **0.36.0:** "Make the max distance on the breath 32. Adjust particle speeds accordingly, but keep the cutoff matching its max distance".
  - The cone's max is 32 (30 before), and it's a hard limit: the splash where it lands no longer hits past it, and no particle flies past it.
  - Then "apply this distance to rimefrost dragons too": like the Withering Rime II bite, it's a **Rimefrost trait**. Syndrigossa is the only Rimefrost dragon so far (it's all in her class), so give any Rimefrost dragon added later (one hatched from her egg) both.
  - Particle speeds follow the cone's length on their own.
- **0.35.1:** "Confirm the dragons breath particles match their distance". Checked by running vanilla's particle motion on them:
  - flame and smoke from her mouth carried 93–110% of their mark, so they matched;
  - vanilla's snowflakes flew 70–123%;
  - the particles set in the cone drifted 1.5–4.4 blocks on, past the end.
  - Now every particle ends within what the breath reaches (the cone and its splash), and `DragonBreathTest` holds it there.
- **0.35.0:** "Dread mobs should ignore dragon corpses so they are not stuck attacking them forever", then "perhaps patch the corpse itself for broader compatibility": so it covers every mob and every Ice and Fire dragon's corpse.
- **0.34.0:** "Syndrigossa should have her own boss bar". Ours is white (the queen's is Ice and Fire's blue), and hidden while she's tame and once she's a corpse.

**The queen on her dragon (0.24.0–0.26.0)**
- As the boss, she rises riding it.
- The Soul Tether splits healing between them within 28 blocks. This is the 2nd notes' version, not the 1st notes' split damage.
- She can't be hit in the saddle, so the dragon's health is what counts. Once, at half its health, it's forced down, she gets off, and it's grounded for 10 s (0.26.0).

**Whiteout Dominion (0.27.0)**
- **Origin:** the 1st notes' one line (a zero-visibility night storm, glass-rime cones, 10% of max health a tick), remade to the owner's terms:
  - a large, localized weather event that cuts visibility and slows modestly;
  - a point of freezing damage every few seconds;
  - no share-of-max-health damage, and no cones;
  - anything killed in hers rises as a dread mob.
- **Numbers:** radius 32, 45 s, every 120 s, standing where it's cast.
- **The scroll:** a player's storm doesn't raise the dead.
- **0.28.0:** snow covers the ground for a while, and every mob rises except golems.
- **0.30.0:** its water freezes like Frost Walker's, in a player's storm too.

**Other additions**
- **Rimewalk (0.29.0):** "a frostwalker like ability to prevent water kiting".
- **Her Dread Lich (0.30.0):**
  - The owner: "its cooldown should be at least a minute or so for balance".
  - Ours: 30 s in, then at most every 90 s, one at a time.
- **The Dread Gate (0.32.0):**
  - The plan offered a summoning gate (A), a Dread realm (B) or her own rift (C).
  - The owner: "A, both, consume the key, recipe is fine". So it's buildable anywhere and in the Mausoleum, and it uses up the Gravemaster's Key.
  - The key got a recipe: 3 dread shards, a bone, 2 gold.

## What Ice and Fire already has (2.1-beta.1)

Read Ice and Fire's source on the `1.21.1` branch of [IAFEnvoy/IceAndFire-CE](https://github.com/IAFEnvoy/IceAndFire-CE). `master` is the 26.1 port. The `1.21.1` branch is ahead of the pack's 2.1-beta.1 (its `mod_version` is 2.2, unreleased), so when exact behavior matters, check the jar. `~/.gradle` caches it after a build; use `javap` from the JDK at `~/.jdks/java21`.

### The queen

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

### Dragons (for Syndrigossa)

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
- **Targeting players:**
  - The wild-dragon target goals (`DragonAITargetNonTamedGoal` at 5, `DragonAITargetGoal` at 6) take a player only if config `dragon.neutralToPlayer` is false, and never one in creative. **The owner's pack sets it true.**
  - The non-tamed goal stands down when the dragon is tame or roosting. Asleep, it only takes a player within 4 blocks.
  - Dragons sleep at night.
- **Riding:**
  - `getControllingPassenger` is the first living passenger, but only if the dragon is tame and that passenger is its owner.
  - Every other passenger is prey: `positionRider` hands it to `updatePreyInMouth`, which shakes it, bites it for twice its bite, and calls `stopRiding`.
  - Ice and Fire's queen branch in `updateRider` only runs for a controlling `DreadQueenEntity`, so never on a wild dragon. Nothing steers for a mob.
  - With any passenger aboard, a dragon won't take off (`isAllowedToTriggerFlight`, public), sleep or sit, and its corpse ejects every passenger.
  - `getRiderPosition()` is where a rider sits.
- **Drawing riders:**
  - `DragonGeoAttachmentLayer` draws every passenger on the `BodyUpper` bone through the entity render dispatcher, listing them in `DragonRiderRenderState.RENDERING_RIDERS` meanwhile.
  - GeckoLib 4.9 hands `renderForBone` the pose at the model's origin turned with the bone, not the pose at the bone. So Ice and Fire's rider transform puts a rider upside down at the dragon's feet: our queen hung under Syndrigossa in 0.24.0. Nobody checked whether Ice and Fire's own player riders look the same.
  - Only players are kept from also drawing at their own position (`ClientEvents.disablePlayerRenderWhenNeed`).
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

### The Dread Lich

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

### The Mausoleum and its key

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

### Necromancy, never called

`DreadMobEntity.necromancyEntity(LivingEntity)` (public static) makes the dread mob a dead thing would rise as:
- an arthropod → a scuttler;
- a `Zombie` or an `IHumanoid` → a ghoul, sized by the dead's width;
- an undead, an `AbstractSkeleton` or a `Player` → a thrall wearing the dead's equipment (the same `ItemStack`s, not copies);
- an `AbstractHorse` → a dread horse (a `SkeletonHorse`, with no commander);
- an `Animal` → a dread beast;
- anything else → null.

It calls `finalizeSpawn` itself (as `MOB_SUMMONED`, not through `EventHooks`), except for the horse. `DreadMobEntity.onKillEntity` would raise whatever a lich kills, but nothing calls it in CE.

## What this mod adds

Everything is under `io.github.hammerknife404.dreadpatch`. `magic/` holds the rules that don't need a running game, each with a unit test.

### The queen

**`entity/DreadQueen`**, our subclass:
- **Animation:** it ticks her animations.
- **Rising** (`ANIMATION_SPAWN` ticks 0–29):
  - `isImmobile()` is true, so there's no AI step;
  - she throws ground particles;
  - Ice and Fire's thrall instead zeroes its velocity at the end of `aiStep`, after its AI has already moved it.
- **Goals:** `registerGoals` swaps Ice and Fire's `MeleeAttackGoal` for `KeepDistanceGoal`.
- **Casting:** in `customServerAiStep` (so never while rising), on her target, in sight:
  - a crown every 15 s within 20 blocks, then her next bolt 1.25 s later to shatter it;
  - a bolt every 4 s at 3–32 blocks.
- **Drops:** `getEquipmentDropChance` is 0 for her hands (her loot table drops the sword and staff). It also beats a saved `HandDropChances`.

**Attributes** (in `DreadPatch`):
- Ice and Fire's `bakeAttributes()` with our health 300, armor 20 and toughness 12.
- A queen saved before 0.12.0 keeps her saved base values.
- Vanilla toughness keeps the armor's cut (at most 80%) against bigger hits.
- Iron's spell damage doesn't bypass armor.

**Registration** (`registry/ModRegistries`): `dread_queen` (0.6 × 1.95, tracking range 10) and the spawn eggs.

**Movement** (`entity/KeepDistanceGoal`, `magic/Spacing`):
- **Models:** after Iron's `WizardAttackGoal` and Ice and Fire's `DreadLichAIStrifeGoal`. Neither works for her: one needs an `IMagicEntity`, the other a `DreadLichEntity`.
- **By distance:**
  - out of sight, or past 16: she closes in;
  - 9–16: she circles;
  - under 9: she gives ground;
  - under 5: she breaks away, once every 3 s. That's Barrow-Step if it's off its 12 s cooldown, else on foot (`DefaultRandomPos.getPosAway`).
- **Barrow-Step's spot is her own search** (`DreadQueen.barrowStep`):
  - 16 tries, 10–15 blocks away, within ±45°, then ±90°;
  - dropped to the ground, ≥ 9 blocks from her target, on sturdy, dry ground with room for her;
  - `getPosAway` found nothing often enough on the test platform that she ran instead.
- Her sword swings at whoever is in reach.

**Withering Rime** (`effect/WitheringRimeEffect`, `effect/DreadEvents`):
- **What it does:**
  - withers 1 through armor every 20 ticks;
  - builds freezing faster than the body thaws;
  - cancels all healing (`LivingHealEvent`).
- **Who refuses it:** the Dread (`IDreadMob`), through `MobEffectEvent.Applicable`.
- **The siphon** (`effect/RimeSiphon`):
  - `MobEffectEvent.Added` records who laid each victim's rime. Every place we apply it passes a source, and the last to lay it owns it.
  - Each withering tick of a `DreadQueen`'s that takes health heals her by difficulty, within 32 blocks.
  - A withering hit inside the victim's `invulnerableTime` after a bigger hit takes nothing and feeds nothing.
- **The cap** (`magic/DreadDamage`): the share-of-max-health part of her damage, capped at 20.

**Rimewalk** (`entity/Rimewalk`), run from her `aiStep` on the server every tick (so NoAI queens too):
- **Base:** vanilla Frost Walker II's `replace_disk`:
  - radius 4, on the layer under her feet;
  - still water with air above, and `isUnobstructed`;
  - `setBlockAndUpdate` to frosted ice, with a `BLOCK_PLACE` game event.
- **Unlike the enchantment:**
  - aged frosted ice in her disk is set back to age 0;
  - in water, the layer is the top water block in her column, so the surface freezes round her and she climbs out.
- **Pathing:** her `PathType.WATER` and `WATER_BORDER` malus are 0. With Ice and Fire's `FloatGoal`, her paths run along the surface, and she walks them on the new ice.
- **Griefing:** not gated on `mobGriefing` (Frost Walker isn't either).
- **Shared:** `Rimewalk.canFreeze` is also used by the storm.

**Frosted ice**, vanilla's:
- **Melting:** `onPlace` schedules a tick 60–120 out. In light above 11 − age it ages every 20–40 ticks, and turns to water at age 3. It never melts in the dark.
- **Its `neighborChanged` melts it outright** when a neighbouring frosted ice block changes and it has fewer than 2 frosted neighbours.

### Her spells

All five are Iron's Ice spells, levels 1–5. They're kept out of random loot and the scroll forge (`allowLooting()` false, `setAllowCrafting(false)`); only her loot drops them, named with `"force": true`.

**Death messages:** a spell's damage source (`getDamageSource`) dies as `death.attack.<spell id>` (Iron's `SpellDamageSource`: victim, then caster). The bolt's (also used by Beater's Decree) and the crown's (its shards) are in our lang, from 0.33.1; until then a death showed the raw key. Give any spell that deals damage under its own id one, plus the `.item` form Iron's own spells carry.

**Withering Rime-Bolt** (`entity/WitheringRimeBolt`, `spell/WitheringRimeBoltSpell`)
- **What it is:** an Iron's `AbstractMagicProjectile`. Flat damage plus a share of max health go through Iron's `DamageSources.applyDamage`, so Ice resistance counts; then it applies Withering Rime.
- **Numbers:**
  - hers: 8 + 15%, and 4.5 s of rime;
  - the spell: (6 + 2/level) + 8%.
- **Pass-through:** a queen's bolt passes through the Dread, and anyone's passes through its caster's own husk.
- **Look:** drawn only by particles: Syndrigossa's breath mix, smaller.

**Rimefire Crown** (`entity/RimefireCrown`, `magic/CrownShape`, `spell/RimefireCrownSpell`)
- **The entity:** a marker, never saved, holding a cylinder of radius 3.
- **Each tick:**
  - first tick: it drinks fire blocks and lit campfires inside;
  - every tick: it renews Rimebound and puts out burning on everyone inside it may harm;
  - at its lifetime: it shatters.
- **Rimebound** (`effect/RimeboundEffect`): speed and jump ×0.
- **Who it may harm** (`RimefireCrown.canHarm`): not its caster, not their allies, and not the Dread if a Dread cast it.
- **`shatterAround`:** a bolt into someone a crown holds breaks the crown and frees them. Its shrapnel hits everyone within 3 blocks for 4 + 6% and 3 s of rime. `invulnerableTime` is reset so the shrapnel lands right after the bolt.
- **The spell** places the crown where the caster looks: Iron's `RaycastBuilder`, 24 blocks, then `moveToRelativeGroundLevel`.
- **Drawing** (`client/RimefireCrownRenderer`, `client/IceSpike`):
  - tapered ice spikes, cut from vanilla's `packed_ice`/`blue_ice` sprites at their true width;
  - flames of our animated `dreadpatch:block/dread_fire_0`/`_1` (any `textures/block/` png is stitched into the block atlas);
  - all in one `entityCutoutNoCull(BLOCK_ATLAS)` buffer.
  - The entity type is 6 wide so the ring isn't culled.

**Barrow-Step** (`spell/BarrowStepSpell`, `entity/BarrowHusk`, `client/BarrowHuskRenderer`)
- **Casting:**
  - A player goes where they look (Iron's `TeleportSpell.findTeleportLocation`, 10 + level blocks).
  - `step` teleports through Iron's `Utils.handleSpellTeleport`, which **returns whether its event was cancelled**, not whether the caster went.
- **The husk:** if the caster went, a husk stands where they stood. It's a bare `LivingEntity`: never saved, no effects, not pushable.
  - Mobs within 16 blocks hunting the caster turn on it.
  - It shatters when struck by anyone but its caster, or after 100 ticks, putting Rimebound and rime (40 ticks each) on everyone within 3 blocks. Only attributed damage counts; `/kill` removes it quietly.
- **How it looks:** the caster's UUID and whether they're a player are synced.
  - **A player's husk:**
    - vanilla's `PlayerModel`, slim or wide, in their skin: the tab list's `PlayerInfo`, else `DefaultPlayerSkin`;
    - with the skin layers they show;
    - at a scale of 0.9375, with a player's hitbox.
  - **Anyone else's** is her model, eyes lit.
  - Both draw `entityTranslucent`, tinted ice blue.
- **A vanilla `EntityModel` starts out `young`**, which draws a `HumanoidModel` at a baby's half size. `LivingEntityRenderer` resets it every frame, but the husk's renderer is a plain `EntityRenderer`, so it must set it itself. Until 0.32.1 every husk was half size.

**Summon Court** (`spell/SummonCourtSpell`, `entity/SummonedCourt`, `DreadPatchConfig`)
- **The spell:** levels 1–5, 120 s cooldown, 60 + 15/level mana. 1 + level members (`CourtRules.summonCount`) of her court's mix, standing (60 + 10 × level) s (`summonSeconds`).
- **Raising:** her court's way (`DreadCourt.create`, `DreadCourt.findSpot` round the caster, `EventHooks.finalizeMobSpawn`), commanded by the caster (`CommanderUUID`; Ice and Fire's `getCommander` resolves players first, then any entity), `setPersistenceRequired`. A new cast dismisses the caster's last court (scanning the level).
- **Membership:** `dreadpatch:court_expires` (game time) in the mob's persistent data, saved under `NeoForgeData`. Every second a member past it crumbles, with any `IDreadMob` it rides (a knight's horse).
- **Its targeting is replaced** on every `EntityJoinLevelEvent` (goals aren't saved):
  - Ice and Fire's dread mobs target every player and every non-Dread living thing, so all their target goals are removed (`targetSelector.removeAllGoals`);
  - in their place Iron's `SummonedZombie`'s: `GenericOwnerHurtByTargetGoal` 1, `GenericOwnerHurtTargetGoal` 2, `GenericCopyOwnerTargetGoal` 3, vanilla `HurtByTargetGoal` 4, `GenericProtectOwnerTargetGoal` 5;
  - and `GenericFollowOwnerGoal` at 4 (Ice and Fire's melee is 2, its stroll 5), as Iron's summons follow: from 15 blocks out to 5, teleporting past 25.
- **What it may fight** (`CourtRules.mayFight`, by `Stance`):
  - its caster's side (the caster, their team, pets, Iron's summons, their court and husk): never;
  - the Dread: never;
  - hostile mobs (`Enemy`): always;
  - players and friendly mobs: only as the config allows (joining in, or `retaliate` for what last hurt it).
  - It's enforced on `LivingChangeTargetEvent` (cancelled) and on `LivingIncomingDamageEvent` (a hit it lands on anything it mayn't fight is cancelled), and its scuttlers' bolts pass through those (`WitheringRimeBolt.canHitEntity`).
- **Friendly mobs leave it be:** while `attack_friendly_mobs` is off, a mob that isn't hostile, a player or Dread can't take a member as its target (an iron golem goes for any `Enemy`).
- **Its caster's side can't hurt it** (`LivingIncomingDamageEvent`; Iron's friendly fire only knows `IMagicSummon`), and the caster's crown and storm skip it (`RimefireCrown.canHarm`). `/kill` still works.
- **Config:** `DreadPatchConfig`, a NeoForge server config, written to `config/dreadpatch-server.toml`; `courtTerms()` gives the defaults before it loads.

**Whiteout Dominion:** under its own heading, below.

**Icons and textures:** spell icons, effect icons, Syndrigossa's egg, the dread fire and the gate's texture come from `tools/sprites/make_icons.py` (deterministic; needs Pillow). Edit and re-run it rather than editing the PNGs.

### Her court and lich

**The court** (`entity/DreadCourt`, `magic/CourtRules`)
- **Waves:**
  - every 20 s with a target, up to 3 at a time;
  - weighted thrall 3 : ghoul 2 : knight 1 : scuttler 1;
  - up to 6 standing at once;
  - on solid ground 2–5 blocks off.
- **Spawning:** through `EventHooks.finalizeMobSpawn`, so they get their own rise and gear and other mods see the spawn. She commands them (`CommanderUUID`).
- **Targets:** Ice and Fire only syncs targets for a lich's minions, so every second she hands hers her target.
- **Ground spot:** Iron's `moveToRelativeGroundLevel` returns a point a hair under the surface (199.996 on a block top at 200). Lift it before checking the block below.

**Beater's Decree** (every 15 s)
- The member pressing an enemy closest (within 2.5 blocks) is consumed: discarded, with no loot.
- The burst does 2 + 12% (at most 20) and 4.5 s of rime within 3 blocks.
- Her court within 8 blocks frenzies: Speed I and Strength I for 6 s.
- All Dread are allied (`DreadMobEntity.isAlliedTo`), so her magic never hits her court.

**Her lich**
- **Timing:**
  - first at 600 ticks in (her cooldowns tick whenever she can act);
  - then every 1,800 ticks;
  - never while one of hers stands (she retries every 20 ticks).
- **Spawning:** on a court spot, through `EventHooks.finalizeMobSpawn`, with her `CommanderUUID`, so it gets her target.
- **Not court:** it isn't counted in her court's 6, it's never burnt for the decree, and its minions answer to it, not to her.

**Scuttler bolts** (`entity/ScuttlerBolts`)
- Every Ice and Fire `DreadScuttlerEntity` spits a weak bolt: 3 + 4%, and 2 s of rime.
- At its target 3–12 blocks off, in sight, every 60–80 ticks. The next time is kept in its persistent data.
- Not while it's rising.
- It runs from `EntityTickEvent.Post`, since the class is Ice and Fire's.

### Whiteout Dominion

**The storm** (`entity/WhiteoutDominion`, `magic/WhiteoutStorm`, `effect/WhiteoutEffect`, `spell/WhiteoutDominionSpell`)
- **The entity:** never saved; a 1×1 point whose culling box is its whole cylinder.
- **Its space:** a cylinder of `Radius` round where it was raised, from 16 below to 48 above.
- **Every 10 ticks:** everyone inside it may harm gets 40 ticks of Whiteout: speed −20%, follow range −50%. The Dread refuse it.
- **Every 60 ticks:** everyone not freeze-immune takes 1 `dreadpatch:whiteout` damage.
  - That type is tagged as vanilla's `freeze` is.
  - With no caster, the damage is vanilla's `freeze` itself, so a death message never lacks its `%2$s`.
- **Her storms:**
  - raised on the ground under her (searching 64 down, so under her dragon too);
  - first at 300 ticks in, then every 2,400 ticks, one at a time;
  - it clears over 60 ticks when she dies or is gone.
- **Recasting:** a caster's new storm discards their last.
- **Summon NBT:** `Lifetime`, `Radius`, `Owner`, `RaisesDead`.

**Raising the dead** (only hers, `RaisesDead`)
- **When:** on `LivingDeathEvent` at `LOWEST`, skipped if cancelled; up to 8 a storm.
- **Who rises:** a `Mob` or a player. Not:
  - an armor stand or a husk;
  - the Dread, a dragon, or `#c:bosses`;
  - a golem (an `AbstractGolem` other than a shulker, or any entity whose id path contains `golem`).
- **What it rises as:**
  - through Ice and Fire's `necromancyEntity`;
  - else, by shape, a thrall (`WhiteoutStorm.risesAsThrall`: at least a block tall and 1.5× as tall as wide);
  - else a dread beast sized `width / 1.2`, kept to 0.4–2.5.
- **After rising:**
  - She commands them, so they count in her court's 6.
  - They take her target.
  - Every equipment slot is copied with drop chance 0, since the dead drop their own.

**Its water**
- **Freezing:** every 10 ticks (`frost`), it looks at the top block of every column, in loaded chunks only (`hasChunk`; `getHeight` would load one).
  - Still water that `Rimewalk.canFreeze` accepts, where its snow has begun to settle, turns to frosted ice and is recorded.
  - "Begun to settle" is `WhiteoutStorm.ices`, with the same `patch(x, z)` hash the snow uses.
- **Holding and thawing:**
  - its own ice is held at age 0 while the cover lies;
  - then it ages a step a look, and turns to water at age 3;
  - `remove` with a destroying reason thaws what's left;
  - on an unload, what's left melts as Frost Walker's does.
- **Laid and held with `UPDATE_CLIENTS` only.** Patchy ice laid with neighbour updates melts as fast as it's laid (frosted ice's `neighborChanged`, above).
- **Thawing** uses `setBlockAndUpdate` plus `neighborChanged`, as vanilla's protected `IceBlock.melt` does. That tells neighbours it's water, so it doesn't cascade.

**Drawing** (`client/WhiteoutRenderer`)
- **What's drawn:**
  - vanilla's `renderSnowAndRain`, redone in `textures/environment/snow.png`;
  - a wall of snow and a `powder_snow` haze round its edge;
  - a snow cover that's drawn, never placed.
- **Not drawn in the entity pass:**
  - Vanilla draws translucent terrain (water) after the entities' translucent batches. So translucent geometry an entity draws in front of water writes depth there, and the water fails the depth test. The owner saw a lake vanish inside her storm (0.28.0).
  - Instead, the entity renderer only notes each storm vanilla has culled into view.
  - `drawStorms` runs on `RenderLevelStageEvent` `AFTER_WEATHER`, in our own render type: vanilla's `entity_translucent` state with `WEATHER_TARGET` output.
    - It writes no depth, except under Fabulous, whose transparency composite sorts each target by depth.
    - It draws back to front, one batch at a time, then puts back what the weather pass expects.
- **In 1.21.1, `BufferSource.getBuffer` for another non-fixed render type ends the last one's batch,** and writing to the old consumer then throws ("Not building!"). Draw everything of one type before asking for the next.
- **The snow cover:**
  - on every block top a snow layer could survive on, but not over fluids or frosted ice;
  - within 64 blocks, recomputed every 40 ticks;
  - drifting in and melting by `WhiteoutStorm.patch`.
- **Compatibility:** Sodium keeps the render stage. Iris hasn't been checked.

**Fog and sound** (`client/WhiteoutClient`)
- **Fog:** under Whiteout, the camera's fog closes to 10 blocks (`ViewportEvent.RenderFog`, `ComputeFogColor`, scaled by `getSkyDarken`).
- **Inside any storm:** snowflakes blow, and `client/WhiteoutWindSound` plays (the soul sand valley loop).
- **Casters:** a caster and their allies get no Whiteout, so no fog.

### The Dread Gate and the key

**The shape** (`magic/GateShape`, after vanilla's `PortalShape`)
- An open rectangle, 2–5 wide and 3–7 tall, with frame along all four sides. The corners are optional.
- It's found from any open cell inside.
- **Frame:** `#dreadpatch:dread_gate_frame`, which is Ice and Fire's `dread_stone`, `_bricks`, `_bricks_chiseled`, `_bricks_cracked`, `_bricks_mossy`, `_tile` and `dread_stone_face`.
- **Open:** air.

**The Mausoleum's ready-made gates**
- The far hall's ice forge shrine has four arches, each 3 wide and 6 tall inside, framed entirely in the tag's blocks.
- So every Mausoleum, old worlds' included, has four gates, and nothing of Ice and Fire's structure is changed or shipped.

**Opening it** (`entity/DreadGate.onUseKey`, on `PlayerInteractEvent.RightClickBlock`, both sides)
- **Trigger:** a `dread_key` used on a frame block, with air at the clicked face.
- **The plane:** a side face gives its axis; a top or bottom face tries X, then Z.
- **No gate there:** an action-bar hint.
- **A gate:** the event is cancelled with success.
- **On Peaceful:** a hint, and the key is kept. The queen is a `Monster` and wouldn't stay.
- **Otherwise, on the server:**
  - a `DreadGate` entity is made, holding the axis and the opener's side and UUID;
  - it fills the opening;
  - the key is used up (`ItemStack.consume`; kept in creative).

**The ritual** (`DreadGate`)
- **Saving:** the entity is saved. Summoned bare, it finds its gate round itself on either axis, or discards itself.
- **Panes:**
  - set with `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE`, as vanilla's `PortalShape` fills (otherwise the first pane sees open air and falls);
  - every tick it checks every pane is still there, or it shatters with nothing summoned.
- **At 100 ticks it summons a `DreadQueen`:**
  - through `EventHooks.finalizeMobSpawn` as `MobSpawnType.EVENT`, so she's the boss, with her dragon;
  - where an ancient Syndrigossa fits; else at the gate's foot, on foot (`DreadQueen.setWithDragon(false)`);
  - her target is the opener.
- **Checking room** (`dragonRoom`):
  - a probe Syndrigossa, never added, made ancient (`makeAncient`);
  - set on the ground its half-width + 1.5 out from the plane, the opener's side first;
  - tested with `level.noCollision`.
- **Afterwards:** it shatters (panes to air with `UPDATE_ALL`), leaving the frame.

**The pane** (`block/DreadGateBlock`)
- **Properties:** no item, no loot, no collision, light 11, unbreakable except in creative.
- **Model:** `HORIZONTAL_AXIS`, with the Nether portal's models and our texture.
- **Collapse:** `updateShape` turns it to air when a neighbour in its plane is neither pane nor frame, so a broken frame collapses it.
- **Cleanup:** a random tick with no `DreadGate` within 8 blocks clears it.

**The key's recipe** (`data/dreadpatch/recipe/dread_key.json`)
- `SBS / S / GG`: dread shards, a bone, `#c:ingots/gold`.
- Unlocked by holding a dread shard.
- The tooltip lines on Ice and Fire's key come from `DreadPatchClient`.

### Her model and rendering

**`client/DreadQueenModel`**
- **Origin:** Ice and Fire's unused `ModelDreadQueen` (LGPL-3.0, branch 1.18.2 of AlexModGuy/Ice_and_Fire), rebuilt as a vanilla `HumanoidModel`:
  - the same boxes and texture offsets;
  - extras (headware, chestplate, necklace, cloak, sleeves, robe) hanging off the vanilla parts;
  - an empty `hat`, because `HumanoidModel` requires one.
- **Her bust** is ours:
  - two 4×4×3 boxes on the chestplate, tipped forward and splayed, with a gap between them for the pendant;
  - in the chestplate's armor from rows 39–45 of her texture;
  - rows 32–38 hold the chestplate's cut-out straps and neckline, so faces taken from there would show holes.
- **The spawn animation:** Ice and Fire's `DreadBaseModel.animate` + `setRotationAnglesSpawn`, redone on vanilla parts in `client/RisePose`.

**Previewing a model change** before a release: render its quads offline.
- A throwaway test dumps a baked model's `ModelPart.render` through a recording `VertexConsumer`.
- A Python script rasterizes the quads with the texture.
- Otherwise a missing or misnamed part only shows up as a crash when a client loads.

**`client/DreadQueenRenderer`**
- A `HumanoidMobRenderer`, so held items come with it.
- Ice and Fire's texture, plus an `EyesLayer` for `_eyes`.
- `_inactive` and `_injured` aren't used yet.

### Her staff and sword

Both are Ice and Fire's items, changed through default components and events. The components come from `ModifyDefaultComponentsEvent`, so stacks already dropped get them too: a stack stores only changes.

**The staff** (`item/DreadQueenStaff`, `magic/PermafrostConduit`)
- **Spells:** `irons_spellbooks:spell_container` holds three of her spells (bolt, crown, Summon Court), locked at level 3: `create(3, true, false)`.
  - That puts them on the spell wheel, not "must equip".
  - "Must equip" would keep them off the wheel in hand (Iron's `SpellSelectionManager`).
- **Casting:** `casting_implement`. Iron's casts the selected spell on right-click with any item that has it.
- **Attributes:** in the main hand, +15% Ice Spell Power and +10% Cooldown Reduction (`ADD_MULTIPLIED_BASE`).
- **Locked in:** the spells can't be taken out, because the Inscription Table takes only `SpellBook`s.
- **The conduit:** on `SpellOnCastEvent` (players only, before the mana is taken):
  - an Ice spell, the staff in the main hand, standing on or in `#dreadpatch:permafrost` (`#minecraft:ice`, `#minecraft:snow`, and Ice and Fire's dragon ice and frozen ground);
  - costs 20% less;
  - Iron's checks the full cost before the cast.

**The sword** (`item/DreadQueenSword`, `magic/RimeCombo`, `effect/BrittleEffect`, `client/SwordGuardPose`)
- **Attributes:**
  - attack modifiers on vanilla's base ids (`Item.BASE_ATTACK_DAMAGE_ID`/`_SPEED_ID`), so tooltips add the player's base;
  - +0.5 `entity_interaction_range`;
  - `unbreakable`.
- **Thermodynamic Theft:** on `LivingDamageEvent.Pre` from a `player_attack` or `mob_attack`, sweeps included.
- **Glass-Rime Shatter:**
  - counted per wielder, per target: up to 16 targets, each count lapsing 100 ticks after its last strike;
  - until 0.10.1 it counted strikes in a row, which a Better Combat swing through a crowd reset.
  - Player strikes count only:
    - at ≥ 0.9 charge (NeoForge resets the charge after the attack, so it's still readable in `Pre`);
    - on the swing's own target, recorded in `AttackEntityEvent` (vanilla's sweep hurts others with the same damage type).
  - A target is chilled if it has Withering Rime or `TicksFrozen` > 0, checked before this strike's rime lands.
- **The guard:**
  - **Starting it:** `RightClickItem` starts using the sword, and `LivingEntityUseItemEvent.Start` makes the use last 72,000 ticks (a sword's is 0).
  - **Not when** the off hand `hasOwnUse`: a use animation, a `BlockItem`, a `ProjectileItem`, an ender pearl, or a casting implement.
  - **Our own blocking:** the use animation stays `NONE`, so vanilla's `isBlocking` is false. Ours rises 5 ticks after starting (`72000 - remaining`).
  - **What it stops:** it cancels `#dreadpatch:fire_drinking_guard` from the front, by vanilla's shield test, unless the damage `bypasses_shield`. The tag holds:
    - `#minecraft:is_fire`;
    - `#neoforge:is_magic`, with Iron's schools;
    - optional `iceandfire:dragon_fire`, which Ice and Fire doesn't tag as fire.
  - **Charging:** a stopped hit sets `dreadpatch:frost_charge` and a glint, and the next full strike shatters at once.
  - **The pose:** an `IClientItemExtensions`. Ice and Fire registers none for the sword, and a duplicate would throw, so ours is caught and logged.

**Better Combat** (in the owner's pack, and in the dev runs since 2026-11-16)
- Its server handler restores the attack charge before each target in a swing, and attacks each through `Player.attack`.
- So each target gets its own `AttackEntityEvent`, as the swing's own target.
- Its range is `entity_interaction_range` plus the weapon's.

### Rimefrost dragons and Syndrigossa

**The split (0.37.0):** `entity/RimefrostDragon` (`dreadpatch:rimefrost_dragon`) is the subtype; `entity/Syndrigossa` (`dreadpatch:syndrigossa`, its id kept so saved ones load) extends it.
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
  - the heart's drop rolls `.../heart` instead: shards and witherbones.
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

### The queen and her dragon

**Mounting** (`entity/SoulTether`)
- **When:** a queen spawned as the boss (`DragonAge.spawnsAncient`), or with `WithSyndrigossa:1b`.
- **On her first server tick:**
  - she raises a Syndrigossa, before Uranus sends her rise, which is then cleared;
  - bonds the two (UUIDs saved on each);
  - and rides it.
- **Seating:** Syndrigossa seats a `DreadQueenEntity` passenger at `getRiderPosition()` plus her height, instead of shaking her as prey. 0.24.0 left off the height and put her inside the dragon's body, out of reach.
- **Flying with her aboard:** takeoff, Shatter-Spine, and Ice and Fire's flight checks ignore a queen aboard (`carriesOthers`).

**In the saddle**
- Her `KeepDistanceGoal` stands down.
- She casts as on foot.
- Her court rises on the ground under her (`DreadCourt.findSpot`, searching 64 down).
- The two share prey every second.

**Getting down**
- **Forced landing:** at half its health, with her aboard, the dragon is forced down (`doesWantToLand`; flying turned to hovering; no takeoff).
- **On the ground:** she `leaveSaddle`s, and the dragon is grounded for 200 ticks.
- **Backstop:** at half her own health, or thrown off in the air, she Barrow-Steps to the ground, or gets Slow Falling if there's nowhere to land.
- She never remounts.

**The tether, and targets**
- **Tether:** her Rime siphon and its breath siphon heal half each, while the other lives within 28 blocks.
- **Dropping bad targets:** each tick, both drop a target they can't attack (a player gone creative, one of the Dread).
  - A target handed over by the other, or kept from before, isn't rechecked by a target goal.
  - The owner had Syndrigossa breathing at them in creative.

**Drawing her in the saddle** (`client/SaddleLayer`)
- **Where:**
  - at `SaddleSeat`, the top of `BodyUpper`'s cube, read from Ice and Fire's `icedragon.geo.json` (as `SaddleSeatTest` checks);
  - measured from the model origin, in the bone-turned pose GeckoLib hands the layer.
- **How:**
  - turned by the body yaw − 180, with the dragon's scale undone, and 0.6 down;
  - her shadow is off while she rides.
- **Elsewhere:** a `RenderLivingEvent.Pre` listener skips drawing her anywhere else while she rides.
- **History:** 0.24.0 used Ice and Fire's rider transform, and the owner saw her "hanging beneath the dragon".

### Dragon corpses (`entity/DragonCorpses`)

- **The bug:** `DragonBaseEntity.isAlive()` is `!isRemoved()` while `isModelDead()`, so a corpse counts as alive until it's removed. A mob whose target died kept it (`TargetGoal.canContinueToUse` saw it alive). Ice and Fire's `DreadAITargetNonDreadGoal` only skips corpses (`DragonUtils.isAlive`, which is `!isMobDead`) when picking a new one. In a probe a dread thrall still targeted the corpse 6 s on.
- **The corpse itself** (the owner's suggestion, for other mods' sake): each tick a corpse that isn't yet is made invulnerable (saved as `Invulnerable`).
  - `LivingEntity.canBeSeenAsEnemy` is `!isInvulnerable() && canBeSeenByAnyone()`, which `Mob.canAttack` and `TargetingConditions` use, so a running target goal drops it and nothing picks it.
  - Nothing else changes: Ice and Fire's `hurt` on a corpse takes only `fellOutOfWorld`, which bypasses invulnerability, and harvesting is `mobInteract`.
- **Any mob's target:** `LivingChangeTargetEvent` turns a corpse into no target, and every 10 ticks a mob holding one drops it. That covers targets handed over outside the goals (the queen's rally to her court).
- **Smoke test:** a dread thrall, then an iron golem (one at a time: a golem goes for a thrall), takes an ice dragon for its target. The dragon is killed into a corpse; the corpse is invulnerable, and the fighter has no target 1 s and 4 s on. With no target, `execute ... on target` prints nothing at all.

### Data

**Her loot table** (`data/dreadpatch/loot_table/entities/dread_queen.json`), the owner's list
- **Her sword and staff:** each its own pool under `minecraft:random_chance_with_enchanted_bonus` (looting: 25%, 30% at Looting I, +5% a level; vanilla's wither skeleton skull is the model).
- **Ink:** one, epic or legendary, at 23 : 10 (the odds of Iron's `great_ink`).
- **Scroll:** one, through `irons_spellbooks:randomize_spell` at quality 0.5–1, with `"spell_filter": {"spells": [...], "force": true}`.
  - **Add each new spell of hers to that list.**
  - Level = 1 + round(quality × (maxLevel − 1)).
  - The spell itself is picked weighted by rarity.
- No `killed_by_player` conditions yet.

**Entity type tags:** `#minecraft:undead`, `#minecraft:freeze_immune_entity_types`, and `#c:bosses` (so other mods treat her as a boss).

## Testing

### Unit tests

`./gradlew test`, part of `build`:
- **Rules:** one test per class in `magic/`.
- **Client and data:**
  - `UndeadSkinTest`;
  - `IceSpikeTest` (spike geometry, through a `VertexConsumer`);
  - `RisePoseTest`;
  - `SaddleSeatTest` and `SyndrigossaAnimationsTest` (against Ice and Fire's files on the test classpath).
- **`DreadQueenModelTest`:**
  - bakes her layer and builds the `HumanoidModel` from it, so a missing part fails here, not in a client;
  - checks that her bust's texels are opaque.
- **Limits:** game classes are on the test classpath (`addModdingDependenciesTo(sourceSets.test)`), but there's no running game.

### Dev server smoke test

**Running it**
- `tools/devserver/queen_smoke.py` (Python 3, stdlib) drives `./gradlew runServer` over RCON, and stops the server when done (`--keep` leaves it up).
- It needs `enable-rcon=true`, `rcon.password=<anything>` and `server-ip=127.0.0.1` in `run/server.properties` (gitignored; already set on the owner's machine).

**What it covers**
- **The queen:** her stats, items and loot, and her rise.
- **Magic:** Withering Rime and the siphon, and each spell, cast through Iron's `/cast` by armor stands.
- **Fighting:** the sword, her spacing and Barrow-Step, her court, decree, storm and lich.
- **Summon Court:** cast by a NoAI villager (an armor stand records no attacker, and the court defends its caster from what hurts it). The count by level; leaving a cow and a dread thrall be when they hit the caster or a member; the caster's hit cancelled; a husk that hits the caster attacked; a recast replacing the court; expiry (set through `data merge ... {NeoForgeData:{...}}`). With `attack_friendly_mobs` and `retaliate` switched on by hand, the court killed the cow within 4 s, so the check does catch the filter missing.
- **Rimefrost dragons:** a summoned one keeps its colour (sapphire), is undead with the ice dragon's tags, refuses Withering Rime and Wither, starts whole from a tag at Ice and Fire's health for its age (unchanged on Hard, unlike Syndrigossa's), and its corpse gives its own colour's scales and shards, its heart never her egg. Summoned without NBT it's a random age and ice colour (a summon with any NBT skips `finalizeSpawn`: a colourless hatchling).
- **Her drops:** at most one sword and one staff, one ink and one scroll a kill; over 40 rolls each of sword and staff 3–20 times; over four deaths of a queen saved with `HandDropChances` 2.0, not all eight.
- **Ice:** Rimewalk, and the storm's ice.
- **Syndrigossa:**
  - stats by difficulty, tags and effects;
  - corpse tables;
  - attacks, without her AI.
- **The pair:** the queen on her dragon (the tether, getting down).
- **The Dread Gate:** summoned in its frame, which it finds as the key would.

**What it learned**
- **`/reload` first:** Iron's builds its spell config on datapack sync (a player joining, or `/reload`). Before that, every spell reads max level 1.
- **World setup:** it sets noon, because dragons sleep at night and monsters spawn on dark platforms. It sets Easy, and puts the difficulty back at the end.
- **Command pitfalls:**
  - Commands under `execute` need `run`.
  - A check on a failed summon passes vacuously, so summons are checks too.
  - NBT tests go inside a selector's brackets.
  - `has_effect` stops the run on a malformed command, rather than reading it as a fail.
  - With no vehicle, `execute ... on vehicle` prints nothing at all.
- **Timing:**
  - An instant effect from `/effect` lands on the next tick, and so do held items' attributes.
  - NoAI mobs keep the `OnGround` they're summoned with.
  - A wild dragon ages a tick each tick.
- **Cleanup:** every queen's court is cleared with her (`clear_court`), or strays turn up in later checks.
- **The crown check** polls every 0.1 s. Her next bolt comes within 1.25 s of a crown, sometimes within a few ticks, and shatters it.
- **Shards:** once, a core's burst counted 8 shards (it makes 5–7), never explained. That check prints where they were.
- **The seat:** once (0.37.0), "she sits on its back" read her 14.7 blocks above her dragon's feet. The same setup repeated eight times seated her at 9.24, its dragon on the ground, and the next full run passed: likely her dragon stirring in the 0.3 s it has its AI before the test sets NoAI.
- **A court member:** "its caster can't hurt it" once read two different members (`limit=1` alone may pick another between reads); it now tags one and reads `/damage`'s own output.

**What it doesn't cover**
- **Needs a player:**
  - the key on a frame, its hint, Peaceful, the recipe and tooltips;
  - the Permafrost Conduit and the sword's guard;
  - Syndrigossa's corpse harvest and egg;
  - her bite's Withering Rime II (a bite needs her AI and a target in reach);
  - a summoned court following a moving player, and an iron golem leaving it be (the owner saw both in 0.33.0: "the court follows and ignores golems");
  - her hunting a player in the owner's pack.
- **Needs her AI in flight:** the forced landing from the air.

### Client

- **Ask first:** rendering has no automated check, and on the owner's machine `./gradlew runClient` opens a window on their desktop (there's no Xvfb). **Ask before launching it.**
- **Creative mode:** Iron's spends no mana and sets no cooldowns in creative (`CREATIVE_MANA_COST` / `CREATIVE_COOLDOWN`), so check those in survival.
- **The owner's checks:** they've checked each release in the dev client or their pack as it shipped, through 0.35.0 (2026-11-21: at 0.33.0 "everything works, the court follows and ignores golems"; at 0.34.0 "the boss bar works"; at 0.35.0 "everything works, they ignore the corpse now"). Their reports are behind the decisions above.
- **Never checked:** Fabulous graphics and Iris shader packs, for the storm and its fog.

## Working here

### Building and dependencies

**Build**
- `./gradlew build`.
- CI (`build.yml`) runs the same on every PR and every push to `main`, and uploads the jar, versioned `<mod_version>-dev.<commit>` (`-dev` locally).
- Only the Release workflow builds a plain `<mod_version>` jar.

**Dependencies**
- **Sources:** Ice and Fire, Uranus, Jupiter and GeckoLib come from Modrinth's maven (`maven.modrinth:<slug>:<version id>`).
  - They're pinned in `gradle.properties` to the exact files the pack ships.
  - Use version ids, not version numbers: Uranus 2.4.1 names both a NeoForge and a Fabric file.
- **As mods:** they're `implementation`, so the dev runs load them as mods, along with the jars bundled inside them (Architectury from Uranus, Integration from Ice and Fire).
- **Iron's Spells:** it and its required libraries (Iron's Lib, playerAnimator, Curios) come the same way.
- **Test-only mods** go in `localRuntime`: Better Combat 2.4.0, Better Mob Combat Reimagined 1.1.22, and Cloth Config 15.0.140.
  - They're the owner's files, from the untracked `compat_mods_testing_better_combat/`, pinned by the Modrinth version ids their SHA-1s resolve to.
  - The owner's pack export lists Reimagined 1.1.21.
  - The compat jars there for Cataclysm, Cataclysm: Spellbooks and Entity Model Features need mods the dev runs don't have, so they're left out.

### Dev runs and network

**Dev runs**
- **Server:** `./gradlew runServer` boots to `Done` with all 16 mods; the dev environment skips the EULA check.
- **Stopping:** `stop` over RCON, or SIGTERM to the java process. Both save the world.
- **Expected noise:**
  - after a `mod_version` change, a warning that the world's `dreadpatch` version changed;
  - Ice and Fire's `iceandfire:delight/*` recipes failing to parse (they need Farmer's Delight);
  - missing Jupiter config files on a fresh `run/`;
  - refmap warnings from GeckoLib, Iron's Spells, Iron's Lib and Curios;
  - `Not a list: null` whenever an ice dragon is summoned.
- **After changes:** grep the log for `FAILED during APPLY|was not applied`.

**Network:** a build needs:
- `maven.neoforged.net` and the Mojang hosts;
- `api.modrinth.com` and `cdn.modrinth.com`;
- `maven.parchmentmc.org`;
- the foojay JDK download.

### Pull requests and releases

**PRs**
- Into `main`, based on `main`. A PR based on another PR's branch merges into that branch, not `main` (#2 did).
- Bump `mod_version` with user-visible changes; merging the bump releases it. Keep the README in step.
- **Merging (since 2026-11-14):**
  - the owner wants each PR merged as soon as its CI passes, without waiting for their review;
  - with merge commits, as they merged #1 and #2.
- **One release at a time:** merge PRs that each bump the version one at a time, letting each Release run finish first. `release.yml`'s concurrency group cancels a pending run when another queues behind it, which would skip that version's release.

**Release**
- **Trigger:** every push to `main` runs `release.yml`.
- **What it does:** if `v<mod_version>` isn't released yet, it:
  - builds with `RELEASE=true`;
  - creates the tag on that commit;
  - publishes the jar, with notes GitHub generates from the merged PRs (`.github/release-notes.md`, if present, goes above them).
- **Otherwise** it stops after the check.
- **Retrying a failed release:** run it by hand on `main` (GitHub MCP `actions_run_trigger`, `release.yml`, ref `main`).
- **Safety:** it never replaces an existing release.
- **Tags:** sessions here can't push tags.

### The owner's other rules

**Shared CI**
- These files are identical in all the owner's mod repos (phantom_storage, golemarmor, friendlybeacons, catspells-nosumagro, gvs-noatk-players, dread-patch):
  - `.github/workflows/`: `build.yml`, `release.yml`, and `wiki.yml` where there's a wiki;
  - `.github/dependabot.yml`;
  - `.gitattributes`.
- phantom_storage's `CLAUDE.md` has the fullest notes.
- Change them in every repo at once, and keep repo-specific text out of them.

**Modrinth:** never publish anything there (a version, the description, gallery images) without the owner's consent, and no emoji in Modrinth text.

**Modpack exports:** the owner drops `.mrpack` exports of their pack in the repo root for reference (gitignored). The `modrinth.index.json` inside lists every mod with its Modrinth version id.

## The owner's pack

"The Living World" (1.3.6c, about 200 mods).

**Platform:** NeoForge 21.1.265.

**Ice and Fire**
- Ice and Fire CE 2.1-beta.1, plus `iceandfire_create`.
- Its `config/iceandfire/iaf-common.json`, in the export's overrides:
  - dragon `maxHealth` 1,000 and `attackDamage` 20;
  - breath 1.8 / 2.1 / 3.5 (the dev run's defaults are 2.0 / 2.5 / 3.5);
  - `neutralToPlayer` true, and `griefing` false.
- So a stage-5 ice dragon at 100 days has 808 health, 17 bite and 16.2 armor.
- The dev run keeps the defaults: `maxHealth` 500, `attackDamage` 17.

**Libraries:** Uranus 2.4.1, Jupiter 2.3.7, GeckoLib 4.9.3, Architectury 13.0.12.

**Iron's Spells:** 'n Spellbooks 1.21.1-3.16.3, with Iron's Lib 2.3.0, playerAnimator 2.0.4, Curios 9.5.1, and addons such as Cataclysm: Spellbooks.

**Other mods**
- JEI and Jade.
- L_Ender's Cataclysm 3.33.
- Better Combat 2.4.0, with `better_mob_combat_reimagined` (which gives mobs its attacks) and Cloth Config.
- AttributeFix: it raises attribute caps, but vanilla's armor math still caps the cut at 80%.

**Graphics**
- Iris 1.8.14-beta.1, Sodium 0.8.13, and ImmediatelyFast 1.6.15.
- Per phantom_storage's `CLAUDE.md`, the owner plays with Complementary shaders and with ImmediatelyFast's screen batching on, which broke screen overlays there.

**Debugging pack-only reports:** read a mod's code rather than running downloaded jars when a report only happens in the pack.
