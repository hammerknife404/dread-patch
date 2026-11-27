"""Smoke test for the Dread Queen against a running dev server, over RCON. Python 3, stdlib only.

Setup once, in run/server.properties (gitignored): enable-rcon=true, rcon.password=<anything>,
server-ip=127.0.0.1. Then start `./gradlew runServer`, wait for "RCON running", and run
`python3 tools/devserver/queen_smoke.py`. It prints PASS/FAIL per check, stops the server when it
is done (pass --keep to leave it running) and exits non-zero if anything failed.
"""
import atexit
import math
import pathlib
import random
import re
import socket
import struct
import sys
import time

PROPERTIES = pathlib.Path(__file__).resolve().parents[2] / "run" / "server.properties"
# The server config, which NeoForge reloads when it changes on disk.
SERVER_CONFIG = pathlib.Path(__file__).resolve().parents[2] / "run" / "config" / "dreadpatch-server.toml"


def set_config(key, value):
    """Sets a value in the server config (NeoForge re-reads it) and returns what it was, or None if it isn't there."""
    text = SERVER_CONFIG.read_text()
    found = re.search(rf"^\s*{key}\s*=\s*(\S+)", text, flags=re.M)
    if not found:
        return None
    if found.group(1) != str(value):
        SERVER_CONFIG.write_text(text[:found.start(1)] + str(value) + text[found.end(1):])
        time.sleep(2.5)
    return found.group(1)


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
                  "dreadpatch:summon_court", "dreadpatch:create_rimefrost_dragon", "dreadpatch:summon_dread_phantoms",
                  "dreadpatch:rimefire_blast", "dreadpatch:sublimate"}
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
    # No whiteout weather in the Dreadlands (0.54.0) but where a check asks for it: one could come with any
    # "weather rain" and put Whiteout on what a check reads. Put back on the way out, however the run ends.
    whiteouts_were = set_config("whiteout_storms", 0)
    if whiteouts_were is not None:
        atexit.register(set_config, "whiteout_storms", whiteouts_were)
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
    # At midnight: by day Ice and Fire wakes its own on their first tick, so a read a tick late failed (0.60.0's
    # run on NeoForge 21.1.230; eight tries there slept on at midnight, and woke within a second at noon).
    # No mob spawning meanwhile: monsters spawn on the dark platform, and one stayed to push the rising queen
    # off her spot in the next check (0.60.0's first run with it).
    spawning = re.search(r"set to: (\w+)", rcon.run("gamerule doMobSpawning"))
    rcon.run("gamerule doMobSpawning false")
    rcon.run("time set midnight")
    rcon.run(surface + 'summon iceandfire:ice_dragon ~16 ~ ~ {NoAI:1b,AgeTicks:2500000,Sleeping:1b,Tags:["smoke_sleeper"]}')
    rcon.run(surface + 'summon dreadpatch:rimefrost_dragon ~16 ~ ~8 {NoAI:1b,AgeTicks:2500000,Sleeping:1b,Tags:["smoke_waker"]}')
    time.sleep(1.0)
    check("an ice dragon loaded asleep sleeps", "data get entity @e[tag=smoke_sleeper,limit=1] Sleeping", "1b")
    check("a Rimefrost dragon loaded asleep is awake, even at night", "data get entity @e[tag=smoke_waker,limit=1] Sleeping", "0b")
    rcon.run("time set noon")
    rcon.run("gamerule doMobSpawning " + (spawning.group(1) if spawning else "true"))
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

    # Her bolt as a player's spell: an armor stand casts it (level 5: 14 damage + 7% of 100: the share by level
    # since 0.51.0, 3% at level 1 and a point a level) at a golem 6 blocks off.
    rcon.run(golem.format(x=8.5))
    rcon.run('summon minecraft:armor_stand 2.5 200 4.5 {Tags:["smoke_caster"],Rotation:[-90f,0f]}')
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:withering_rime_bolt 5")
    time.sleep(1.0)
    hp = health(target)
    record("her bolt (cast) hits for 14 + 7% of max health at level 5", 76 <= hp <= 81, f"golem health {hp}, expected about 79")
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

    # Sublimate (0.74.0): an instant piercing cone, 12 blocks at level 5, from an armor stand facing +x: a golem
    # 6 off and one 10 off behind it are both struck (8 damage) and left Rimefrost I; one 6 off but 8 to the
    # side, outside the cone, isn't touched.
    rcon.run('summon minecraft:iron_golem 8.5 200 4.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target","smoke_sub_near"]}')
    rcon.run('summon minecraft:iron_golem 12.5 200 4.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target","smoke_sub_far"]}')
    rcon.run('summon minecraft:iron_golem 8.5 200 12.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target","smoke_sub_side"]}')
    rcon.run('summon minecraft:armor_stand 2.5 200 4.5 {Tags:["smoke_caster"],Rotation:[-90f,0f]}')
    rcon.run("cast @e[tag=smoke_caster,limit=1] dreadpatch:sublimate 5")
    time.sleep(0.5)
    near, far, side = (health(f"@e[tag=smoke_sub_{n},limit=1]") for n in ("near", "far", "side"))
    record("Sublimate pierces: both golems in its cone are struck", 88 <= near < 100 and 88 <= far < 100, f"near {near}, far {far}")
    record("Sublimate leaves Rimefrost and (0.74.1) Whiteout on both",
           all(has_effect(f"@e[tag=smoke_sub_{n},limit=1]", e) for n in ("near", "far") for e in ("dreadpatch:rimefrost", "dreadpatch:whiteout")),
           "no Rimefrost or Whiteout")
    record("Sublimate misses one outside its cone", side == 100 and not has_effect("@e[tag=smoke_sub_side,limit=1]", "dreadpatch:rimefrost"), f"side {side}")
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[tag=smoke_caster]")
    # A Dread Lich killed by a player now and then leaves a Sublimate scroll: its table (10%; none in 60 is 0.2%)
    # rolled here; the loot modifier adding it to Ice and Fire's lich (on a player's kill) needs a player.
    rcon.run("kill @e[type=item]")
    rcon.run('summon iceandfire:dread_lich 4.5 200 8.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_lich_loot"]}')
    for _ in range(60):
        rcon.run("execute as @e[tag=smoke_lich_loot,limit=1] run loot spawn 4.5 201 8.5 loot dreadpatch:entities/dread_lich_sublimate")
    scrolls = count("irons_spellbooks:scroll")
    record("the lich's table gives a Sublimate scroll now and then", 1 <= scrolls <= 20, f"{scrolls} in 60")
    rcon.run("kill @e[tag=smoke_lich_loot]")
    rcon.run("kill @e[type=item]")

    # Rimefire Blast (0.51.0): its cone, summoned (a mob can't hold a continuous spell through /cast: Iron's
    # Cone of Cold doesn't fire that way either), owned by a villager facing a golem, hits it for its
    # damage and leaves Withering Rime.
    rcon.run('summon minecraft:villager 2.5 200 4.5 {NoAI:1b,UUID:[I;2,2,2,2],Tags:["smoke_caster"],Rotation:[-90f,0f]}')
    rcon.run(golem.format(x=6.5))
    rcon.run('summon dreadpatch:rimefire_blast 2.5 201 4.5 {Owner:[I;2,2,2,2],Damage:4.5f}')
    time.sleep(0.6)
    hp = health(target)
    record("Rimefire Blast's cone hits for its damage", 93.0 <= hp <= 96.0, f"golem health {hp}, expected about 95")
    record("and leaves Withering Rime", has_effect(target, "dreadpatch:withering_rime"), "no Withering Rime on the golem")
    rcon.run("kill @e[type=dreadpatch:rimefire_blast]")
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
    rcon.run("weather clear")  # and the weather turns: a phantom doesn't burn in the rain (0.51.0's last run)
    rcon.run('summon minecraft:phantom 3.5 212 12.5 {NoAI:1b,Tags:["smoke_vanilla_phantom"]}')
    rcon.run('summon dreadpatch:dread_phantom 6.5 212 12.5 {NoAI:1b,Tags:["smoke_phantom"]}')
    phantom = "@e[tag=smoke_phantom,limit=1]"
    check("a dread phantom summons", "execute if entity " + phantom, "Test passed")
    time.sleep(6.0)  # a phantom in full sun catches fire about 4% of ticks: 2 s once left the control unlit (0.62.0)
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
    rcon.run("effect give @e[tag=smoke_slowed,limit=1] dreadpatch:rimefrost 10 0")
    time.sleep(0.2)
    mild = speed()
    record("Rimefrost I slows by a tenth (plain snow)", before > 0 and abs(mild / before - 0.90) < 0.01, f"speed {before} -> {mild}")
    rcon.run("effect clear @e[tag=smoke_slowed,limit=1] dreadpatch:rimefrost")
    rcon.run("effect give @e[tag=smoke_slowed,limit=1] dreadpatch:rimefrost 10 1")
    time.sleep(0.2)
    after = speed()
    record("Rimefrost II slows by a quarter", before > 0 and abs(after / before - 0.75) < 0.01, f"speed {before} -> {after}")
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

    # The Dread Fish (0.71.0), in a pool of its own off the platforms: sized as it spawns (0.6-1.4 long, 10-18
    # health with it), 4 armour, undead and aquatic, of the Dread; it eats dropped food; in a fight its lure's
    # pulse lays Rimefrost or Whiteout on what's near and sends it into a Frenzy; its loot.
    rcon.run("forceload add 40 40 48 48")
    rcon.run("fill 40 196 40 48 199 48 minecraft:stone")
    rcon.run("fill 40 200 40 48 204 48 minecraft:air")
    rcon.run("fill 42 197 42 45 199 45 minecraft:water")
    rcon.run("summon dreadpatch:dread_fish 43.5 197.5 43.5")
    rcon.run("tag @e[type=dreadpatch:dread_fish,x=43.5,y=198,z=43.5,distance=..3] add smoke_eel")
    fish = "@e[tag=smoke_eel,limit=1]"
    check("a Dread Fish summons", "execute if entity " + fish, "Test passed, count: 1")
    # Renamed from the Rime Eel (0.86.0), its id too: the old id is an alias, so old ones (and commands) still load.
    check("the old Rime Eel id still summons a Dread Fish", "summon dreadpatch:rime_eel 46.5 201 46.5 {Tags:[\"smoke_old_eel\"]}", "Summoned new Dread Fish")
    rcon.run("kill @e[tag=smoke_old_eel]")
    m = re.search(r"is ([0-9.]+)", rcon.run(f"attribute {fish} minecraft:generic.scale base get"))
    scale = float(m.group(1)) if m else -1.0
    record("sized 0.6-4 blocks across", 0.6 - 1e-6 <= scale * 2.0 <= 4.0 + 1e-6, f"scale {scale}")
    expected = math.floor(2 * (10 + 8 * (max(0.6, min(4.0, scale * 2.0)) - 0.6) / 1.9) + 0.5) / 2  # Java's Math.round
    m = re.search(r"is ([0-9.]+)", rcon.run(f"attribute {fish} minecraft:generic.max_health get"))
    record("its health with its size, 10-24.5", m is not None and abs(float(m.group(1)) - expected) < 0.01,
           f"{m.group(0) if m else 'none'}, expected {expected}")
    check("4 armour", f"attribute {fish} minecraft:generic.armor get", "is 4.0")
    for tag in ("#minecraft:undead", "#minecraft:aquatic"):
        check("the fish in " + tag, f"execute if entity @e[tag=smoke_eel,type={tag}]", "Test passed")
    rcon.run(f"data merge entity {fish} {{Health:4.0f}}")
    hurt = health(fish)
    rcon.run('summon item 43.5 199.2 43.5 {Item:{id:"minecraft:rotten_flesh",count:3},PickupDelay:0s,Tags:["smoke_food"]}')
    rcon.run('summon item 40.5 201 40.5 {Item:{id:"minecraft:golden_apple",count:1},PickupDelay:0s,Tags:["smoke_food_refused"]}')
    ate = False
    for _ in range(30):
        time.sleep(0.5)
        if "Test failed" in rcon.run("execute if entity @e[tag=smoke_food]"):
            ate = True
            break
    record("it eats dropped rotten flesh", ate, rcon.run("data get entity @e[tag=smoke_food,limit=1] Item.count").strip())
    record("healing by it", health(fish) > hurt, f"health {hurt} -> {health(fish)}")
    check("but leaves a golden apple", "execute if entity @e[tag=smoke_food_refused]", "Test passed")
    rcon.run('summon dreadpatch:dread_fish 47.5 200 47.5 {Tags:["smoke_eel_ashore"]}')
    back = False
    for _ in range(30):
        time.sleep(0.5)
        if "Test passed" in rcon.run("execute as @e[tag=smoke_eel_ashore] at @s if block ~ ~ ~ minecraft:water"):
            back = True
            break
    record("ashore, it makes for water", back, rcon.run("data get entity @e[tag=smoke_eel_ashore,limit=1] Pos").strip())
    rcon.run("kill @e[tag=smoke_eel_ashore]")
    rcon.run("kill @e[type=item]")
    # The Dread eat what's dropped (0.83.0, the owner: "Extend the food eating behavior of the dread fish to the dread beasts
    # ... most organic dropped items, but only food heals"): a hurt dread beast on a closed pad eats a bone (organic, no
    # healing) and rotten flesh (food: it heals), and leaves a golden apple.
    rcon.run("forceload add 60 40 68 48")
    rcon.run("fill 59 199 39 69 199 49 minecraft:stone")
    rcon.run("fill 59 200 39 69 203 49 minecraft:air")
    rcon.run("fill 59 200 39 69 201 39 minecraft:stone")
    rcon.run("fill 59 200 49 69 201 49 minecraft:stone")
    rcon.run("fill 59 200 39 59 201 49 minecraft:stone")
    rcon.run("fill 69 200 39 69 201 49 minecraft:stone")
    rcon.run('summon iceandfire:dread_beast 64.5 200 44.5 {PersistenceRequired:1b,Tags:["smoke_beast"]}')
    time.sleep(3.0)  # past its rise
    beast = "@e[tag=smoke_beast,limit=1]"
    rcon.run(f"data merge entity {beast} {{Health:4.0f}}")
    beast_hurt = health(beast)
    rcon.run('summon item 66.5 200.2 44.5 {Item:{id:"minecraft:bone",count:1},PickupDelay:0s,Tags:["smoke_beast_bone"]}')
    rcon.run('summon item 62.5 200.2 44.5 {Item:{id:"minecraft:rotten_flesh",count:1},PickupDelay:0s,Tags:["smoke_beast_food"]}')
    rcon.run('summon item 64.5 200.2 47.5 {Item:{id:"minecraft:golden_apple",count:1},PickupDelay:0s,Tags:["smoke_beast_refused"]}')
    eaten = False
    for _ in range(80):
        time.sleep(0.25)
        if "Test failed" in rcon.run("execute if entity @e[tag=smoke_beast_bone]") and "Test failed" in rcon.run("execute if entity @e[tag=smoke_beast_food]"):
            eaten = True
            break
    record("a dread beast eats a dropped bone and rotten flesh", eaten, "one was still there 20 s on")
    record("only the food heals it", health(beast) > beast_hurt and health(beast) <= beast_hurt + 1.0 + 1e-6,
           f"health {beast_hurt} -> {health(beast)} (rotten flesh heals 1, the bone none)")
    check("and it leaves a golden apple", "execute if entity @e[tag=smoke_beast_refused]", "Test passed")
    rcon.run("kill @e[tag=smoke_beast]")
    rcon.run("kill @e[type=item]")
    # Scuttlers too (0.84.0, the owner: "extend the beast eating to scuttlers too"): one hurt on the same pad eats rotten
    # flesh, healing by it.
    rcon.run('summon iceandfire:dread_scuttler 64.5 200 44.5 {PersistenceRequired:1b,Tags:["smoke_beast"]}')
    time.sleep(3.0)
    rcon.run(f"data merge entity {beast} {{Health:4.0f}}")
    scuttler_hurt = health(beast)
    rcon.run('summon item 66.5 200.2 44.5 {Item:{id:"minecraft:rotten_flesh",count:1},PickupDelay:0s,Tags:["smoke_beast_food"]}')
    eaten = False
    for _ in range(80):
        time.sleep(0.25)
        if "Test failed" in rcon.run("execute if entity @e[tag=smoke_beast_food]"):
            eaten = True
            break
    record("a dread scuttler eats dropped rotten flesh, healing by it", eaten and health(beast) > scuttler_hurt,
           f"eaten {eaten}, health {scuttler_hurt} -> {health(beast)}")
    rcon.run("kill @e[tag=smoke_beast]")
    rcon.run("kill @e[type=item]")
    # Dread thralls are never empty-handed, and shoot a bow or crossbow (0.84.4, the owner: "make sure dread thralls
    # can spawn with a bow or cross bow, and never without a weapon"). A summon with NBT skips Ice and Fire's own gear,
    # so each of these joins empty-handed.
    armed = []
    for i in range(6):
        rcon.run(f'summon iceandfire:dread_thrall {61.5 + i} 200 41.5 {{NoAI:1b,PersistenceRequired:1b,Tags:["smoke_thrall","smoke_thrall{i}"]}}')
    time.sleep(1.0)
    for i in range(6):
        m = re.search(r'"([a-z_]+:[a-z_]+)"', rcon.run(f"data get entity @e[tag=smoke_thrall{i},limit=1] HandItems[0].id"))
        armed.append(m.group(1) if m else None)
    record("a dread thrall that joins empty-handed is given a weapon", all(armed), f"main hands: {armed}")
    rcon.run("kill @e[tag=smoke_thrall]")
    for weapon in ("bow", "crossbow"):
        rcon.run("kill @e[type=arrow]")
        rcon.run('summon minecraft:cow 67.5 200 47.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_thrall_prey"]}')
        rcon.run(f'summon iceandfire:dread_thrall 60.5 200 40.5 {{PersistenceRequired:1b,Tags:["smoke_thrall"],'
                 f'HandItems:[{{id:"minecraft:{weapon}",count:1}},{{}}]}}')
        shot = False
        for _ in range(40):
            time.sleep(0.25)
            # An arrow that hits is gone at once: a hurt cow (10 health) counts as a shot too.
            if "Test passed" in rcon.run("execute if entity @e[type=arrow]") or health("@e[tag=smoke_thrall_prey,limit=1]") < 10.0:
                shot = True
                break
        record(f"a dread thrall with a {weapon} shoots at its prey", shot, "no arrow and no hurt cow within 10 s")
        rcon.run("kill @e[tag=smoke_thrall]")
        rcon.run("kill @e[tag=smoke_thrall_prey]")
        rcon.run("kill @e[type=arrow]")
    # 0.85.0: one whose arming was rolled stays unarmed (its claws); a knight's weapon reaches further under Better Mob
    # Combat Reimagined (the dev runs load it); a whole dread beast eats too.
    rcon.run('summon iceandfire:dread_thrall 62.5 200 42.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_claws"],NeoForgeData:{"dreadpatch:armed":1b}}')
    time.sleep(0.5)
    claws = rcon.run("data get entity @e[tag=smoke_claws,limit=1] HandItems[0]")
    record("a dread thrall rolled unarmed stays unarmed", "{}" in claws or "id" not in claws, claws.strip())
    rcon.run("kill @e[tag=smoke_claws]")
    rcon.run('summon iceandfire:dread_knight 62.5 200 44.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_reach"],'
             'HandItems:[{id:"minecraft:iron_sword",count:1},{}]}')
    time.sleep(0.5)
    reach = rcon.run("data get entity @e[tag=smoke_reach,limit=1] HandItems[0]")
    record("a dread knight's weapon reaches further under Better Mob Combat", "knight_reach" in reach, reach.strip()[:300])
    rcon.run("kill @e[tag=smoke_reach]")
    rcon.run("kill @e[type=item]")
    rcon.run('summon iceandfire:dread_beast 64.5 200 44.5 {PersistenceRequired:1b,Tags:["smoke_whole_beast"]}')
    time.sleep(2.0)
    rcon.run('summon item 66.5 200.2 44.5 {Item:{id:"minecraft:rotten_flesh",count:1},PickupDelay:0s,Tags:["smoke_whole_food"]}')
    eaten = False
    for _ in range(120):
        time.sleep(0.25)
        if "Test failed" in rcon.run("execute if entity @e[tag=smoke_whole_food]"):
            eaten = True
            break
    record("a whole dread beast eats what's dropped too", eaten, "the rotten flesh still there 30 s on")
    rcon.run("kill @e[tag=smoke_whole_beast]")
    rcon.run("kill @e[type=item]")
    # 0.87.0: Ice and Fire's lich staff carries Whiteout Dominion and Summon Dread Phantoms (its default components).
    rcon.run('summon minecraft:armor_stand 62.5 200 46.5 {NoGravity:1b,Tags:["smoke_lich_staff"],HandItems:[{id:"iceandfire:lich_staff",count:1},{}]}')
    check("the lich staff carries the Dread's spells",
          "execute if items entity @e[tag=smoke_lich_staff,limit=1] weapon.mainhand iceandfire:lich_staff[irons_spellbooks:spell_container]",
          "Test passed")
    rcon.run("kill @e[tag=smoke_lich_staff]")
    # 0.89.0: the overworld's dread weather starts and stops by command (it reaches only where its switches let it; with
    # no player near, its snow, cold and rising aren't seen here).
    rcon.run("dreadpatch config dread_weather_anywhere true")
    check("a dread storm starts over the overworld", "dreadpatch dreadweather start", "dread storm over the overworld")
    time.sleep(3.0)
    check("and stops", "dreadpatch dreadweather stop", "dread storm is over")
    rcon.run("dreadpatch config dread_weather_anywhere false")
    rcon.run("weather clear")
    # 0.88.0: the Dreadlands' village chests (Lootr's too) hold a dread key a fifth of the time; the overworld's don't.
    # One roll at a time: @e reaches every dimension, and dropped items merge.
    def key_rolls(dim, n):
        pre = f"execute in {dim} positioned 0.5 250 0.5 run "
        rcon.run(pre + "forceload add 0 0")
        hits = 0
        for _ in range(n):
            rcon.run(pre + "kill @e[type=item,distance=..8]")
            rcon.run(pre + "loot spawn ~ ~ ~ loot minecraft:chests/village/village_snowy_house")
            hits += "Test passed" in rcon.run(pre + 'execute if entity @e[type=item,distance=..8,nbt={Item:{id:"iceandfire:dread_key"}}]')
        rcon.run(pre + "kill @e[type=item,distance=..8]")
        rcon.run(pre + "forceload remove 0 0")
        return hits
    keys = key_rolls("dreadpatch:dreadlands", 40)
    record("a Dreadlands village chest now and then holds a dread key", keys > 0, f"{keys} in 40 chests")
    plain = key_rolls("minecraft:overworld", 40)
    record("but an overworld village chest never does", plain == 0, f"{plain} in 40 chests")
    rcon.run("forceload remove 60 40 68 48")
    # A wild Rimefrost dragon hunts what the Dread do (0.84.4, the owner: "Rimefrost dragons are also hostile to the same
    # things the dread mobs do"): a cow by it, hungry or not, but not a thrall.
    rcon.run("forceload add 400 400 416 416")
    rcon.run("fill 400 199 400 416 199 416 minecraft:stone")
    rcon.run("fill 400 200 400 416 210 416 minecraft:air")
    rcon.run('summon minecraft:cow 410.5 200 408.5 {PersistenceRequired:1b,Tags:["smoke_wild_prey"]}')
    rcon.run('summon iceandfire:dread_thrall 406.5 200 412.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_wild_thrall"],'
             'HandItems:[{id:"minecraft:stone_sword",count:1},{}]}')
    thrall_health = health("@e[tag=smoke_wild_thrall,limit=1]")
    rcon.run('summon dreadpatch:rimefrost_dragon 405.5 200 405.5 {PersistenceRequired:1b,Hunger:100,AgeTicks:600000,Tags:["smoke_wild_dragon"]}')
    hunted = False
    for _ in range(80):
        time.sleep(0.25)
        cow = health("@e[tag=smoke_wild_prey,limit=1]")
        if cow < 10.0:  # hurt (a cow has 10), or gone (-1)
            hunted = True
            break
    record("a wild Rimefrost dragon hunts a cow, fed or not", hunted, "the cow untouched 20 s on")
    record("and leaves a dread thrall be", health("@e[tag=smoke_wild_thrall,limit=1]") == thrall_health,
           f"thrall health {thrall_health} -> {health('@e[tag=smoke_wild_thrall,limit=1]')}")
    rcon.run("kill @e[tag=smoke_wild_dragon]")
    rcon.run("kill @e[tag=smoke_wild_prey]")
    rcon.run("kill @e[tag=smoke_wild_thrall]")
    rcon.run("kill @e[type=item]")
    # The queen and Syndrigossa never drown or choke in a wall (0.84.4, the owner: "Theyve suvived being burried
    # afterall"): a summoned one of each, NoAI, takes neither.
    rcon.run('summon dreadpatch:dread_queen 404.5 200 404.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_buried"]}')
    rcon.run('summon dreadpatch:syndrigossa 410.5 200 410.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_buried"]}')
    time.sleep(0.5)
    for who in ("dreadpatch:dread_queen", "dreadpatch:syndrigossa"):
        for kind in ("minecraft:in_wall", "minecraft:drown"):
            out = rcon.run(f"damage @e[tag=smoke_buried,type={who},limit=1] 5 {kind}")
            record(f"{who.split(':')[1]} takes no {kind.split(':')[1]} damage", "Applied" not in out, out.strip())
    rcon.run("kill @e[tag=smoke_buried]")
    # Dread dragonsteel dragon armour (0.84.4, the owner: "Should also include a rimefrost dragonsteel dragon armor"):
    # its four pieces and its block exist, and the block drops itself.
    for piece in ("head", "neck", "body", "tail"):
        check(f"dread dragonsteel dragon armour ({piece}) exists",
              f'summon item 408.5 201 408.5 {{Item:{{id:"dreadpatch:dragonarmor_dread_dragonsteel_{piece}",count:1}},Tags:["smoke_dragon_armor"]}}',
              "Summoned new")
    rcon.run("kill @e[tag=smoke_dragon_armor]")
    rcon.run("setblock 408 200 408 dreadpatch:dread_dragonsteel_block")
    out = rcon.run("loot spawn 408 201 408 mine 408 200 408 minecraft:diamond_pickaxe")
    record("a dread dragonsteel block drops itself to a diamond pickaxe", "Block of Dread Dragonsteel" in out or "dread_dragonsteel_block" in out, out.strip())
    rcon.run("setblock 408 200 408 minecraft:air")
    rcon.run("kill @e[type=item]")
    rcon.run("forceload remove 400 400 416 416")
    # Only one in six has its lantern (0.76.0), and only that one pulses: this one's given it.
    rcon.run(f"data merge entity {fish} {{Lantern:1b,Frenzy:0}}")
    rcon.run('summon minecraft:zombie 43.5 200 46.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_eel_prey"]}')
    rcon.run("effect give @e[tag=smoke_eel_prey,limit=1] minecraft:resistance 120 4 true")
    rcon.run("effect give @e[tag=smoke_eel_prey,limit=1] minecraft:fire_resistance 120 0 true")
    prey = "@e[tag=smoke_eel_prey,limit=1]"
    pulsed = frenzied = False
    for _ in range(40):
        time.sleep(0.25)
        if has_effect(prey, "dreadpatch:rimefrost") or has_effect(prey, "dreadpatch:whiteout"):
            pulsed = True
            m = re.search(r"entity data: (\d+)", rcon.run(f"data get entity {fish} Frenzy"))
            frenzied = m is not None and int(m.group(1)) > 0
            break
    record("in a fight its lure's pulse lays Rimefrost or Whiteout", pulsed, "the zombie had neither 10 s on")
    record("and sends it into a Frenzy", frenzied, rcon.run(f"data get entity {fish} Frenzy").strip())
    record("and lights it up (0.75.1): Glowing on the zombie, none on the fish",
           pulsed and has_effect(prey, "minecraft:glowing") and not has_effect(fish, "minecraft:glowing"), "glowing wrong")
    rcon.run("kill @e[tag=smoke_eel_prey]")
    # Without its lantern (0.76.0, asked: "No pulse without it") it never pulses; but Glowing prey (the owner:
    # "Highlighted creatures also send dread fish into frenzies") sends it into a Frenzy all the same.
    rcon.run(f"data merge entity {fish} {{Lantern:0b,Frenzy:0}}")
    rcon.run("effect clear " + fish)
    rcon.run('summon minecraft:zombie 43.5 200 46.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_eel_prey"]}')
    rcon.run("effect give @e[tag=smoke_eel_prey,limit=1] minecraft:resistance 120 4 true")
    rcon.run("effect give @e[tag=smoke_eel_prey,limit=1] minecraft:fire_resistance 120 0 true")
    rcon.run("effect give @e[tag=smoke_eel_prey,limit=1] minecraft:glowing 120 0 true")
    glow_frenzy = False
    for _ in range(24):
        time.sleep(0.25)
        m = re.search(r"entity data: (\d+)", rcon.run(f"data get entity {fish} Frenzy"))
        if m and int(m.group(1)) > 0:
            glow_frenzy = True
            break
    record("Glowing prey sends it into a Frenzy (0.76.0)", glow_frenzy, rcon.run(f"data get entity {fish} Frenzy").strip())
    time.sleep(6.0)
    record("without its lantern it never pulses (8 s)", not has_effect(prey, "dreadpatch:rimefrost") and not has_effect(prey, "dreadpatch:whiteout"),
           "the zombie took the pulse")
    rcon.run("kill @e[tag=smoke_eel_prey]")
    rcon.run("kill @e[type=item]")
    # One spawn in six has its lantern (0.76.0, the owner: "Only about 10-25% spawn with the lantern"): of 36 summoned
    # without data (so each rolls), some have it and most don't. (All or none would be a 1 in 700 fluke.)
    for _ in range(36):
        rcon.run("summon dreadpatch:dread_fish 44.5 202 44.5")
    lanterned = re.search(r"count: (\d+)", rcon.run("execute if entity @e[type=dreadpatch:dread_fish,nbt={Lantern:1b},x=44.5,y=202,z=44.5,distance=..4]"))
    lanterned = int(lanterned.group(1)) if lanterned else 0
    record("about one in six spawns with its lantern", 0 < lanterned < 18, f"{lanterned} of 36")
    rcon.run("kill @e[type=dreadpatch:dread_fish,x=44.5,y=202,z=44.5,distance=..4]")
    # An alpha (0.80.0, the owner: "A mix of heavy natural armor and slow easy to read attacks. Hit hrd and with
    # knockback."): summoned at an alpha's size (its build set as it loads), it has 14 armour, 4 toughness, knockback
    # 1.5, a bite 1.75 times its size's; and against a zombie it still lands its slow, wound-up bite.
    rcon.run('summon dreadpatch:dread_fish 43.5 197.5 43.5 {Tags:["smoke_alpha"],attributes:[{id:"minecraft:generic.scale",base:1.6d}]}')
    alpha = "@e[tag=smoke_alpha,limit=1]"
    attrs = {a: re.search(r"is ([0-9.]+)", rcon.run(f"attribute {alpha} minecraft:generic.{a} get")) for a in ("armor", "armor_toughness", "attack_knockback")}
    attrs = {a: float(m.group(1)) if m else -1.0 for a, m in attrs.items()}
    record("an alpha's heavy hide and knockback", attrs == {"armor": 14.0, "armor_toughness": 4.0, "attack_knockback": 1.5}, str(attrs))
    rcon.run('summon minecraft:zombie 44.5 198 44.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_alpha_prey"]}')
    rcon.run("effect give @e[tag=smoke_alpha_prey,limit=1] minecraft:fire_resistance 60 0 true")
    rcon.run("damage @e[tag=smoke_alpha,limit=1] 1 minecraft:mob_attack by @e[tag=smoke_alpha_prey,limit=1]")
    prey_hp = health("@e[tag=smoke_alpha_prey,limit=1]")
    bitten = False
    for _ in range(64):  # 16 s: it's slow, and it winds up first
        time.sleep(0.25)
        if 0 <= health("@e[tag=smoke_alpha_prey,limit=1]") < prey_hp:
            bitten = True
            break
    record("an alpha's wound-up bite lands", bitten, f"the zombie at {health('@e[tag=smoke_alpha_prey,limit=1]')} of {prey_hp} 16 s on")
    # Named a Rime Wyrm (0.80.0); /kill says whom it killed.
    check("an alpha is a Rime Wyrm", "kill @e[tag=smoke_alpha]", "Rime Wyrm")
    rcon.run("kill @e[tag=smoke_alpha_prey]")
    for _ in range(10):
        rcon.run("loot spawn 44.5 201 44.5 loot dreadpatch:entities/dread_fish")
    time.sleep(0.2)
    record("its loot: bones and dread shards", count("minecraft:bone") > 0 and count("iceandfire:dread_shard") > 0,
           f"{count('minecraft:bone')} bones, {count('iceandfire:dread_shard')} dread shards over 10 rolls")
    rcon.run("kill @e[type=dreadpatch:dread_fish]")
    rcon.run("kill @e[type=item]")
    rcon.run("fill 40 196 40 48 204 48 minecraft:air")
    rcon.run("forceload remove 40 40 48 48")

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
    # The first raised dragon out of the way before the second corpse lies down (0.83.0: full grown, it stood over where
    # the second one is summoned, and could shove it or stand across the spell's ray, which stops at the first thing it
    # meets; it failed now and then). The recast still sends it back, wherever it is.
    rcon.run("tp " + raised + " 11.5 200 40")
    time.sleep(0.3)
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
    # The golem without its AI (she targets it all the same): once (0.52.0's run) she was gone by the time
    # of the check, and a golem with its AI fights her. The summon is checked too, to tell the two apart.
    rcon.run('summon minecraft:iron_golem 8.5 200 3.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_target"]}')
    rcon.run("effect give " + target + " minecraft:resistance 120 4 true")
    check("a wounded queen summoned beside a corpse", 'summon dreadpatch:dread_queen 4.5 200 7.5 {UUID:[I;6,6,6,6],Health:100.0f}', "Summoned new")
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
    # (by their shape). A golem stays dead. When she falls her storm clears.
    rcon.run('summon dreadpatch:dread_queen 2.5 200 2.5 {NoAI:1b,UUID:[I;9,9,9,9]}')
    check("her Whiteout Dominion stands", 'summon dreadpatch:whiteout_dominion 8.5 200 7.5 {Owner:[I;9,9,9,9],RaisesDead:1b,Radius:16f,Lifetime:600}',
          "Summoned new Whiteout Dominion")
    for kind, at, risen in (("cow", "10.5 200 4.5", "iceandfire:dread_beast"), ("villager", "12.5 200 10.5", "iceandfire:dread_thrall"),
                            ("creeper", "4.5 200 10.5", "iceandfire:dread_thrall")):
        rcon.run(f'summon minecraft:{kind} {at} {{NoAI:1b,Tags:["smoke_dead"]}}')
        rcon.run("kill @e[tag=smoke_dead]")
        time.sleep(0.5)
        check(f"a {kind} killed in her storm rises", f"execute positioned {at} if entity @e[type={risen},distance=..2]", "Test passed")
        check(f"the risen {kind} answers to her", f"execute positioned {at} if entity @e[type={risen},distance=..2,nbt={{CommanderUUID:[I;9,9,9,9]}}]",
              "Test passed")
    # Guard Villagers (the owner's pack's, a dev-run mod): a guard keeps its gear in an inventory of its own,
    # which its getItemBySlot reads, so it rises a thrall of hers wearing it; the copy never drops (the guard
    # drops its own: in the pack, always).
    at = "14.5 200 2.5"
    rcon.run(f'summon guardvillagers:guard {at} {{NoAI:1b,Tags:["smoke_dead"]}}')
    for slot, item in (("armor.head", "iron_helmet"), ("armor.chest", "iron_chestplate"), ("weapon.mainhand", "iron_sword"),
                       ("weapon.offhand", "shield")):
        rcon.run(f"item replace entity @e[tag=smoke_dead,limit=1] {slot} with minecraft:{item}")
    check("a guard wears what it's given", "data get entity @e[tag=smoke_dead,limit=1] Inventory", "iron_chestplate")
    rcon.run("kill @e[tag=smoke_dead]")
    time.sleep(0.5)
    guard_thrall = f"@e[type=iceandfire:dread_thrall,limit=1,sort=nearest,x=14.5,y=200,z=2.5,distance=..2]"
    check("a guard killed in her storm rises", f"execute positioned {at} if entity {guard_thrall}", "Test passed")
    check("the risen guard answers to her", f"execute if entity {guard_thrall[:-1]},nbt={{CommanderUUID:[I;9,9,9,9]}}]", "Test passed")
    for path, item in (("ArmorItems[3].id", "iron_helmet"), ("ArmorItems[2].id", "iron_chestplate"), ("HandItems[0].id", "iron_sword"),
                       ("HandItems[1].id", "shield")):
        check(f"the risen guard wears its {item.replace('_', ' ')}", f"data get entity {guard_thrall} {path}", f'"minecraft:{item}"')
    check("and won't drop it", f"data get entity {guard_thrall} ArmorDropChances[2]", "0.0f")
    # Risen Dread (0.63.0): a bat or a parrot rises as a miniature dread phantom of hers; a cod in water a Dread Fish
    # of hers (0.71.0), refusing Withering Rime.
    for kind, at in (("bat", "5.5 200 4.5"), ("parrot", "7.5 200 4.5")):
        rcon.run(f'summon minecraft:{kind} {at} {{NoAI:1b,Tags:["smoke_dead"]}}')
        rcon.run("kill @e[tag=smoke_dead]")
        time.sleep(0.5)
        check(f"a {kind} killed in her storm rises a miniature dread phantom of hers",
              f"execute positioned {at} if entity @e[type=dreadpatch:miniature_dread_phantom,distance=..3,nbt={{Summoner:[I;9,9,9,9]}}]", "Test passed")
    rcon.run("kill @e[type=dreadpatch:miniature_dread_phantom]")
    rcon.run("setblock 6 201 12 minecraft:stone")  # covered, so her storm doesn't freeze it over
    rcon.run("setblock 6 200 12 minecraft:water")
    rcon.run('summon minecraft:cod 6.5 200 12.5 {NoAI:1b,Tags:["smoke_dead"]}')
    rcon.run("kill @e[tag=smoke_dead]")
    time.sleep(0.5)
    # 0.71.0: a water creature rises a Dread Fish of hers (0.63.0-0.70.0: itself, Dread).
    fish = '@e[type=dreadpatch:dread_fish,limit=1,x=6.5,y=200,z=12.5,distance=..3]'
    check("a cod killed in her storm rises a Dread Fish", "execute if entity " + fish, "Test passed")
    check("hers", "data get entity " + fish + ' NeoForgeData."dreadpatch:risen_by"', "[I; 9, 9, 9, 9]")
    check("and no Dread cod", "execute if entity @e[type=minecraft:cod,x=6.5,y=200,z=12.5,distance=..3]", "Test failed")
    rcon.run(f"effect give {fish} dreadpatch:withering_rime 10")
    time.sleep(0.1)
    record("the fish refuses Withering Rime, as the Dread do", not has_effect(fish, "dreadpatch:withering_rime"), "it took it")
    rcon.run("kill @e[type=dreadpatch:dread_fish]")
    rcon.run("kill @e[type=minecraft:cod]")
    rcon.run("fill 6 200 12 6 201 12 minecraft:air")
    # Counted before and after (0.83.0: the chickens' risen beasts now wander over to eat what the dead dropped, and one
    # stood by the golem): nothing more there once it's dead.
    # Beasts aside too: they come for its poppy (a golem would rise a thrall, by its shape, if it rose).
    near_golem = ("execute positioned 13.5 200 6.5 if entity @e[distance=..2.5,type=!minecraft:item,type=!minecraft:experience_orb,"
                  "type=!minecraft:iron_golem,type=!iceandfire:dread_beast]")
    before = re.search(r"count: (\d+)", rcon.run(near_golem))
    rcon.run('summon minecraft:iron_golem 13.5 200 6.5 {NoAI:1b,Tags:["smoke_dead"]}')
    rcon.run("kill @e[tag=smoke_dead]")
    time.sleep(0.5)
    after = re.search(r"count: (\d+)", rcon.run(near_golem))
    record("a golem killed in her storm stays dead", (int(after.group(1)) if after else 0) <= (int(before.group(1)) if before else 0),
           f"{before.group(1) if before else 0} near it before, {after.group(1) if after else 0} after")
    # Her storm's places (0.79.0, the owner: "about 50-100% its current cap ... converting more of them at lower stats is
    # ok"; asked, "Small dead count half"): 12 places, a small one half. So far a cow, villager, creeper and guard (whole)
    # and a bat, parrot and cod (half): 11 half-places of 24. Of 20 chickens killed along its south edge, 13 rise (8
    # places before 0.79.0 would have let one through).
    for i in range(20):
        x = 2.5 + (i % 10) * 1.2
        z = 13.5 + (i // 10)
        rcon.run(f'summon minecraft:chicken {x:.1f} 200 {z} {{NoAI:1b,Tags:["smoke_dead"]}}')
        rcon.run("kill @e[tag=smoke_dead]")
        time.sleep(0.15)
    time.sleep(0.5)
    # Counted by whom they answer to, wherever they've wandered (a box round them missed one): hers, less the cow's.
    m = re.search(r"count: (\d+)", rcon.run("execute if entity @e[type=iceandfire:dread_beast,nbt={CommanderUUID:[I;9,9,9,9]}]"))
    chicks = (int(m.group(1)) if m else 0) - 1
    record("her storm raises 12 places, the small dead half a place: 13 of 20 chickens after 11 half-places", chicks == 13, f"{chicks} risen")
    rcon.run("kill @e[type=minecraft:chicken]")
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
    time.sleep(0.6)
    record("her storm takes the golem", stormed and has_effect(target, "dreadpatch:whiteout"), "no Whiteout on the golem")
    # Her dread phantoms (0.45.0): 1-2, the first 15 s into a fight, hers. Her storm's 15 s count from her
    # spawn and her phantoms' from her first target, a second or two later: wait for them (once, a single
    # look right after the storm caught them not yet called).
    called = 0
    for _ in range(20):
        m = re.search(r"count: (\d+)", rcon.run("execute if entity @e[type=dreadpatch:dread_phantom]"))
        called = int(m.group(1)) if m else 0
        if called:
            break
        time.sleep(0.5)
    record("she calls 1-2 dread phantoms", 1 <= called <= 2, f"{called} dread phantoms")
    summoner = re.search(r"\[I; [-0-9, ]+\]", rcon.run("data get entity @e[type=dreadpatch:dread_phantom,limit=1] Summoner"))
    record("her phantoms answer to her", bool(queen_id) and bool(summoner) and summoner.group(0) == queen_id.group(0),
           f"queen {queen_id and queen_id.group(0)}, phantom's summoner {summoner and summoner.group(0)}")
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
    # platform's north edge. Since 0.70.0 (the owner: "Leaving a dread portal open no longer summons the
    # Dread Queen or Syndragossa. It remains just a portal ... It should slowly build a few layers of snow
    # layers around it up to 3-4 blocks out") it stays open, nothing comes, and it lays snow round its foot
    # (the storm above laid a cover here, so it's cleared first). (Stepping into it needs a player.)
    queen_sel = "@e[type=dreadpatch:dread_queen,limit=1]"
    rcon.run("fill 3 199 2 7 204 2 iceandfire:dread_stone_bricks")
    rcon.run("fill 4 200 2 6 203 2 minecraft:air")
    rcon.run("fill -1 199 -3 11 203 7 minecraft:air replace minecraft:snow")
    check("a gate summoned in its frame", "summon dreadpatch:dread_gate 5.5 200 2.5", "Summoned new")
    time.sleep(0.5)
    record("it fills its frame", all(block_is(x, y, 2, "dreadpatch:dread_gate") for x in (4, 5, 6) for y in (200, 203)),
           "no gate across the opening")
    time.sleep(13.5)
    record("it stays open and nothing comes through (14 s on)", "Test failed" in rcon.run("execute if entity " + queen_sel)
           and "Test failed" in rcon.run("execute if entity @e[type=dreadpatch:syndrigossa]")
           and block_is(5, 201, 2, "dreadpatch:dread_gate") and "Test passed" in rcon.run("execute if entity @e[type=dreadpatch:dread_gate]"),
           "a queen or dragon came, or the gate closed")

    def gate_snow():
        filled = re.search(r"filled (\d+)", rcon.run("fill -1 199 -3 11 203 7 minecraft:sponge replace minecraft:snow"))
        if filled and int(filled.group(1)):
            rcon.run("fill -1 199 -3 11 203 7 minecraft:snow replace minecraft:sponge")
            return int(filled.group(1))
        return 0
    snow = 0
    for _ in range(15):
        snow = gate_snow()
        if snow:
            break
        time.sleep(2.0)
    record("it lays snow round its foot", snow > 0, "no snow within 4 of it in 44 s")
    # It lets the Dread out now and then (0.76.0) on its panes' random ticks; but random ticks run only in chunks near a
    # player (forceloaded or not), so that's for the client (its odds are GateTimingTest's).
    # Broken, it collapses; a frame with a gap opens nothing.
    rcon.run("setblock 3 201 2 minecraft:air")
    time.sleep(0.5)
    record("breaking its frame collapses it", block_is(5, 201, 2, "minecraft:air") and block_is(4, 200, 2, "minecraft:air")
           and "Test failed" in rcon.run("execute if entity @e[type=dreadpatch:dread_gate]"), "the gate outlived its frame")
    rcon.run("summon dreadpatch:dread_gate 5.5 200 2.5")
    time.sleep(0.5)
    record("a frame with a gap opens nothing", block_is(5, 201, 2, "minecraft:air")
           and "Test failed" in rcon.run("execute if entity @e[type=dreadpatch:dread_gate]"), "a gate opened in a broken frame")
    rcon.run("fill -1 199 -3 11 203 7 minecraft:air replace minecraft:snow")
    # The Dreadlands (0.46.0): the dimension, its frozen ground (snow over snow blocks and ice), a spire
    # standing far over the land round it; a Dread Gate keyed there leads home: it opens at once, no queen
    # comes, and (0.70.0) it stays open till its frame breaks. (Going through, the arrival by a spire, and
    # its phantoms need a player.)
    dl = "execute in dreadpatch:dreadlands "
    check("the Dreadlands' plains exist", dl + "run locate biome dreadpatch:dreadlands", "is at")
    # Its three biomes (0.66.0): the plains, the sunken Sastrugi Wastes, the Rime Chasma's trenches.
    check("the Sastrugi Wastes exist", dl + "positioned 2000000 0 2000000 run locate biome dreadpatch:sastrugi_wastes", "is at")
    check("the Rime Chasma exists", dl + "positioned 2000000 0 2000000 run locate biome dreadpatch:rime_chasma", "is at")
    # An ice dragon's skeleton lying out (0.76.0, the owner: "ice dragon skeleton corpses spawn in the dreadlands"), one
    # chunk in 600 since 0.84.0 (100 before; the owner: "much more rare ... now that real rimefrost dragons will live
    # here"): placed by hand on new ground (its rarity aside), it's Ice and Fire's ice dragon, dead, down to bones.
    sx, sz = random.choice((-1, 1)) * random.randrange(300_000, 900_000), random.choice((-1, 1)) * random.randrange(300_000, 900_000)
    rcon.run(dl + f"run forceload add {sx} {sz}")
    time.sleep(4.0)
    placed = rcon.run(dl + f"positioned {sx + 8} 0 {sz + 8} positioned over ocean_floor run place feature dreadpatch:dragon_skeleton ~ ~ ~")
    time.sleep(0.5)
    bones = dl + f"if entity @e[type=iceandfire:ice_dragon,x={sx - 8},y=-64,z={sz - 8},dx=32,dy=400,dz=32,nbt={{ModelDead:1b}}]"
    record("an ice dragon's skeleton lies in the Dreadlands", "Test passed" in rcon.run(bones)
           and "Test failed" in rcon.run(bones[:-1] + ",nbt={DeathStage:0}]"), f"place said: {placed.strip()}")
    rcon.run(dl + f"run kill @e[type=iceandfire:ice_dragon,x={sx - 8},y=-64,z={sz - 8},dx=32,dy=400,dz=32]")
    # The Glacial Plains (0.84.0): the ice sheet to about y 200 over 40% of the land, sheer-walled; its top snow and ice,
    # its body ice (blue-veined, stone buried in it). Found far out on new ground, its columns probed on a grid round
    # where locate puts its edge (80 apart, a pause after each: each lone chunk needs its neighbours).
    check("the Glacial Plains exist", dl + "positioned 1500000 0 -1500000 run locate biome dreadpatch:glacial_plains", "is at")

    def surface_y(x, z):
        rcon.run(dl + f'positioned {x} 0 {z} positioned over world_surface run summon minecraft:marker ~ ~ ~ {{Tags:["smoke_probe"]}}')
        pos = re.search(r"\[[-0-9.]+d, ([-0-9.]+)d", rcon.run("data get entity @e[tag=smoke_probe,limit=1] Pos"))
        rcon.run("kill @e[tag=smoke_probe]")
        return int(float(pos.group(1))) - 1 if pos else None

    def dl_block(x, y, z, block):
        return "Test passed" in rcon.run(dl + f"if block {x} {y} {z} {block}")
    gx0, gz0 = random.choice((-1, 1)) * random.randrange(300_000, 900_000), random.choice((-1, 1)) * random.randrange(300_000, 900_000)
    m = re.search(r"at \[(-?\d+), \S+, (-?\d+)\]", rcon.run(dl + f"positioned {gx0} 0 {gz0} run locate biome dreadpatch:glacial_plains"))
    glacial_tops = []
    if m:
        lx, lz = int(m.group(1)), int(m.group(2))
        # All loaded first, then measured once they're done (0.84.1: a lone chunk measured at once had its heightmap
        # 1-6 blocks over its finished top, so the "top" was air).
        points = [(lx + 80 * i, lz + 80 * j) for j in range(-3, 4) for i in range(-3, 4)]
        for x, z in points:
            rcon.run(dl + f"run forceload add {x} {z}")
            time.sleep(0.6)
        time.sleep(8.0)
        for x, z in points:
            if "Test passed" in rcon.run(dl + f"positioned {x} 100 {z} if biome ~ ~ ~ dreadpatch:glacial_plains"):
                y = surface_y(x, z)
                if y is not None:
                    glacial_tops.append((x, y, z))
    high = [t for t in glacial_tops if t[1] >= 170]
    # Capped at 187 since 0.90.0 (spires on it reach higher: none past 235).
    median = sorted(t[1] for t in glacial_tops)[len(glacial_tops) // 2] if glacial_tops else 0
    record("the Glacial Plains stand up to about y 187", len(glacial_tops) >= 6 and len(high) >= len(glacial_tops) // 2
           and median <= 188 and all(t[1] <= 235 for t in glacial_tops), f"median {median}, tops {[(t[0], t[1], t[2]) for t in glacial_tops]} from {m and m.group(0)}")
    ice = "#minecraft:ice"
    # A spire stands on the glacier now and then (its bricks, tiles, bones and torches): not the glacier's surface.
    spire = ("iceandfire:dread_stone_bricks", "iceandfire:dread_stone_bricks_cracked", "iceandfire:dread_stone_bricks_chiseled",
             "iceandfire:dread_stone_tile", "iceandfire:dragon_bone_block", "iceandfire:dread_torch")
    high = [t for t in high if not any(dl_block(t[0], t[1], t[2], b) for b in spire)]
    on_top = [t for t in high if any(dl_block(t[0], t[1], t[2], b) for b in
                                     ("minecraft:snow_block", ice, "minecraft:snow", "iceandfire:dread_stone", "minecraft:stone"))]
    # Under it: ice, its buried stone, or a cave in it (0.84.2's run: a snow block 30 under, a cave's floor in the ice,
    # which the surface rule snows over as it does the top).
    body = [t for t in high if any(dl_block(t[0], t[1] - 30, t[2], b) for b in
                                   (ice, "iceandfire:dread_stone", "minecraft:stone", "minecraft:air", "minecraft:cave_air",
                                    "minecraft:snow_block", "minecraft:snow", "minecraft:water"))]
    # Most of its top: snow (mostly layers), sheets of ice, its buried stone and dreadstone breaking through (0.84.0's
    # first runs: 22 of 24, then 34 of 43 with a spire's top among them).
    record("the glacier's top is snow and ice, its body ice", len(high) > 0 and len(on_top) >= 0.8 * len(high) and len(body) == len(high),
           f"top {len(on_top)}/{len(high)}, 30 under {len(body)}/{len(high)} at {high}")
    # A wild Rimefrost dragon's nest (0.84.0), one glacier chunk in 400 (160 until 0.84.2): placed by hand up on the glacier (somewhere flat
    # enough), a bowl of hoard piles and its dragon, wild, stage 2 or more, its home the nest. Its piles stay (0.84.1:
    # with Ice and Fire's griefing on, as in the dev runs, a big dragon broke the piles it stood in; hoards are now in
    # its dragon_block_break_blacklist). A whole hoard, one pile a column (113 at the least, radius 6): until 0.84.4 its bed
    # was mostly packed ice, which Ice and Fire's piles fall off, and 40 to 50 were left.
    nest = None
    for t in high[:6]:
        placed = rcon.run(dl + f"positioned {t[0]} 0 {t[2]} positioned over world_surface run place feature dreadpatch:rimefrost_nest ~ ~ ~")
        if "laced" in placed and "ailed" not in placed:
            nest = t
            break
    time.sleep(0.5)
    if nest:
        nx, ny, nz = nest
        near = f"x={nx - 16},y={ny - 16},z={nz - 16},dx=32,dy=40,dz=32"
        dragon = dl + f"if entity @e[type=dreadpatch:rimefrost_dragon,{near}]"
        piles = 0
        for block in ("iceandfire:gold_pile", "iceandfire:silver_pile", "iceandfire:copper_pile", "dreadpatch:iron_pile"):
            area = f"{nx - 12} {ny - 8} {nz - 12} {nx + 12} {ny + 4} {nz + 12}"
            filled = re.search(r"filled (\d+)", rcon.run(dl + f"run fill {area} minecraft:glass replace {block}"))
            if filled:
                piles += int(filled.group(1))
                rcon.run(dl + f"run fill {area} {block}[layers=4] replace minecraft:glass")
        age = re.search(r"entity data: (\d+)", rcon.run(dl + f"run data get entity @e[type=dreadpatch:rimefrost_dragon,{near},limit=1] AgeTicks"))
        record("a wild Rimefrost dragon nests on its hoard", "Test passed" in rcon.run(dragon) and piles >= 100
               and "Test failed" in rcon.run(dragon[:-1] + ",nbt={TamedDragon:1b}]")
               and "Test passed" in rcon.run(dragon[:-1] + ",nbt={HasHomePosition:1b}]")
               and age is not None and int(age.group(1)) >= 25 * 24000, f"piles {piles}, age {age and age.group(1)} at {nest}")
        rcon.run(dl + f"run kill @e[type=dreadpatch:rimefrost_dragon,{near}]")
    else:
        record("a wild Rimefrost dragon nests on its hoard", False, f"no nest would place on {high[:6]}")
    rcon.run(dl + "run forceload remove all")
    # The hoard's iron pile drops its nuggets (0.84.0), two a layer. (Its blacksteel pile and dragon armour were taken
    # out in 0.84.2.)
    rcon.run("setblock 30 199 30 minecraft:stone")
    rcon.run("setblock 30 200 30 dreadpatch:iron_pile[layers=3]")
    check("an iron pile drops two iron nuggets a layer", "loot spawn 30 202 30 mine 30 200 30 minecraft:diamond_pickaxe", "Dropped 6")
    rcon.run("fill 30 199 30 30 200 30 minecraft:air")
    rcon.run("kill @e[type=item]")
    # The deeps flooded (0.77.0, the owner: "Water should fill most of the open air spaces just above the worlds bedrock
    # layers"): over 3 x 3 chunks there, from just over bedrock's bottom to y -52, water and no open air. Counted by
    # swapping to glass and back: not sponge, which soaks up the water round it (the first run counted its own damage).
    # A spot can be solid rock that deep (0.79.0's run found one), so up to three spots 3 x 3 chunks, 200 blocks apart:
    # no open air in any, water in at least one.
    deep_water, deep_air, looked = 0, 0, []

    def deep_count(deep, block):
        filled = re.search(r"filled (\d+)", rcon.run(dl + f"run fill {deep} minecraft:glass replace {block}"))
        if filled and int(filled.group(1)):
            rcon.run(dl + f"run fill {deep} {block} replace minecraft:glass")
            return int(filled.group(1))
        return 0
    for k in range(3):
        bx, bz = sx + 200 * k, sz
        rcon.run(dl + f"run forceload add {bx - 16} {bz - 16} {bx + 31} {bz + 31}")
        time.sleep(6.0)
        deep = f"{bx - 16} -63 {bz - 16} {bx + 31} -52 {bz + 31}"
        deep_water += deep_count(deep, "minecraft:water")
        deep_air += deep_count(deep, "minecraft:air")
        looked.append(deep)
        rcon.run(dl + "run forceload remove all")
        if deep_water > 0:
            break
    record("the deeps are flooded: water over bedrock, no open air to y -52", deep_water > 0 and deep_air == 0,
           f"{deep_water} water, {deep_air} air in {looked}")
    # The Rime Chasma's floor (0.68.0, the owner's pictures): far out on new ground, its middle found on a coarse grid
    # (80 apart round where locate puts its edge; 32 before 0.77.0), then every 2 blocks over 64 x 64 there. Most of it is the flat
    # floor (top block 30), its plates' ridges a block or two over it in snow, its cracks at least 6 deep (fall
    # damage); its top clear blue ice and snow; water sealed in its ice; slabs heaved up at its cracks.
    def top_block(x, z):
        rcon.run(dl + f'positioned {x} 0 {z} positioned over world_surface run summon minecraft:marker ~ ~ ~ {{Tags:["smoke_probe"]}}')
        pos = re.search(r"\[[-0-9.]+d, ([-0-9.]+)d", rcon.run("data get entity @e[tag=smoke_probe,limit=1] Pos"))
        rcon.run("kill @e[tag=smoke_probe]")
        return int(float(pos.group(1))) - 1 if pos else None

    def chasma_floor(cfx, cfz):
        """Round where locate finds a chasma from (cfx, cfz): its flattest floor point, or None; and its locate point."""
        m = re.search(r"at \[(-?\d+), \S+, (-?\d+)\]", rcon.run(dl + f"positioned {cfx} 0 {cfz} run locate biome dreadpatch:rime_chasma"))
        if not m:
            return None, None, None
        lx, lz = int(m.group(1)), int(m.group(2))
        # 64 apart since 0.77.0: a chasma is 2.3 times as wide, its slopes too.
        # 9 x 9, a pause after each: each lone chunk needs its neighbours generated, and 121 of them queued at once held
        # one tick past the watchdog's 60 s.
        grid = [(lx + i * 80, lz + j * 80) for j in range(-4, 5) for i in range(-4, 5)]
        for gx, gz in grid:
            rcon.run(dl + f"run forceload add {gx} {gz}")
            time.sleep(0.4)
        time.sleep(8.0)
        coarse = {p: top_block(*p) for p in grid}
        # In the chasma itself: since 0.77.0 the wastes' dune valleys go down past 30 too.
        in_chasma = {p for p in grid if "Test passed" in rcon.run(dl + f"positioned {p[0]} 31 {p[1]} if biome ~ ~ ~ dreadpatch:rime_chasma")}
        rcon.run(dl + "run forceload remove all")
        coarse = {p: (h if p in in_chasma else None) for p, h in coarse.items()}
        floor_points = [p for p in grid if coarse[p] is not None and 29 <= coarse[p] <= 32]
        # Its middle: the floor point with the most floor (30-31: not slope, not crack) round it. Scoring by anything
        # at or under 32 counted cracks and picked an edge twice in 0.77.0's wider chasmas.
        ranked = sorted(floor_points, key=lambda p: -sum(1 for q in grid if abs(q[0] - p[0]) <= 160 and abs(q[1] - p[1]) <= 160
                                                         and coarse[q] is not None and 30 <= coarse[q] <= 31))
        # The biome reaches past the flat floor onto its slopes, so a point at 30 there can be slope: the best three are
        # each looked at closer (5 x 5, 8 apart), and the flattest kept.
        # Only a point with some flat floor round it (0.84.0: the Glacial Plains cover 40% of the land, so fewer of the
        # grid's points are in a chasma, and a slope's point was kept with no floor at all).
        # At least 10 of its 25 (0.84.0's next run kept one with a few, the glacier over the rest).
        mid, flattest = None, 9
        for cx, cz in ranked[:3]:
            rcon.run(dl + f"run forceload add {cx - 16} {cz - 16} {cx + 16} {cz + 16}")
            time.sleep(4.0)
            flat = sum(1 for x in range(cx - 16, cx + 17, 8) for z in range(cz - 16, cz + 17, 8) if top_block(x, z) == 30)
            rcon.run(dl + "run forceload remove all")
            if flat > flattest:
                mid, flattest = (cx, cz), flat
        return mid, lx, lz

    # Some chasmas never quite flatten (the biome reaches past where the floor's mask saturates): up to five tried (three before 0.84.0).
    m = mid = None
    for _ in range(5):
        cfx, cfz = random.choice((-1, 1)) * random.randrange(300_000, 900_000), random.choice((-1, 1)) * random.randrange(300_000, 900_000)
        mid, lx, lz = chasma_floor(cfx, cfz)
        m = lx is not None
        if mid:
            break
    if m:
        record("the Rime Chasma has a floor near where it's found", mid is not None, f"no floor at 29-32 round three chasmas (the last at {lx} {lz})")
        if mid:
            mx, mz = mid
            rcon.run(dl + f"run forceload add {mx - 32} {mz - 32} {mx + 32} {mz + 32}")
            time.sleep(8.0)
            tops = [top_block(x, z) for x in range(mx - 32, mx + 32, 2) for z in range(mz - 32, mz + 32, 2)]
            # The glacier's columns (0.84.0) are another biome over the chasma: left out.
            tops = [t for t in tops if t is not None and t < 120]
            chasma_share = lambda test: sum(1 for t in tops if test(t)) / max(1, len(tops))
            record("most of the Rime Chasma is its flat floor at 30", chasma_share(lambda t: t == 30) >= 0.4,
                   f"{chasma_share(lambda t: t == 30):.0%} at 30; at {mx} {mz}")
            record("its plates have ridges over the floor", chasma_share(lambda t: t in (31, 32)) >= 0.03, f"{chasma_share(lambda t: t in (31, 32)):.0%} at 31-32")
            cracks = [t for t in tops if t < 30]
            record("its cracks are deep enough to hurt a fall", bool(cracks) and max(cracks) <= 30 - 6,
                   f"{len(cracks)} crack columns, the shallowest's top at {max(cracks) if cracks else None}")
            # Counted by swapping to sponge and back (as count_blocks below; it isn't defined yet here).
            def chasma_count(block, y0, y1):
                total = 0
                for x in range(mx - 32, mx + 32, 32):
                    for z in range(mz - 32, mz + 32, 32):
                        box = f"{x} {y0} {z} {x + 31} {y1} {z + 31}"
                        filled = re.search(r"filled (\d+)", rcon.run(dl + f"run fill {box} minecraft:sponge replace {block}"))
                        if filled and int(filled.group(1)):
                            rcon.run(dl + f"run fill {box} {block} replace minecraft:sponge")
                            total += int(filled.group(1))
                return total
            record("its top is clear blue ice and snow", chasma_count("minecraft:blue_ice", 30, 30) > 0 and chasma_count("minecraft:snow_block", 30, 30) > 0, "one's missing")
            record("its ridges are snow", chasma_count("minecraft:snow_block", 31, 32) > 0, "no snow at 31-32")
            water = chasma_count("minecraft:water", 14, 28)
            record("water's sealed in its ice", water > 0, f"{water} water blocks at 14-28")
            heaved = chasma_share(lambda t: 33 <= t <= 40)
            record("slabs are heaved up at its cracks", heaved > 0, f"{heaved:.1%} of columns at 33-40")
            # The world's floor (0.69.0, the owner: bedrock "at least 1 layer thick throughout the entire dimension"):
            # no cave, crack or trench opens it, here where they're thickest.
            bedrock = chasma_count("minecraft:bedrock", -64, -64)
            record("the Dreadlands' floor is bedrock throughout", bedrock == 64 * 64, f"{bedrock} of {64 * 64} at -64")
            rcon.run(dl + "run forceload remove all")
    else:
        record("the Rime Chasma is found far out", False, "locate found none")
    out = rcon.run(dl + "run locate structure dreadpatch:spire")
    m = re.search(r"at \[(-?\d+), ~, (-?\d+)\]", out)
    record("a spire stands in them", m is not None, out.strip())
    if m:
        sx, sz = (int(m.group(1)) & ~15) + 8, (int(m.group(2)) & ~15) + 8
        # The arrival (0.60.0, the owner: "somewhere random, but never on a tower or within 48 blocks from one"):
        # through a gate standing right at a spire's middle, and at the world's origin, someone arrives within 256
        # blocks, on dry ground with room to stand, and 69+ blocks from any spire's middle (48 past the most a
        # spire reaches).
        for gx, gz in ((sx, sz), (sx, sz), (0, 0)):
            out = rcon.run(f"dreadpatch dreadlands arrival {gx} {gz}")
            a = re.search(r"arrive at (-?\d+) (-?\d+) (-?\d+), (\S+) blocks", out)
            if not a:
                record("an arrival is found", False, out.strip())
                continue
            ax, ay, az, far = int(a.group(1)), int(a.group(2)), int(a.group(3)), a.group(4)
            rcon.run(dl + f"run forceload add {ax} {az}")
            time.sleep(1.0)
            dry = "Test passed" in rcon.run(dl + f"if block {ax} {ay} {az} #minecraft:replaceable") \
                and "Test failed" in rcon.run(dl + f"if block {ax} {ay - 1} {az} #minecraft:replaceable") \
                and "Test failed" in rcon.run(dl + f"if block {ax} {ay - 1} {az} minecraft:water")
            rcon.run(dl + f"run forceload remove {ax} {az}")
            record(f"through a gate at {gx} {gz}, an arrival on dry ground, within 256, clear of the spires",
                   dry and abs(ax - gx) <= 257 and abs(az - gz) <= 257 and (far == "-" or int(far) >= 69), out.strip())
        rcon.run(dl + f"run forceload add {sx - 32} {sz - 32} {sx + 32} {sz + 32}")
        time.sleep(6.0)

        def surface_y(x, z):
            rcon.run(dl + f'positioned {x} 0 {z} positioned over world_surface run summon minecraft:marker ~ ~ ~ {{Tags:["smoke_probe"]}}')
            pos = re.search(r"\[[-0-9.]+d, ([-0-9.]+)d", rcon.run("data get entity @e[tag=smoke_probe,limit=1] Pos"))
            rcon.run("kill @e[tag=smoke_probe]")
            return float(pos.group(1)) if pos else -999.0

        summit, ground = surface_y(sx, sz), min(surface_y(sx + 30, sz), surface_y(sx - 30, sz), surface_y(sx, sz + 30), surface_y(sx, sz - 30))
        record("tall: its summit 50+ blocks over the ground round it", summit - ground >= 50, f"summit {summit}, ground {ground}")
        rcon.run(dl + "run forceload remove all")
    # Its own patch of ground, cleared again at the end (the world persists between runs, and once the
    # last run's gate frame stood where the ground was probed).
    rcon.run(dl + "run forceload add 32 32 63 63")
    time.sleep(4.0)
    # The ground's probed just off the pad's corner (a run stopped part-way once left its pad over 48,48).
    rcon.run(dl + 'positioned 34 0 34 positioned over world_surface run summon minecraft:marker ~ ~ ~ {Tags:["smoke_probe"]}')
    pos = re.search(r"\[[-0-9.]+d, ([-0-9.]+)d", rcon.run("data get entity @e[tag=smoke_probe,limit=1] Pos"))
    rcon.run("kill @e[tag=smoke_probe]")
    ground = int(float(pos.group(1))) if pos else 80
    frozen = [b for b in ("minecraft:snow", "minecraft:snow_block", "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:ice", "iceandfire:dread_stone", "minecraft:gravel")
              if "Test passed" in rcon.run(dl + f"if block 34 {ground - 1} 34 {b}")]
    record("its ground is frozen", bool(frozen), f"the top block at 34,{ground - 1},34 is none of snow, ice or dreadstone")
    rcon.run(dl + 'positioned 48 0 48 positioned over world_surface run summon minecraft:marker ~ ~ ~ {Tags:["smoke_probe"]}')
    pos = re.search(r"\[[-0-9.]+d, ([-0-9.]+)d", rcon.run("data get entity @e[tag=smoke_probe,limit=1] Pos"))
    rcon.run("kill @e[tag=smoke_probe]")
    gy = int(float(pos.group(1))) if pos else 80
    # A stone pad over the ground, and on it a gate: frame 5 wide, 6 tall round a 3x4 opening.
    pad = gy + 2
    rcon.run(dl + f"run fill 38 {pad} 38 60 {pad} 58 minecraft:stone")
    rcon.run(dl + f"run fill 38 {pad + 1} 38 60 {pad + 9} 58 minecraft:air")
    rcon.run(dl + f"run fill 44 {pad + 1} 44 48 {pad + 6} 44 iceandfire:dread_stone_bricks")
    rcon.run(dl + f"run fill 45 {pad + 2} 44 47 {pad + 5} 44 minecraft:air")
    rcon.run(dl + f"run summon dreadpatch:dread_gate 46.5 {pad + 2} 44.5")
    time.sleep(0.5)
    record("a gate in the Dreadlands opens", "Test passed" in rcon.run(dl + f"if block 46 {pad + 3} 44 dreadpatch:dread_gate"), "no gate in its frame")
    time.sleep(11.0)
    record("it leads home: no queen, and still open past the ritual",
           "Test failed" in rcon.run("execute if entity @e[type=dreadpatch:dread_queen]")
           and "Test passed" in rcon.run(dl + "if entity @e[type=dreadpatch:dread_gate]"), "a queen came, or the gate closed at 11 s")
    time.sleep(20.0)
    record("it stays open (31 s on)", "Test passed" in rcon.run(dl + "if entity @e[type=dreadpatch:dread_gate]")
           and "Test passed" in rcon.run(dl + f"if block 46 {pad + 3} 44 dreadpatch:dread_gate"), "it closed")
    rcon.run(dl + f"run setblock 44 {pad + 3} 44 minecraft:air")
    time.sleep(0.5)
    record("until its frame breaks", "Test failed" in rcon.run(dl + "if entity @e[type=dreadpatch:dread_gate]")
           and "Test passed" in rcon.run(dl + f"if block 44 {pad + 2} 44 iceandfire:dread_stone_bricks"), "still open, or the rest of its frame went")
    # A spire's queen falls (0.47.0): where she fell, a Dread Gate frame rises, open home for 5 minutes
    # (the only gate that still closes on its own, 0.70.0). (Her coming to a summit needs a player.)
    rcon.run(dl + f'run summon dreadpatch:dread_queen 54.5 {pad + 1} 52.5 {{NoAI:1b,NeoForgeData:{{"dreadpatch:spire":12345L}}}}')
    time.sleep(0.5)
    rcon.run(dl + "run kill @e[type=dreadpatch:dread_queen]")
    time.sleep(1.5)
    opened = "Test passed" in rcon.run(dl + f"positioned 54.5 {pad + 1} 52.5 if entity @e[type=dreadpatch:dread_gate,distance=..3]") \
        and "Test passed" in rcon.run(dl + f"if block 54 {pad + 2} 52 dreadpatch:dread_gate")
    record("where a spire's queen falls, a way home opens", opened, "no gate where she fell")
    time.sleep(30.0)
    record("and stays open past 30 s", "Test passed" in rcon.run(dl + "if entity @e[type=dreadpatch:dread_gate]"), "it closed")
    rcon.run(dl + "run kill @e[type=dreadpatch:dread_gate]")
    rcon.run(dl + "run kill @e[type=item]")
    rcon.run(dl + "run kill @e[type=minecraft:experience_orb]")
    # The storms (0.48.0). Rimefire, in clear weather: it heals one of the Dread a quarter a second, and
    # chills (Rimefrost) and puts out anyone else. Then snow (the overworld's rain): everyone under the
    # open sky but the Dread is slowed by Rimefrost; under a roof, no. (Lightning comes only near players.)
    rcon.run("weather clear")
    # With their AI (a block only acts on what moves, and a NoAI mob never does), held still by Slowness
    # 255, one at a time (with a husk in reach, the ghoul's AI went for it, off the fire, and killed it):
    # a dread ghoul (a thrall's AI walked it off the fire anyway), then a husk (the sun doesn't light it).
    still = 'active_effects:[{id:"minecraft:slowness",amplifier:255b,duration:1200}]'
    # Walled into a cell a block across over the fire: alone and slowed, it still wandered off it once.
    rcon.run(dl + f"run fill 49 {pad + 1} 39 51 {pad + 3} 41 minecraft:stone")
    rcon.run(dl + f"run fill 50 {pad + 1} 40 50 {pad + 3} 40 minecraft:air")
    rcon.run(dl + f'run summon iceandfire:dread_ghoul 50.5 {pad + 1} 40.5 {{PersistenceRequired:1b,Tags:["smoke_rimefire_dread"],{still}}}')
    time.sleep(2.0)  # past the ghoul's rise
    rcon.run(dl + "run damage @e[tag=smoke_rimefire_dread,limit=1] 8 minecraft:generic")
    dread_hp = health("@e[tag=smoke_rimefire_dread,limit=1]")
    rcon.run(dl + f"run setblock 50 {pad + 1} 40 dreadpatch:rimefire")
    record("rimefire lies on the ground", "Test passed" in rcon.run(dl + f"if block 50 {pad + 1} 40 dreadpatch:rimefire"), "no rimefire set")
    time.sleep(4.5)
    healed = health("@e[tag=smoke_rimefire_dread,limit=1]") - dread_hp
    record("rimefire heals one of the Dread a quarter a second", 0.7 <= healed <= 1.6, f"healed {healed:.2f} in 4.5 s")
    rcon.run(dl + "run kill @e[tag=smoke_rimefire_dread]")
    # Walled in too (0.56.0's run: it walked off the fire and took nothing).
    rcon.run(dl + f"run fill 39 {pad + 1} 39 41 {pad + 3} 41 minecraft:stone")
    rcon.run(dl + f"run fill 40 {pad + 1} 40 40 {pad + 3} 40 minecraft:air")
    rcon.run(dl + f'run summon minecraft:husk 40.5 {pad + 1} 40.5 {{PersistenceRequired:1b,Tags:["smoke_rimefire_zombie"],Fire:200s,{still}}}')
    rcon.run(dl + f"run setblock 40 {pad + 1} 40 dreadpatch:rimefire")
    time.sleep(2.5)
    record("and chills anyone else", has_effect("@e[tag=smoke_rimefire_zombie,limit=1]", "dreadpatch:rimefrost"), "no Rimefrost on the husk")
    fire = re.search(r"entity data: (-?\d+)s", rcon.run("data get entity @e[tag=smoke_rimefire_zombie,limit=1] Fire"))
    record("and puts them out", fire is not None and int(fire.group(1)) <= 0, f"Fire {fire and fire.group(1)}")
    # And burns them with its cold (0.55.0): read once it has put them out, so no fire of theirs counts.
    before = health("@e[tag=smoke_rimefire_zombie,limit=1]")
    time.sleep(2.5)
    after = health("@e[tag=smoke_rimefire_zombie,limit=1]")
    record("and hurts anyone not of the Dread", after < before, f"the husk went from {before} to {after} in 2.5 s on it")
    # A Withering Rime-Bolt into the ground (0.55.0) leaves a block of embers, gone in 2-4 s.
    rcon.run(dl + f"run fill 45 {pad + 1} 45 45 {pad + 6} 45 minecraft:air")
    rcon.run(dl + f"run summon dreadpatch:withering_rime_bolt 45.5 {pad + 4} 45.5 {{Motion:[0.0d,-1.0d,0.0d]}}")
    embers = False
    for _ in range(10):
        time.sleep(0.2)
        if "Test passed" in rcon.run(dl + f"if block 45 {pad + 1} 45 dreadpatch:rimefire[brief=true]"):
            embers = True
            break
    record("a bolt into the ground leaves rimefire embers", embers, "no brief rimefire where it struck")
    time.sleep(4.5)
    record("and they're gone in seconds", "Test failed" in rcon.run(dl + f"if block 45 {pad + 1} 45 dreadpatch:rimefire"), "still burning 4.5 s on")
    rcon.run(dl + "run kill @e[tag=smoke_rimefire_dread]")
    rcon.run(dl + "run kill @e[tag=smoke_rimefire_zombie]")
    rcon.run(dl + f"run fill 38 {pad + 1} 38 60 {pad + 1} 58 minecraft:air replace dreadpatch:rimefire")
    rcon.run(dl + f"run fill 56 {pad + 3} 38 60 {pad + 3} 42 minecraft:stone")
    rcon.run(dl + f'run summon minecraft:husk 41.5 {pad + 1} 44.5 {{NoAI:1b,PersistenceRequired:1b,Tags:["smoke_storm_out"]}}')
    rcon.run(dl + f'run summon minecraft:husk 58.5 {pad + 1} 40.5 {{NoAI:1b,PersistenceRequired:1b,Tags:["smoke_storm_in"]}}')
    regalia = ",".join(f'{{id:"dreadpatch:dread_queen_{p}",count:1}}' for p in ("greaves", "robe", "bodice", "crown"))
    rcon.run(dl + f'run summon minecraft:husk 44.5 {pad + 1} 52.5 {{NoAI:1b,PersistenceRequired:1b,Tags:["smoke_storm_regal"],ArmorItems:[{regalia}]}}')
    rcon.run("weather rain")
    time.sleep(3.0)
    record("her full regalia eases plain snow's slow away (0.58.0)", not has_effect("@e[tag=smoke_storm_regal,limit=1]", "dreadpatch:rimefrost"), "Rimefrost on the husk in her regalia")
    rcon.run(dl + "run kill @e[tag=smoke_storm_regal]")
    record("in a Dreadlands storm, Rimefrost under the open sky", has_effect("@e[tag=smoke_storm_out,limit=1]", "dreadpatch:rimefrost"), "no Rimefrost on the husk outside")
    record("but not under a roof", not has_effect("@e[tag=smoke_storm_in,limit=1]", "dreadpatch:rimefrost"), "Rimefrost on the husk indoors")
    rcon.run("weather clear")
    rcon.run(dl + "run kill @e[tag=smoke_storm_out]")
    rcon.run(dl + "run kill @e[tag=smoke_storm_in]")
    rcon.run(dl + "run kill @e[type=item]")
    # Whiteout weather (0.54.0): with whiteout_storms 2, every storm that may be one is, so they alternate,
    # never two in a row; in one, Whiteout and freezing damage under the open sky, not under a roof; a queen
    # coming breaks it into thunder; and none comes while she's about.
    rcon.run(dl + f'run summon minecraft:husk 41.5 {pad + 1} 44.5 {{NoAI:1b,PersistenceRequired:1b,Tags:["smoke_whiteout_out"]}}')
    rcon.run(dl + f'run summon minecraft:husk 58.5 {pad + 1} 40.5 {{NoAI:1b,PersistenceRequired:1b,Tags:["smoke_whiteout_in"]}}')
    out_husk, in_husk = "@e[tag=smoke_whiteout_out,limit=1]", "@e[tag=smoke_whiteout_in,limit=1]"
    set_config("whiteout_storms", 2)

    def next_storm():
        # The last storm's Whiteout (60 ticks) has run out by the time this one's read.
        rcon.run("weather clear")
        time.sleep(2.5)
        rcon.run("weather rain")
        time.sleep(3.5)
        return has_effect(out_husk, "dreadpatch:whiteout")

    storms = [next_storm() for _ in range(3)]
    record("whiteout weather: every other storm at most, never two in a row", storms in ([True, False, True], [False, True, False]), f"whiteouts in three storms: {storms}")
    if not storms[-1]:
        storms.append(next_storm())
    record("a whiteout doesn't reach under a roof", not has_effect(in_husk, "dreadpatch:whiteout"), "Whiteout on the husk indoors")
    before = health(out_husk)
    time.sleep(3.5)
    after = health(out_husk)
    record("it freezes what's out in it", after < before, f"the husk outside went from {before} to {after} in 3.5 s")
    record("and it's still a storm: Rimefrost too", has_effect(out_husk, "dreadpatch:rimefrost"), "no Rimefrost on the husk outside")
    rcon.run(dl + f'run summon dreadpatch:dread_queen 50.5 {pad + 1} 50.5 {{NoAI:1b,UUID:[I;23,23,23,23],Tags:["smoke_whiteout_queen"]}}')
    # A level counts as thundering once its thunder, rising 0.01 a tick, passes 0.9: 91 ticks on (0.54.0's
    # first run read it at 4.5 s and found none, though the whiteout had lifted).
    thunder = ""
    for _ in range(20):
        time.sleep(0.5)
        thunder = rcon.run(dl + 'run execute if predicate {condition:"minecraft:weather_check",thundering:true}')
        if "Test passed" in thunder:
            break
    record("a queen coming breaks it into a thunderstorm", "Test passed" in thunder, "not thundering 10 s on: " + thunder.strip())
    record("and its Whiteout lifts", not has_effect(out_husk, "dreadpatch:whiteout"), "the husk outside still has Whiteout after the thunder came")
    storms = [next_storm() for _ in range(2)]
    record("no whiteout comes while a queen's about", storms == [False, False], f"whiteouts in two storms with her there: {storms}")
    rcon.run("kill @e[tag=smoke_whiteout_queen]")
    clear_court()
    rcon.run("weather clear")
    set_config("whiteout_storms", 0)
    # /dreadpatch (0.56.0): its weather set and read back; the Dread raised on the pad.
    # /dreadpatch config (0.58.0): a setting read, set (saved to the file) and put back; a bad value refused.
    out = rcon.run("dreadpatch config flame_fade_max_seconds 50")
    record("/dreadpatch config sets a setting", "Set flame_fade_max_seconds to 50" in out, out.strip())
    out = rcon.run("dreadpatch config flame_fade_max_seconds")
    record("and reads it back", "= 50" in out, out.strip())
    out = rcon.run("dreadpatch config flame_fade_max_seconds banana")
    record("and refuses a bad value", "can't be" in out, out.strip())
    rcon.run("dreadpatch config flame_fade_max_seconds 45")
    rcon.run("dreadpatch weather whiteout 60")
    out = rcon.run("dreadpatch weather")
    record("/dreadpatch weather whiteout makes one", "a whiteout" in out, out.strip())
    rcon.run("dreadpatch weather clear 30")
    out = rcon.run("dreadpatch weather")
    record("/dreadpatch weather clear clears it", ": clear" in out, out.strip())
    out = rcon.run(dl + f"positioned 50 {pad + 1} 50 run dreadpatch rise 3")
    record("/dreadpatch rise raises the Dread", "3 of the Dread rose" in out, out.strip())
    out = rcon.run(dl + f"positioned 50 {pad + 1} 50 run dreadpatch patrol")
    record("/dreadpatch patrol raises a patrol", "of the Dread rose" in out, out.strip())
    for kind in court_types:
        rcon.run(dl + f"run kill @e[type={kind},x=50,y={pad + 1},z=50,distance=..30]")
    rcon.run(dl + "run kill @e[tag=smoke_whiteout_out]")
    rcon.run(dl + "run kill @e[tag=smoke_whiteout_in]")
    rcon.run(dl + "run kill @e[type=item]")
    # A Dread patrol (0.52.0): its leader (a thrall here, on the pad) walks to its waypoint, its follower keeps
    # up; the leader killed, the follower leads. (Patrols rising, and the Dread rising by weather, need a player.)
    rcon.run(dl + f'run summon iceandfire:dread_thrall 40.5 {pad + 1} 48.5 {{PersistenceRequired:1b,UUID:[I;21,21,21,21],Tags:["smoke_patrol_lead"],'
             f'NeoForgeData:{{"dreadpatch:patrol_leader":[I;21,21,21,21],"dreadpatch:patrol_waypoint":[I;58,{pad + 1},48]}}}}')
    rcon.run(dl + f'run summon iceandfire:dread_ghoul 39.5 {pad + 1} 46.5 {{PersistenceRequired:1b,Tags:["smoke_patrol_follow"],'
             f'NeoForgeData:{{"dreadpatch:patrol_leader":[I;21,21,21,21]}}}}')
    lead_x = lambda: float((re.search(r"\[([-0-9.]+)d", rcon.run(dl + "run data get entity @e[tag=smoke_patrol_lead,limit=1] Pos")) or [0, "0"])[1])
    time.sleep(10.0)
    moved = lead_x() - 40.5
    record("a patrol's leader walks to its waypoint", moved >= 6.0, f"it went {moved:.1f} blocks toward x 58")
    close = "Test passed" in rcon.run(dl + "as @e[tag=smoke_patrol_lead,limit=1] at @s if entity @e[tag=smoke_patrol_follow,distance=..10]")
    record("and the rest keep up", close, "the ghoul is more than 10 blocks from its leader")
    rcon.run(dl + "run kill @e[tag=smoke_patrol_lead]")
    time.sleep(3.0)
    leads = "Test passed" in rcon.run(dl + 'if data entity @e[tag=smoke_patrol_follow,limit=1] NeoForgeData."dreadpatch:patrol_waypoint"')
    record("its leader killed, the next leads", leads, "the ghoul has no waypoint of its own")
    rcon.run(dl + "run kill @e[tag=smoke_patrol_follow]")
    # The Dreadlands' sun burns no undead (0.60.0): at noon, clear, a zombie on the pad stays unlit and
    # whole, where one in the overworld's sun (the control) burns. Summoned with NBT, so no helmet.
    rcon.run("time set noon")
    rcon.run("weather clear")
    rcon.run(dl + f'run summon minecraft:zombie 58.5 {pad + 1} 56.5 {{NoAI:1b,PersistenceRequired:1b,Tags:["smoke_sunless"]}}')
    rcon.run('summon minecraft:zombie 3.5 212 12.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_sunlit"]}')
    # 8 s: at full sun a zombie catches fire with a chance of about 4% a tick, so 4 s once left the control unlit.
    time.sleep(8.0)
    fire_read = lambda command: int((re.search(r"entity data: (-?\d+)s", rcon.run(command)) or [0, "0"])[1])
    lit = fire_read("data get entity @e[tag=smoke_sunlit,limit=1] Fire")
    unlit = fire_read(dl + "run data get entity @e[tag=smoke_sunless,limit=1] Fire")
    whole = (re.search(r"entity data: ([0-9.]+)f", rcon.run(dl + "run data get entity @e[tag=smoke_sunless,limit=1] Health")) or [0, "-1"])[1]
    record("a zombie burns in the overworld's sun (the control)", lit > 0, "it isn't burning")
    record("one doesn't in the Dreadlands'", unlit <= 0 and float(whole) >= 20.0, f"Fire {unlit}, health {whole}")
    rcon.run("kill @e[tag=smoke_sunlit]")
    rcon.run(dl + "run kill @e[tag=smoke_sunless]")
    # Dreadwood saplings in the Dreadlands take only graveyard soil (0.61.0): bone-mealed by a dispenser beside
    # it, one on graveyard soil there grows, one on dirt there doesn't; one on dirt in the overworld (the control)
    # grows. (Random ticks come only near a player, so a sped-up randomTickSpeed grew nothing. A player's
    # placing one on dirt there being refused needs a player.)
    def bone_meal(prefix, x, y, z):
        rcon.run(prefix + f"run setblock {x} {y - 1} {z} " + ("iceandfire:graveyard_soil" if prefix == dl and x == 40 else "minecraft:dirt"))
        rcon.run(prefix + f"run setblock {x} {y} {z} iceandfire:dreadwood_sapling")
        rcon.run(prefix + f'run setblock {x + 1} {y} {z} minecraft:dispenser[facing=west]{{Items:[{{Slot:0b,id:"minecraft:bone_meal",count:64}}]}}')
    for prefix, x, y, z in ((dl, 40, pad + 1, 56), (dl, 46, pad + 1, 56), ("execute ", 28, 220, 3)):
        bone_meal(prefix, x, y, z)
    for _ in range(24):
        for prefix, x, y, z in ((dl, 40, pad + 1, 56), (dl, 46, pad + 1, 56), ("execute ", 28, 220, 3)):
            rcon.run(prefix + f"run setblock {x + 2} {y} {z} minecraft:redstone_block")
        time.sleep(0.25)
        for prefix, x, y, z in ((dl, 40, pad + 1, 56), (dl, 46, pad + 1, 56), ("execute ", 28, 220, 3)):
            rcon.run(prefix + f"run setblock {x + 2} {y} {z} minecraft:air")
        time.sleep(0.25)
    grown = lambda prefix, x, y, z: "Test passed" in rcon.run(prefix + f"if block {x} {y} {z} iceandfire:dreadwood_log")
    record("a dreadwood sapling on dirt grows in the overworld (the control)", grown("execute ", 28, 220, 3), "it didn't grow")
    record("in the Dreadlands, one on graveyard soil grows", grown(dl, 40, pad + 1, 56), "it didn't grow")
    record("and one on dirt doesn't", "Test passed" in rcon.run(dl + f"if block 46 {pad + 1} 56 iceandfire:dreadwood_sapling"),
           "it grew, or it's gone")
    rcon.run(dl + f"run fill 36 {pad} 52 50 {pad + 12} 60 minecraft:air")
    rcon.run("fill 24 219 0 32 232 7 minecraft:air")
    rcon.run("kill @e[type=item]")
    # Dreadwood leaves drop saplings (and sticks) now; Ice and Fire ships no loot table for them, so they dropped nothing.
    rcon.run(dl + f"run setblock 50 {pad + 1} 56 iceandfire:dreadwood_leaves[persistent=true]")
    for _ in range(400):
        rcon.run(dl + f"run loot spawn 50.5 {pad + 2} 56.5 mine 50 {pad + 1} 56")
    saplings, leaves = count("iceandfire:dreadwood_sapling"), count("iceandfire:dreadwood_leaves")
    record("dreadwood leaves drop saplings, broken by hand", saplings >= 1 and leaves == 0, f"{saplings} saplings, {leaves} leaves in 400")
    rcon.run(dl + f"run setblock 50 {pad + 1} 56 minecraft:air")
    rcon.run(dl + "run kill @e[type=item]")
    rcon.run(dl + f"run fill 38 {pad} 38 60 {pad + 9} 58 minecraft:air")
    rcon.run(dl + "run forceload remove all")

    # Risen in the open air on her dragon (as she comes beside a spire's summit, 0.47.0), it takes wing:
    # 3 s on, it's hovering, still far over the platform (it sinks a little as it takes wing: 260 to 242).
    # (Over the platform, in loaded chunks: summoned past them, she never ticks.)
    rcon.run('summon dreadpatch:dread_queen 8.5 260 20.5 {WithSyndrigossa:1b}')
    time.sleep(3.0)
    rode = re.search(r"\[[-0-9.]+d, ([-0-9.]+)d", rcon.run("execute as @e[type=dreadpatch:dread_queen,limit=1] on vehicle run data get entity @s Pos"))
    hovering = "1b" in rcon.run("data get entity @e[type=dreadpatch:syndrigossa,limit=1] Hovering") \
        or "1b" in rcon.run("data get entity @e[type=dreadpatch:syndrigossa,limit=1] Flying")
    record("risen in the air, her dragon takes wing", rode is not None and float(rode.group(1)) > 225.0 and hovering,
           f"her dragon at y {rode and rode.group(1)} (the platform at 200), hovering or flying: {hovering}")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")
    clear_court()
    rcon.run("kill @e[type=item]")

    # The Dreadlands' structures (0.53.0), far out where the ground is new: an abandoned village is all
    # Dread wood and stone, none of vanilla's spruce planks or cobblestone left; spires' summits keep a chest
    # half the time; and every chest is Lootr's (Lootr's in the dev runs, the owner's pack's version).
    def count_blocks(block, x0, z0, x1, z1, y0, y1):
        # Each fill swaps the block for sponge and back, counting what it swapped (/fill counts no same-block "changes").
        total = 0
        for x in range(x0, x1 + 1, 32):
            for z in range(z0, z1 + 1, 32):
                for y in range(y0, y1 + 1, 16):
                    box = f"{x} {y} {z} {min(x + 31, x1)} {min(y + 15, y1)} {min(z + 31, z1)}"
                    filled = re.search(r"filled (\d+)", rcon.run(dl + f"run fill {box} minecraft:sponge replace {block}"))
                    if filled and int(filled.group(1)):
                        rcon.run(dl + f"run fill {box} {block} replace minecraft:sponge")
                        total += int(filled.group(1))
        return total

    # Somewhere new each run (the world is kept between runs, and old chunks keep what they were made with).
    # Anywhere in a wide ring, either way on each axis: 32 spots on one diagonal (until 0.61.0) came round
    # again, and once found a village made before 0.57.0 turned its lights (all 4 still vanilla's).
    far_x = random.choice((-1, 1)) * random.randrange(8_000, 200_000)
    far_z = random.choice((-1, 1)) * random.randrange(8_000, 200_000)
    # Every village made from here on has its lich (0.70.0), till it's put back after the check.
    rcon.run("dreadpatch config village_liches 1")
    out = rcon.run(dl + f"positioned {far_x} 0 {far_z} run locate structure dreadpatch:dread_village")
    v = re.search(r"at \[(-?\d+), ~, (-?\d+)\]", out)
    record("abandoned villages stand in the Dreadlands", v is not None, out.strip())
    if v:
        vx, vz = (int(v.group(1)) & ~15) + 8, (int(v.group(2)) & ~15) + 8
        rcon.run(dl + f"run forceload add {vx - 64} {vz - 64} {vx + 64} {vz + 64}")
        time.sleep(20.0)
        area = (vx - 56, vz - 56, vx + 56, vz + 56, 50, 110)
        dread_wood = count_blocks("iceandfire:dreadwood_planks", *area) + count_blocks("iceandfire:dreadwood_log", *area)
        left = count_blocks("minecraft:spruce_planks", *area) + count_blocks("minecraft:cobblestone", *area) + count_blocks("minecraft:spruce_log", *area)
        record("its wood and stone are the Dread's", dread_wood > 50 and left == 0, f"{dread_wood} dreadwood, {left} spruce planks, logs and cobblestone left")
        # Wood's other shapes too (0.65.0): dreadwood's stairs, slabs, fences and doors where spruce's stood.
        spruce = sum(count_blocks(f"minecraft:spruce_{shape}", *area) for shape in ("stairs", "slab", "fence", "door", "trapdoor"))
        shapes = sum(count_blocks(f"dreadpatch:dreadwood_{shape}", *area) for shape in ("stairs", "slab", "fence", "door", "trapdoor"))
        record("its stairs, slabs, fences and doors are dreadwood", spruce == 0 and shapes > 0, f"{shapes} dreadwood, {spruce} spruce left")
        flames = sum(count_blocks(b, *area) for b in ("minecraft:lantern", "minecraft:soul_lantern", "minecraft:torch", "minecraft:wall_torch"))
        dread_flames = sum(count_blocks(b, *area) for b in ("dreadpatch:dread_lantern", "iceandfire:dread_torch", "iceandfire:dread_torch_wall"))
        record("its lanterns and torches are the Dread's", flames == 0, f"{flames} vanilla lanterns and torches left, {dread_flames} of the Dread's")
        loot_chests, plain_chests = count_blocks("lootr:lootr_chest", *area), count_blocks("minecraft:chest", *area)
        record("its chests are Lootr's", plain_chests == 0, f"{loot_chests} Lootr chests, {plain_chests} plain chests")
        # A Dread Lich at its heart (0.70.0, the owner: "Ice and Fire's Liches Should sometimes spawn in dread villages").
        out = rcon.run(dl + f"if entity @e[type=iceandfire:dread_lich,x={vx - 64},y=-64,z={vz - 64},dx=128,dy=400,dz=128]")
        record("a Dread Lich keeps an abandoned village", "Test passed" in out, out.strip())
        rcon.run(dl + "run forceload remove all")
    rcon.run("dreadpatch config village_liches 3")
    summit_chests, plain_summit, torches = 0, 0, 0
    forge_cores, forge_ok = [], True
    for sx_far, sz_far in ((-far_x, far_z), (far_x, -far_z)):
        out = rcon.run(dl + f"positioned {sx_far} 0 {sz_far} run locate structure dreadpatch:spire")
        m = re.search(r"at \[(-?\d+), ~, (-?\d+)\]", out)
        if not m:
            continue
        sx, sz = (int(m.group(1)) & ~15) + 8, (int(m.group(2)) & ~15) + 8
        rcon.run(dl + f"run forceload add {sx - 24} {sz - 24} {sx + 24} {sz + 24}")
        time.sleep(12.0)
        rcon.run(dl + f'positioned {sx} 0 {sz} positioned over world_surface run summon minecraft:marker ~ ~ ~ {{Tags:["smoke_probe"]}}')
        top = re.search(r"\[[-0-9.]+d, ([-0-9.]+)d", rcon.run("data get entity @e[tag=smoke_probe,limit=1] Pos"))
        rcon.run("kill @e[tag=smoke_probe]")
        ty = int(float(top.group(1))) if top else 150
        summit = (sx - 12, sz - 12, sx + 12, sz + 12, ty - 6, ty + 6)
        summit_chests += count_blocks("lootr:lootr_chest", *summit)
        plain_summit += count_blocks("minecraft:chest", *summit)
        torches += count_blocks("iceandfire:dread_torch", *summit)
        # A ruined Rimefrost dragonforge now and then (0.78.0; about one spire in seven, so mostly none here): never more
        # than one core a summit, and any core with some of its bricks still round it. (Counted by swapping, so a core
        # found loses its empty block entity; nothing's in it.)
        cores = count_blocks("dreadpatch:rimefrost_forge_core", *summit)
        bricks = count_blocks("dreadpatch:rimefrost_forge_brick", *summit)
        forge_cores.append(cores)
        forge_ok = forge_ok and cores <= 1 and (cores == 0 or bricks > 0)
        rcon.run(dl + "run forceload remove all")
    record("a spire's summit chest is Lootr's", plain_summit == 0, f"{summit_chests} Lootr chests, {plain_summit} plain chests on two summits ({torches} dread torches)")
    record("a summit's forge ruin, if any, is one core among its bricks", forge_ok, f"cores per summit {forge_cores}")

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
        # Her regalia also takes 5% a piece off Rimefrost damage (0.51.0): 30% of it, then 80% of that.
        share = 0.3 * 0.8 if label.startswith("the Dread Queen") else 0.3
        record(f"{label} takes {100 - round(share * 100)}% off Rimefrost breath", struck > 0 and abs(breathed - share * struck) < 0.6,
               f"breath took {breathed:.2f}, a plain hit {struck:.2f} ({round(share * 100)}% would be {share * struck:.2f})")
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
    # Its powers (0.51.0): +150 max mana a piece, indestructible, the full set can't freeze, and 5% a piece
    # off Rimefrost damage taken and onto Rimefrost damage dealt (her school's magic here, which nothing
    # else of the armour's touches).
    queen_mana, steel_mana = worn("smoke_regalia", "irons_spellbooks:max_mana"), worn("smoke_steel", "irons_spellbooks:max_mana")
    record("her regalia gives +150 max mana a piece", queen_mana is not None and steel_mana is not None and queen_mana - steel_mana == 600.0,
           f"regalia {queen_mana}, dread dragonsteel {steel_mana}")
    check("her regalia is indestructible", "execute if items entity @e[tag=smoke_regalia,limit=1] armor.* *[minecraft:unbreakable]", "count: 4")
    for tag in ("smoke_regalia", "smoke_steel"):
        rcon.run(f"data merge entity @e[tag={tag},limit=1] {{TicksFrozen:140}}")
    time.sleep(0.3)
    frozen = lambda tag: int((re.search(r"entity data: (\d+)", rcon.run(f"data get entity @e[tag={tag},limit=1] TicksFrozen")) or [0, "0"])[1])
    record("in her whole regalia, nothing freezes", frozen("smoke_regalia") == 0 and frozen("smoke_steel") > 0,
           f"frozen ticks: regalia {frozen('smoke_regalia')}, dread dragonsteel {frozen('smoke_steel')}")
    for tag in ("smoke_regalia", "smoke_steel"):
        rcon.run(f"data merge entity @e[tag={tag},limit=1] {{Health:100.0f}}")
        rcon.run(f"damage @e[tag={tag},limit=1] 20 dreadpatch:rimefrost_magic")
    queen_took, steel_took = 100.0 - health("@e[tag=smoke_regalia,limit=1]"), 100.0 - health("@e[tag=smoke_steel,limit=1]")
    record("her regalia takes 5% a piece off Rimefrost magic", steel_took > 0 and abs(queen_took - 0.8 * steel_took) < 0.3,
           f"regalia took {queen_took:.2f}, dread dragonsteel {steel_took:.2f} (80% would be {0.8 * steel_took:.2f})")
    # And deals 5% a piece more (the owner: it's "to add rimefrost damage"): the same blow into two bare
    # zombies, one from the one in her regalia, one from the one in dread dragonsteel.
    for tag, dealer in (("smoke_hit_by_queen", "smoke_regalia"), ("smoke_hit_by_steel", "smoke_steel")):
        rcon.run(f'summon minecraft:zombie 12.5 240 {4.5 if dealer == "smoke_regalia" else 8.5} {{NoAI:1b,PersistenceRequired:1b,Tags:["{tag}"]}}')
        rcon.run(f"attribute @e[tag={tag},limit=1] minecraft:generic.max_health base set 100")
        rcon.run(f"data merge entity @e[tag={tag},limit=1] {{Health:100.0f}}")
    time.sleep(0.2)
    for tag, dealer in (("smoke_hit_by_queen", "smoke_regalia"), ("smoke_hit_by_steel", "smoke_steel")):
        rcon.run(f"damage @e[tag={tag},limit=1] 20 dreadpatch:rimefrost_magic by @e[tag={dealer},limit=1]")
    by_queen, by_steel = 100.0 - health("@e[tag=smoke_hit_by_queen,limit=1]"), 100.0 - health("@e[tag=smoke_hit_by_steel,limit=1]")
    record("and deals 5% a piece more", by_steel > 0 and abs(by_queen - 1.2 * by_steel) < 0.3,
           f"from her regalia {by_queen:.2f}, from dread dragonsteel {by_steel:.2f} (120% would be {1.2 * by_steel:.2f})")
    for tag in ("smoke_hit_by_queen", "smoke_hit_by_steel"):
        rcon.run(f"kill @e[tag={tag}]")
    # Withering Rime's withering counts too (the owner: "both ways on what counts as rimefrost"), as dealt by
    # whoever laid the rime: Rimefire Blast's cone (4.5, and 3 s of rime) from a zombie in her regalia and
    # one in dread dragonsteel into two golems; everything the regalia's golem loses, 20% more.
    for uuid, row, pieces in (("11", 4.5, regalia), ("12", 8.5, pieces_of("dreadpatch:dread_dragonsteel"))):
        dressed = ",".join(f'{{id:"{piece}",count:1}}' for piece in pieces)
        rcon.run(f'summon minecraft:zombie 16.5 240 {row} {{NoAI:1b,PersistenceRequired:1b,UUID:[I;{uuid},{uuid},{uuid},{uuid}],'
                 f'Tags:["smoke_blaster"],Rotation:[-90f,0f],ArmorItems:[{dressed}]}}')
        rcon.run(f'summon minecraft:iron_golem 20.5 240 {row} {{NoAI:1b,PersistenceRequired:1b,Tags:["smoke_blasted_{uuid}"]}}')
    time.sleep(0.3)
    for uuid, row in (("11", 4.5), ("12", 8.5)):
        rcon.run(f"summon dreadpatch:rimefire_blast 16.5 241 {row} {{Owner:[I;{uuid},{uuid},{uuid},{uuid}],Damage:4.5f}}")
    time.sleep(3.6)
    by_queen, by_steel = 100.0 - health("@e[tag=smoke_blasted_11,limit=1]"), 100.0 - health("@e[tag=smoke_blasted_12,limit=1]")
    record("her regalia's withering counts as Rimefrost dealt", by_steel > 4.5 and abs(by_queen - 1.2 * by_steel) < 0.5,
           f"from her regalia {by_queen:.2f}, from dread dragonsteel {by_steel:.2f} (120% would be {1.2 * by_steel:.2f})")
    rcon.run("kill @e[type=dreadpatch:rimefire_blast]")
    for tag in ("smoke_blaster", "smoke_blasted_11", "smoke_blasted_12"):
        rcon.run(f"kill @e[tag={tag}]")
    rcon.run("kill @e[tag=smoke_regalia]")
    rcon.run("kill @e[tag=smoke_steel]")
    # Her winged bodice (0.81.0) is her bodice in every way: in its place her set is whole (+600 mana, nothing freezes),
    # and it's indestructible; made at a smithing table from a scale, her bodice and an elytra.
    winged = ["dreadpatch:dread_queen_greaves", "dreadpatch:dread_queen_robe", "dreadpatch:dread_queen_winged_bodice", "dreadpatch:dread_queen_crown"]
    armoured("smoke_winged", 4.5, winged)
    armoured("smoke_steel", 8.5, pieces_of("dreadpatch:dread_dragonsteel"))
    time.sleep(0.3)
    winged_mana, steel_mana = worn("smoke_winged", "irons_spellbooks:max_mana"), worn("smoke_steel", "irons_spellbooks:max_mana")
    rcon.run("data merge entity @e[tag=smoke_winged,limit=1] {TicksFrozen:140}")
    time.sleep(0.3)
    record("her winged bodice counts as her bodice: the set's mana, and nothing freezes",
           winged_mana is not None and steel_mana is not None and winged_mana - steel_mana == 600.0 and frozen("smoke_winged") == 0,
           f"mana {winged_mana} against {steel_mana}, frozen ticks {frozen('smoke_winged')}")
    check("her winged bodice is indestructible", "execute if items entity @e[tag=smoke_winged,limit=1] armor.chest *[minecraft:unbreakable]", "count: 1")
    check("its smithing recipe is there", "recipe give @a dreadpatch:dread_queen_winged_bodice", "No player was found")
    rcon.run("kill @e[tag=smoke_winged]")
    rcon.run("kill @e[tag=smoke_steel]")
    # Her staff: +5% Rimefrost spell power in the hand (0.51.0).
    rcon.run('summon minecraft:armor_stand 12.5 240 4.5 {Tags:["smoke_staff"],ShowArms:1b,HandItems:[{id:"iceandfire:dread_queen_staff",count:1},{}]}')
    time.sleep(0.2)
    power = worn("smoke_staff", "dreadpatch:rimefrost_spell_power")
    record("her staff gives +5% Rimefrost spell power", power is not None and abs(power - 1.05) < 1e-6, f"Rimefrost spell power {power}")
    rcon.run("kill @e[tag=smoke_staff]")
    # A dread shard is a school focus at Iron's Scroll Forge (0.51.1: its slot takes only #irons_spellbooks:school_focus).
    rcon.run('summon minecraft:armor_stand 12.5 240 8.5 {Tags:["smoke_focus"],ShowArms:1b,HandItems:[{id:"iceandfire:dread_shard",count:1},{}]}')
    check("a dread shard is a Scroll Forge focus", "execute if items entity @e[tag=smoke_focus,limit=1] weapon.mainhand #irons_spellbooks:school_focus", "Test passed")
    check("Rimefrost's own", "execute if items entity @e[tag=smoke_focus,limit=1] weapon.mainhand #dreadpatch:rimefrost_focus", "Test passed")
    rcon.run("kill @e[tag=smoke_focus]")
    # In her whole regalia the Dread leave you be (0.51.0): a dread thrall (with its AI, held still) goes for
    # a zombie beside it, but not for one in her regalia, until that one hurts it.
    still = 'active_effects:[{id:"minecraft:slowness",amplifier:255b,duration:1200}]'
    rcon.run(f'summon iceandfire:dread_thrall 4.5 240 10.5 {{PersistenceRequired:1b,Tags:["smoke_dread_judge"],{still}}}')
    rcon.run('summon minecraft:zombie 8.5 240 10.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_plain"]}')
    judge_target = lambda: rcon.run("execute as @e[tag=smoke_dread_judge,limit=1] on target run data get entity @s Tags")
    took = False
    for _ in range(16):
        time.sleep(0.5)
        if "smoke_plain" in judge_target():
            took = True
            break
    record("a dread thrall goes for a zombie (the control)", took, "it never took the plain zombie for its target")
    rcon.run("kill @e[tag=smoke_plain]")
    worn_list = ",".join(f'{{id:"{piece}",count:1}}' for piece in regalia)
    rcon.run(f'summon minecraft:zombie 8.5 240 10.5 {{NoAI:1b,PersistenceRequired:1b,Tags:["smoke_crowned"],ArmorItems:[{worn_list}]}}')
    time.sleep(5.0)
    record("but leaves one in her whole regalia be", "smoke_crowned" not in judge_target(), judge_target().strip())
    rcon.run("damage @e[tag=smoke_dread_judge,limit=1] 1 minecraft:mob_attack by @e[tag=smoke_crowned,limit=1]")
    fought = False
    for _ in range(10):
        time.sleep(0.5)
        if "smoke_crowned" in judge_target():
            fought = True
            break
    record("until it hurts the Dread", fought, "the thrall didn't turn on the one in her regalia that hit it")
    for tag in ("smoke_dread_judge", "smoke_crowned"):
        rcon.run(f"kill @e[tag={tag}]")
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

    # The Rimefrost dragonforge (0.59.0): built of its bricks round its core, Ice and Fire's dragon bone
    # blocks on the corners above and below, its aperture in the east side (core at 8 241 7).
    def build_forge(cx, cy, cz, brick, core, aperture, west=False):
        for y in (cy - 1, cy, cy + 1):
            rcon.run(f"fill {cx - 1} {y} {cz - 1} {cx + 1} {y} {cz + 1} {brick}")
        for y in (cy - 1, cy + 1):
            for dx in (-1, 1):
                for dz in (-1, 1):
                    rcon.run(f"setblock {cx + dx} {y} {cz + dz} iceandfire:dragon_bone_block")
        rcon.run(f"setblock {cx} {cy} {cz} {core}")
        rcon.run(f"setblock {cx + (-1 if west else 1)} {cy} {cz} {aperture}")

    def forge_items(x, y, z):
        return rcon.run(f"data get block {x} {y} {z} Items")

    # By day: Ice and Fire's ice dragon (the control, below) sleeps at night and never breathes on its
    # forge, and by here in a full run it's night (0.59.0's first run). Ours never sleep.
    rcon.run("time set noon")
    rcon.run("weather clear")
    rcon.run("fill 1 240 1 30 250 14 minecraft:air")
    build_forge(8, 241, 7, "dreadpatch:rimefrost_forge_brick", "dreadpatch:rimefrost_forge_core", "dreadpatch:rimefrost_forge_input")
    time.sleep(0.5)
    check("the Rimefrost dragonforge stands whole (its core lit)", "execute if block 8 241 7 dreadpatch:rimefrost_forge_core[lit=true]", "Test passed")
    check("its grill shows beside the core", "execute if block 8 241 6 dreadpatch:rimefrost_forge_brick[grill=true]", "Test passed")
    rcon.run("setblock 7 240 6 dreadpatch:rimefrost_forge_brick")
    time.sleep(0.5)
    check("a brick where a bone corner goes: not whole", "execute if block 8 241 7 dreadpatch:rimefrost_forge_core[lit=false]", "Test passed")
    rcon.run("setblock 7 240 6 iceandfire:dragon_bone_block")
    time.sleep(0.5)
    check("whole again with the bone back", "execute if block 8 241 7 dreadpatch:rimefrost_forge_core[lit=true]", "Test passed")
    # Hoppers through its bricks (their capability): from above into what goes in, from a side into its blood.
    rcon.run('setblock 8 243 7 minecraft:hopper[facing=down]{Items:[{Slot:0b,id:"dreadpatch:rimefrost_dragon_scale",count:1}]}')
    rcon.run('setblock 8 241 5 minecraft:hopper[facing=south]{Items:[{Slot:0b,id:"minecraft:glass_bottle",count:1}]}')
    time.sleep(1.5)
    out = forge_items(8, 241, 7)
    record("a hopper on its top brick fills what goes in", re.search(r'Slot: 0b, id: "dreadpatch:rimefrost_dragon_scale"', out) is not None, out.strip())
    record("a hopper on a side brick fills its blood", re.search(r'Slot: 1b, id: "minecraft:glass_bottle"', out) is not None, out.strip())
    rcon.run("setblock 8 243 7 minecraft:air")
    rcon.run("setblock 8 241 5 minecraft:air")
    # A tame Rimefrost dragon (stage 3) 9 blocks east is drawn to its aperture and breathes on it: the
    # aperture glows and the cook starts. Then the cook is set near its end (750 ticks), so the blood
    # comes within a few seconds while the breath keeps it lit.
    check("a tame Rimefrost dragon summons by it",
          'summon dreadpatch:rimefrost_dragon 18.5 240 7.5 {Owner:[I;11,11,11,11],AgeTicks:1300000,Rotation:[90f,0f],'
          'PersistenceRequired:1b,Tags:["smoke_forge_dragon"]}', "Summoned new")
    lit, glowed = False, False
    for _ in range(40):
        time.sleep(0.5)
        glowed = glowed or block_is(9, 241, 7, "dreadpatch:rimefrost_forge_input[active=true]")
        m = re.search(r"block data: ([0-9.]+)d", rcon.run("data get block 8 241 7 CookTime"))
        if m and float(m.group(1)) > 0:
            lit = True
            break
    record("its breath lights the forge", lit, "no cook after 20 s; dragon at " + rcon.run("data get entity @e[tag=smoke_forge_dragon,limit=1] Pos").strip())
    record("its aperture glows under the breath", glowed or block_is(9, 241, 7, "dreadpatch:rimefrost_forge_input[active=true]"), "never active")
    rcon.run("data merge block 8 241 7 {CookTime:700.0d}")
    made = False
    for _ in range(30):
        time.sleep(0.5)
        if re.search(r'Slot: 2b, id: "dreadpatch:rimefrost_dragon_blood"', forge_items(8, 241, 7)):
            made = True
            break
    out = forge_items(8, 241, 7)
    record("it makes Rimefrost dragon blood (scale and bottle used)", made and "glass_bottle" not in out and "rimefrost_dragon_scale" not in out, out.strip())
    rcon.run("kill @e[tag=smoke_forge_dragon]")
    # Only the new forge: Ice and Fire's ice dragonforge, lit by a tame ice dragon, no longer makes
    # Rimefrost blood of our scale; it makes what it makes of anything it has no recipe for, dragon ice.
    build_forge(24, 241, 7, "iceandfire:dragonforge_ice_brick", "iceandfire:dragonforge_ice_core_disabled", "iceandfire:dragonforge_ice_input", west=True)
    time.sleep(0.5)
    rcon.run("item replace block 24 241 7 container.0 with dreadpatch:rimefrost_dragon_scale")
    rcon.run("item replace block 24 241 7 container.1 with minecraft:glass_bottle")
    rcon.run('summon iceandfire:ice_dragon 14.5 240 7.5 {Owner:[I;11,11,11,11],AgeTicks:1300000,Rotation:[-90f,0f],'
             'PersistenceRequired:1b,Tags:["smoke_forge_dragon"]}')
    ice = False
    for _ in range(50):
        time.sleep(0.5)
        if "iceandfire:dragon_ice" in forge_items(24, 241, 7):
            ice = True
            break
    out = forge_items(24, 241, 7)
    record("Ice and Fire's ice forge makes dragon ice of our scale, not our blood", ice and "rimefrost_dragon_blood" not in out, out.strip())
    rcon.run("kill @e[tag=smoke_forge_dragon]")
    rcon.run("fill 1 240 1 30 250 14 minecraft:air")
    rcon.run("kill @e[type=item]")
    rcon.run("fill 1 239 1 30 239 14 minecraft:air")

    # Dread lights at most 6 (0.64.0): a dread lantern and a Dread Gate's face, each alone in a stone box, light it to 6;
    # Ice and Fire's dread torch is 5 of its own.
    for block, level in (("iceandfire:dread_torch", 5), ("dreadpatch:dread_lantern", 6), ("dreadpatch:dread_gate", 6)):
        rcon.run("fill 25 229 11 27 231 13 minecraft:stone")
        rcon.run(f"setblock 26 230 12 {block}")
        time.sleep(0.5)
        lit = lambda lo, hi: "Test passed" in rcon.run("execute positioned 26 230 12 if predicate "
                                                     f'{{condition:"minecraft:location_check",predicate:{{light:{{light:{{min:{lo},max:{hi}}}}}}}}}')
        record(f"{block.split(':')[1]} lights to {level}, no more than 6", lit(level, level), f"not {level} (more than 6: {lit(7, 15)})")
    rcon.run("fill 25 229 11 27 231 13 minecraft:air")
    # Her storm puts out her fire, as everyone's (0.64.0, the owner: she burned on in her own).
    rcon.run('summon dreadpatch:dread_queen 20.5 230 3.5 {NoAI:1b,UUID:[I;24,24,24,24],Tags:["smoke_burning"]}')
    rcon.run("summon dreadpatch:whiteout_dominion 20.5 230 3.5 {Owner:[I;24,24,24,24],Lifetime:200,Radius:6f}")
    rcon.run("data merge entity @e[tag=smoke_burning,limit=1] {Fire:300s}")
    time.sleep(1.5)
    burn = int((re.search(r"entity data: (-?\d+)s", rcon.run("data get entity @e[tag=smoke_burning,limit=1] Fire")) or [0, "999"])[1])
    record("in her own storm her fire goes out", burn <= 0, f"Fire {burn}")
    rcon.run("kill @e[tag=smoke_burning]")
    rcon.run("kill @e[type=dreadpatch:whiteout_dominion]")
    clear_court()
    # Dreadwood's block set (0.65.0): each of its blocks sets.
    for shape in ("stairs", "slab", "fence", "fence_gate", "door", "trapdoor", "button", "pressure_plate"):
        check(f"dreadwood {shape.replace('_', ' ')} is a block", f"setblock 26 230 12 dreadpatch:dreadwood_{shape}", "Changed the block")
        rcon.run("setblock 26 230 12 minecraft:air")
    # Dread lights as sources of cold (0.65.0): with the freezing time cut to 3 s (a second a stage), water beside a
    # dread torch and under a dread lantern turns to blue ice, stage by stage as it's counted (every 5 s); water out of
    # reach stays water.
    freeze_was = set_config("dread_light_freeze_minutes", 0.05) or "20.0"
    atexit.register(set_config, "dread_light_freeze_minutes", freeze_was)
    time.sleep(2.0)
    # Each water a sealed cell in stone (water flowing into a torch's block washes it away: the first run's did), the
    # torch and lantern on the stone over them, 2 up and 1 across.
    rcon.run("fill 15 224 2 23 226 6 minecraft:stone")
    rcon.run("setblock 16 225 4 minecraft:water")
    rcon.run("setblock 22 225 4 minecraft:water")
    rcon.run("setblock 17 227 4 iceandfire:dread_torch")
    rcon.run("setblock 21 227 4 dreadpatch:dread_lantern")
    rcon.run("fill 18 228 3 20 229 5 minecraft:stone")  # out of reach: 2 over the torch and the lantern
    rcon.run("setblock 19 229 4 minecraft:water")
    frozen = False
    for _ in range(40):
        time.sleep(1.0)
        frozen = all("Test passed" in rcon.run(f"execute if block {x} 225 4 minecraft:blue_ice") for x in (16, 22))
        if frozen:
            break
    record("water by a dread torch and under a dread lantern freezes to blue ice", frozen,
           "; ".join(f"{x}: " + ",".join(b for b in ("water", "ice", "packed_ice", "blue_ice") if "Test passed" in rcon.run(f"execute if block {x} 225 4 minecraft:{b}")) for x in (16, 22)))
    record("water out of their reach stays water", "Test passed" in rcon.run("execute if block 19 229 4 minecraft:water"), "it froze")
    set_config("dread_light_freeze_minutes", freeze_was)
    rcon.run("fill 15 224 2 23 229 6 minecraft:air")
    # A crusted lava pocket (0.66.0), placed in a block of stone: lava at its heart, sealed in its crust.
    rcon.run("fill 4 224 10 12 232 14 minecraft:stone")
    out = rcon.run("place feature dreadpatch:crusted_lava_pocket 8 228 12")
    record("a crusted lava pocket: lava sealed in its crust", "Placed" in out and "Test passed" in rcon.run("execute if block 8 228 12 minecraft:lava")
           and "Test failed" in rcon.run("execute if block 8 233 12 minecraft:lava"), out.strip())
    rcon.run("fill 3 223 9 13 233 15 minecraft:air")
    # A grave patch (0.67.0), placed on a stone pad: graveyard soil round its foot, and a wither rose or more on it.
    rcon.run("fill 2 225 2 12 225 12 minecraft:stone")
    out = rcon.run("place feature dreadpatch:grave_patch 7 226 7")
    soil = sum("Test passed" in rcon.run(f"execute if block {x} 225 {z} iceandfire:graveyard_soil") for x in range(4, 11) for z in range(4, 11))
    roses = sum("Test passed" in rcon.run(f"execute if block {x} 226 {z} minecraft:wither_rose") for x in range(4, 11) for z in range(4, 11))
    record("a grave patch: graveyard soil, and wither roses on it", "Placed" in out and soil >= 3 and roses >= 1, f"{out.strip()}; {soil} soil, {roses} roses")
    rcon.run("fill 1 224 1 13 236 13 minecraft:air")
    # What her defeat stirs (0.62.0): a queen killed by damage is a defeat (the world's first: the Mausoleums stir for
    # good) and brings a defeat storm of 30-60 s; another within the cooldown brings no new one; /kill is no defeat.
    # (The storm's fog, rime, sound and Rimefrost, and the Dread rising round Mausoleums, are a player's.)
    status = lambda: rcon.run("dreadpatch escalation")
    left = lambda: int((re.search(r"on, (\d+) s left", status()) or [0, "0"])[1])
    rcon.run("dreadpatch escalation reset")
    rcon.run('summon dreadpatch:dread_queen 10.5 230 10.5 {NoAI:1b,Tags:["smoke_defeat"]}')
    rcon.run("kill @e[tag=smoke_defeat]")
    time.sleep(0.5)
    record("/kill is no defeat", "defeated in this world: No" in status() and left() == 0, status().strip())
    rcon.run('summon dreadpatch:dread_queen 10.5 230 10.5 {NoAI:1b,Tags:["smoke_defeat"]}')
    rcon.run("damage @e[tag=smoke_defeat,limit=1] 1000 minecraft:generic")
    time.sleep(1.0)
    first = left()
    record("her defeat stirs the Mausoleums and brings a 30-60 s storm", "defeated in this world: Yes" in status() and 25 <= first <= 60, status().strip())
    rcon.run('summon dreadpatch:dread_queen 10.5 230 10.5 {NoAI:1b,Tags:["smoke_defeat"]}')
    time.sleep(1.5)
    rcon.run("damage @e[tag=smoke_defeat,limit=1] 1000 minecraft:generic")
    time.sleep(0.5)
    record("another within the cooldown brings none", 0 < left() < first, f"{first} s left, then {left()} s")
    rcon.run("dreadpatch escalation reset")
    record("/dreadpatch escalation reset forgets it", "defeated in this world: No" in status() and left() == 0, status().strip())
    rcon.run("kill @e[type=item]")
    clear_court()
    # Whiteout and bosses (0.60.0): a storm halves a zombie's follow range, but a boss (#c:bosses: Iron's Dead
    # King; vanilla's Wither and dragon take no effects at all) keeps its own, and only slows. In the air over the
    # platform, everything NoAI.
    rcon.run("summon dreadpatch:whiteout_dominion 24.5 220 7.5 {Lifetime:200,Radius:6f}")
    rcon.run('summon irons_spellbooks:dead_king 22.5 221 7.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_boss"]}')
    rcon.run('summon minecraft:zombie 26.5 221 7.5 {NoAI:1b,PersistenceRequired:1b,Tags:["smoke_blinded"]}')
    time.sleep(1.5)
    reach = lambda sel, how: float((re.search(r"is ([0-9.]+)", rcon.run(f"attribute {sel} minecraft:generic.follow_range {how}")) or [0, "-1"])[1])
    boss, zombie = "@e[tag=smoke_boss,limit=1]", "@e[tag=smoke_blinded,limit=1]"
    record("a storm's Whiteout halves a zombie's sight", has_effect(zombie, "dreadpatch:whiteout") and reach(zombie, "get") <= reach(zombie, "base get") / 2 + 0.01,
           f"follow range {reach(zombie, 'get')} of {reach(zombie, 'base get')}")
    record("but a boss in it keeps its own, and is slowed", has_effect(boss, "dreadpatch:whiteout") and reach(boss, "get") >= reach(boss, "base get") - 0.01,
           f"Whiteout {has_effect(boss, 'dreadpatch:whiteout')}, follow range {reach(boss, 'get')} of {reach(boss, 'base get')}")
    rcon.run("kill @e[tag=smoke_boss]")
    rcon.run("kill @e[tag=smoke_blinded]")
    rcon.run("kill @e[type=dreadpatch:whiteout_dominion]")
    rcon.run("kill @e[type=item]")
    # The Dreadlands' music on a disc (0.60.0): in a jukebox, its song plays (a jukebox only starts a song its
    # registry knows); a spire's summit chest holds one now and then (one in ten: none in 60 is 0.2%).
    rcon.run("setblock 20 220 12 minecraft:jukebox")
    rcon.run("item replace block 20 220 12 container.0 with dreadpatch:music_disc_entombed_in_rimefrost")
    time.sleep(0.5)
    playing = "Test passed" in rcon.run("execute if block 20 220 12 minecraft:jukebox[has_record=true]") \
        and "ticks_since_song_started" in rcon.run("data get block 20 220 12")
    record("the disc plays in a jukebox", playing, rcon.run("data get block 20 220 12").strip())
    rcon.run("setblock 20 220 12 minecraft:air")
    rcon.run("kill @e[type=item]")
    for _ in range(60):
        rcon.run("loot spawn 20.5 221 12.5 loot dreadpatch:chests/spire_summit")
    discs = count("dreadpatch:music_disc_entombed_in_rimefrost")
    record("spire summit chests hold the disc now and then", 1 <= discs <= 20, f"{discs} in 60 chests")
    rcon.run("kill @e[type=item]")

    # Her boss theme (0.72.0): a fresh queen or Syndrigossa is marked as she spawns (the start goes to players
    # near her; a marked one never plays it again). Its disc plays in a jukebox, and Syndrigossa's heart holds
    # it one time in four (none in 40 is 1e-5).
    out = rcon.run('summon dreadpatch:dread_queen 24.5 221 8.5 {UUID:[I;72,72,72,1],NoAI:1b,PersistenceRequired:1b}')
    record("a summoned queen spawns (boss music)", "Summoned" in out, out.strip())
    out = rcon.run('summon dreadpatch:syndrigossa 24.5 221 4.5 {UUID:[I;72,72,72,2],NoAI:1b,PersistenceRequired:1b}')
    record("a summoned Syndrigossa spawns (boss music)", "Summoned" in out, out.strip())
    marked = ["Test passed" in rcon.run(f'execute as {who} if data entity @s NeoForgeData."dreadpatch:boss_music_played"')
              for who in ("00000048-0000-0048-0000-004800000001", "00000048-0000-0048-0000-004800000002")]
    record("a fresh queen and Syndrigossa are marked for her theme", all(marked), str(marked))
    rcon.run("kill @e[type=item]")
    for _ in range(40):
        rcon.run("execute as 00000048-0000-0048-0000-004800000002 run loot spawn 20.5 221 12.5 loot dreadpatch:entities/syndrigossa/heart")
    discs = count("dreadpatch:music_disc_beneath_the_permafrost")
    record("Syndrigossa's heart holds her theme's disc now and then", 1 <= discs <= 25, f"{discs} in 40 hearts")
    rcon.run("kill @e[type=item]")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[type=dreadpatch:syndrigossa]")
    rcon.run("kill @e[type=item]")

    # Dread knights and webs (0.75.0), at x 100-125 (forceloaded):
    rcon.run("forceload add 96 -8 127 15")
    rcon.run("fill 96 219 -6 127 226 12 minecraft:air")
    rcon.run("fill 96 218 -6 127 218 12 minecraft:stone")
    time.sleep(2.0)  # the forceloaded chunks ticking, or a summon there isn't found at once
    # - a knight is half again as large, an eighth faster, and steps a full block;
    rcon.run('summon iceandfire:dread_knight 100.5 219 10.5 {UUID:[I;75,75,75,1],NoAI:1b,PersistenceRequired:1b,Silent:1b}')
    knight = "0000004b-0000-004b-0000-004b00000001"
    check("a dread knight is half again as large", f"attribute {knight} minecraft:generic.scale get", "is 1.5")
    check("a dread knight is an eighth faster", f"attribute {knight} minecraft:generic.movement_speed get", "is 0.28125")
    check("a dread knight steps a full block", f"attribute {knight} minecraft:generic.step_height get", "is 1.0")
    # - standing in the open it's 2.7 tall (its box reaches 221.5 from 219); under a ceiling 2 blocks up it
    #   crouches to 1.8 (its box stays under 221);
    standing = "Test passed" in rcon.run(f"execute positioned 100.0 221.5 10.0 if entity @e[type=iceandfire:dread_knight,dx=0,dy=0,dz=0]")
    rcon.run("fill 102 221 9 104 221 11 minecraft:stone")
    rcon.run('summon iceandfire:dread_knight 103.5 219 10.5 {UUID:[I;75,75,75,2],NoAI:1b,PersistenceRequired:1b,Silent:1b}')
    time.sleep(1.0)
    crouched = "Test passed" not in rcon.run(f"execute positioned 103.0 220.9 10.0 if entity @e[type=iceandfire:dread_knight,dx=0,dy=0,dz=0]") \
        and "Test passed" in rcon.run(f"execute positioned 103.0 220.5 10.0 if entity @e[type=iceandfire:dread_knight,dx=0,dy=0,dz=0]")
    record("a dread knight stands 2.7 tall in the open", standing, "its box doesn't reach 221.5")
    record("a dread knight crouches under a 2-block ceiling", crouched, "its box reaches past 220.9, or isn't there")
    rcon.run("kill 0000004b-0000-004b-0000-004b00000001")
    rcon.run("kill 0000004b-0000-004b-0000-004b00000002")
    # - with its AI, from an open room it walks crouched down a 2-high, 1-wide tunnel 11 long to a zombie
    #   (NoAI, resistance) at its end, within 30 s (by hand, 10 s).
    rcon.run("fill 100 219 -6 125 224 4 minecraft:stone")
    rcon.run("fill 101 219 -5 106 222 3 minecraft:air")
    rcon.run("fill 107 219 -1 117 220 -1 minecraft:air")
    time.sleep(0.5)
    rcon.run('summon minecraft:zombie 117.5 219 -0.5 {UUID:[I;75,75,75,9],NoAI:1b,PersistenceRequired:1b,Silent:1b,active_effects:[{id:"minecraft:resistance",amplifier:4b,duration:99999}]}')
    rcon.run('summon iceandfire:dread_knight 102.5 219 -0.5 {UUID:[I;75,75,75,3],PersistenceRequired:1b,Silent:1b}')
    # Standing in the room its eyes (221.3) are over the tunnel's roof, so it can't see the zombie down it; the
    # zombie's "hit" makes it the knight's target (revenge needs no sight). Unprovoked, a knight wandered in
    # by hand, but not every run.
    time.sleep(0.5)
    rcon.run("damage 0000004b-0000-004b-0000-004b00000003 1 minecraft:mob_attack by 0000004b-0000-004b-0000-004b00000009")
    walked = None
    for _ in range(60):
        time.sleep(0.5)
        pos = re.search(r"\[([-0-9.]+)d,", rcon.run("data get entity 0000004b-0000-004b-0000-004b00000003 Pos"))
        if pos and float(pos.group(1)) >= 113.0:
            walked = float(pos.group(1))
            break
    record("a dread knight walks crouched down a 2-high tunnel", walked is not None,
           "x " + (pos.group(1) if pos else "?") + " after 30 s (tunnel 107-117)")
    rcon.run("kill 0000004b-0000-004b-0000-004b00000003")
    rcon.run("kill 0000004b-0000-004b-0000-004b00000009")
    rcon.run("fill 100 219 -6 125 224 4 minecraft:air")
    # - (0.75.1) a scuttler notices what's 24 off and is 10% faster (Ice and Fire's 12 and 0.34);
    rcon.run('summon iceandfire:dread_scuttler 120.5 219 10.5 {UUID:[I;75,75,75,5],NoAI:1b,PersistenceRequired:1b,Silent:1b}')
    check("a dread scuttler notices what's 24 blocks off", "attribute 0000004b-0000-004b-0000-004b00000005 minecraft:generic.follow_range get", "is 24.0")
    check("a dread scuttler is 10% faster", "attribute 0000004b-0000-004b-0000-004b00000005 minecraft:generic.movement_speed get", "is 0.374")
    rcon.run("kill 0000004b-0000-004b-0000-004b00000005")
    # - a scuttler's web melts after 3 s; its shot, falling onto the floor, leaves one (mobGriefing on).
    rcon.run("setblock 110 219 8 dreadpatch:dread_web")
    placed = "Test passed" in rcon.run("execute if block 110 219 8 dreadpatch:dread_web")
    time.sleep(4.0)
    melted = "Test passed" in rcon.run("execute if block 110 219 8 minecraft:air")
    record("a dread web melts away after 3 s", placed and melted, f"placed {placed}, melted {melted}")
    rcon.run('summon dreadpatch:scuttler_web 114.5 222 8.5 {Motion:[0.0d,-0.6d,0.0d]}')
    time.sleep(1.0)
    shot = "Test passed" in rcon.run("execute if block 114 219 8 dreadpatch:dread_web")
    record("a scuttler's web shot leaves a web where it lands", shot, rcon.run("execute if block 114 219 8 dreadpatch:dread_web").strip())
    time.sleep(3.5)
    rcon.run("fill 96 218 -6 127 226 12 minecraft:air")
    rcon.run("forceload remove 96 -8 127 15")

    # The Dread's hits (0.73.0): a melee hit (`/damage ... mob_attack by` it) from a scuttler, a knight and a
    # thrall on a sturdy NoAI zombie, 80 each, 0.55 s apart (past its half second of invulnerability after a
    # hit), its effects cleared between: a scuttler's leave Withering Rime II now and then (8%) and 2 s of
    # Rimefrost often (35%); a knight's Withering Rime I now and then (8%) and 3 s of Rimefrost sometimes (13%,
    # 0.73.1); a thrall's each at 5% (0.73.1). (None in 80 at 8% is 0.1%, at 5% 1.7%: both a thrall's are
    # counted together, none of either in 80 being 0.03%.) The one-source cap is on players, so it needs one.
    rcon.run('summon minecraft:zombie 24.5 221 12.5 {UUID:[I;73,73,73,9],NoAI:1b,PersistenceRequired:1b,Silent:1b,'
             'attributes:[{id:"minecraft:generic.max_health",base:1000d}],Health:1000f}')
    dread_kinds = {"scuttler": "iceandfire:dread_scuttler", "knight": "iceandfire:dread_knight", "thrall": "iceandfire:dread_thrall"}
    tallies = {}
    for n, (kind, entity_type) in enumerate(dread_kinds.items()):
        uuid = f"00000049-0000-0049-0000-00490000000{n + 1}"
        rcon.run(f'summon {entity_type} 28.5 221 12.5 {{UUID:[I;73,73,73,{n + 1}],NoAI:1b,PersistenceRequired:1b,Silent:1b}}')
        tally = {"wr1": 0, "wr2": 0, "rf": 0, "rf_long": 0}
        for _ in range(80):
            time.sleep(0.55)
            rcon.run("effect clear 00000049-0000-0049-0000-004900000009")
            rcon.run("data merge entity 00000049-0000-0049-0000-004900000009 {Health:1000f}")
            rcon.run(f"damage 00000049-0000-0049-0000-004900000009 2 minecraft:mob_attack by {uuid}")
            effects = rcon.run("data get entity 00000049-0000-0049-0000-004900000009 active_effects")
            wr = re.search(r'\{[^{}]*id: "dreadpatch:withering_rime"[^{}]*\}', effects)
            if wr:
                tally["wr2" if "amplifier: 1b" in wr.group(0) else "wr1"] += 1
            rf = re.search(r'\{[^{}]*id: "dreadpatch:rimefrost"[^{}]*\}', effects)
            if rf:
                tally["rf"] += 1
                tally["rf_long"] += int(re.search(r"duration: (\d+)", rf.group(0)).group(1)) > 40
        tallies[kind] = tally
        rcon.run(f"kill {uuid}")
    s, k, t = tallies["scuttler"], tallies["knight"], tallies["thrall"]
    record("a scuttler's hits leave Withering Rime II now and then and a brief Rimefrost often",
           1 <= s["wr2"] <= 20 and s["wr1"] == 0 and 12 <= s["rf"] <= 48 and s["rf_long"] == 0, str(s))
    record("a knight's hits leave Withering Rime now and then, and a 3 s Rimefrost sometimes",
           1 <= k["wr1"] <= 20 and k["wr2"] == 0 and 2 <= k["rf"] <= 26 and k["rf_long"] == k["rf"], str(k))
    record("a thrall's hits leave Withering Rime and Rimefrost rarely", 1 <= t["wr1"] + t["rf"] <= 24 and t["wr1"] <= 14 and t["rf"] <= 14, str(t))
    rcon.run("kill 00000049-0000-0049-0000-004900000009")
    rcon.run("kill @e[type=item]")
    rcon.run("setblock 20 220 12 minecraft:jukebox")
    rcon.run("item replace block 20 220 12 container.0 with dreadpatch:music_disc_beneath_the_permafrost")
    time.sleep(0.5)
    playing = "Test passed" in rcon.run("execute if block 20 220 12 minecraft:jukebox[has_record=true]") \
        and "ticks_since_song_started" in rcon.run("data get block 20 220 12")
    record("her theme's disc plays in a jukebox", playing, rcon.run("data get block 20 220 12").strip())
    rcon.run("setblock 20 220 12 minecraft:air")
    rcon.run("kill @e[type=item]")

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
