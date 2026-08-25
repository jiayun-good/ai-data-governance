from vector.chroma_client import embedding
from vector.chroma_client import vector_store


def test_embedding(text: str):

    vector = embedding.embed_query(text)

    return {
        "text": text,
        "dimension": len(vector),
        "vector_preview": vector[:5]
    }

# 写入向量库
def add_rule(text: str, metadata: dict = None):

    ids = vector_store.add_texts(
        texts=[text],
        metadatas=[metadata] if metadata else None
    )

    return {
        "success": True,
        "ids": ids
    }

# 相似度查询
def search_rule(query: str, k: int = 3):

    docs = vector_store.similarity_search(
        query,
        k=k
    )

    result = []

    for doc in docs:
        result.append({
            "content": doc.page_content,
            "metadata": doc.metadata
        })

    return result