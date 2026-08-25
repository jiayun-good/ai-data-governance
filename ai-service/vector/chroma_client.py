import chromadb
from langchain_chroma import Chroma
from app.config import settings
from langchain_community.embeddings import DashScopeEmbeddings


embedding = DashScopeEmbeddings(
    model=settings.EMBEDDING_MODEL,
    dashscope_api_key=settings.API_KEY
)

# ChromaDB 服务器运行在 8001 端口，与 FastAPI 服务（8000）隔离
client = chromadb.HttpClient(
    host="localhost",
    port=8000
)

vector_store = Chroma(
    client=client,
    collection_name="data_quality_rules",
    embedding_function=embedding
)