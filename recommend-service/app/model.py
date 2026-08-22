from dataclasses import dataclass
from datetime import datetime
import hashlib
import threading
import time

from .cf import CfSnapshot, build_snapshot, score_candidates
from .config import Settings
from .loader import BehaviorLoader


@dataclass(frozen=True)
class ModelState:
    snapshot: CfSnapshot | None = None
    model_version: str | None = None
    built_at: datetime | None = None
    build_duration_ms: int = 0
    last_error: str | None = None

    @property
    def ready(self) -> bool:
        return self.snapshot is not None and self.model_version is not None


class ModelManager:
    def __init__(self, settings: Settings, loader: BehaviorLoader | None = None):
        self.settings = settings
        self.loader = loader or BehaviorLoader(settings)
        self._lock = threading.Lock()
        self._state = ModelState()

    def state(self) -> ModelState:
        with self._lock:
            return self._state

    def rebuild(self, days: int | None = None) -> ModelState:
        started = time.perf_counter()
        try:
            records = self.loader.load(days)
            snapshot = build_snapshot(
                records,
                top_k=self.settings.top_k,
                decay_days=self.settings.decay_days,
                max_user_item_weight=self.settings.max_user_item_weight,
            )
            built_at = datetime.now()
            digest = hashlib.sha1(
                f"{built_at.isoformat()}:{snapshot.user_count}:{snapshot.item_count}:{snapshot.behavior_count}".encode()
            ).hexdigest()[:10]
            new_state = ModelState(
                snapshot=snapshot,
                model_version=f"CF_V1_{digest}",
                built_at=built_at,
                build_duration_ms=int((time.perf_counter() - started) * 1000),
            )
            with self._lock:
                self._state = new_state
            return new_state
        except Exception as exc:  # model failure must never terminate FastAPI or clear the old model
            with self._lock:
                if self._state.ready:
                    return self._state
                self._state = ModelState(
                    build_duration_ms=int((time.perf_counter() - started) * 1000),
                    last_error=type(exc).__name__,
                )
                return self._state

    def scores(self, user_id: int, candidate_ids: list[int]) -> list[tuple[int, float]]:
        state = self.state()
        if not state.ready or state.snapshot is None:
            return []
        return score_candidates(state.snapshot, user_id, candidate_ids,
                                self.settings.max_user_item_weight)

