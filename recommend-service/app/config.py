from dataclasses import dataclass
import os


@dataclass(frozen=True)
class Settings:
    db_host: str = os.getenv("DB_HOST", "")
    db_port: int = int(os.getenv("DB_PORT", "3306"))
    db_user: str = os.getenv("DB_USER", "")
    db_password: str = os.getenv("DB_PASSWORD", "")
    db_name: str = os.getenv("DB_NAME", "south_stand")
    top_k: int = max(1, int(os.getenv("ITEM_CF_TOP_K", "50")))
    training_window_days: int = max(1, int(os.getenv("ITEM_CF_TRAINING_WINDOW_DAYS", "30")))
    decay_days: float = max(1.0, float(os.getenv("ITEM_CF_DECAY_DAYS", "14")))
    max_user_item_weight: float = max(1.0, float(os.getenv("ITEM_CF_MAX_USER_ITEM_WEIGHT", "8.0")))


settings = Settings()

