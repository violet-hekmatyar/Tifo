from dataclasses import dataclass
from datetime import datetime, timedelta

from .config import Settings


@dataclass(frozen=True)
class BehaviorRecord:
    user_id: int
    behavior_type: str
    content_id: int
    dwell_ms: int | None
    event_time: datetime


class BehaviorLoader:
    """Read-only loader. It deliberately knows only user_behavior_log."""

    def __init__(self, settings: Settings):
        self.settings = settings

    def load(self, days: int | None = None) -> list[BehaviorRecord]:
        if not self.settings.db_host:
            raise RuntimeError("DB_HOST is not configured")
        import pymysql

        window = days or self.settings.training_window_days
        connection = pymysql.connect(
            host=self.settings.db_host,
            port=self.settings.db_port,
            user=self.settings.db_user,
            password=self.settings.db_password,
            database=self.settings.db_name,
            charset="utf8mb4",
            connect_timeout=3,
            read_timeout=10,
            autocommit=True,
        )
        try:
            with connection.cursor() as cursor:
                cursor.execute(
                    "SELECT user_id, behavior_type, target_id, dwell_ms, event_time "
                    "FROM user_behavior_log "
                    "WHERE status='ACTIVE' AND is_deleted=0 AND user_id IS NOT NULL "
                    "AND target_type='CONTENT' "
                    "AND behavior_type IN ('CLICK','DETAIL','LIKE','FAVORITE','COMMENT') "
                    "AND event_time >= %s",
                    (datetime.now() - timedelta(days=window),),
                )
                return [
                    BehaviorRecord(
                        user_id=int(row[0]),
                        behavior_type=str(row[1]),
                        content_id=int(row[2]),
                        dwell_ms=None if row[3] is None else int(row[3]),
                        event_time=row[4],
                    )
                    for row in cursor.fetchall()
                ]
        finally:
            connection.close()

