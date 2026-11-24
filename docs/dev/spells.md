# Her spells and the Rimefrost school

Part of the notes for agents working here: [CLAUDE.md](../../CLAUDE.md) has the rules and the index.

Her spells (levels 1–5; Create Rimefrost Dragon and Summon Dread Phantoms have one; Rimefire Blast, 1–10) are of the **Rimefrost** school since 0.51.0 (Ice before; below). They're kept out of random loot (`allowLooting()` false) and, but for Rimefire Blast, the Scroll Forge (`setAllowCrafting(false)`); her loot drops them all, named with `"force": true`.

**The Rimefrost school** (`spell/RimefrostSchool`, `spell/DreadSpells`; 0.51.0, the owner: "a new spell school, subtype of ice")
- An Iron's `SchoolType` registered in `SchoolRegistry.SCHOOL_REGISTRY_KEY` as `dreadpatch:rimefrost`: our attributes `dreadpatch:rimefrost_spell_power` / `_magic_resist` (Iron's `MagicPercentAttribute`, 1.0 = none, synced; added to every entity type on `EntityAttributeModificationEvent`, as Iron's adds its own), Iron's Ice cast sound, its focus `#dreadpatch:rimefrost_focus` (a dread shard), its damage type `dreadpatch:rimefrost_magic` (in `#neoforge:is_magic`, so the sword's guard stops it), her cyan.
- **A subtype of Ice:** its `getPowerFor` and `getResistanceFor` multiply Rimefrost's attribute by Ice's, so Ice gear helps; `RimefrostSchool.isIce` (Ice or Rimefrost) is what the Permafrost Conduit and the regalia's mana discount check.
- **Iron's per-world spell config stores each spell's school** (`ServerConfigs`: a `School` entry per spell, defaulting to its `DefaultConfig`), so worlds made before 0.51.0 hold "ice" for her old spells. Each of her spells therefore overrides `getSchoolType` to return Rimefrost whatever the config says.
- **The Scroll Forge's focus slot takes only `#irons_spellbooks:school_focus`** (Iron's lists its schools' focus tags in it): 0.51.0 left Rimefrost's out and the owner's dread shard wouldn't go in; since 0.51.1 `data/irons_spellbooks/tags/item/school_focus.json` adds `#dreadpatch:rimefrost_focus`.
- **Iron's wants item models for every school** (0.51.1): `<ns>:item/scroll_<school>` and `<ns>:item/affinity_ring_<school>`. Missing, 0.51.0's Rimefrost scrolls (and any Rimefrost affinity ring) drew as the missing texture; the client log said `Unable to load model: 'dreadpatch:item/scroll_rimefrost'`. Ours are Iron's Ice ones recoloured by the `dreadpatch:dread_recolor` sprite source (`assets/minecraft/atlases/blocks.json`), nothing of Iron's copied. After a client run, grep its log for `Unable to load model: 'dreadpatch:`.
- **Iron's `/cast` on a mob doesn't fire a continuous spell** (its own Cone of Cold neither): the smoke test summons Rimefire Blast's cone with an `Owner` instead.

**Rimefire Blast** (`spell/RimefireBlastSpell`, `entity/RimefireBlastProjectile`; 0.51.0): Iron's Cone of Cold remade: continuous, levels 1–10, 12 s cooldown, mana 5 + 1/level, its cone an `AbstractConeProjectile` (Iron's: follows the caster's look, four parts to about 7 blocks; each later cast tick sets `dealDamageActive`). Damage 0.5 + 0.4 × spell power (Cone of Cold's 1 + 0.75: "low"), and every hit renews 60 ticks of Withering Rime (its owner the caster, so only a queen's feeds). Particles: our small rime flame and soot, snowflakes. Death message `death.attack.dreadpatch.rimefire_blast`. The one spell of hers the Scroll Forge makes (dread shard focus, any ink from common); also in her loot. Its cone draws nothing (`NoopRenderer`).

**Death messages:** a spell's damage source (`getDamageSource`) dies as `death.attack.<spell id>` (Iron's `SpellDamageSource`: victim, then caster). The bolt's (also used by Beater's Decree) and the crown's (its shards) are in our lang, from 0.33.1; until then a death showed the raw key.

**Withering Rime-Bolt** (`entity/WitheringRimeBolt`, `spell/WitheringRimeBoltSpell`)
- **What it is:** an Iron's `AbstractMagicProjectile`. Flat damage plus a share of max health go through Iron's `DamageSources.applyDamage`, so Ice resistance counts; then it applies Withering Rime.
- **Numbers:**
  - hers: 8 + 5% (8% on Hard), and 4.5 s of rime (15% until 0.51.0);
  - the spell: (6 + 2/level) + (2 + level)% (`RimeBoltRules`: 3% at level 1, 7% at 5; 8% flat until 0.51.0).
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
- **Its caster** is resolved from its `CommanderUUID` (`SummonedCourt.commander`), not Ice and Fire's `getCommander`: **its scuttler's always returns null** (as its lich's does), so until 0.42.0 a scuttler of a summoned court followed and defended no one and its caster could hurt it. The smoke test checks a scuttler member every time.
- **Friendly mobs leave it be:** while `attack_friendly_mobs` is off, a mob that isn't hostile, a player or Dread can't take a member as its target (an iron golem goes for any `Enemy`).
- **Its caster's side can't hurt it** (`LivingIncomingDamageEvent`; Iron's friendly fire only knows `IMagicSummon`), and the caster's crown and storm skip it (`RimefireCrown.canHarm`). `/kill` still works.
- **Config:** `DreadPatchConfig`, a NeoForge server config, written to `config/dreadpatch-server.toml`; `courtTerms()` gives the defaults before it loads.

**Create Rimefrost Dragon** (`spell/CreateRimefrostDragonSpell`, `magic/RimefrostRaising`; 0.38.0)
- **The spell:** Rimefrost (Ice until 0.51.0), legendary, one level, 500 mana, `RANGE` 24. Scroll only (her loot list). It raises an Ice and Fire ice dragon corpse (`isCorpse`: an `IceDragonEntity`, not a Rimefrost, `isModelDead`) as a Rimefrost dragon where it lay: `setVariant`, `setGender`, `setAgeInTicks` copied, `refreshDirtyAttributes`, health `RimefrostRaising.risingHealth` (half the caster's missing health, floored, at least 1, at most its max), `RaisedBy` saved, persistent; a player's `tame(player)`. The corpse is discarded. `dismiss` first crumbles every Rimefrost the caster raised in the level (one at a time). A raised Rimefrost's `getCommander` is its raiser unless that's a player, and every second it takes its commander's target.
- **Targeting:** players go through `checkPreCastConditions`, Iron's `Utils.preCastTargetHelper` with an `isCorpse` filter (its "no target" hint; nothing spent). It resolves a dragon's `PartEntity` to its parent, and doesn't mind a corpse being invulnerable. Its ray stops at the first entity it meets: a living dragon in the way means no target. Casters without Iron's pre-cast (`/cast` on a mob) use `findCorpse`, a `RaycastBuilder` with the same filter.
- **Mana, window and cooldown** (Iron's recast system, the only way to cast during a cooldown):
  - `castSpell` takes the mana before `onCast` unless a recast is open; a recast is free and skips the cooldown check.
  - The first cast (no recast open) sets the player's mana to 0 (`SyncManaPacket`) and opens a `RecastInstance` (`getRecastCount` 2, `WINDOW_TICKS` 3,600, `WindowData`, an empty `ICastDataSerializable`). Iron's then decrements it to 1.
  - A recast raises the new corpse (sending the last back) and uses it up. The cooldown starts in `onRecastFinished` (Iron's would add the full one): ours adds only `getTicksRemaining()` of the window (`addCooldown` + `SyncCooldownPacket`), none on a timeout. So it's 3 minutes from the first cast either way.
  - **Iron's refuses to cast a spell with recasts from a scroll** (`cast_error_scroll`), so it's inscribed and cast from a spellbook.
- **The queen** (`DreadQueen.raiseDragon`, in `customServerAiStep`): free. With a target, `FIRST_RAISE` 200 ticks in, then `RAISE_COOLDOWN` 3,600 (retrying every 20), once she's at or under `QUEEN_RAISES_BELOW` 75% health, and with no Rimefrost of hers alive within 128: the nearest corpse within 32.

## Whiteout Dominion

**The storm** (`entity/WhiteoutDominion`, `magic/WhiteoutStorm`, `effect/WhiteoutEffect`, `spell/WhiteoutDominionSpell`)
- **The entity:** never saved; a 1×1 point whose culling box is its whole cylinder.
- **Its space:** a cylinder of `Radius` round where it was raised, from 16 below to 48 above.
- **Every 10 ticks:** everyone inside it may harm gets 40 ticks of Whiteout: speed −20%, follow range −50%. The Dread refuse it.
- **Bosses keep their sight (0.60.0;** the owner's dump: verify it "does not cause large external melee bosses (#c:bosses, such as Cataclysm bruisers) to drop aggro prematurely inside the 32-block dome"**):** it did (a `TargetGoal` drops a target past follow range), so `WhiteoutEffect.onEffectStarted` takes the follow-range modifier off anything in `#c:bosses`. Vanilla's `addEffect` calls it after every application, renewals too, just after putting the modifiers back; the slow stays. The whiteout weather's Whiteout is the same effect.
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
- **Laid and held with `UPDATE_CLIENTS` only.** Patchy ice laid with neighbour updates melts as fast as it's laid (frosted ice's `neighborChanged`: [queen.md](queen.md), Frosted ice).
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
