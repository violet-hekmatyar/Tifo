from pydantic import BaseModel, Field


class ContentScoreRequest(BaseModel):
    userId: int
    candidateIds: list[int] = Field(min_length=1, max_length=500)


class ContentScoreItem(BaseModel):
    contentId: int
    cfScore: float


class ContentScoreResponse(BaseModel):
    success: bool
    modelReady: bool
    algorithmVersion: str = "CF_V1"
    modelVersion: str | None = None
    items: list[ContentScoreItem] = Field(default_factory=list)
    errorCode: str | None = None


class ModelStatus(BaseModel):
    algorithmVersion: str = "CF_V1"
    modelVersion: str | None = None
    modelReady: bool
    builtAt: str | None = None
    buildDurationMs: int = 0
    userCount: int = 0
    itemCount: int = 0
    behaviorCount: int = 0
    topK: int
    trainingWindowDays: int

