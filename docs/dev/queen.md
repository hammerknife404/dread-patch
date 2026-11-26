# The Dread Queen

Part of the notes for agents working here: [CLAUDE.md](../../CLAUDE.md) has the rules and the index.

## The queen

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

## Her court and lich

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

**Scuttler climbing** (`entity/ScuttlerClimbing`; 0.41.0)
- Every `DreadScuttlerEntity` climbs walls as vanilla's spider does, done from outside (Ice and Fire's class):
  - as it joins a level, its `navigation` (a protected field, set by reflection: `ObfuscationReflectionHelper.findField(Mob.class, "navigation")`; mojmap at runtime) becomes a `WallClimberNavigation`, which heads straight for a target it has no ground path to;
  - each server tick it's against a wall (`horizontalCollision`; not riding, not NoAI) its upward motion is at least 0.2, a climbing spider's, and its fall distance is reset.
- Probed: from 6 blocks off a 4-block wall, up and on top by 3 s.

**Scuttler bolts** (`entity/ScuttlerBolts`)
- Every Ice and Fire `DreadScuttlerEntity` spits a weak bolt: 3 + 4%, and 2 s of rime.
- At its target 3–12 blocks off, in sight, every 60–80 ticks. The next time is kept in its persistent data.
- Not while it's rising.
- It runs from `EntityTickEvent.Post`, since the class is Ice and Fire's.

**Scuttler webs** (`entity/ScuttlerWebs`, `entity/ScuttlerWeb`, `block/DreadWebBlock`, `magic/WebRules`; 0.75.0)
- **The owner** (Feature_Dump_11-24): "Scuttler Web Shots: Implemented via temporary/self-cleaning web or snare projectiles so arenas and bases aren't permanently choked with cobwebs, tuned to avoid frustrating movement stunlock."
- **The shot:** every `DreadScuttlerEntity` (not rising) with a live target 4-24 blocks off in sight (0.75.1; 12 before), at most every 100-160 ticks (0.75.1; 200-300 before) (`dreadpatch:next_web`, persistent), not in a tick it spat a bolt (`NEXT_BOLT - now >= COOLDOWN`), and never at a target standing in a cobweb or dread web: a `ThrowableItemProjectile` drawn as a cobweb (`ThrownItemRenderer`), lobbed (velocity 1.1, aimed 12% of the distance high, spread 2), harmless.
- **Their reach and pace** (0.75.1, the owner: "engage via web spit much further away - 24 ... reduce the time inbetween the scuttlers spit to be 5 - 8 seconds. Increase their movement speed by 10%"): as each joins a level, transient modifiers raise its `follow_range` (Ice and Fire's 12) to 24, so it notices and hunts what's that far, and its speed by 10% (`ADD_MULTIPLIED_BASE`: 0.34 to 0.374). Its bolt keeps its 3-12.
- **Where it lands** (an entity: its block; a block: the face's neighbour): a `dreadpatch:dread_web` if that block is air and `EventHooks.canEntityGrief` (its scuttler's) allows.
- **The web:** no collision, no item, no loot, `PushReaction.DESTROY`, cobweb's sound; holds `makeStuckInBlock(0.4, 0.15, 0.4)` (a cobweb's 0.25, 0.05) on all but the Dread (`RisenDread.dread`); a scheduled tick 60 ticks after it's placed removes it (scheduled ticks are saved, so a reload doesn't keep it). Drawn as vanilla's cobweb through `dread_recolor` (cutout `cross`).

**Dread knights grown** (`entity/DreadKnights`, `client/DreadKnightCrouch`, `magic/KnightRules`; 0.75.0)
- **The owner** (Feature_Dump_11-24): "Scaled 50% larger, move faster, adjust step height to match size, and can duck into 2-block-tall hallways and still attack (keep bounding box, reach, and pathing honest so they don't hit through solid walls or wedge on doorways)"; then "should not be 50% faster, keep them the same speed or at most an 8th faster. They should also duck like a player to 'squeez' into lower areas."
- **Size and speed** (every `DreadKnightEntity`, as it joins a level; transient modifiers, so old ones grow too): `generic.scale` +50% (`ADD_MULTIPLIED_BASE`: Ice and Fire's 0.6 x 1.8 becomes 0.9 x 2.7, and its model, gear and melee box with it), `movement_speed` +12.5% (0.25 to 0.28125), `step_height` +0.4 (to 1.0: "to match size", read as a full block).
- **Crouching:** each server tick (standing or crouching, not riding) it crouches (`Pose.CROUCHING`) when its standing box, above its crouched height (1.8), would meet a block where it is or at its path's next node (else 0.6 along its motion), and stands when neither would. `EntityEvent.Size` gives a crouching knight a box 1.8 tall (eyes 0.85 of it). Its navigation (set by reflection, as the scuttlers') plans with a `WalkNodeEvaluator` whose `entityHeight` is its crouched height's (2), so it paths into 2-high passages.
- **Honest reach:** Ice and Fire's knight uses vanilla's `MeleeAttackGoal`, which strikes only within its (scaled) attack box and with a line of sight from its eyes; crouched, its eyes are lower too.
- **Drawn crouched** (`client/DreadKnightCrouch`): Ice and Fire's model can't be posed from outside, so a crouched knight leans 25° forward from its feet (about the axis up x its body's facing) and is squashed (`drawnSquash`) to stand 1.8 tall; pushed on `RenderLivingEvent.Pre` (lowest priority), popped on `Post` (only what was pushed).
- **Seen:** standing in a room, its eyes (2.3 up) are above a 2-high tunnel's roof, so it can't see a target down one: it follows one it already has (the smoke test provokes it) or wanders in.

**The Dread's hits and the one-source cap** (`effect/DreadHits`, `magic/DreadDebuffs`; 0.73.0)
- **The owner** (Feature_Dump_11-24): scuttlers "Low-chance roll for Withering Rime II (matching Syndrigossa's bite) plus a 2-second, non-stacking frost/freezing proc on bite (refreshes rather than stacking)"; heavy knights "Low-chance roll for Withering Rime"; "All Other Dread Mobs: Match the Dread Phantom's debuff odds"; "A player can only suffer one active source of Withering Rime or frost/rime debuffs at a time"; multi-target healing "open to debate/configuration".
- **The hit** (`LivingDamageEvent.Post`, damage over 0): a melee hit (`mob_attack` or `mob_attack_no_aggro`, its direct entity its attacker, not a projectile) by anything `RisenDread.dread` but the queen, Rimefrost dragons, dread phantoms and Rime Eels (their own rules). Two rolls, apart:
  - a `DreadScuttlerEntity`: Withering Rime II (amplifier 1) for 3 s at 8%; Rimefrost II (`HEAVY`) for 2 s at 35% (vanilla's `addEffect` renews it with a longer remaining time, never raises it);
  - a `DreadKnightEntity` (every hit: Ice and Fire's knight has no separate heavy attack): Withering Rime for 3 s at 8%; Rimefrost II for 3 s at 13% (0.73.1);
  - the rest: Withering Rime 3 s at 5%, Rimefrost II 5 s at 5% (0.73.1; 0.73.0 had the phantom's 15% and 35%).
- **The cap** (players only, `MobEffectEvent.Applicable` at low priority, after the Dread's own refusal): while a player has Withering Rime or Rimefrost, a laying by another source than the one recorded is `DO_NOT_APPLY`. The same source may renew it; the queen and Syndrigossa always land theirs (so a pack's never keeps her siphon from her). The source is recorded on `Added` (by UUID, per effect; weak map, server only) and forgotten when it's removed or runs out. A laying with no source (the weather, rimefire) is no one's: it always lands and doesn't change the record; a player with an effect and no record (after a relog) takes the next one.
- **Her siphon over several targets** (`RimeSiphon.feed`, the config's `[siphon]`): `siphon_targets` (0, every one) lets only that many feed her, the nearest her first; `siphon_split` (false) divides each target's heal by how many feed her, so together they heal her what one would. Counted over everyone in her level within 32 blocks carrying her Withering Rime.

## Her model and rendering

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

## Her staff and sword

Both are Ice and Fire's items, changed through default components and events. The components come from `ModifyDefaultComponentsEvent`, so stacks already dropped get them too: a stack stores only changes.

**The staff** (`item/DreadQueenStaff`, `magic/PermafrostConduit`)
- **Spells:** `irons_spellbooks:spell_container` holds three of her spells (bolt, crown, Summon Court), locked at level 3: `create(3, true, false)`.
  - That puts them on the spell wheel, not "must equip".
  - "Must equip" would keep them off the wheel in hand (Iron's `SpellSelectionManager`).
- **Casting:** `casting_implement`. Iron's casts the selected spell on right-click with any item that has it.
- **Attributes:** in the main hand, +15% Ice Spell Power, +5% Rimefrost Spell Power (0.51.0) and +10% Cooldown Reduction (`ADD_MULTIPLIED_BASE`).
- **Locked in:** the spells can't be taken out, because the Inscription Table takes only `SpellBook`s.
- **The conduit:** on `SpellOnCastEvent` (players only, before the mana is taken):
  - an Ice spell (or Rimefrost, since 0.51.0), the staff in the main hand, standing on or in `#dreadpatch:permafrost` (`#minecraft:ice`, `#minecraft:snow`, and Ice and Fire's dragon ice and frozen ground);
  - costs 20% less;
  - Iron's checks the full cost before the cast.

**Her staff's tooltip (`client/StaffTooltip`, 0.50.0):** with up to three spells Iron's (`ClientPlayerEvents.handleImbuedSpellTooltip`) writes each in full via `TooltipsUtils.formatActiveSpellTooltip`: a blank line, its name, its unique info, cast time, mana, cooldown, with its header inserted after the first blank line; four or more it lists a line each with Shift for the selected one. For her staff only, at `EventPriority.LOWEST`, that block (found by Iron's header key, its length recomputed from the same formatter) becomes a blank line, the header, a line a spell, and a "hold Shift" line; with Shift, each spell's details under its name, indented, no blank lines.

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

## The queen and her dragon

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

Drawing her in the saddle: [dragons.md](dragons.md), Riders.

## What her defeat stirs (`world/Escalation`, `client/DefeatStormClient`, `magic/EscalationRules`; 0.62.0)

- **The owner** (the dump's "Post-Defeat Overworld Escalation"; asked: the storm "should last about 30-60 seconds varying"): a defeat is her `LivingDeathEvent` (not cancelled) by anything but `generic_kill` (`/kill`) or `fell_out_of_world`, in any level (`EscalationRules.defeat`).
- **The Mausoleums stir:** the world's first defeat sets `DreadlandsData.queenFallen` (saved under `Escalation`, in the overworld's data) for good and tells every player (`message.dreadpatch.queen_fallen`). From then, in the overworld, on the Dreadlands' 200- and 2,400-tick rolls (`mausoleum_dread`, one in 12, ours; `patrols`), a player near a Mausoleum (`#dreadpatch:mausoleums` = Ice and Fire's `mausoleum`, `findNearestMapStructure` within 12 chunks, its chunk middle within 128 + 48; searched only after the roll comes up) gets 1-3 of the Dread, or a patrol, 24-48 off on ground (`DreadlandsSpawns.ground` with a filter) within 128 of its middle (the dump's "100-to-128": ours, 128), in `#c:is_cold` or `#c:is_snowy`, in a snow layer or on `#minecraft:snow`, `#minecraft:ice` or dreadstone (`#dreadpatch:dread_gate_frame`), outside any village piece (ours: Ice and Fire's Dread attack villagers). The Dread cap is `dread_cap`'s.
- **The defeat storm:** each defeat, if `defeat_storm_hours` (6, real time) have passed since the last one began (`DreadlandsData.lastDefeatStorm`), sets `defeatStormUntil` (overworld game time, so sleeping, which sets only the day's time, doesn't end it; no vanilla weather) 600-1,200 ticks on, and sends every client `DefeatStorm(until)` (also on login and on changing dimension). Every 20 ticks while it lasts, every overworld player (not spectating) with the sky over their eyes, in a cold or snowy biome (unless `defeat_storm_cold_only` is off), not in her full regalia (with `regalia_eases_weather`), gets 60 ticks of Rimefrost I, ambient. Nothing else: no damage, freezing, raising or griefing.
- **On the client** (`DefeatStormClient`, the same test, eased over 40 ticks): fog to 48 (never further than it was) in ash grey × the sky's light, unless her crown's sight clears storms; snowflakes blown along the ground on the Dreadlands' daily wind (5 a tick within 14 blocks); and `DreadlandsAmbience` in its defeat-storm mode, its loop and ice cracking only (no rumbles, whispers or roars), not while a Whiteout Dominion's wind plays (the dump's guard against "duplicate wind tracks").
- **`/dreadpatch escalation [storm [seconds] | reset]`.**

## Her loot table and tags

**Her loot table** (`data/dreadpatch/loot_table/entities/dread_queen.json`), the owner's list
- **Her sword and staff:** each its own pool under `minecraft:random_chance_with_enchanted_bonus` (looting: 25%, 30% at Looting I, +5% a level; vanilla's wither skeleton skull is the model).
- **Ink:** one, epic or legendary, at 23 : 10 (the odds of Iron's `great_ink`).
- **Scroll:** one, through `irons_spellbooks:randomize_spell` at quality 0.5–1, with `"spell_filter": {"spells": [...], "force": true}`.
  - Every spell of hers is in it (keep it so: CLAUDE.md, Her spells); the lesser ones are in the spire summit chest's too (`chests/spire_summit`, 0.53.0).
  - Level = 1 + round(quality × (maxLevel − 1)).
  - The spell itself is picked weighted by rarity.
- No `killed_by_player` conditions yet.

**Entity type tags:** `#minecraft:undead`, `#minecraft:freeze_immune_entity_types`, and `#c:bosses` (so other mods treat her as a boss).
