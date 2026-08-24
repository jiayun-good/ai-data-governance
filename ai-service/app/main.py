from fastapi import FastAPI

from api import rule


app = FastAPI(
    title="AI Data Governance Service"
)


app.include_router(
    rule.router,
    prefix="/ai/rule",
    tags=["AI规则"]
)


@app.get("/")
def index():
    return {
        "msg":"AI service running"
    }