from contextlib import asynccontextmanager

from fastapi import FastAPI, Query

from .config import settings
from .model import ModelManager
from .schemas import ContentScoreItem, ContentScoreRequest, ContentScoreResponse, ModelStatus


manager = ModelManager(settings)


@asynccontextmanager
async def lifespan(_: FastAPI):
    manager.rebuild()
    yield


app = FastAPI(title="tifo-content-recommendation", version="CF_V1", lifespan=lifespan)


def status_payload() -> ModelStatus:
    state = manager.state()
    snapshot = state.snapshot
    return ModelStatus(
        modelVersion=state.model_version,
        modelReady=state.ready,
        builtAt=state.built_at.isoformat(timespec="milliseconds") if state.built_at else None,
        buildDurationMs=state.build_duration_ms,
        userCount=snapshot.user_count if snapshot else 0,
        itemCount=snapshot.item_count if snapshot else 0,
        behaviorCount=snapshot.behavior_count if snapshot else 0,
        topK=settings.top_k,
        trainingWindowDays=settings.training_window_days,
    )


@app.get("/health")
def health() -> dict:
    status = status_payload().model_dump()
    status["status"] = "UP"
    return status


@app.get("/api/internal/recommend/stats", response_model=ModelStatus)
def stats() -> ModelStatus:
    return status_payload()


@app.post("/api/internal/recommend/content-scores", response_model=ContentScoreResponse)
def content_scores(request: ContentScoreRequest) -> ContentScoreResponse:
    state = manager.state()
    if not state.ready:
        return ContentScoreResponse(success=False, modelReady=False, errorCode="MODEL_NOT_READY")
    candidate_ids = list(dict.fromkeys(content_id for content_id in request.candidateIds if content_id > 0))
    if not candidate_ids:
        return ContentScoreResponse(success=False, modelReady=True,
                                    modelVersion=state.model_version, errorCode="NO_VALID_CANDIDATES")
    items = [ContentScoreItem(contentId=content_id, cfScore=score)
             for content_id, score in manager.scores(request.userId, candidate_ids)]
    return ContentScoreResponse(success=True, modelReady=True,
                                modelVersion=state.model_version, items=items)


@app.post("/api/internal/recommend/reload", response_model=ModelStatus)
def reload_model(days: int = Query(default=30, ge=1, le=90)) -> ModelStatus:
    manager.rebuild(days)
    return status_payload()

