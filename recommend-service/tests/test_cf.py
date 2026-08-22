from datetime import datetime, timedelta

from app.cf import behavior_weight, build_snapshot, score_candidates
from app.loader import BehaviorRecord


NOW = datetime(2026, 8, 22, 12, 0, 0)


def record(user: int, kind: str, content: int, days: int = 0, dwell: int | None = None):
    return BehaviorRecord(user, kind, content, dwell, NOW - timedelta(days=days))


def test_behavior_weights_detail_dwell_and_time_decay():
    click = behavior_weight(record(1, "CLICK", 1), NOW, 10)
    detail = behavior_weight(record(1, "DETAIL", 1, dwell=60_000), NOW, 10)
    old_click = behavior_weight(record(1, "CLICK", 1, days=10), NOW, 10)
    assert click == 1.0
    assert detail == 2.5
    assert 0 < old_click < click
    assert behavior_weight(record(1, "EXPOSE", 1), NOW, 10) == 0


def test_top_k_normalization_cap_and_strong_interaction_penalty():
    records = [
        record(1, "CLICK", 10), record(1, "CLICK", 11), record(1, "LIKE", 12),
        record(2, "CLICK", 10), record(2, "CLICK", 11), record(2, "CLICK", 13),
        record(3, "CLICK", 10), record(3, "CLICK", 12), record(3, "CLICK", 13),
    ] + [record(1, "COMMENT", 10) for _ in range(20)]
    snapshot = build_snapshot(records, top_k=1, decay_days=30, max_user_item_weight=8, now=NOW)
    assert all(len(neighbors) <= 1 for neighbors in snapshot.neighbors.values())
    assert snapshot.user_items[1][10] == 8
    scores = dict(score_candidates(snapshot, 1, [10, 11, 12, 13, 999], 8))
    assert all(0 <= value <= 1 for value in scores.values())
    assert scores[10] < scores[11]  # strong interaction content is heavily reduced
    assert scores[999] == 0


def test_cold_start_is_stable_zero_and_only_requested_candidates_returned():
    snapshot = build_snapshot([record(1, "CLICK", 1)], 50, 30, 8, NOW)
    assert score_candidates(snapshot, 999, [3, 2, 2], 8) == [(2, 0.0), (3, 0.0)]

