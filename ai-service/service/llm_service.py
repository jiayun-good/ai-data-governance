# 调用大模型
from langchain_openai import ChatOpenAI
from app.config import settings


llm = ChatOpenAI(
    model=settings.MODEL,
    api_key=settings.API_KEY,
    base_url=settings.BASE_URL
)


def chat(prompt):

    result = llm.invoke(prompt)

    return result.content
