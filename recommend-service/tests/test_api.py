from datetime import datetime

from fastapi.testclient import TestClient

from app.config import Settings
from app.loader import BehaviorRecord
from app.model import ModelManager
import app.main as main


class Loader:
    def __init__(self, records=None, fail=False):
        self.records = records or []
        self.fail = fail
    def load(self, _days=None):
        if self.fail:
            raise RuntimeError("database unavailable")
        return self.records


def test_health_stays_up_when_database_is_unavailable(monkeypatch):
    manager = ModelManager(Settings(db_host="x"), Loader(fail=True))
    monkeypatch.setattr(main, "manager", manager)
    with TestClient(main.app) as client:
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "UP"
        assert response.json()["modelReady"] is False
        score_response = client.post("/api/internal/recommend/content-scores",
                                     json={"userId": 1, "candidateIds": [1]})
        assert score_response.json()["success"] is False


def test_scores_are_bounded_and_reload_failure_preserves_old_model(monkeypatch):
    now = datetime.now()
    records = [
        BehaviorRecord(1, "CLICK", 10, None, now),
        BehaviorRecord(1, "CLICK", 11, None, now),
        BehaviorRecord(2, "CLICK", 10, None, now),
        BehaviorRecord(2, "CLICK", 12, None, now),
    ]
    loader = Loader(records)
    manager = ModelManager(Settings(db_host="x"), loader)
    manager.rebuild()
    monkeypatch.setattr(main, "manager", manager)
    with TestClient(main.app) as client:
        version = manager.state().model_version
        response = client.post("/api/internal/recommend/content-scores",
                               json={"userId": 1, "candidateIds": [10, 12]})
        body = response.json()
        assert body["success"] is True
        assert body["modelReady"] is True
        assert all(0 <= item["cfScore"] <= 1 for item in body["items"])
        loader.fail = True
        reload_response = client.post("/api/internal/recommend/reload?days=30")
        assert reload_response.json()["modelReady"] is True
        assert reload_response.json()["modelVersion"] == version


def test_invalid_empty_candidates_get_validation_error():
    with TestClient(main.app) as client:
        response = client.post("/api/internal/recommend/content-scores",
                               json={"userId": 1, "candidateIds": []})
        assert response.status_code == 422
