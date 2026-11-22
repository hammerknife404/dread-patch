"""Smoke test for the Dread Queen against a running dev server, over RCON. Python 3, stdlib only.

Setup once, in run/server.properties (gitignored): enable-rcon=true, rcon.password=<anything>,
server-ip=127.0.0.1. Then start `./gradlew runServer`, wait for "RCON running", and run
`python3 tools/devserver/queen_smoke.py`. It prints PASS/FAIL per check, stops the server when it
is done (pass --keep to leave it running) and exits non-zero if anything failed.
"""
import pathlib
import re
import socket
import struct
import sys
import time

PROPERTIES = pathlib.Path(__file__).resolve().parents[2] / "run" / "server.properties"


def server_properties():
    props = {}
    for line in PROPERTIES.read_text().splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


class Rcon:
    def __init__(self, host, port, password):
        self.sock = socket.create_connection((host, port), timeout=10)
        self.request = 0
        if self._send(3, password)[0] == -1:
            raise SystemExit("RCON login failed: check rcon.password in " + str(PROPERTIES))

    def _send(self, kind, body):
        self.request += 1
        data = struct.pack("<ii", self.request, kind) + body.encode() + b"\0\0"
        self.sock.sendall(struct.pack("<i", len(data)) + data)
        size = struct.unpack("<i", self._read(4))[0]
        request, _ = struct.unpack("<ii", self._read(8))
        return request, self._read(size - 8)[:-2].decode("utf-8", "replace")

    def _read(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("RCON connection closed")
            buf += chunk
        return buf

    def run(self, command):
        return self._send(2, command)[1]


def main():
    props = server_properties()
    if props.get("enable-rcon") != "true":
        raise SystemExit("Set enable-rcon=true (and rcon.password) in " + str(PROPERTIES) + ", then restart the server")
    rcon = Rcon(props.get("server-ip") or "127.0.0.1", int(props.get("rcon.port", "25575")), props.get("rcon.password", ""))
    results = []

    def check(label, command, expect):
        out = rcon.run(command)
        ok = expect in out
        results.append(ok)
        print(("PASS  " if ok else "FAIL  ") + label + ("" if ok else "\n      > " + command + "\n      < " + out.strip()))

    def record(label, ok, detail):
        results.append(ok)
        print(("PASS  " if ok else "FAIL  ") + label + ("" if ok else "\n      " + detail))

    def count(item):
        out = rcon.run("execute if items entity @e[type=item] contents " + item)
        return int(out.rsplit(" ", 1)[1]) if "Test passed, count:" in out else 0

    # A scroll the loot function wrote a spell on; a blank scroll must not count (checked below).
    SPELL_SCROLL = "irons_spellbooks:scroll[irons_spellbooks:spell_container]"
    HER_SPELLS = {"dreadpatch:withering_rime_bolt", "dreadpatch:rimefire_crown", "dreadpatch:barrow_step", "dreadpatch:whiteout_dominion",
                  "dreadpatch:summon_court", "dreadpatch:create_rimefrost_dragon", "dreadpatch:summon_dread_phantoms"}
    # Her staff as an Iron's staff: three of her spells locked in at level 3, and right-click casting.
    STAFF_WITH_SPELLS = ('iceandfire:dread_queen_staff[irons_spellbooks:spell_container={maxSpells:3,spellWheel:1b,mustEquip:0b,data:['
                         '{id:"dreadpatch:withering_rime_bolt",index:0,level:3,locked:1b},{id:"dreadpatch:rimefire_crown",index:1,level:3,locked:1b},'
                         '{id:"dreadpatch:summon_court",index:2,level:3,locked:1b}]},'
                         'irons_spellbooks:casting_implement={}]')

    def health(selector):
        out = rcon.run("data get entity " + selector + " Health")
        m = re.search(r"entity data: ([0-9.]+)f", out)
        return float(m.group(1)) if m else -1.0

    court_types = ["iceandfire:dread_thrall", "iceandfire:dread_ghoul", "iceandfire:dread_knight", "iceandfire:dread_scuttler"]

    def clear_court():
        # Every queen raises a court (and her storm raises the dead into it); clear it, and her storm,
        # with her so they can't wander into later checks.
        for kind in court_types + ["iceandfire:dread_beast", "iceandfire:dread_horse", "iceandfire:dread_lich", "dreadpatch:whiteout_dominion", "dreadpatch:dread_gate",
                                   "dreadpatch:dread_phantom"]:
            rcon.run(f"kill @e[type={kind}]")

    def has_effect(selector, effect):
        # The NBT test goes inside the selector's own brackets: "@e[tag=x,limit=1]" -> "@e[tag=x,limit=1,nbt=...]".
        with_nbt = selector[:-1] + f',nbt={{active_effects:[{{id:"{effect}"}}]}}]'
        out = rcon.run("execute if entity " + with_nbt)
        if "Test passed" not in out and "Test failed" not in out:
            raise SystemExit("has_effect: bad command: " + out)
        return "Test passed" in out

    def check_loot(where):
        # Her loot table (0.33.0, the owner's numbers): her sword and her staff, each at 25% (at most
        # one each: the ones she holds never drop too), one ink, epic or legendary, and one scroll
        # with a spell on it. Returns how many swords and staffs dropped.
        gear = {}
        for item in ("iceandfire:dread_queen_sword", "iceandfire:dread_queen_staff"):
            gear[item] = count(item)
            record(where + ": at most one " + item.split(":")[1], gear[item] <= 1, f"found {gear[item]}")
        epic, legendary = count("irons_spellbooks:epic_ink"), count("irons_spellbooks:legendary_ink")
        record(where + ": one epic/legendary ink", epic + legendary == 1, f"epic {epic}, legendary {legendary}")
        scrolls = count("irons_spellbooks:scroll")
        with_spell = count(SPELL_SCROLL)
        record(where + ": one scroll, with a spell", scrolls == 1 and with_spell == 1, f"{scrolls} scrolls, {with_spell} with a spell")
        ids = re.findall(r'entity data: "([a-z_:]+)"', rcon.run(
            'execute as @e[type=item] if items entity @s contents irons_spellbooks:scroll run data get entity @s '
            'Item.components."irons_spellbooks:spell_container".data[0].id'))
        record(where + ": the scrolls hold her spells", bool(ids) and all(i in HER_SPELLS for i in ids), f"spells {ids}")
        staffs = count(STAFF_WITH_SPELLS)
        record(where + ": her staff holds her spells", staffs == gear["iceandfire:dread_queen_staff"],
               f"{staffs} of {gear['iceandfire:dread_queen_staff']} staffs with her three spells locked in at level 3")
        # Her regalia (0.43.0) comes only from a player's kill, never from her table or a /kill.
        regalia = sum(count(f"dreadpatch:dread_queen_{piece}") for piece in ("crown", "bodice", "robe", "greaves"))
        record(where + ": no regalia without a player's kill", regalia == 0, f"{regalia} pieces")
        rcon.run("kill @e[type=item]")
        return gear["iceandfire:dread_queen_sword"] + gear["iceandfire:dread_queen_staff"]

    queen = "@e[type=dreadpatch:dread_queen,limit=1]"
    surface = "execute positioned 0 0 0 positioned over world_surface run "

    # Iron's Spells builds its spell config (max levels and so on) when datapacks sync to players; on a
    # server nobody has joined every spell reads max level 1. A reload syncs it.
    rcon.run("reload")
    time.sleep(3)
    # By day: at night monsters spawn on the dark stone platforms and draw her off her targets (her crown
    # check failed in alternate runs as the dev world's clock went round), and dragons sleep.
    rcon.run("time set noon")
    rcon.run("weather clear")
    # On Easy, as the dev run is (Syndrigossa's health is by difficulty: 500 at full strength on Easy);
    # put back at the end.
    first_difficulty = re.search(r"The difficulty is (\w+)", rcon.run("difficulty"))
    rcon.run("difficulty easy")
    # Chunks x 0-1, z 0: the platforms in the sky, the longest 30 blocks.
    rcon.run("forceload add 0 0 31 15")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    clear_court()
    rcon.run("kill @e[type=item]")

    check("summons", surface + "summon dreadpatch:dread_queen ~ ~ ~", "Summoned new Dread Queen")
    # Her own numbers, in place of Ice and Fire's 750 health and 30 armor.
    check("300 health", "data get entity " + queen + " Health", "300.0f")
    check("armor 20", "attribute " + queen + " minecraft:generic.armor get", "is 20.0")
    check("toughness 12", "attribute " + queen + " minecraft:generic.armor_toughness get", "is 12.0")
    check("holds her sword", "data get entity " + queen + " HandItems", "iceandfire:dread_queen_sword")
    check("holds her staff", "data get entity " + queen + " HandItems", "iceandfire:dread_queen_staff")
    for tag in ("#minecraft:undead", "#minecraft:freeze_immune_entity_types", "#c:bosses"):
        check("in " + tag, "execute as " + queen + " if entity @s[type=" + tag + "]", "Test passed")
    time.sleep(5)  # 100 ticks of her AI
    check("alive after 5 s of AI", "execute if entity @e[type=dreadpatch:dread_queen]", "Test passed, count: 1")
    # Her sword as the chat has it: 11.5 attack damage in a player's hand (their base 1 + 10.5), so
    # her own 5 + 10.5; and unbreakable.
    check("her sword: +10.5 attack damage", "attribute " + queen + " minecraft:generic.attack_damage get", "is 15.5")
    check("her sword: unbreakable", "execute if items entity " + queen + " weapon.* iceandfire:dread_queen_sword[minecraft:unbreakable={}]",
          "Test passed")

    rcon.run(surface + 'summon item ~ ~2 ~ {Item:{id:"irons_spellbooks:scroll",count:1}}')
    record("a blank scroll doesn't count as having a spell", count(SPELL_SCROLL) == 0, "the spell check can't tell blank scrolls apart")
    rcon.run("kill @e[type=item]")
    check("loot table rolls", surface + "loot spawn ~ ~2 ~ loot dreadpatch:entities/dread_queen", "Dropped")
    check_loot("loot table")
    # Scrolls at quality 0.5-1: the upper half of each spell's levels (a spell that only has level 1
    # stays 1), so over five rolls some scroll should be well above 1.
    for _ in range(5):
        rcon.run(surface + "loot spawn ~ ~2 ~ loot dreadpatch:entities/dread_queen")
    out = rcon.run('execute as @e[type=item] if items entity @s contents irons_spellbooks:scroll run data get entity @s '
                   'Item.components."irons_spellbooks:spell_container".data[0].level')
    levels = sorted(int(n) for n in re.findall(r"entity data: (\d+)", out))
    record("scroll spells reach high levels", bool(levels) and levels[-1] >= 3, f"levels {levels}")
    rcon.run("kill @e[type=item]")
    # Her sword and staff at 25% each: over 40 rolls each turns up sometimes, never every time (each
    # count is 10 on average; outside 3-20 is about 1 in 1,000), and her staff always holds her spells.
    for _ in range(40):
        rcon.run(surface + "loot spawn ~ ~2 ~ loot dreadpatch:entities/dread_queen")
    swords, staffs = count("iceandfire:dread_queen_sword"), count("iceandfire:dread_queen_staff")
    record("her sword and staff drop about 1 time in 4", 3 <= swords <= 20 and 3 <= staffs <= 20, f"over 40 rolls: {swords} swords, {staffs} staffs")
    record("every staff she drops holds her spells", count(STAFF_WITH_SPELLS) == staffs, f"{count(STAFF_WITH_SPELLS)} of {staffs}")
    rcon.run("kill @e[type=item]")

    check("dies", "kill @e[type=dreadpatch:dread_queen]", "Killed Dread Queen")
    time.sleep(1.5)
    check_loot("death drops")

    # A queen whose saved drop chances say her held items always drop (2.0) still drops them only from
    # her loot table, never from her hands: over four such deaths not every one drops both (with her
    # hands dropping, every one would; from her table alone, all eight is 1 in 65,536).
    held = ('{HandItems:[{id:"iceandfire:dread_queen_sword",count:1},{id:"iceandfire:dread_queen_staff",count:1}],'
            'HandDropChances:[2.0f,2.0f]}')
    dropped = 0
    for i in range(4):
        check("summons with drop chances 2.0", surface + "summon dreadpatch:dread_queen ~ ~ ~ " + held, "Summoned new Dread Queen")
        rcon.run("kill @e[type=dreadpatch:dread_queen]")
        clear_court()
        time.sleep(1.5)
        dropped += check_loot(f"forced hand drops {i + 1}")
    record("her held sword and staff never drop from her hands", dropped < 8, f"{dropped} of 8 over four deaths")

    # A dispenser only fires a spawn egg that knows its entity type.
    rcon.run("setblock 4 200 4 minecraft:dispenser[facing=up]")
    check("egg goes in a dispenser", "item replace block 4 200 4 container.0 with dreadpatch:dread_queen_spawn_egg", "Replaced")
    rcon.run("setblock 4 199 4 minecraft:redstone_block")
    time.sleep(1.5)
    check("dispensed egg spawns her", "execute if entity @e[type=dreadpatch:dread_queen]", "Test passed, count: 1")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    # The Ancient Syndrigossa egg (her egg carrying Ancient:1b, NoAI here to keep her put): the data
    # merges into the dragon the egg made at a random age, so she must come out ancient and whole.
    rcon.run("setblock 4 199 4 minecraft:air")
    check("an ancient Syndrigossa egg goes in a dispenser", "item replace block 4 200 4 container.0 with dreadpatch:syndrigossa_spawn_egg"
          '[entity_data={id:"dreadpatch:syndrigossa",Ancient:1b,NoAI:1b}]', "Replaced")
    rcon.run("setblock 4 199 4 minecraft:redstone_block")
    time.sleep(1.5)
    hatched = "@e[type=dreadpatch:syndrigossa,limit=1]"
    m_age = re.search(r"entity data: (\d+)", rcon.run("data get entity " + hatched + " AgeTicks"))
    m_max = re.search(r"is ([0-9.]+)", rcon.run("attribute " + hatched + " minecraft:generic.max_health get"))
    egg_health = health(hatched)
    record("the ancient egg's dragon is ancient and whole", m_age is not None and int(m_age.group(1)) >= 125 * 24000
           and m_max is not None and egg_health == float(m_max.group(1)) == 500.0,
           f"AgeTicks {m_age and m_age.group(1)}, health {egg_health} of {m_max and m_max.group(1)}")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")

    # Her staff in the main hand: +15% Ice Spell Power, +10% Cooldown Reduction, as Iron's staffs give
    # theirs (and, like theirs, nothing from the off hand). Held items' attributes apply on the
    # holder's next tick, so wait for it.
    holder = "@e[tag=staff_holder,limit=1]"
    check("summons a staff holder", surface + 'summon minecraft:armor_stand ~2 ~ ~ {Tags:["staff_holder"],'
          'HandItems:[{id:"iceandfire:dread_queen_staff",count:1},{}]}', "Summoned new Armor Stand")
    time.sleep(0.5)
    check("her staff: +15% Ice Spell Power", "attribute " + holder + " irons_spellbooks:ice_spell_power get", "is 1.15")
    check("her staff: +10% Cooldown Reduction", "attribute " + holder + " irons_spellbooks:cooldown_reduction get", "is 1.1")
    rcon.run("item replace entity " + holder + " weapon.offhand with iceandfire:dread_queen_staff")
    rcon.run("item replace entity " + holder + " weapon.mainhand with minecraft:air")
    time.sleep(0.5)
    check("her staff: nothing from the off hand", "attribute " + holder + " irons_spellbooks:ice_spell_power get", "is 1.0")
    rcon.run("kill @e[tag=staff_holder]")
    clear_court()
    rcon.run("kill @e[type=item]")
    rcon.run("setblock 4 199 4 minecraft:air")
    rcon.run("setblock 4 200 4 minecraft:air")

    # Syndrigossa (proof of concept): an Ice and Fire ice dragon underneath, always white (our renderer
    # paints over it), one of the Dread: no Withering Rime, and she and the queen leave each other be.
    # (Ice and Fire logs "Not a list: null" when any ice dragon is summoned; its own does too.)
    dragon = "@e[type=dreadpatch:syndrigossa,limit=1]"
    check("Syndrigossa summons", surface + "summon dreadpatch:syndrigossa ~8 ~ ~", "Summoned new Syndrigossa")
    check("she's a white ice dragon underneath", "data get entity " + dragon + " Variant", '"white"')
    check("she's one of the Dread: no Withering Rime", "effect give " + dragon + " dreadpatch:withering_rime 5", "Unable to apply")
    time.sleep(6.0)
    check("she lives through 6 s of AI", "execute if entity @e[type=dreadpatch:syndrigossa]", "Test passed, count: 1")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")
    # Summoned full-grown from a tag (which skips finalizeSpawn), she starts whole: Ice and Fire's own
    # ice dragons start at a hatchling's 20 of their 419 and fall to one warden hit.
    rcon.run(surface + 'summon dreadpatch:syndrigossa ~8 ~ ~ {AgeTicks:2500000}')
    time.sleep(0.5)
    grown = re.search(r"is ([0-9.]+)", rcon.run("attribute " + dragon + " minecraft:generic.max_health get"))
    record("summoned full-grown, she starts at full health", grown is not None and float(grown.group(1)) > 100 and health(dragon) == float(grown.group(1)),
           f"health {health(dragon)} of {grown and grown.group(1)}")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")

    def age_ticks(selector):
        m = re.search(r"entity data: (\d+)", rcon.run("data get entity " + selector + " AgeTicks"))
        return int(m.group(1)) if m else -1

    def max_health(selector):
        m = re.search(r"is ([0-9.]+)", rcon.run("attribute " + selector + " minecraft:generic.max_health get"))
        return float(m.group(1)) if m else -1.0

    # Summoned by hand she gets Ice and Fire's random age (1-80 days) and grows as an ice dragon does;
    # the boss is ancient: 125 days, Ice and Fire's full strength (its maxHealth, 500 by default as in
    # the dev run; 1,000 in the owner's pack), and whole.
    ANCIENT_TICKS = 125 * 24000
    rcon.run(surface + "summon dreadpatch:syndrigossa ~8 ~ ~")
    plain_age = age_ticks(dragon)
    record("summoned by hand, she's Ice and Fire's random age", 0 < plain_age <= 80 * 24000, f"AgeTicks {plain_age}")
    check("and not ancient", "data get entity " + dragon + " Ancient", "0b")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")
    rcon.run(surface + "summon dreadpatch:syndrigossa ~8 ~ ~ {NoAI:1b,Ancient:1b}")
    old_age, old_max, old_health = age_ticks(dragon), max_health(dragon), health(dragon)
    # (Dragons age a tick each tick, NoAI or not, so the read may come a tick or two later.)
    record("ancient, she's 125 days old", ANCIENT_TICKS <= old_age < ANCIENT_TICKS + 200, f"AgeTicks {old_age}")
    record("at full strength (Easy's 500)", old_max == 500.0, f"max health {old_max}")
    record("and whole", old_health == old_max, f"health {old_health} of {old_max}")
    # Her health is hers by difficulty (the owner's: 500 Easy, 750 Normal, 1,000 Hard at full strength),
    # and follows a change of difficulty, keeping her share of it (NoAI or not: it's her aiStep).
    rcon.run("difficulty hard")
    time.sleep(0.3)
    hard_max, hard_health = max_health(dragon), health(dragon)
    record("on Hard she has 1,000, whole", hard_max == 1000.0 and hard_health == 1000.0, f"health {hard_health} of {hard_max}")
    rcon.run("data merge entity " + dragon + " {Health:500.0f}")
    rcon.run("difficulty normal")
    time.sleep(0.3)
    normal_max, normal_health = max_health(dragon), health(dragon)
    record("on Normal 750, at the same share", normal_max == 750.0 and abs(normal_health - 375.0) < 0.01,
           f"health {normal_health} of {normal_max} from 500 of 1,000")
    rcon.run("difficulty easy")
    time.sleep(0.3)
    record("back on Easy, 500", max_health(dragon) == 500.0, f"max health {max_health(dragon)}")
    check("she stays ancient", "data get entity " + dragon + " Ancient", "1b")
    # Undead (tagged: Poison and Regeneration don't take, Harming heals), no Wither, and the ice
    # dragon's own tags (freeze immune, breathes under water, an ice dragon to Ice and Fire's weapons).
    for tag in ("#minecraft:undead", "#minecraft:freeze_immune_entity_types", "#minecraft:can_breathe_under_water", "#iceandfire:ice_dragon"):
        check("Syndrigossa in " + tag, "execute as " + dragon + " if entity @s[type=" + tag + "]", "Test passed")
    check("no Wither on her", "effect give " + dragon + " minecraft:wither 5", "Unable to apply")
    check("no Poison on her", "effect give " + dragon + " minecraft:poison 5", "Unable to apply")
    rcon.run("data merge entity " + dragon + " {Health:100.0f}")
    rcon.run("effect give " + dragon + " minecraft:instant_damage 1 0")
    time.sleep(0.3)  # an instant effect from /effect lands on the target's next tick
    harmed = health(dragon)
    record("Harming heals her", harmed > 100.0, f"health {harmed} from 100")

    def item_total(item):
        out = rcon.run("execute as @e[type=item] if items entity @s contents " + item + " run data get entity @s Item.count")
        return sum(int(n) for n in re.findall(r"entity data: (\d+)", out))

    # Her corpse is Dread (rolled here as Ice and Fire rolls a corpse, with her as the entity): her white
    # scales or dread shards, never flesh; her bones; and in place of a heart, dread shards and witherbones.
    rcon.run("kill @e[type=item]")
    for _ in range(20):
        rcon.run("execute as " + dragon + " at @s run loot spawn ~ ~3 ~ loot dreadpatch:entities/syndrigossa/corpse")
    scales, shards = item_total("dreadpatch:rimefrost_dragon_scale"), item_total("iceandfire:dread_shard")
    others = count("*") - count("dreadpatch:rimefrost_dragon_scale") - count("iceandfire:dread_shard")
    record("her corpse gives Rimefrost dragon scales and dread shards, nothing else", scales > 0 and shards > 0 and others == 0,
           f"{scales} scales, {shards} shards, {others} other stacks")
    rcon.run("kill @e[type=item]")
    rcon.run("execute as " + dragon + " at @s run loot spawn ~ ~3 ~ loot dreadpatch:entities/syndrigossa/heart")
    heart_shards, heart_bones = item_total("iceandfire:dread_shard"), item_total("iceandfire:witherbone")
    record("her heart is 4-8 dread shards and 2-4 witherbones", 4 <= heart_shards <= 8 and 2 <= heart_bones <= 4,
           f"{heart_shards} shards, {heart_bones} witherbones")
    eggs = item_total("dreadpatch:syndrigossa_egg")
    record("an ancient one leaves her egg with it", eggs == 1, f"{eggs} eggs")
    rcon.run("kill @e[type=item]")
    rcon.run("execute as " + dragon + " at @s run loot spawn ~ ~3 ~ loot dreadpatch:entities/syndrigossa/skeleton")
    bones = item_total("iceandfire:dragonbone")
    record("her skeleton gives dragon bones", 1 <= bones <= 6, f"{bones} dragon bones")
    rcon.run("kill @e[type=item]")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")
    # Only the boss leaves her egg: a grown one that isn't ancient, never, over five hearts.
    rcon.run(surface + "summon dreadpatch:syndrigossa ~8 ~ ~ {NoAI:1b,AgeTicks:2500000}")
    for _ in range(5):
        rcon.run("execute as " + dragon + " at @s run loot spawn ~ ~3 ~ loot dreadpatch:entities/syndrigossa/heart")
    young_eggs, young_shards = item_total("dreadpatch:syndrigossa_egg"), item_total("iceandfire:dread_shard")
    record("one that isn't ancient never leaves her egg", young_eggs == 0 and young_shards >= 20, f"{young_eggs} eggs, {young_shards} shards from 5 hearts")
    rcon.run("kill @e[type=item]")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")

    # A Rimefrost dragon (0.37.0), the subtype Syndrigossa is the unique one of: an ice dragon of the
    # Dread in its own colour (summoned sapphire, it stays sapphire), undead with the ice dragon's tags,
    # refusing Withering Rime and Wither, whole from a tag, with Ice and Fire's health for its age (not
    # Syndrigossa's by difficulty: on Hard it keeps it), and a Dread corpse in its own colour's scales
    # with no egg in its heart. Summoned by hand it's Ice and Fire's random age and ice dragon colour.
    rime = "@e[type=dreadpatch:rimefrost_dragon,limit=1]"
    check("a Rimefrost dragon summons", surface + 'summon dreadpatch:rimefrost_dragon ~8 ~ ~ {NoAI:1b,AgeTicks:2500000,Variant:"sapphire"}',
          "Summoned new Rimefrost Dragon")
    check("it keeps its colour", "data get entity " + rime + " Variant", '"sapphire"')
    check("it's one of the Dread: no Withering Rime", "effect give " + rime + " dreadpatch:withering_rime 5", "Unable to apply")
    check("no Wither on it", "effect give " + rime + " minecraft:wither 5", "Unable to apply")
    for tag in ("#minecraft:undead", "#minecraft:freeze_immune_entity_types", "#minecraft:can_breathe_under_water", "#iceandfire:ice_dragon"):
        check("a Rimefrost dragon in " + tag, "execute as " + rime + " if entity @s[type=" + tag + "]", "Test passed")
    rime_max = max_health(rime)
    record("whole from a tag, with Ice and Fire's health for its age", rime_max > 100 and health(rime) == rime_max,
           f"health {health(rime)} of {rime_max}")
    rcon.run("difficulty hard")
    time.sleep(0.3)
    record("its health isn't Syndrigossa's by difficulty", max_health(rime) == rime_max, f"max health {rime_max} on Easy, {max_health(rime)} on Hard")
    rcon.run("difficulty easy")
    # Undead, it never sleeps (0.40.0): loaded asleep it wakes, where Ice and Fire's own ice dragon sleeps on.
    rcon.run(surface + 'summon iceandfire:ice_dragon ~16 ~ ~ {NoAI:1b,AgeTicks:2500000,Sleeping:1b,Tags:["smoke_sleeper"]}')
    rcon.run(surface + 'summon dreadpatch:rimefrost_dragon ~16 ~ ~8 {NoAI:1b,AgeTicks:2500000,Sleeping:1b,Tags:["smoke_waker"]}')
    check("an ice dragon loaded asleep sleeps", "data get entity @e[tag=smoke_sleeper,limit=1] Sleeping", "1b")
    check("a Rimefrost dragon loaded asleep is awake", "data get entity @e[tag=smoke_waker,limit=1] Sleeping", "0b")
    rcon.run("kill @e[tag=smoke_sleeper]")
    rcon.run("kill @e[tag=smoke_waker]")
    rcon.run("kill @e[type=item]")
    for _ in range(20):
        rcon.run("execute as " + rime + " at @s run loot spawn ~ ~3 ~ loot dreadpatch:entities/rimefrost_dragon/corpse")
    rime_scales, rime_shards = item_total("dreadpatch:rimefrost_dragon_scale"), item_total("iceandfire:dread_shard")
    others = count("*") - count("dreadpatch:rimefrost_dragon_scale") - count("iceandfire:dread_shard")
    record("its corpse gives Rimefrost dragon scales and dread shards", rime_scales > 0 and rime_shards > 0 and others == 0,
           f"{rime_scales} Rimefrost scales, {rime_shards} shards, {others} other stacks")
    rcon.run("kill @e[type=item]")
    for _ in range(5):
        rcon.run("execute as " + rime + " at @s run loot spawn ~ ~3 ~ loot dreadpatch:entities/rimefrost_dragon/heart")
    record("its heart is shards and witherbones, never her egg",
           item_total("dreadpatch:syndrigossa_egg") == 0 and item_total("iceandfire:dread_shard") >= 20 and item_total("iceandfire:witherbone") >= 10,
           f"{item_total('dreadpatch:syndrigossa_egg')} eggs, {item_total('iceandfire:dread_shard')} shards, {item_total('iceandfire:witherbone')} witherbones from 5 hearts")
    rcon.run("kill @e[type=item]")
    rcon.run("kill @e[type=dreadpatch:rimefrost_dragon]")
    # (Without NBT: a summon with any skips Ice and Fire's finalizeSpawn, its age and colour.)
    rcon.run(surface + "summon dreadpatch:rimefrost_dragon ~8 ~ ~")
    rime_age = age_ticks(rime)
    colour = re.search(r'"(\w+)"', rcon.run("data get entity " + rime + " Variant"))
    record("summoned by hand, it's a random age and ice dragon colour",
           0 < rime_age <= 80 * 24000 and colour is not None and colour.group(1) in ("white", "sapphire", "silver", "blue"),
           f"AgeTicks {rime_age}, Variant {colour and colour.group(1)}")
    rcon.run("kill @e[type=dreadpatch:rimefrost_dragon]")

    # Her spawn animation: 30 ticks rising out of the ground, unable to move, with a pig to tempt her.
    spot = "execute positioned 0 0 0 positioned over world_surface positioned ~0.5 ~ ~0.5 "
    check("summons for the rise", spot + "run summon dreadpatch:dread_queen ~ ~ ~", "Summoned new Dread Queen")
    rcon.run('execute positioned 2 0 0 positioned over world_surface run summon minecraft:pig ~0.5 ~ ~0.5 {Tags:["queen_smoke"]}')
    time.sleep(1.25)  # 25 ticks in: as late as is safe, to give her the most time to move
    check("holds still while rising", spot + "if entity @e[type=dreadpatch:dread_queen,distance=..0.05]", "Test passed")
    time.sleep(4.0)
    check("moves once risen", spot + "unless entity @e[type=dreadpatch:dread_queen,distance=..0.05]", "Test passed")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    clear_court()
    rcon.run("kill @e[tag=queen_smoke]")
    rcon.run("kill @e[type=item]")

    # Her magic, on a stone platform in the sky (inside the force-loaded chunk) so terrain can't get in the way.
    rcon.run("fill 1 199 2 15 199 6 minecraft:stone")
    rcon.run("fill 1 200 2 15 204 6 minecraft:air")
    target = "@e[tag=smoke_target,limit=1]"
    golem = 'summon minecraft:iron_golem {x} 200 4.5 {{NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target"]}}'

    # Withering Rime: freezes, withers through armor, and nothing heals under it.
    rcon.run(golem.format(x=8.5))
    check("Withering Rime applies", "effect give " + target + " dreadpatch:withering_rime 5", "Applied effect")
    time.sleep(2.5)
    frozen = re.search(r"entity data: (\d+)", rcon.run("data get entity " + target + " TicksFrozen"))
    record("Withering Rime freezes", frozen is not None and int(frozen.group(1)) > 0, f"TicksFrozen {frozen and frozen.group(1)}")
    before = health(target)
    record("Withering Rime withers", 0 < before < 100, f"health {before}")
    rcon.run("effect give " + target + " minecraft:instant_health 1 1")
    time.sleep(0.3)
    after = health(target)
    record("nothing heals under Withering Rime", 0 < after <= before + 0.01, f"{before} -> {after} after Instant Health II")
    rcon.run("kill @e[tag=smoke_target]")
    # The Dread don't take it.
    rcon.run('summon iceandfire:dread_thrall 4.5 200 4.5 {NoAI:1b,Tags:["smoke_target"]}')
    check("the Dread are immune to Withering Rime", "effect give " + target + " dreadpatch:withering_rime 5", "Unable to apply")
    rcon.run("kill @e[tag=smoke_target]")

    # Her bolt as a player's spell: an armor stand casts it (level 5: 14 damage + 8% of 100) at a golem 6 blocks off.
    rcon.run(golem.format(x=8.5))
    rcon.run('summon minecraft:armor_stand 2.5 200 4.5 {Tags:["smoke_caster"],Rotation:[-90f,0f]}')
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:withering_rime_bolt 5")
    time.sleep(1.0)
    hp = health(target)
    record("her bolt (cast) hits for 14 + 8% of max health", 70 <= hp <= 79, f"golem health {hp}, expected about 78")
    record("her bolt (cast) leaves Withering Rime", has_effect(target, "dreadpatch:withering_rime"), "no Withering Rime on the golem")
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[tag=smoke_caster]")

    # No share-of-max-health hit takes more than 20 from anything: the same bolt into a golem given a
    # dragon's 1,000 health takes 14 + 20 (uncapped, 14 + 80).
    rcon.run('summon minecraft:iron_golem 8.5 200 4.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target"],'
             'attributes:[{id:"minecraft:generic.max_health",base:1000.0d}],Health:1000.0f}')
    rcon.run('summon minecraft:armor_stand 2.5 200 4.5 {Tags:["smoke_caster"],Rotation:[-90f,0f]}')
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:withering_rime_bolt 5")
    time.sleep(1.0)
    hp = health(target)
    record("her bolt's share is capped at 20", 963 <= hp <= 967, f"golem health {hp}, expected about 966 (uncapped it'd be 906)")
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[tag=smoke_caster]")

    # The queen bolts a golem from 12 blocks (her bolt: 8 + 15%; only her bolt leaves Withering Rime),
    # and its withering feeds her: on Hard, 3 for each tick that takes health. Wounded, she doesn't
    # heal until it's withering.
    was = re.search(r"The difficulty is (\w+)", rcon.run("difficulty"))
    rcon.run("difficulty hard")
    rcon.run(golem.format(x=14.5))
    rcon.run("summon dreadpatch:dread_queen 2.5 200 4.5")
    rcon.run("data merge entity " + queen + " {Health:100f}")
    time.sleep(1.0)
    unfed = health(queen)
    record("wounded, she doesn't heal on her own", unfed == 100.0, f"her health {unfed} a second after being set to 100")
    bolted = False
    for _ in range(30):
        time.sleep(0.5)
        if has_effect(target, "dreadpatch:withering_rime"):
            bolted = True
            break
    record("she bolts a target at range", bolted, "no Withering Rime on the golem within 15 s")
    fed_from = health(queen)
    time.sleep(3.0)
    fed_to = health(queen)
    record("her Rime feeds her 3 a tick on Hard", fed_to >= fed_from + 6,
           f"her health {fed_from} -> {fed_to} over 3 s of the golem withering (3 a tick)")
    rcon.run("difficulty " + (was.group(1).lower() if was else "easy"))
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[type=dreadpatch:rimefire_crown]")  # by now she's crowned the golem too
    clear_court()
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[type=item]")
    crowns = "execute if entity @e[type=dreadpatch:rimefire_crown]"
    caster = 'summon minecraft:armor_stand 2.5 200 4.5 {Tags:["smoke_caster"],Rotation:[-90f,0f]}'

    # Rimefire Crown, cast by an armor stand on a golem 6 blocks off, with a fire burning inside the ring.
    rcon.run(golem.format(x=8.5))
    rcon.run(caster)
    rcon.run("setblock 9 199 5 minecraft:netherrack")
    rcon.run("setblock 9 200 5 minecraft:fire")
    check("a fire burns before the crown", "execute if block 9 200 5 minecraft:fire", "Test passed")
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:rimefire_crown 5")
    time.sleep(0.5)
    check("the crown stands", crowns, "Test passed")
    record("the crown roots its target", has_effect(target, "dreadpatch:rimebound"), "no Rimebound on the golem")
    check("rooted: no movement speed", "attribute " + target + " minecraft:generic.movement_speed get", "is 0.0")
    check("its dread fire drinks the fire", "execute unless block 9 200 5 minecraft:fire", "Test passed")
    before = health(target)
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:withering_rime_bolt 5")
    time.sleep(0.6)
    after = health(target)
    check("a bolt shatters the crown", crowns, "Test failed")
    record("shattering frees the target", not has_effect(target, "dreadpatch:rimebound"), "still rooted")
    record("bolt and shrapnel both land (22 + 10)", before - after >= 31, f"health {before} -> {after}")
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[tag=smoke_caster]")
    rcon.run("setblock 9 200 5 minecraft:air")
    rcon.run("setblock 9 199 5 minecraft:stone")

    # A crown ends on its own: level 1 stands 3.5 s.
    rcon.run(golem.format(x=8.5))
    rcon.run(caster)
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:rimefire_crown 1")
    time.sleep(0.5)
    record("a level 1 crown roots", has_effect(target, "dreadpatch:rimebound"), "no Rimebound on the golem")
    time.sleep(4.0)
    check("it's gone after 3.5 s", crowns, "Test failed")
    check("and its target walks again", "attribute " + target + " minecraft:generic.movement_speed get", "is 0.25")
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[tag=smoke_caster]")

    # The queen raises a crown on her target, and it spares the Dread standing inside it.
    rcon.run(golem.format(x=12.5))
    rcon.run('summon iceandfire:dread_thrall 12.5 200 5.8 {NoAI:1b,Tags:["smoke_dread"]}')
    rcon.run("summon dreadpatch:dread_queen 2.5 200 4.5")
    # Her next bolt can follow a crown within a few ticks (it comes at most 1.25 s after) and shatter it,
    # freeing the golem; polled every half second, that was missed in 4 runs of 11. So: every tenth of a
    # second, her crown standing or the golem rooted; and a trace each second for when it still fails.
    crowned = False
    spared = True
    trace = []
    for i in range(250):
        time.sleep(0.1)
        if "Test passed" in rcon.run("execute positioned 12.5 200 4.5 if entity @e[type=dreadpatch:rimefire_crown,distance=..4]") \
                or has_effect(target, "dreadpatch:rimebound"):
            crowned = True
            spared = not has_effect("@e[tag=smoke_dread,limit=1]", "dreadpatch:rimebound")
            break
        if i % 10 == 9:
            about = []
            for kind in ("dreadpatch:syndrigossa", "iceandfire:dread_thrall", "iceandfire:dread_ghoul", "iceandfire:dread_knight",
                         "iceandfire:dread_scuttler", "minecraft:zombie", "minecraft:skeleton"):
                m = re.search(r"count: (\d+)", rcon.run(f"execute positioned 8 200 4 if entity @e[type={kind},distance=..64]"))
                if m:
                    about.append(kind.split(":")[1] + "=" + m.group(1))
            qpos = re.search(r"\[([-0-9.]+)d, ([-0-9.]+)d, ([-0-9.]+)d\]", rcon.run("data get entity @e[type=dreadpatch:dread_queen,limit=1] Pos"))
            trace.append(f"{(i + 1) / 10:4.1f}s golem {health(target)} queen {qpos and tuple(round(float(v), 1) for v in qpos.groups())} {' '.join(about)}")
    if not crowned:
        print("\n".join("      " + line for line in trace))
    record("she raises a crown on her target", crowned, "no crown of hers on the golem within 25 s")
    record("her crown spares the Dread", crowned and spared, "the dread thrall was rooted" if crowned else "no crown to check")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    clear_court()
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[tag=smoke_dread]")
    rcon.run("kill @e[type=item]")

    # Her sword in anyone's hand (here a husk's, which goes for iron golems): every strike leaves
    # Withering Rime, and the third in a row on a chilled target shatters it, leaving it Brittle.
    rcon.run(golem.format(x=8.5))
    rcon.run('summon minecraft:husk 6.5 200 4.5 {PersistenceRequired:1b,Tags:["smoke_wielder"],'
             'HandItems:[{id:"iceandfire:dread_queen_sword",count:1},{}],HandDropChances:[0.0f,0.0f]}')
    struck = brittle = False
    for _ in range(24):
        time.sleep(0.5)
        struck = struck or has_effect(target, "dreadpatch:withering_rime")
        if has_effect(target, "dreadpatch:brittle"):
            brittle = True
            break
    record("her sword's strikes leave Withering Rime", struck, "no Withering Rime on the golem within 12 s")
    record("her sword's third strike shatters", brittle, "no Brittle on the golem within 12 s")
    check("Brittle: armor -10%", "attribute " + target + " minecraft:generic.armor modifier value get dreadpatch:brittle_armor", "is -0.1")
    rcon.run("kill @e[tag=smoke_wielder]")
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[type=item]")

    # Syndrigossa's attacks, without waiting on her AI. A Shatter-Spine shard dropped on a golem cuts
    # it and leaves Withering Rime; a core bursts into 5 to 7 shards; and her breath's damage, dealt
    # through /damage as hers, takes 0.5 more past armor and heals her that (the siphon).
    rcon.run(golem.format(x=8.5))
    rcon.run('summon dreadpatch:shatter_spine_shard 8.5 204 4.5 {Motion:[0.0d,-0.6d,0.0d]}')
    time.sleep(1.0)
    cut = health(target)
    record("a Shatter-Spine shard cuts", 0 < cut < 100, f"golem health {cut}")
    record("a Shatter-Spine shard leaves Withering Rime", has_effect(target, "dreadpatch:withering_rime"), "no Withering Rime on the golem")
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run('summon dreadpatch:shatter_spine 8.5 210 4.5 {Fuse:3}')
    time.sleep(0.4)
    shards = re.search(r"count: (\d+)", rcon.run("execute if entity @e[type=dreadpatch:shatter_spine_shard]"))
    record("a Shatter-Spine core bursts into 5 to 7 shards", shards is not None and 5 <= int(shards.group(1)) <= 7,
           f"{shards and shards.group(1)} shards; at " + str(re.findall(r"\[[-0-9., d]+\]", rcon.run(
               "execute as @e[type=dreadpatch:shatter_spine_shard] run data get entity @s Pos")))[:400]
           + "; Syndrigossas loaded: " + rcon.run("execute if entity @e[type=dreadpatch:syndrigossa]").strip())
    record("and is gone", "Test failed" in rcon.run("execute positioned 8.5 210 4.5 if entity @e[type=dreadpatch:shatter_spine,distance=..16]"),
           "the core is still there")
    time.sleep(2.5)
    rcon.run("kill @e[type=dreadpatch:shatter_spine_shard]")
    rcon.run(golem.format(x=4.5))
    rcon.run('summon dreadpatch:syndrigossa 12.5 200 4.5 {NoAI:1b,AgeTicks:2500000,Health:300.0f,Tags:["smoke_dragon"]}')
    rcon.run("damage " + target + " 5 dreadpatch:withering_breath by @e[tag=smoke_dragon,limit=1]")
    siphoned_golem, siphoned_dragon = health(target), health("@e[tag=smoke_dragon,limit=1]")
    record("her breath siphons 0.5 more, past armor", abs(siphoned_golem - 94.5) < 0.01, f"golem health {siphoned_golem}, expected 94.5")
    record("into her", abs(siphoned_dragon - 300.5) < 0.01, f"dragon health {siphoned_dragon}, expected 300.5")
    rcon.run("kill @e[tag=smoke_dragon]")
    rcon.run("kill @e[tag=smoke_target]")

    # Ice and Fire's dread scuttlers spit a weakened Withering Rime-Bolt (3 + 4%, 2 s of Withering Rime)
    # at what they hunt from 3 to 12 blocks: one 9 blocks from a golem it can hunt.
    rcon.run(golem.format(x=4.5))
    check("summons a dread scuttler", 'summon iceandfire:dread_scuttler 13.5 200 4.5 {Tags:["smoke_scuttler"]}', "Summoned new")
    spat = False
    for _ in range(16):
        time.sleep(0.5)
        if has_effect(target, "dreadpatch:withering_rime"):
            spat = True
            break
    record("a dread scuttler spits Withering Rime at its prey", spat, "no Withering Rime on the golem within 8 s")
    rcon.run("kill @e[tag=smoke_scuttler]")
    rcon.run("kill @e[tag=smoke_target]")

    # Barrow-Step, cast by an armor stand looking down the platform: it's gone from where it stood,
    # its husk is there instead, and struck, the husk shatters, rooting and riming the golem beside it.
    rcon.run(golem.format(x=14.5))
    rcon.run('summon minecraft:armor_stand 13.5 200 4.5 {Tags:["smoke_caster"],Rotation:[90f,0f]}')
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:barrow_step 5")
    time.sleep(0.3)
    husk_here = "execute positioned 13.5 200 4.5 if entity @e[type=dreadpatch:barrow_husk,distance=..0.5]"
    check("Barrow-Step leaves a husk where its caster stood", husk_here, "Test passed")
    m = re.search(r"entity data: \[([-0-9.]+)d", rcon.run("data get entity @e[tag=smoke_caster,limit=1] Pos"))
    record("Barrow-Step takes its caster up to 15 blocks", m is not None and float(m.group(1)) < 4.0,
           f"caster at x {m and m.group(1)}, from 13.5")
    rcon.run("damage @e[type=dreadpatch:barrow_husk,limit=1] 1 minecraft:mob_attack by @e[tag=smoke_target,limit=1]")
    time.sleep(0.2)
    check("struck, the husk shatters", husk_here, "Test failed")
    record("its shatter roots", has_effect(target, "dreadpatch:rimebound"), "no Rimebound on the golem beside it")
    record("its shatter leaves Withering Rime", has_effect(target, "dreadpatch:withering_rime"), "no Withering Rime on the golem beside it")
    rcon.run("kill @e[tag=smoke_caster]")
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[type=dreadpatch:barrow_husk]")
    rcon.run("fill 1 199 2 15 199 6 minecraft:air")

    # Court of the Dread, on a bigger platform: she raises her court against a golem she can't hurt
    # (Resistance V: an invulnerable one she wouldn't target at all), and burns one for Beater's Decree.
    def court(extra=""):
        total = 0
        for kind in court_types:
            m = re.search(r"count: (\d+)", rcon.run(f"execute positioned 8 200 8 if entity @e[type={kind},distance=..24{extra}]"))
            total += int(m.group(1)) if m else 0
        return total

    rcon.run("fill 1 199 1 15 199 14 minecraft:stone")
    rcon.run("fill 1 200 1 15 205 14 minecraft:air")

    # Syndrigossa and the queen, up here away from the wildlife: they leave each other be.
    dragon = "@e[type=dreadpatch:syndrigossa,limit=1]"
    rcon.run('summon dreadpatch:syndrigossa 4.5 200 7.5 {AgeTicks:1300000}')
    check("summons the queen beside Syndrigossa", "summon dreadpatch:dread_queen 12.5 200 7.5", "Summoned new Dread Queen")
    dragon_hp, queen_hp = health(dragon), health(queen)
    time.sleep(6.0)
    dragon_after, queen_after = health(dragon), health(queen)
    record("Syndrigossa and the queen leave each other be", dragon_hp > 0 and dragon_after >= dragon_hp and queen_after >= queen_hp,
           f"dragon {dragon_hp} -> {dragon_after}, queen {queen_hp} -> {queen_after}")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    clear_court()
    rcon.run("kill @e[type=item]")

    # Sovereign's Eclipse, Syndrigossa's 16-block crown (summoned as hers, 3 s long): it slows heavily
    # rather than roots and keeps Withering Rime on a golem inside it, spares a dread thrall inside it,
    # isn't shattered by a Withering Rime-Bolt, and is gone when its time is up.
    rcon.run('summon dreadpatch:syndrigossa 2.5 200 2.5 {NoAI:1b,AgeTicks:2500000,UUID:[I;7,7,7,7],Tags:["smoke_dragon"]}')
    rcon.run('summon minecraft:iron_golem 12.5 200 7.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target"]}')
    rcon.run('summon iceandfire:dread_thrall 6.5 200 10.5 {NoAI:1b,Tags:["smoke_dread"]}')
    eclipses = "execute if entity @e[type=dreadpatch:sovereigns_eclipse]"
    check("Sovereign's Eclipse stands", 'summon dreadpatch:sovereigns_eclipse 8.5 200 7.5 {Owner:[I;7,7,7,7],Lifetime:60}', "Summoned new Sovereign's Eclipse")
    time.sleep(0.5)
    slowed = re.search(r"is ([0-9.]+)", rcon.run("attribute " + target + " minecraft:generic.movement_speed get"))
    record("the Eclipse slows heavily but doesn't root", slowed is not None and 0.05 < float(slowed.group(1)) < 0.15,
           f"golem movement speed {slowed and slowed.group(1)}, from 0.25")
    record("the Eclipse keeps Withering Rime on it", has_effect(target, "dreadpatch:withering_rime"), "no Withering Rime on the golem")
    record("the Eclipse spares the Dread", not has_effect("@e[tag=smoke_dread,limit=1]", "minecraft:slowness"), "the dread thrall was slowed")
    rcon.run('summon minecraft:armor_stand 2.5 200 7.5 {Tags:["smoke_caster"],Rotation:[-90f,0f]}')
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:withering_rime_bolt 5")
    time.sleep(0.6)
    check("a bolt doesn't shatter the Eclipse", eclipses, "Test passed")
    time.sleep(2.5)
    check("the Eclipse is gone when its time is up", eclipses, "Test failed")
    for tag in ("smoke_dragon", "smoke_target", "smoke_dread", "smoke_caster"):
        rcon.run(f"kill @e[tag={tag}]")

    # Whiteout Dominion, cast by an armor stand (level 1: 16 blocks round, 25 s) over a golem 10 blocks
    # off and a dread thrall: the golem is slowed 20% and its sight halved, and takes a point of
    # freezing damage 3 s in (and no more by 4 s); the thrall, one of the Dread, is untouched. A
    # caster's storm doesn't raise the dead, and a new one of theirs ends the last.
    storms = "execute if entity @e[type=dreadpatch:whiteout_dominion]"
    thrall = "@e[tag=smoke_dread,limit=1]"
    rcon.run('summon minecraft:iron_golem 12.5 200 7.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target"]}')
    rcon.run('summon iceandfire:dread_thrall 6.5 200 10.5 {NoAI:1b,Tags:["smoke_dread"]}')
    rcon.run('summon minecraft:armor_stand 2.5 200 7.5 {Tags:["smoke_caster"],Rotation:[-90f,0f]}')
    golem_hp, thrall_hp = health(target), health(thrall)
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:whiteout_dominion 1")
    time.sleep(0.8)
    check("Whiteout Dominion stands", storms, "Test passed")
    record("its storm puts Whiteout on the golem", has_effect(target, "dreadpatch:whiteout"), "no Whiteout on the golem")
    speed = re.search(r"is ([0-9.]+)", rcon.run("attribute " + target + " minecraft:generic.movement_speed get"))
    record("Whiteout slows by a fifth", speed is not None and 0.19 < float(speed.group(1)) < 0.21,
           f"golem movement speed {speed and speed.group(1)}, from 0.25")
    sight = re.search(r"is ([0-9.]+)", rcon.run("attribute " + target + " minecraft:generic.follow_range get"))
    record("Whiteout halves a mob's sight", sight is not None and 7.9 < float(sight.group(1)) < 8.1,
           f"golem follow range {sight and sight.group(1)}, from 16")
    record("the Dread are untouched by it", not has_effect(thrall, "dreadpatch:whiteout"), "the dread thrall has Whiteout")
    time.sleep(3.2)
    hp = health(target)
    record("its cold takes a point every 3 s", golem_hp - 1.5 < hp < golem_hp - 0.5, f"golem health {golem_hp} -> {hp} 4 s in")
    record("its cold spares the Dread", health(thrall) >= thrall_hp, f"dread thrall health {thrall_hp} -> {health(thrall)}")
    rcon.run('summon minecraft:cow 10.5 200 4.5 {NoAI:1b,Tags:["smoke_dead"]}')
    rcon.run("kill @e[tag=smoke_dead]")
    time.sleep(0.5)
    check("a caster's storm doesn't raise the dead", "execute if entity @e[type=iceandfire:dread_beast]", "Test failed")
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:whiteout_dominion 1")
    time.sleep(0.3)
    check("a caster's new storm ends their last", storms, "Test passed, count: 1")
    rcon.run("kill @e[type=dreadpatch:whiteout_dominion]")
    time.sleep(2.5)
    record("out of it, Whiteout wears off", not has_effect(target, "dreadpatch:whiteout"), "the golem still has Whiteout 2.5 s after")
    for tag in ("smoke_target", "smoke_dread", "smoke_caster"):
        rcon.run(f"kill @e[tag={tag}]")

    # Summon Court (0.33.0), cast by a villager (NoAI, Resistance V; an armor stand records no attacker,
    # and the court defends its caster from what hurts it): level 3 raises 4 of her court, answering to
    # the villager. A husk (a zombie that doesn't burn at noon) that hurts the caster, they fall on; a
    # cow that does, or that hurts one of them, they leave be (the server config's defaults; with
    # attack_friendly_mobs and retaliate on, they killed it within 4 s); nor do they hurt the cow, nor
    # one of the Dread. Their caster can't hurt them. A new cast replaces the last; their time up, they go.
    def summoned(extra=""):
        return court(",nbt={CommanderUUID:[I;7,7,7,7]}" + extra)

    member = "@e[type=!minecraft:villager,nbt={CommanderUUID:[I;7,7,7,7]},limit=1]"
    rcon.run('summon minecraft:villager 7.5 200 7.5 {NoAI:1b,UUID:[I;7,7,7,7],Tags:["smoke_caster"]}')
    rcon.run("effect give @e[tag=smoke_caster,limit=1] minecraft:resistance 300 4 true")
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:summon_court 3")
    time.sleep(0.5)
    n = summoned()
    record("Summon Court raises 4 at level 3, answering to its caster", n == 4, f"{n} raised")
    rcon.run('summon minecraft:cow 9.5 200 9.5 {NoAI:1b,Tags:["smoke_cow"]}')
    rcon.run('summon iceandfire:dread_thrall 5.5 200 9.5 {NoAI:1b,Tags:["smoke_dread"]}')
    cow = "@e[tag=smoke_cow,limit=1]"
    time.sleep(2.0)  # past their rise
    cow_hp, thrall_hp = health(cow), health(thrall)
    rcon.run("damage @e[tag=smoke_caster,limit=1] 1 minecraft:mob_attack by " + cow)
    rcon.run("damage " + member + " 1 minecraft:mob_attack by " + cow)
    rcon.run("damage @e[tag=smoke_caster,limit=1] 1 minecraft:mob_attack by " + thrall)
    rcon.run("damage " + cow + " 2 minecraft:mob_attack by " + member)
    time.sleep(3.0)
    record("the court leaves a friendly mob be, even one that hits it or its caster", health(cow) >= cow_hp, f"cow health {cow_hp} -> {health(cow)}")
    record("the court never fights the Dread", health(thrall) >= thrall_hp, f"dread thrall health {thrall_hp} -> {health(thrall)}")
    # One member, pinned by a tag ("limit=1" alone may pick another between reads), and /damage's own word.
    rcon.run("tag " + member + " add smoke_member")
    pinned = "@e[tag=smoke_member,limit=1]"
    before = health(pinned)
    out = rcon.run("damage " + pinned + " 4 minecraft:mob_attack by @e[tag=smoke_caster,limit=1]")
    record("its caster can't hurt it", "Applied" not in out and health(pinned) >= before,
           f"court member health {before} -> {health(pinned)}: {out.strip()}")
    # A scuttler in the court, every time (the court's kinds are random): Ice and Fire's scuttler's own
    # getCommander is always null, which until 0.42.0 let its caster hurt it (once each in 0.37.0 and 0.42.0's runs).
    rcon.run('summon iceandfire:dread_scuttler 5.5 200 5.5 {NoAI:1b,CommanderUUID:[I;7,7,7,7],Tags:["smoke_court_scuttler"],'
             'NeoForgeData:{"dreadpatch:court_expires":999999999L}}')
    time.sleep(0.2)
    out = rcon.run("damage @e[tag=smoke_court_scuttler,limit=1] 4 minecraft:mob_attack by @e[tag=smoke_caster,limit=1]")
    record("its caster can't hurt a scuttler of it", "Applied" not in out, out.strip())
    rcon.run("kill @e[tag=smoke_court_scuttler]")
    rcon.run('summon minecraft:husk 10.5 200 5.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_zombie"]}')
    rcon.run("damage @e[tag=smoke_caster,limit=1] 1 minecraft:mob_attack by @e[tag=smoke_zombie,limit=1]")
    fought = False
    for _ in range(16):
        time.sleep(0.5)
        zombie_hp = health("@e[tag=smoke_zombie,limit=1]")
        if zombie_hp < 20.0:
            fought = True
            break
    record("the court falls on a hostile mob that hurts its caster", fought, "the husk was untouched 8 s after it hit the caster")
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:summon_court 1")
    time.sleep(0.5)
    n = summoned()
    record("a new cast replaces the last court (2 at level 1)", n == 2, f"{n} standing")
    rcon.run('execute as @e[nbt={CommanderUUID:[I;7,7,7,7]}] run data merge entity @s {NeoForgeData:{"dreadpatch:court_expires":0L}}')
    time.sleep(1.5)
    n = summoned()
    record("their time up, they go", n == 0, f"{n} still standing")
    for tag in ("smoke_caster", "smoke_cow", "smoke_dread", "smoke_zombie"):
        rcon.run(f"kill @e[tag={tag}]")
    clear_court()

    # The dread phantom (0.45.0). At noon in open sky a phantom burns and a dread phantom doesn't; it's
    # undead and of the Dread (no Withering Rime, Whiteout or Rimefrost for it). Rimefrost slows a zombie
    # by a quarter. Its loot table gives the dread thrall's drops, and membrane and Rimefrost scales
    # only on a player's kill.
    rcon.run("time set noon")  # the clock runs: minutes in, it's no longer day
    rcon.run('summon minecraft:phantom 3.5 212 12.5 {NoAI:1b,Tags:["smoke_vanilla_phantom"]}')
    rcon.run('summon dreadpatch:dread_phantom 6.5 212 12.5 {NoAI:1b,Tags:["smoke_phantom"]}')
    phantom = "@e[tag=smoke_phantom,limit=1]"
    check("a dread phantom summons", "execute if entity " + phantom, "Test passed")
    time.sleep(2.0)
    fire = lambda sel: int((re.search(r"entity data: (-?\d+)s", rcon.run("data get entity " + sel + " Fire")) or [0, "0"])[1])
    record("a phantom burns at noon (the control)", fire("@e[tag=smoke_vanilla_phantom,limit=1]") > 0, "the vanilla phantom isn't burning")
    record("a dread phantom doesn't burn by day", fire(phantom) <= 0 and health(phantom) >= 20.0,
           f"Fire {fire(phantom)}, health {health(phantom)}")
    rcon.run("kill @e[tag=smoke_vanilla_phantom]")
    check("a dread phantom is undead", "execute if entity @e[tag=smoke_phantom,type=#minecraft:undead]", "Test passed")
    for effect in ("dreadpatch:withering_rime", "dreadpatch:whiteout", "dreadpatch:rimefrost"):
        rcon.run(f"effect give {phantom} {effect} 10")
        time.sleep(0.1)
        record(f"a dread phantom refuses {effect.split(':')[1]}", not has_effect(phantom, effect), "it took it")
    rcon.run('summon minecraft:zombie 3.5 200 12.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_slowed"]}')
    speed = lambda: float((re.search(r"is ([0-9.]+)", rcon.run("attribute @e[tag=smoke_slowed,limit=1] minecraft:generic.movement_speed get")) or [0, "-1"])[1])
    before = speed()
    rcon.run("effect give @e[tag=smoke_slowed,limit=1] dreadpatch:rimefrost 10")
    time.sleep(0.2)
    after = speed()
    record("Rimefrost slows by a quarter", before > 0 and abs(after / before - 0.75) < 0.01, f"speed {before} -> {after}")
    rcon.run("kill @e[tag=smoke_slowed]")
    rcon.run("kill @e[type=item]")
    for _ in range(8):
        rcon.run("loot spawn 6.5 201 12.5 kill " + phantom)
    time.sleep(0.2)
    thrall_drops = count("minecraft:bone") + count("iceandfire:dread_shard")
    record("its loot is a thrall's", thrall_drops > 0, "no bones or dread shards over 8 rolls")
    no_player = count("minecraft:phantom_membrane") + count("dreadpatch:rimefrost_dragon_scale")
    record("no membrane or scales without a player's kill", no_player == 0, f"{no_player} dropped")
    rcon.run("kill @e[type=item]")
    rcon.run("kill " + phantom)
    time.sleep(0.3)
    rcon.run("kill @e[type=item]")

    # Summon Dread Phantoms, cast by a villager (as Summon Court is): 2, answering to it, kept until their
    # time's up; they circle over it; its caster can't hurt one; a husk that hurts the caster they dive on
    # (revenge, diving and the bite); killed, they leave nothing; a recast replaces them; their time up, they go.
    def phantoms_of_caster(extra=""):
        m = re.search(r"count: (\d+)", rcon.run("execute if entity @e[type=dreadpatch:dread_phantom,nbt={Summoner:[I;7,7,7,7]" + extra + "}]"))
        return int(m.group(1)) if m else 0

    rcon.run('summon minecraft:villager 7.5 200 7.5 {NoAI:1b,UUID:[I;7,7,7,7],Tags:["smoke_caster"]}')
    rcon.run("effect give @e[tag=smoke_caster,limit=1] minecraft:resistance 300 4 true")
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:summon_dread_phantoms 1")
    time.sleep(0.5)
    n = phantoms_of_caster()
    record("Summon Dread Phantoms calls 2, answering to its caster", n == 2, f"{n} called")
    marked = "Test passed" in rcon.run('execute as @e[type=dreadpatch:dread_phantom,nbt={Summoner:[I;7,7,7,7]},limit=1] '
                                      'if data entity @s NeoForgeData."dreadpatch:court_expires"')
    record("they're a summoned court's, with a time to go", marked, "no court_expires on them")
    time.sleep(4.0)
    near = re.search(r"count: (\d+)", rcon.run("execute positioned 7.5 200 7.5 if entity @e[type=dreadpatch:dread_phantom,distance=..28,nbt={Summoner:[I;7,7,7,7]}]"))
    record("they circle over their caster", near is not None and int(near.group(1)) == 2, f"{near and near.group(1)} of 2 within 28 blocks after 4 s")
    rcon.run("tag @e[type=dreadpatch:dread_phantom,nbt={Summoner:[I;7,7,7,7]},limit=1] add smoke_member_phantom")
    out = rcon.run("damage @e[tag=smoke_member_phantom,limit=1] 4 minecraft:mob_attack by @e[tag=smoke_caster,limit=1]")
    record("its caster can't hurt one", "Applied" not in out, out.strip())
    rcon.run('summon minecraft:husk 10.5 200 5.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_zombie"]}')
    rcon.run("damage @e[tag=smoke_caster,limit=1] 1 minecraft:mob_attack by @e[tag=smoke_zombie,limit=1]")
    dove = False
    for _ in range(40):
        time.sleep(0.5)
        if health("@e[tag=smoke_zombie,limit=1]") < 20.0:
            dove = True
            break
    record("they dive on a hostile mob that hurts their caster", dove, "the husk was untouched 20 s after it hit the caster")
    rcon.run("kill @e[tag=smoke_zombie]")
    rcon.run("kill @e[type=item]")
    rcon.run("kill @e[tag=smoke_member_phantom]")
    time.sleep(0.3)
    left = sum(count(i) for i in ("minecraft:bone", "iceandfire:dread_shard", "minecraft:phantom_membrane", "dreadpatch:rimefrost_dragon_scale"))
    record("killed, a summoned one leaves nothing", left == 0, f"{left} items")
    rcon.run("kill @e[type=item]")
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:summon_dread_phantoms 1")
    time.sleep(0.5)
    n = phantoms_of_caster()
    record("a new cast replaces the last (still 2)", n == 2, f"{n} standing")
    rcon.run('execute as @e[type=dreadpatch:dread_phantom,nbt={Summoner:[I;7,7,7,7]}] run data merge entity @s {NeoForgeData:{"dreadpatch:court_expires":0L}}')
    time.sleep(1.5)
    n = phantoms_of_caster()
    record("their time up, they go", n == 0, f"{n} still standing")
    rcon.run("kill @e[tag=smoke_caster]")

    # A Dread Lich calls them too (its countdown set short): 1-2, answering to it, at the golem it fights.
    rcon.run('summon minecraft:iron_golem 12.5 200 12.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target"]}')
    rcon.run("effect give @e[tag=smoke_target,limit=1] minecraft:resistance 120 4 true")
    rcon.run('summon iceandfire:dread_lich 4.5 200 12.5 {UUID:[I;3,3,3,3],PersistenceRequired:1b,NeoForgeData:{"dreadpatch:phantom_call":2}}')
    lich_called = 0
    for _ in range(20):
        time.sleep(0.5)
        m = re.search(r"count: (\d+)", rcon.run("execute if entity @e[type=dreadpatch:dread_phantom,nbt={Summoner:[I;3,3,3,3]}]"))
        lich_called = int(m.group(1)) if m else 0
        if lich_called:
            break
    record("a Dread Lich calls 1-2 dread phantoms, its own", 1 <= lich_called <= 2, f"{lich_called} within 10 s")
    rcon.run("kill @e[type=iceandfire:dread_lich]")
    rcon.run("kill @e[tag=smoke_target]")
    clear_court()
    rcon.run("kill @e[type=item]")

    # Create Rimefrost Dragon (0.38.0), cast by a villager (NoAI, Resistance V, at 6 of 20 health) at
    # an Ice and Fire ice dragon's corpse in its sights: the corpse rises as a Rimefrost dragon of the
    # same age and colour, with half the caster's missing health (7), raised by it; cast at another,
    # the first is sent back (one at a time). (A player's cooldown, recast window and mana need a client.)
    def ice_corpse(x, colour, tag):
        rcon.run(f'summon iceandfire:ice_dragon {x} 200 7.5 {{NoAI:1b,AgeTicks:1300000,Variant:"{colour}",Tags:["{tag}"]}}')
        rcon.run(f"damage @e[tag={tag},limit=1] 100000 minecraft:generic")
        time.sleep(0.5)

    raised = "@e[type=dreadpatch:rimefrost_dragon,limit=1]"
    rcon.run('summon minecraft:villager 2.5 200 7.5 {NoAI:1b,UUID:[I;5,5,5,5],Rotation:[-90f,5f],Tags:["smoke_caster"]}')
    rcon.run("effect give @e[tag=smoke_caster,limit=1] minecraft:resistance 300 4 true")
    rcon.run("data merge entity @e[tag=smoke_caster,limit=1] {Health:6.0f}")
    ice_corpse(11.5, "silver", "smoke_ice_corpse")
    check("an ice dragon's corpse lies there", "execute if entity @e[tag=smoke_ice_corpse,nbt={ModelDead:1b}]", "Test passed")
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:create_rimefrost_dragon 1")
    # Straight away, before its AI moves it or it heals (its breath feeds it, fighting).
    rcon.run("execute as @e[type=dreadpatch:rimefrost_dragon] run data merge entity @s {NoAI:1b}")
    hp_risen = health(raised)
    time.sleep(0.5)
    check("the corpse is gone", "execute if entity @e[tag=smoke_ice_corpse]", "Test failed")
    check("a Rimefrost dragon rises in its place", "execute positioned 11.5 200 7.5 if entity @e[type=dreadpatch:rimefrost_dragon,distance=..3]", "Test passed")
    check("in its colour", "data get entity " + raised + " Variant", '"silver"')
    record("at its age", 1300000 <= age_ticks(raised) < 1300000 + 400, f"AgeTicks {age_ticks(raised)}")
    record("with half its caster's missing health, rounded down", hp_risen == 7.0, f"health {hp_risen} (the caster at 6 of 20)")
    check("raised by its caster", "data get entity " + raised + " RaisedBy", "[I; 5, 5, 5, 5]")
    ice_corpse(6.5, "blue", "smoke_ice_corpse_2")
    # Looking lower: a corpse lies low, and 6 blocks off a ray at 5 degrees passes just over it.
    rcon.run("tp @e[tag=smoke_caster,limit=1] 0.5 200 7.5 -90 12")
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:create_rimefrost_dragon 1")
    time.sleep(0.5)
    check("raising another sends the first back: one at a time", "execute if entity @e[type=dreadpatch:rimefrost_dragon]", "Test passed, count: 1")
    check("the new one is the second corpse", "data get entity " + raised + " Variant", '"blue"')
    rcon.run("kill @e[type=dreadpatch:rimefrost_dragon]")
    rcon.run("kill @e[tag=smoke_caster]")
    rcon.run("kill @e[type=iceandfire:ice_dragon]")

    # And the queen casts it herself (for nothing), once she's lost a quarter of her health: here at
    # 100 of 300, against a golem under Resistance V, an ice dragon's corpse 8 blocks off. Her first
    # raising is 10 s in; hers answers to her, and rises with half what she's lost.
    ice_corpse(12.5, "white", "smoke_ice_corpse_3")
    rcon.run('summon minecraft:iron_golem 8.5 200 3.5 {PersistenceRequired:1b,Tags:["smoke_target"]}')
    rcon.run("effect give " + target + " minecraft:resistance 120 4 true")
    rcon.run('summon dreadpatch:dread_queen 4.5 200 7.5 {UUID:[I;6,6,6,6],Health:100.0f}')
    risen = False
    for _ in range(40):
        time.sleep(0.5)
        if "Test passed" in rcon.run("execute if entity @e[type=dreadpatch:rimefrost_dragon,nbt={RaisedBy:[I;6,6,6,6]}]"):
            risen = True
            break
    if not risen:
        # Alone this passed 3 runs of 3, each at 10 s; once (0.40.0) not within the full run. Show why if it happens again.
        state = rcon.run("data get entity @e[type=dreadpatch:dread_queen,limit=1] Pos") + " | " + rcon.run(
            "data get entity @e[type=dreadpatch:dread_queen,limit=1] Health") + " | corpse: " + rcon.run(
            "execute if entity @e[tag=smoke_ice_corpse_3,nbt={ModelDead:1b}]") + " | " + rcon.run(
            "execute as @e[type=dreadpatch:dread_queen,limit=1] on target run data get entity @s id")
    record("the queen raises a Rimefrost dragon of hers from a corpse", risen, "none raised by her within 20 s" + ("" if risen else ": " + state.replace("\n", " ")))
    if risen:
        hp = health("@e[type=dreadpatch:rimefrost_dragon,limit=1]")
        hp_max = max_health("@e[type=dreadpatch:rimefrost_dragon,limit=1]")
        # At most half her 300 (her health moves meanwhile: the golem hits her, her rime heals her, and
        # its own breath feeds it a little), and not whole.
        record("it rises with half what she's lost, not whole", 1.0 <= hp <= 160.0 and hp < hp_max, f"health {hp} of {hp_max}")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[type=dreadpatch:rimefrost_dragon]")
    rcon.run("kill @e[type=iceandfire:ice_dragon]")
    rcon.run("kill @e[tag=smoke_target]")
    clear_court()

    # Hatching Syndrigossa's egg (0.39.0): laid on dreadstone (summoned as a player would lay it, with
    # its owner), each full Whiteout Dominion storm over it is one of three; the third hatches it into a
    # white Rimefrost hatchling tamed to its owner. A storm cut short (killed) doesn't count, nor does
    # one over an egg on plain stone.
    rcon.run("kill @e[type=dreadpatch:whiteout_dominion]")
    rcon.run("setblock 4 199 11 iceandfire:dread_stone_bricks")
    egg = "@e[tag=smoke_egg,limit=1]"
    rcon.run('summon dreadpatch:syndrigossa_egg 4.5 200 11.5 {Owner:[I;4,4,4,4],Tags:["smoke_egg"]}')
    rcon.run('summon dreadpatch:syndrigossa_egg 6.5 200 11.5 {Tags:["smoke_egg_off"]}')
    time.sleep(0.3)

    def egg_storm(lifetime=40):
        rcon.run(f"summon dreadpatch:whiteout_dominion 5.5 200 11.5 {{Lifetime:{lifetime},Radius:6f}}")

    def soaked(selector):
        m = re.search(r"entity data: (\d+)", rcon.run("data get entity " + selector + " Soaked"))
        return int(m.group(1)) if m else -1

    egg_storm()
    time.sleep(2.5)
    record("a full storm over her egg on dreadstone: one of three", soaked(egg) == 1, f"Soaked {soaked(egg)}")
    record("an egg on plain stone soaks nothing", soaked("@e[tag=smoke_egg_off,limit=1]") == 0, f"Soaked {soaked('@e[tag=smoke_egg_off,limit=1]')}")
    egg_storm(200)
    time.sleep(0.5)
    rcon.run("kill @e[type=dreadpatch:whiteout_dominion]")
    time.sleep(0.3)
    record("a storm cut short doesn't count", soaked(egg) == 1, f"Soaked {soaked(egg)}")
    egg_storm()
    time.sleep(2.5)
    record("a second full storm: two", soaked(egg) == 2, f"Soaked {soaked(egg)}")
    egg_storm()
    time.sleep(2.5)
    check("the third hatches it", "execute if entity @e[tag=smoke_egg]", "Test failed")
    hatchling = "@e[type=dreadpatch:rimefrost_dragon,limit=1]"
    check("into a Rimefrost dragon", "execute positioned 4.5 200 11.5 if entity @e[type=dreadpatch:rimefrost_dragon,distance=..2]", "Test passed")
    check("white, like her", "data get entity " + hatchling + " Variant", '"white"')
    record("a hatchling", 0 <= age_ticks(hatchling) < 400, f"AgeTicks {age_ticks(hatchling)}")
    check("tamed to whoever laid the egg", "data get entity " + hatchling + " Owner", "[I; 4, 4, 4, 4]")
    rcon.run("kill @e[type=dreadpatch:rimefrost_dragon]")
    rcon.run("kill @e[type=dreadpatch:syndrigossa_egg]")
    rcon.run("kill @e[type=dreadpatch:whiteout_dominion]")
    rcon.run("setblock 4 199 11 minecraft:stone")

    # Hers (summoned as a NoAI queen's): whatever dies in it rises in her court where it fell, answering
    # to her: a dread beast from a cow (Ice and Fire's own mapping); a thrall from a villager or a creeper
    # and a dread beast from a bat (by their shape). A golem stays dead. When she falls her storm clears.
    rcon.run('summon dreadpatch:dread_queen 2.5 200 2.5 {NoAI:1b,UUID:[I;9,9,9,9]}')
    check("her Whiteout Dominion stands", 'summon dreadpatch:whiteout_dominion 8.5 200 7.5 {Owner:[I;9,9,9,9],RaisesDead:1b,Radius:16f,Lifetime:600}',
          "Summoned new Whiteout Dominion")
    for kind, at, risen in (("cow", "10.5 200 4.5", "iceandfire:dread_beast"), ("villager", "12.5 200 10.5", "iceandfire:dread_thrall"),
                            ("creeper", "4.5 200 10.5", "iceandfire:dread_thrall"), ("bat", "5.5 200 4.5", "iceandfire:dread_beast")):
        rcon.run(f'summon minecraft:{kind} {at} {{NoAI:1b,Tags:["smoke_dead"]}}')
        rcon.run("kill @e[tag=smoke_dead]")
        time.sleep(0.5)
        check(f"a {kind} killed in her storm rises", f"execute positioned {at} if entity @e[type={risen},distance=..2]", "Test passed")
        check(f"the risen {kind} answers to her", f"execute positioned {at} if entity @e[type={risen},distance=..2,nbt={{CommanderUUID:[I;9,9,9,9]}}]",
              "Test passed")
    rcon.run('summon minecraft:iron_golem 13.5 200 6.5 {NoAI:1b,Tags:["smoke_dead"]}')
    rcon.run("kill @e[tag=smoke_dead]")
    time.sleep(0.5)
    check("a golem killed in her storm stays dead",
          "execute positioned 13.5 200 6.5 if entity @e[distance=..2.5,type=!minecraft:item,type=!minecraft:experience_orb]", "Test failed")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    time.sleep(4.0)
    check("her storm clears when she falls", storms, "Test failed")
    clear_court()
    rcon.run("kill @e[type=item]")

    rcon.run('summon minecraft:iron_golem 12.5 200 7.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target"]}')
    rcon.run("effect give " + target + " minecraft:resistance 120 4 true")
    rcon.run("summon dreadpatch:dread_queen 2.5 200 7.5")
    raised = 0
    for _ in range(30):
        time.sleep(0.5)
        raised = court()
        if raised:
            break
    record("she raises her court", 1 <= raised <= 3, f"{raised} dread mobs near her (a wave is up to 3)")
    queen_id = re.search(r"\[I; [-0-9, ]+\]", rcon.run("data get entity @e[type=dreadpatch:dread_queen,limit=1] UUID"))
    commanders = [re.search(r"\[I; [-0-9, ]+\]", rcon.run(f"execute positioned 8 200 8 run data get entity @e[type={kind},limit=1,distance=..24] CommanderUUID"))
                  for kind in court_types]
    commanders = [c.group(0) for c in commanders if c]
    record("her court answers to her", bool(queen_id) and bool(commanders) and all(c == queen_id.group(0) for c in commanders),
           f"queen {queen_id and queen_id.group(0)}, commanders {commanders}")
    frenzied = 0
    for _ in range(60):
        time.sleep(0.5)
        frenzied = court(',nbt={active_effects:[{id:"minecraft:strength"}]}')
        if frenzied:
            break
    record("Beater's Decree frenzies her court", frenzied >= 1, "no court member got Strength within 30 s")
    # Her Whiteout Dominion: the first 15 s into a fight, over the golem too.
    stormed = False
    for _ in range(50):
        stormed = "Test passed" in rcon.run("execute if entity @e[type=dreadpatch:whiteout_dominion,nbt={RaisesDead:1b}]")
        if stormed:
            break
        time.sleep(0.5)
    record("she raises Whiteout Dominion", stormed, "no storm of hers within 25 s more")
    # Her dread phantoms (0.45.0): 1-2, the first 15 s into a fight (she's been at it at least that long by now), hers.
    m = re.search(r"count: (\d+)", rcon.run("execute if entity @e[type=dreadpatch:dread_phantom]"))
    called = int(m.group(1)) if m else 0
    record("she calls 1-2 dread phantoms", 1 <= called <= 2, f"{called} dread phantoms")
    summoner = re.search(r"\[I; [-0-9, ]+\]", rcon.run("data get entity @e[type=dreadpatch:dread_phantom,limit=1] Summoner"))
    record("her phantoms answer to her", bool(queen_id) and bool(summoner) and summoner.group(0) == queen_id.group(0),
           f"queen {queen_id and queen_id.group(0)}, phantom's summoner {summoner and summoner.group(0)}")
    time.sleep(0.6)
    record("her storm takes the golem", stormed and has_effect(target, "dreadpatch:whiteout"), "no Whiteout on the golem")
    # Her Dread Lich: the first 30 s into her fight (she's been at it about 20 s by now), answering to her.
    lich = "@e[type=iceandfire:dread_lich,limit=1]"
    liches = 0
    for _ in range(50):
        m = re.search(r"count: (\d+)", rcon.run("execute if entity @e[type=iceandfire:dread_lich]"))
        liches = int(m.group(1)) if m else 0
        if liches:
            break
        time.sleep(0.5)
    record("she raises a Dread Lich", liches == 1, f"{liches} liches within 25 s more")
    lich_commander = re.search(r"\[I; [-0-9, ]+\]", rcon.run("data get entity " + lich + " CommanderUUID"))
    record("her lich answers to her", bool(queen_id) and bool(lich_commander) and lich_commander.group(0) == queen_id.group(0),
           f"queen {queen_id and queen_id.group(0)}, lich's commander {lich_commander and lich_commander.group(0)}")
    time.sleep(3.0)
    m = re.search(r"count: (\d+)", rcon.run("execute if entity @e[type=iceandfire:dread_lich]"))
    record("and only one", m is not None and int(m.group(1)) == 1, f"{m and m.group(1)} liches 3 s later")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    clear_court()
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[type=item]")

    # She keeps her distance, like Iron's necromancer: risen 2 blocks from a golem (one she can't
    # hurt), she Barrow-Steps away, leaving her husk, and stays out of its reach rather than standing
    # in melee. The platform runs on to x 30, so she has somewhere to step to.
    rcon.run("fill 16 199 1 30 199 14 minecraft:stone")
    rcon.run("fill 16 200 1 30 205 14 minecraft:air")
    rcon.run('summon minecraft:iron_golem 8.5 200 7.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target"]}')
    rcon.run("effect give " + target + " minecraft:resistance 120 4 true")
    check("summons beside a golem", "summon dreadpatch:dread_queen 10.5 200 7.5", "Summoned new Dread Queen")
    near = "execute positioned 8.5 200 7.5 if entity @e[type=dreadpatch:dread_queen,distance=..4.5]"
    stepped = False
    for _ in range(8):
        time.sleep(0.5)
        stepped = stepped or "Test passed" in rcon.run("execute if entity @e[type=dreadpatch:barrow_husk]")
    record("cornered, she Barrow-Steps away", stepped, "no husk of hers within 4 s")
    close_calls = 0
    for _ in range(12):
        close_calls += "Test passed" in rcon.run(near)
        time.sleep(0.5)
    record("she keeps her distance", close_calls == 0, f"within 4.5 blocks of the golem at {close_calls} of 12 looks over 6 s")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    clear_court()
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[type=item]")
    rcon.run("kill @e[type=dreadpatch:barrow_husk]")

    # The queen rising with her dragon, as the boss will (WithSyndrigossa:1b): an ancient Syndrigossa
    # under her, the two bonded, carrying her in the saddle (not shaking her in its jaws as prey). What
    # it siphons is shared with her (the Soul Tether), and below half health she gets down off it.
    rider = "@e[type=dreadpatch:dread_queen,limit=1]"
    mount = "@e[type=dreadpatch:syndrigossa,limit=1]"
    check("summons the queen with her dragon", "summon dreadpatch:dread_queen 8.5 200 7.5 {WithSyndrigossa:1b}", "Summoned new Dread Queen")
    time.sleep(0.3)
    rcon.run("data merge entity " + mount + " {NoAI:1b}")
    check("she rides her Syndrigossa", "execute as " + rider + " on vehicle if entity @s[type=dreadpatch:syndrigossa]", "Test passed")
    # (A wild dragon ages a tick each tick.)
    record("her dragon is ancient", ANCIENT_TICKS <= age_ticks(mount) < ANCIENT_TICKS + 200, f"AgeTicks {age_ticks(mount)}")
    queen_uuid = re.search(r"entity data: (\[I;[-0-9, ]+\])", rcon.run("data get entity " + rider + " UUID"))
    dragon_uuid = re.search(r"entity data: (\[I;[-0-9, ]+\])", rcon.run("data get entity " + mount + " UUID"))
    bonded_q = re.search(r"entity data: (\[I;[-0-9, ]+\])", rcon.run("data get entity " + rider + " Syndrigossa"))
    bonded_d = re.search(r"entity data: (\[I;[-0-9, ]+\])", rcon.run("data get entity " + mount + " Queen"))
    record("the two are bonded", None not in (queen_uuid, dragon_uuid, bonded_q, bonded_d)
           and bonded_q.group(1) == dragon_uuid.group(1) and bonded_d.group(1) == queen_uuid.group(1),
           f"queen {queen_uuid and queen_uuid.group(1)} -> {bonded_q and bonded_q.group(1)}, dragon {dragon_uuid and dragon_uuid.group(1)} -> {bonded_d and bonded_d.group(1)}")
    # Seated as Ice and Fire seats a rider: her feet at its rider position plus her height, on its back
    # (full-grown, about 9 blocks up), not inside its body where nothing can reach her.
    def y_of(selector):
        m = re.search(r"entity data: \[[-0-9.]+d, ([-0-9.]+)d", rcon.run("data get entity " + selector + " Pos"))
        return float(m.group(1)) if m else -1000.0
    time.sleep(0.2)
    seat = y_of(rider) - y_of(mount)
    record("she sits on its back", 8.0 <= seat <= 11.0, f"her feet {seat:.2f} above its")
    time.sleep(3.0)
    record("she rides in the saddle, not in its jaws", health(rider) == 300.0 and "Test passed" in rcon.run(
        "execute as " + rider + " on vehicle if entity @s[type=dreadpatch:syndrigossa]"), f"queen health {health(rider)} after 3 s riding")
    rcon.run('summon minecraft:iron_golem 13.5 200 3.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target"]}')
    # (The dragon above half its health, or it lands to let her off.)
    rcon.run("data merge entity " + rider + " {Health:200.0f}")
    rcon.run("data merge entity " + mount + " {Health:400.0f}")
    rcon.run("damage " + target + " 5 dreadpatch:withering_breath by " + mount)
    shared_q, shared_d = health(rider), health(mount)
    record("its breath's siphon is shared with her", abs(shared_q - 200.25) < 0.01 and abs(shared_d - 400.25) < 0.01,
           f"queen {shared_q}, dragon {shared_d}, expected 200.25 and 400.25")
    rcon.run("data merge entity " + rider + " {Health:140.0f}")
    time.sleep(1.0)
    # With no vehicle, "on vehicle" leaves nothing to test: no output at all.
    off = "Test passed" not in rcon.run("execute as " + rider + " on vehicle if entity @s")
    record("below half health she gets down", off, "she's still riding")
    stepped_down = "Test passed" in rcon.run("execute if entity @e[type=dreadpatch:barrow_husk]") or has_effect(rider, "minecraft:slow_falling")
    record("by Barrow-Step, or drifting down", stepped_down, "no husk of hers and no Slow Falling")
    time.sleep(2.0)
    record("and lands alive", health(rider) > 0, f"queen health {health(rider)}")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")
    rcon.run("kill @e[type=dreadpatch:barrow_husk]")
    # Her dragon at half its health (250 of the dev run's 500): it lands (it's on the platform already) and
    # she steps off, alive. The owner couldn't hit her in the saddle, so this is what brings her down.
    rcon.run("summon dreadpatch:dread_queen 8.5 200 7.5 {WithSyndrigossa:1b}")
    time.sleep(0.3)
    rcon.run("data merge entity " + mount + " {NoAI:1b}")
    rcon.run("data merge entity " + mount + " {Health:240.0f}")
    time.sleep(1.5)
    record("her dragon at half, she's set down", "Test passed" not in rcon.run("execute as " + rider + " on vehicle if entity @s")
           and health(rider) > 0, f"queen health {health(rider)}")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")
    rcon.run("kill @e[type=dreadpatch:barrow_husk]")
    # Her dragon killed under her, she's thrown clear and lives.
    rcon.run("summon dreadpatch:dread_queen 8.5 200 7.5 {WithSyndrigossa:1b}")
    time.sleep(0.3)
    rcon.run("data merge entity " + mount + " {NoAI:1b}")
    rcon.run("damage " + mount + " 100000 minecraft:generic")
    time.sleep(2.0)
    record("her dragon dead, she's off it and alive", "Test passed" not in rcon.run("execute as " + rider + " on vehicle if entity @s")
           and health(rider) > 0, f"queen health {health(rider)}")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")
    clear_court()
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[type=item]")
    rcon.run("kill @e[type=dreadpatch:barrow_husk]")

    # Rimewalk, her Frost Walker: still water round her feet freezes to frosted ice, under 4 blocks
    # out (Frost Walker II), and ice of hers that's melting is made whole; in water, the surface round
    # her freezes, but not where she is. A pool sunk in the platform (x 18-28, z 3-11); NoAI queens.
    def block_is(x, y, z, block):
        return "Test passed" in rcon.run(f"execute if block {x} {y} {z} {block}")
    rcon.run("fill 18 198 3 28 198 11 minecraft:stone")
    rcon.run("fill 18 199 3 28 199 11 minecraft:water")
    check("summons her at the water's edge", "summon dreadpatch:dread_queen 16.5 200 7.5 {NoAI:1b,OnGround:1b}", "Summoned new Dread Queen")
    time.sleep(0.5)
    record("the water by her freezes", block_is(18, 199, 7, "minecraft:frosted_ice") and block_is(19, 199, 7, "minecraft:frosted_ice"),
           "no frosted ice 2 and 3 blocks out")
    record("not 4 blocks out", block_is(20, 199, 7, "minecraft:water"), "the water 4 blocks out froze")
    rcon.run("setblock 18 199 7 minecraft:frosted_ice[age=3]")
    time.sleep(0.3)
    record("her ice stays whole", block_is(18, 199, 7, "minecraft:frosted_ice[age=0]"), "ice by her stayed melting")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    check("summons her in the water", "summon dreadpatch:dread_queen 25.5 199 7.5 {NoAI:1b}", "Summoned new Dread Queen")
    time.sleep(0.5)
    record("in water, the surface round her freezes", block_is(23, 199, 7, "minecraft:frosted_ice") and block_is(27, 199, 7, "minecraft:frosted_ice"),
           "no frosted ice 2 blocks either side of her")
    record("but not where she is", block_is(25, 199, 7, "minecraft:water"), "she's frozen in")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    clear_court()

    # Whiteout Dominion freezes its water too, in patches as its snow drifts in (all of it at full
    # cover), holds that ice whole while it stands, and thaws it when it ends. The pool again, all
    # water, under a storm 8 blocks round (no caster: anyone's) that lasts 10 s: its cover passes 0.6,
    # where every block's ice lies, between 6 s and 8.2 s.
    def frosted_in_pool():
        # Cloned out of the storm's reach (x 1-11), filtered: the count is the pool's frosted ice.
        out = rcon.run("clone 18 199 3 28 199 11 1 230 3 filtered minecraft:frosted_ice")
        rcon.run("fill 1 230 3 11 230 11 minecraft:air")
        m = re.search(r"cloned (\d+)", out)
        return int(m.group(1)) if m else 0
    rcon.run("fill 18 199 3 28 199 11 minecraft:water")
    check("a storm over the pool", "summon dreadpatch:whiteout_dominion 23.5 200 7.5 {Lifetime:200,Radius:8f}", "Summoned new")
    iced = 0
    for _ in range(40):
        time.sleep(0.25)
        iced = frosted_in_pool()
        if iced == 99:
            break
    record("its water freezes over", iced == 99, f"{iced} of the pool's 99 blocks frozen in 10 s")
    rcon.run("setblock 20 199 7 minecraft:frosted_ice[age=3]")
    time.sleep(0.6)
    record("and its ice holds", block_is(20, 199, 7, "minecraft:frosted_ice[age=0]"), "ice in the storm stayed melting")
    gone = False
    for _ in range(30):
        time.sleep(0.25)
        gone = "Test failed" in rcon.run("execute if entity @e[type=dreadpatch:whiteout_dominion]")
        if gone:
            break
    time.sleep(0.2)
    left = frosted_in_pool()
    record("its ice thaws when it ends", gone and left == 0 and block_is(18, 199, 3, "minecraft:water") and block_is(23, 199, 7, "minecraft:water"),
           f"storm gone {gone}, {left} frosted ice left")
    # A Dread Gate. A Gravemaster's Key used on its frame needs a player; a gate summoned by hand finds
    # its frame round where it stands, the same way. A dreadstone frame round a 3x4 opening at the
    # platform's north edge, open sky in front of it: after the 5 s ritual the queen comes through
    # riding an ancient Syndrigossa, and the gate shatters, its frame left.
    queen_sel = "@e[type=dreadpatch:dread_queen,limit=1]"
    rcon.run("fill 3 199 2 7 204 2 iceandfire:dread_stone_bricks")
    rcon.run("fill 4 200 2 6 203 2 minecraft:air")
    check("a gate summoned in its frame", "summon dreadpatch:dread_gate 5.5 200 2.5", "Summoned new")
    time.sleep(0.5)
    record("it fills its frame", all(block_is(x, y, 2, "dreadpatch:dread_gate") for x in (4, 5, 6) for y in (200, 203)),
           "no gate across the opening")
    came = False
    for _ in range(32):
        time.sleep(0.25)
        came = "Test passed" in rcon.run("execute if entity " + queen_sel)
        if came:
            break
    record("the Dread Queen comes through", came, "no queen within 8 s")
    time.sleep(0.6)
    record("riding an ancient Syndrigossa, with room for one",
           "Test passed" in rcon.run("execute as " + queen_sel + " on vehicle if entity @s[type=dreadpatch:syndrigossa,nbt={Ancient:1b}]"),
           "she isn't on an ancient Syndrigossa")
    record("and the gate is gone, its frame left", block_is(5, 201, 2, "minecraft:air") and block_is(3, 201, 2, "iceandfire:dread_stone_bricks")
           and "Test failed" in rcon.run("execute if entity @e[type=dreadpatch:dread_gate]"), "the gate's still there, or its frame went")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")
    clear_court()
    rcon.run("kill @e[type=item]")
    # Broken mid-ritual, it collapses and nothing comes; a frame with a gap opens nothing.
    rcon.run("summon dreadpatch:dread_gate 5.5 200 2.5")
    time.sleep(0.5)
    rcon.run("setblock 3 201 2 minecraft:air")
    time.sleep(0.5)
    record("breaking its frame collapses it", block_is(5, 201, 2, "minecraft:air") and block_is(4, 200, 2, "minecraft:air")
           and "Test failed" in rcon.run("execute if entity @e[type=dreadpatch:dread_gate]"), "the gate outlived its frame")
    time.sleep(5.0)
    record("and nothing comes through", "Test failed" in rcon.run("execute if entity @e[type=dreadpatch:dread_queen]"), "a queen came anyway")
    rcon.run("summon dreadpatch:dread_gate 5.5 200 2.5")
    time.sleep(0.5)
    record("a frame with a gap opens nothing", block_is(5, 201, 2, "minecraft:air")
           and "Test failed" in rcon.run("execute if entity @e[type=dreadpatch:dread_gate]"), "a gate opened in a broken frame")
    # In a room too low for her dragon (as a Mausoleum's hall is), she comes on foot, out of the gate itself.
    rcon.run("fill 9 199 1 17 206 13 minecraft:stone hollow")
    rcon.run("fill 11 200 7 15 204 7 iceandfire:dread_stone_bricks")
    rcon.run("fill 12 201 7 14 203 7 minecraft:air")
    rcon.run("summon dreadpatch:dread_gate 13.5 201 7.5")
    came = False
    for _ in range(32):
        time.sleep(0.25)
        came = "Test passed" in rcon.run("execute positioned 13.5 201 7.5 if entity @e[type=dreadpatch:dread_queen,distance=..2]")
        if came:
            break
    time.sleep(0.6)
    record("in a low room she comes on foot, in the gate", came and "Test failed" in rcon.run("execute if entity @e[type=dreadpatch:syndrigossa]"),
           f"queen in the gate: {came}; a Syndrigossa came too, or she didn't come")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")
    clear_court()
    rcon.run("kill @e[type=item]")
    # Dread scuttlers climb walls like spiders (0.41.0): one at the foot of a solid block 4 high, a
    # golem (Resistance V) on top that has hit it, is up on top with it within 10 s; it couldn't jump it.
    rcon.run("fill 1 239 1 30 239 14 minecraft:stone")
    rcon.run("fill 1 240 1 30 250 14 minecraft:air")
    rcon.run("fill 14 240 4 18 243 10 minecraft:stone")
    rcon.run('summon minecraft:iron_golem 16.5 244 7.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_climb_target"]}')
    rcon.run("effect give @e[tag=smoke_climb_target,limit=1] minecraft:resistance 60 4 true")
    rcon.run('summon iceandfire:dread_scuttler 9.5 240 7.5 {PersistenceRequired:1b,Tags:["smoke_climber"]}')
    time.sleep(2.0)  # past its rise
    rcon.run("damage @e[tag=smoke_climber,limit=1] 1 minecraft:mob_attack by @e[tag=smoke_climb_target,limit=1]")
    climbed = False
    for _ in range(20):
        time.sleep(0.5)
        m = re.search(r"entity data: \[[-0-9.]+d, ([-0-9.]+)d", rcon.run("data get entity @e[tag=smoke_climber,limit=1] Pos"))
        if m and float(m.group(1)) >= 243.9:
            climbed = True
            break
    record("a dread scuttler climbs a wall to its target", climbed, f"still at y {m and m.group(1)} 10 s on, the top at 244")
    rcon.run("kill @e[tag=smoke_climber]")
    rcon.run("kill @e[tag=smoke_climb_target]")
    rcon.run("fill 1 239 1 30 250 14 minecraft:air")

    # Dread dragonsteel and Rimefrost dragonscale (0.42.0). Dragon armour against Rimefrost breath, as Ice
    # and Fire's against its own: two zombies in the same armour (100 health), one hit by Rimefrost breath,
    # one by a mob's attack, as hard: past the armour, the breath lands at 30% of the attack (helmet 10%, chestplate 30%,
    # leggings 20%, boots 10% off), for our dread dragonsteel and for Ice and Fire's ice dragonsteel.
    rcon.run("fill 1 239 1 30 239 14 minecraft:stone")
    rcon.run("fill 1 240 1 30 250 14 minecraft:air")

    def pieces_of(set_name):
        return [f"{set_name}_{piece}" for piece in ("boots", "leggings", "chestplate", "helmet")]

    regalia = ["dreadpatch:dread_queen_greaves", "dreadpatch:dread_queen_robe", "dreadpatch:dread_queen_bodice", "dreadpatch:dread_queen_crown"]

    def armoured(tag, x, pieces):
        worn = ",".join(f'{{id:"{piece}",count:1}}' for piece in pieces)
        rcon.run(f'summon minecraft:zombie {x} 240 4.5 {{NoAI:1b,PersistenceRequired:1b,Tags:["{tag}"],ArmorItems:[{worn}]}}')
        rcon.run(f"attribute @e[tag={tag},limit=1] minecraft:generic.max_health base set 100")
        rcon.run(f"data merge entity @e[tag={tag},limit=1] {{Health:100.0f}}")

    for pieces, label in ((pieces_of("dreadpatch:dread_dragonsteel"), "dread dragonsteel"),
                          (pieces_of("iceandfire:dragonsteel_ice"), "Ice and Fire's ice dragonsteel"),
                          (regalia, "the Dread Queen's regalia")):
        armoured("smoke_breathed", 4.5, pieces)
        armoured("smoke_struck", 8.5, pieces)
        time.sleep(0.3)
        rcon.run("damage @e[tag=smoke_breathed,limit=1] 40 dreadpatch:withering_breath")
        # (A mob's attack: armour cuts it as it does the breath; vanilla's generic damage goes straight past armour.)
        rcon.run("damage @e[tag=smoke_struck,limit=1] 40 minecraft:mob_attack")
        breathed, struck = 100.0 - health("@e[tag=smoke_breathed,limit=1]"), 100.0 - health("@e[tag=smoke_struck,limit=1]")
        record(f"{label} takes 70% off Rimefrost breath", struck > 0 and abs(breathed - 0.3 * struck) < 0.6,
               f"breath took {breathed:.2f}, a plain hit {struck:.2f} (30% would be {0.3 * struck:.2f})")
        rcon.run("kill @e[tag=smoke_breathed]")
        rcon.run("kill @e[tag=smoke_struck]")
    # Her regalia (0.43.0) is as strong as dread dragonsteel: the same armour and toughness, worn.
    armoured("smoke_regalia", 4.5, regalia)
    armoured("smoke_steel", 8.5, pieces_of("dreadpatch:dread_dragonsteel"))
    time.sleep(0.3)

    def worn(tag, attribute):
        out = rcon.run(f"attribute @e[tag={tag},limit=1] {attribute} get")
        found = re.search(r"is (-?[\d.]+)", out)
        return float(found.group(1)) if found else None

    for attribute in ("minecraft:generic.armor", "minecraft:generic.armor_toughness"):
        queen_value, steel_value = worn("smoke_regalia", attribute), worn("smoke_steel", attribute)
        record(f"her regalia's {attribute.split('.')[-1]} is dread dragonsteel's", queen_value is not None and queen_value == steel_value and queen_value > 0,
               f"regalia {queen_value}, dread dragonsteel {steel_value}")
    rcon.run("kill @e[tag=smoke_regalia]")
    rcon.run("kill @e[tag=smoke_steel]")
    # Every piece of the three sets takes a trim at the smithing table (0.44.0): #minecraft:trimmable_armor.
    rcon.run('summon minecraft:armor_stand 12.5 240 10.5 {NoGravity:1b,Tags:["smoke_trimmable"]}')
    untrimmable = []
    for piece in regalia + pieces_of("dreadpatch:rimefrost_dragonscale") + pieces_of("dreadpatch:dread_dragonsteel"):
        rcon.run(f"item replace entity @e[tag=smoke_trimmable,limit=1] weapon.mainhand with {piece}")
        if "Test passed" not in rcon.run("execute if items entity @e[tag=smoke_trimmable,limit=1] weapon.mainhand #minecraft:trimmable_armor"):
            untrimmable.append(piece)
    record("all twelve pieces take trims", not untrimmable, f"not trimmable: {untrimmable}")
    rcon.run("kill @e[tag=smoke_trimmable]")
    # A dread dragonsteel sword's hit leaves Withering Rime (a husk's, which goes for iron golems).
    # (No Resistance: a hit brought to nothing carries nothing. An iron golem has 100 health.)
    rcon.run('summon minecraft:iron_golem 12.5 240 7.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_rimed"]}')
    rcon.run('summon minecraft:husk 10.5 240 7.5 {PersistenceRequired:1b,Tags:["smoke_wielder"],'
             'HandItems:[{id:"dreadpatch:dread_dragonsteel_sword",count:1},{}],HandDropChances:[0.0f,0.0f]}')
    rimed = False
    for _ in range(24):
        time.sleep(0.5)
        if has_effect("@e[tag=smoke_rimed,limit=1]", "dreadpatch:withering_rime"):
            rimed = True
            break
    record("a dread dragonsteel sword's hit leaves Withering Rime", rimed, "no Withering Rime on the golem within 12 s")
    rcon.run("kill @e[tag=smoke_wielder]")
    rcon.run("kill @e[tag=smoke_rimed]")
    rcon.run("fill 1 239 1 30 250 14 minecraft:air")

    # Nothing fights a dragon's corpse (0.35.0): Ice and Fire counts a corpse alive, so a dread thrall
    # fighting an ice dragon when it died kept hacking at its corpse (6 s on, still its target, before
    # the fix). On a platform of its own, higher up, one at a time (an iron golem goes for a thrall): a
    # dread thrall, then an iron golem (not of the Dread), takes a dragon for its target, the dragon is
    # killed into a corpse, and within a second the corpse is invulnerable (so vanilla's targeting won't
    # see it as an enemy) and the fighter has dropped it for good. With no target at all, "on target"
    # ends the command with no output (as "on vehicle" does).
    rcon.run("fill 1 239 1 30 239 14 minecraft:stone")
    rcon.run("fill 1 240 1 30 250 14 minecraft:air")
    on_corpse = "execute as @e[tag=smoke_corpse_fighter,limit=1] on target if entity @s[tag=smoke_dead_dragon]"
    for kind, rise, a in (("iceandfire:dread_thrall", 2.5, "a"), ("minecraft:iron_golem", 0.5, "an")):
        name = kind.split(":")[1].replace("_", " ")
        rcon.run('summon iceandfire:ice_dragon 20.5 240 7.5 {NoAI:1b,AgeTicks:1300000,Tags:["smoke_dead_dragon"]}')
        rcon.run(f'summon {kind} 12.5 240 7.5 {{PersistenceRequired:1b,Tags:["smoke_corpse_fighter"]}}')
        time.sleep(rise)
        rcon.run("damage @e[tag=smoke_corpse_fighter,limit=1] 1 minecraft:mob_attack by @e[tag=smoke_dead_dragon,limit=1]")
        time.sleep(1.0)
        check(f"{a} {name} takes a live dragon for its target", on_corpse, "Test passed")
        rcon.run("damage @e[tag=smoke_dead_dragon,limit=1] 100000 minecraft:generic")
        time.sleep(1.0)
        check("the dragon lies as a corpse", "execute if entity @e[tag=smoke_dead_dragon,nbt={ModelDead:1b}]", "Test passed")
        check("its corpse is invulnerable", "execute if entity @e[tag=smoke_dead_dragon,nbt={Invulnerable:1b}]", "Test passed")
        out = rcon.run(on_corpse)
        record(f"the {name} drops the corpse as its target", "Test passed" not in out, "still its target: " + out.strip())
        time.sleep(3.0)
        out = rcon.run(on_corpse)
        record(f"and doesn't take it back ({name})", "Test passed" not in out, "its target again: " + out.strip())
        check(f"the {name} is still there to ask", "execute if entity @e[tag=smoke_corpse_fighter]", "Test passed")
        rcon.run("kill @e[tag=smoke_corpse_fighter]")
        rcon.run("kill @e[tag=smoke_dead_dragon]")
    rcon.run("fill 1 239 1 30 239 14 minecraft:air")

    rcon.run("fill 3 199 2 7 204 2 minecraft:air")
    rcon.run("fill 9 199 1 17 206 13 minecraft:air")

    rcon.run("fill 18 198 3 28 198 11 minecraft:air")
    rcon.run("fill 1 199 1 30 199 14 minecraft:air")
    rcon.run("forceload remove 0 0 31 15")

    rcon.run("difficulty " + (first_difficulty.group(1).lower() if first_difficulty else "easy"))
    print(f"{sum(results)}/{len(results)} passed")
    if "--keep" not in sys.argv:
        rcon.run("stop")
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
