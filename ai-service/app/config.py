from dotenv import load_dotenv
import os


load_dotenv()

class Settings:

    API_KEY = os.getenv("API_KEY")

    BASE_URL = os.getenv("BASE_URL")

    MODEL = os.getenv("MODEL")

    EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL")

    # ChromaDB 配置（独立于 FastAPI 端口）
    CHROMA_HOST = os.getenv("CHROMA_HOST", "localhost")
    CHROMA_PORT = int(os.getenv("CHROMA_PORT", "8000"))

    # 文本切片参数
    CHUNK_SIZE = int(os.getenv("CHUNK_SIZE", "500"))
    CHUNK_OVERLAP = int(os.getenv("CHUNK_OVERLAP", "100"))


settings = Settings()