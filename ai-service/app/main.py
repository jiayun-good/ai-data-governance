from fastapi import FastAPI

from api import rule
from api import knowledge
from service import embedding_service


app = FastAPI(
    title="AI Data Governance Service"
)


app.include_router(
    rule.router,
    prefix="/ai/rule",
    tags=["AI规则"]
)

app.include_router(
    knowledge.router,
    prefix="/ai/knowledge",
    tags=["AI知识库"]
)


@app.get("/")
def index():
    return {
        "msg":"AI service running"
    }

@app.get("/test/embedding")
def embedding_test():

    return embedding_service.test_embedding(
        "用户表名称不能为空"
    )

@app.post("/add")
def add(text:str):

    return embedding_service.add_rule(
        text,
        {
            "type":"quality_rule"
        }
    )


@app.get("/search")
def search(q:str):

    return embedding_service.search_rule(q)