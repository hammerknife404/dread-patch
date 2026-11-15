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
    HER_SPELLS = {"dreadpatch:withering_rime_bolt"}

    def health(selector):
        out = rcon.run("data get entity " + selector + " Health")
        m = re.search(r"entity data: ([0-9.]+)f", out)
        return float(m.group(1)) if m else -1.0

    def has_effect(selector, effect):
        # The NBT test goes inside the selector's own brackets: "@e[tag=x,limit=1]" -> "@e[tag=x,limit=1,nbt=...]".
        with_nbt = selector[:-1] + f',nbt={{active_effects:[{{id:"{effect}"}}]}}]'
        out = rcon.run("execute if entity " + with_nbt)
        if "Test passed" not in out and "Test failed" not in out:
            raise SystemExit("has_effect: bad command: " + out)
        return "Test passed" in out

    def check_loot(where):
        # Her loot table: her sword and staff (exactly one each: the ones she holds never drop too),
        # two inks, epic or legendary, and one or two scrolls with a spell on them.
        for item in ("iceandfire:dread_queen_sword", "iceandfire:dread_queen_staff"):
            n = count(item)
            record(where + ": one " + item.split(":")[1], n == 1, f"found {n}")
        epic, legendary = count("irons_spellbooks:epic_ink"), count("irons_spellbooks:legendary_ink")
        record(where + ": two epic/legendary inks", epic + legendary == 2, f"epic {epic}, legendary {legendary}")
        scrolls = count("irons_spellbooks:scroll")
        with_spell = count(SPELL_SCROLL)
        record(where + ": one or two scrolls, each with a spell", 1 <= scrolls <= 2 and with_spell == scrolls,
               f"{scrolls} scrolls, {with_spell} with a spell")
        ids = re.findall(r'entity data: "([a-z_:]+)"', rcon.run(
            'execute as @e[type=item] if items entity @s contents irons_spellbooks:scroll run data get entity @s '
            'Item.components."irons_spellbooks:spell_container".data[0].id'))
        record(where + ": the scrolls hold her spells", bool(ids) and all(i in HER_SPELLS for i in ids), f"spells {ids}")
        rcon.run("kill @e[type=item]")

    queen = "@e[type=dreadpatch:dread_queen,limit=1]"
    surface = "execute positioned 0 0 0 positioned over world_surface run "

    # Iron's Spells builds its spell config (max levels and so on) when datapacks sync to players; on a
    # server nobody has joined every spell reads max level 1. A reload syncs it.
    rcon.run("reload")
    time.sleep(3)
    rcon.run("forceload add 0 0")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[type=item]")

    check("summons", surface + "summon dreadpatch:dread_queen ~ ~ ~", "Summoned new Dread Queen")
    check("750 health", "data get entity " + queen + " Health", "750.0f")
    check("armor 30", "attribute " + queen + " minecraft:generic.armor get", "is 30.0")
    check("holds her sword", "data get entity " + queen + " HandItems", "iceandfire:dread_queen_sword")
    check("holds her staff", "data get entity " + queen + " HandItems", "iceandfire:dread_queen_staff")
    for tag in ("#minecraft:undead", "#minecraft:freeze_immune_entity_types", "#c:bosses"):
        check("in " + tag, "execute as " + queen + " if entity @s[type=" + tag + "]", "Test passed")
    time.sleep(5)  # 100 ticks of her AI
    check("alive after 5 s of AI", "execute if entity @e[type=dreadpatch:dread_queen]", "Test passed, count: 1")

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

    check("dies", "kill @e[type=dreadpatch:dread_queen]", "Killed Dread Queen")
    time.sleep(1.5)
    check_loot("death drops")

    # A queen whose saved drop chances say her held items always drop (2.0) still drops one of each:
    # those come from her loot table, never from her hands.
    held = ('{HandItems:[{id:"iceandfire:dread_queen_sword",count:1},{id:"iceandfire:dread_queen_staff",count:1}],'
            'HandDropChances:[2.0f,2.0f]}')
    check("summons with drop chances 2.0", surface + "summon dreadpatch:dread_queen ~ ~ ~ " + held, "Summoned new Dread Queen")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    time.sleep(1.5)
    check_loot("forced hand drops")

    # A dispenser only fires a spawn egg that knows its entity type.
    rcon.run("setblock 4 200 4 minecraft:dispenser[facing=up]")
    check("egg goes in a dispenser", "item replace block 4 200 4 container.0 with dreadpatch:dread_queen_spawn_egg", "Replaced")
    rcon.run("setblock 4 199 4 minecraft:redstone_block")
    time.sleep(1.5)
    check("dispensed egg spawns her", "execute if entity @e[type=dreadpatch:dread_queen]", "Test passed, count: 1")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[type=item]")
    rcon.run("setblock 4 199 4 minecraft:air")
    rcon.run("setblock 4 200 4 minecraft:air")

    # Her spawn animation: 30 ticks rising out of the ground, unable to move, with a pig to tempt her.
    spot = "execute positioned 0 0 0 positioned over world_surface positioned ~0.5 ~ ~0.5 "
    check("summons for the rise", spot + "run summon dreadpatch:dread_queen ~ ~ ~", "Summoned new Dread Queen")
    rcon.run('execute positioned 2 0 0 positioned over world_surface run summon minecraft:pig ~0.5 ~ ~0.5 {Tags:["queen_smoke"]}')
    time.sleep(1.25)  # 25 ticks in: as late as is safe, to give her the most time to move
    check("holds still while rising", spot + "if entity @e[type=dreadpatch:dread_queen,distance=..0.05]", "Test passed")
    time.sleep(4.0)
    check("moves once risen", spot + "unless entity @e[type=dreadpatch:dread_queen,distance=..0.05]", "Test passed")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
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

    # The queen bolts a golem from 12 blocks (her bolt: 8 + 15%; only her bolt leaves Withering Rime).
    rcon.run(golem.format(x=14.5))
    rcon.run("summon dreadpatch:dread_queen 2.5 200 4.5")
    bolted = False
    for _ in range(30):
        time.sleep(0.5)
        if has_effect(target, "dreadpatch:withering_rime"):
            bolted = True
            break
    record("she bolts a target at range", bolted, "no Withering Rime on the golem within 15 s")
    rcon.run("kill @e[type=dreadpatch:dread_queen]")
    rcon.run("kill @e[tag=smoke_target]")
    rcon.run("kill @e[type=item]")
    rcon.run("fill 1 199 2 15 199 6 minecraft:air")
    rcon.run("forceload remove 0 0")

    print(f"{sum(results)}/{len(results)} passed")
    if "--keep" not in sys.argv:
        rcon.run("stop")
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
