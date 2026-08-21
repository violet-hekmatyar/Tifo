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
    t16_migration = ROOT / "scripts" / "sql" / "migrations" / "V016__team_roster_player_career.sql"
    t16_seed = ROOT / "scripts" / "sql" / "seed-t16-incremental.sql"
    t16_validation = ROOT / "scripts" / "sql" / "validate-t16-incremental.sql"
    t17_roster = ROOT / "scripts" / "sql" / "seed-t17-roster-expansion.sql"
    t17_seed = ROOT / "scripts" / "sql" / "seed-t17-incremental.sql"
    t17_validation = ROOT / "scripts" / "sql" / "validate-t17-incremental.sql"
    required = [SEED, ROOT / "scripts" / "sql" / "validate-demo-data.sql", t16_migration, t16_seed, t16_validation,
                t17_roster, t17_seed, t17_validation]
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
        "seed_has_no_placeholder": "REPLACE_WITH" not in SEED.read_text(encoding="utf-8"),
        "t16_incremental_is_non_destructive": all(token not in t16_seed.read_text(encoding="utf-8").upper()
            for token in ["DROP DATABASE", "TRUNCATE TABLE", "DELETE FROM"]),
        "t16_models_declared": all(name in t16_migration.read_text(encoding="utf-8") for name in [
            "football_team_season_player", "football_team_honor", "football_player_team_history"]),
        "t16_consistency_validation_present": "roster_stat_mismatch" in t16_validation.read_text(encoding="utf-8"),
        "t17_roster_is_incremental": all(token not in t17_roster.read_text(encoding="utf-8").upper()
            for token in ["DROP DATABASE", "TRUNCATE TABLE", "DELETE FROM"]),
        "t17_roster_has_five_layer_chain": all(name in t17_roster.read_text(encoding="utf-8") for name in [
            "football_player", "team_player", "football_team_season_player", "football_player_team_history", "football_player_competition_stat"]),
        "t17_complete_match_capacity_guard": "complete_matches_below_12" in t17_validation.read_text(encoding="utf-8"),
        "t17_rating_capacity_guard": "active_ratings_below_300" in t17_validation.read_text(encoding="utf-8")
    }
    for name, passed in checks.items():
        print(f"{name}: {'PASS' if passed else 'FAIL'}")
    if not all(checks.values()):
        raise SystemExit(1)
    print(f"seed-demo.sql sha256: {after}")


if __name__ == "__main__":
    main()
