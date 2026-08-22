from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime
import math

from .loader import BehaviorRecord


BEHAVIOR_WEIGHTS = {
    "CLICK": 1.0,
    "DETAIL": 1.5,
    "LIKE": 2.0,
    "FAVORITE": 2.5,
    "COMMENT": 3.0,
}
STRONG_BEHAVIORS = {"LIKE", "FAVORITE", "COMMENT"}


@dataclass(frozen=True)
class CfSnapshot:
    user_items: dict[int, dict[int, float]]
    neighbors: dict[int, tuple[tuple[int, float], ...]]
    strong_items: dict[int, frozenset[int]]
    behavior_count: int

    @property
    def user_count(self) -> int:
        return len(self.user_items)

    @property
    def item_count(self) -> int:
        return len({item for values in self.user_items.values() for item in values})


def behavior_weight(record: BehaviorRecord, now: datetime, decay_days: float) -> float:
    base = BEHAVIOR_WEIGHTS.get(record.behavior_type, 0.0)
    if base <= 0:
        return 0.0
    if record.behavior_type == "DETAIL" and record.dwell_ms:
        base += min(1.0, max(0, record.dwell_ms) / 60_000.0)
    age_days = max(0.0, (now - record.event_time).total_seconds() / 86_400.0)
    return base * math.exp(-age_days / decay_days)


def build_snapshot(records: list[BehaviorRecord], top_k: int, decay_days: float,
                   max_user_item_weight: float, now: datetime | None = None) -> CfSnapshot:
    current = now or datetime.now()
    user_items: dict[int, dict[int, float]] = defaultdict(lambda: defaultdict(float))
    strong_items: dict[int, set[int]] = defaultdict(set)
    used = 0
    for record in records:
        weight = behavior_weight(record, current, decay_days)
        if weight <= 0:
            continue
        existing = user_items[record.user_id][record.content_id]
        user_items[record.user_id][record.content_id] = min(max_user_item_weight, existing + weight)
        if record.behavior_type in STRONG_BEHAVIORS:
            strong_items[record.user_id].add(record.content_id)
        used += 1

    item_users: dict[int, dict[int, float]] = defaultdict(dict)
    for user_id, items in user_items.items():
        for item_id, weight in items.items():
            item_users[item_id][user_id] = weight / max_user_item_weight

    norms = {item: math.sqrt(sum(weight * weight for weight in users.values()))
             for item, users in item_users.items()}
    candidates: dict[int, list[tuple[int, float]]] = defaultdict(list)
    item_ids = sorted(item_users)
    for index, left in enumerate(item_ids):
        for right in item_ids[index + 1:]:
            common = item_users[left].keys() & item_users[right].keys()
            if not common:
                continue
            dot = sum(item_users[left][user] * item_users[right][user] for user in common)
            denominator = norms[left] * norms[right]
            similarity = dot / denominator if denominator > 0 else 0.0
            if similarity > 0:
                candidates[left].append((right, similarity))
                candidates[right].append((left, similarity))

    neighbors = {
        item: tuple(sorted(values, key=lambda pair: (-pair[1], pair[0]))[:top_k])
        for item, values in candidates.items()
    }
    return CfSnapshot(
        user_items={user: dict(items) for user, items in user_items.items()},
        neighbors=neighbors,
        strong_items={user: frozenset(items) for user, items in strong_items.items()},
        behavior_count=used,
    )


def score_candidates(snapshot: CfSnapshot, user_id: int, candidate_ids: list[int],
                     max_user_item_weight: float) -> list[tuple[int, float]]:
    history = snapshot.user_items.get(user_id, {})
    strong = snapshot.strong_items.get(user_id, frozenset())
    scores: list[tuple[int, float]] = []
    for candidate_id in dict.fromkeys(candidate_ids):
        numerator = 0.0
        denominator = 0.0
        for neighbor_id, similarity in snapshot.neighbors.get(candidate_id, ()):
            history_weight = history.get(neighbor_id)
            if history_weight is None:
                continue
            numerator += similarity * min(1.0, history_weight / max_user_item_weight)
            denominator += similarity
        normalized = numerator / denominator if denominator > 0 else 0.0
        if candidate_id in strong:
            normalized *= 0.15
        normalized = min(1.0, max(0.0, normalized))
        scores.append((candidate_id, round(normalized, 6)))
    return sorted(scores, key=lambda pair: (-pair[1], pair[0]))

