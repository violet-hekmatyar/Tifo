from datetime import datetime
import sys
import types

from app.config import Settings
from app.loader import BehaviorLoader


class FakeCursor:
    def __init__(self):
        self.sql = ""
        self.params = None
    def __enter__(self): return self
    def __exit__(self, *_): return False
    def execute(self, sql, params):
        self.sql, self.params = sql, params
    def fetchall(self):
        return [(1, "CLICK", 100, None, datetime(2026, 8, 22))]


class FakeConnection:
    def __init__(self): self.cursor_instance = FakeCursor(); self.closed = False
    def cursor(self): return self.cursor_instance
    def close(self): self.closed = True


def test_loader_is_select_only_content_and_excludes_expose(monkeypatch):
    connection = FakeConnection()
    fake_module = types.SimpleNamespace(connect=lambda **_: connection)
    monkeypatch.setitem(sys.modules, "pymysql", fake_module)
    settings = Settings(db_host="localhost", db_user="reader", db_password="configured")
    records = BehaviorLoader(settings).load(30)
    sql = connection.cursor_instance.sql.upper()
    assert sql.lstrip().startswith("SELECT")
    assert "TARGET_TYPE='CONTENT'" in sql
    assert "EXPOSE" not in sql
    assert records[0].content_id == 100
    assert connection.closed


def test_loader_requires_db_host():
    settings = Settings(db_host="")
    try:
        BehaviorLoader(settings).load()
        assert False, "expected RuntimeError"
    except RuntimeError as exc:
        assert "DB_HOST" in str(exc)

