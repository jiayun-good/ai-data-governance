import chromadb
from langchain_chroma import Chroma
from app.config import settings
from langchain_community.embeddings import DashScopeEmbeddings


embedding = DashScopeEmbeddings(
    model=settings.EMBEDDING_MODEL,
    dashscope_api_key=settings.API_KEY
)

# ChromaDB 运行在独立端口，与 FastAPI（8011）隔离
client = chromadb.HttpClient(
    host=settings.CHROMA_HOST,
    port=settings.CHROMA_PORT
)

# 数据质量规则向量库（原有，用于规则存储）
vector_store = Chroma(
    client=client,
    collection_name="data_quality_rules",
    embedding_function=embedding
)

# 知识库向量库（用于 RAG 检索增强）
knowledge_vector_store = Chroma(
    client=client,
    collection_name="knowledge_base",
    embedding_function=embedding
)
