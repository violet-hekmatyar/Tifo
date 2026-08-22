# Tifo T18 CONTENT Item-CF service

This service only reads `user_behavior_log`, trains CONTENT Item-CF, and returns normalized CF scores.
Java owns business candidates, RULE_V2, MATCH ranking, reasons, experiments, fallbacks and final mixing.

## Run

```powershell
py -m pip install -r requirements.txt
py -m uvicorn app.main:app --host 127.0.0.1 --port 8100
```

Copy `.env.example` values into process environment variables; do not commit a real `.env`.
When MySQL is unavailable the process still starts, `/health` remains available with
`modelReady=false`, and Java falls back to RULE_V2.

## API

- `GET /health`
- `GET /api/internal/recommend/stats`
- `POST /api/internal/recommend/content-scores`
- `POST /api/internal/recommend/reload?days=30`

The MySQL identity used in deployment must have SELECT permission only on `user_behavior_log`.

## Algorithm defaults

Positive weights are CLICK 1.0, DETAIL 1.5 plus up to 1.0 for 60-second dwell, LIKE 2.0, FAVORITE 2.5 and COMMENT 3.0; EXPOSE is excluded. Weights use `exp(-days/14)` time decay and cap each user-item at 8.0. Item cosine keeps 50 neighbors. Candidate score is a bounded similarity-weighted average in `[0,1]`; strongly interacted items receive a 0.15 repeat factor. Reload builds a new model before taking the atomic lock and retains the old model on failure.
