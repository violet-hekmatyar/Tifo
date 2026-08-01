#!/usr/bin/env python3
"""Offline structural checks for generated T14 files (Python standard library only)."""

import hashlib
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "scripts" / "data"
SEED = ROOT / "scripts" / "sql" / "seed-demo.sql"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    config = json.loads((DATA / "demo-config.json").read_text(encoding="utf-8"))
    names = json.loads((DATA / "demo-names.json").read_text(encoding="utf-8"))
    required = [SEED, ROOT / "scripts" / "sql" / "validate-demo-data.sql"]
    missing = [str(path) for path in required if not path.exists()]
    if missing:
        raise SystemExit("Missing generated files: " + ", ".join(missing))
    subprocess.run([sys.executable, str(DATA / "generate-demo-data.py")], cwd=ROOT, check=True, stdout=subprocess.DEVNULL)
    before = digest(SEED)
    subprocess.run([sys.executable, str(DATA / "generate-demo-data.py")], cwd=ROOT, check=True, stdout=subprocess.DEVNULL)
    after = digest(SEED)
    checks = {
        "seed_is_deterministic": before == after,
        "fixed_seed": config["seed"] == 20260722,
        "chinese_names_present": bool(names["teams"]) and any("阿" <= ch <= "龥" for ch in names["teams"][0][0]),
        "content_capacity": config["counts"]["contents"] >= 60,
        "match_capacity": config["counts"]["matches"] >= 60,
        "player_capacity": config["counts"]["teams"] * config["counts"]["players_per_team"] >= 100,
        "rank_league_capacity": config["counts"]["rank_leagues"] >= 5,
        "standing_team_capacity": config["counts"]["standing_teams_per_league"] >= 8,
        "t15_tables_generated": all(name in SEED.read_text(encoding="utf-8") for name in [
            "football_season", "football_competition_stage", "football_standing",
            "football_player_competition_stat", "football_team_competition_stat"]),
        "demo_source_declared": "'DEMO'" in SEED.read_text(encoding="utf-8"),
        "seed_has_no_placeholder": "REPLACE_WITH" not in SEED.read_text(encoding="utf-8")
    }
    for name, passed in checks.items():
        print(f"{name}: {'PASS' if passed else 'FAIL'}")
    if not all(checks.values()):
        raise SystemExit(1)
    print(f"seed-demo.sql sha256: {after}")


if __name__ == "__main__":
    main()
