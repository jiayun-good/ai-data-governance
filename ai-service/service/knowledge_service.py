"""
知识库管理服务：编排文本切片、Embedding 生成和 Chroma 存储的完整生命周期。

核心流程：文本 → chunker 切片 → Chroma 自动 Embedding → 写入向量库
"""

import os
import uuid

from rag.chunker import chunk_text, extract_title
from rag.retriever import retrieve
from vector.chroma_client import knowledge_vector_store


def add_knowledge(title: str, content: str, source: str = None) -> dict:
    """
    添加知识：文本 → 切片 → Embedding → 写入 Chroma

    :param title:   文档标题
    :param content: 文档全文
    :param source:  源文件名（可选，默认使用 title）
    :return:        {"doc_id", "chunk_count", "title", "source"}
    """
    doc_id = str(uuid.uuid4())

    if not title:
        title = extract_title(content)
    if not source:
        source = title

    # 1. 文本切片
    chunks = chunk_text(content)

    if not chunks:
        return {"doc_id": doc_id, "chunk_count": 0, "title": title, "source": source}

    # 2. 构建每个切片的 metadata
    total = len(chunks)
    metadatas = []
    ids = []
    for i in range(total):
        metadatas.append({
            "doc_id": doc_id,
            "source": source,
            "title": title,
            "chunk_index": i,
            "total_chunks": total
        })
        ids.append(f"{doc_id}_chunk_{i}")

    # 3. 写入 Chroma（自动生成 Embedding）
    knowledge_vector_store.add_texts(
        texts=chunks,
        metadatas=metadatas,
        ids=ids
    )

    return {"doc_id": doc_id, "chunk_count": total, "title": title, "source": source}


def list_documents() -> list[dict]:
    """
    列出所有知识文档（按 doc_id 聚合）。

    :return: [{"doc_id", "title", "source", "chunk_count"}, ...]
    """
    collection = knowledge_vector_store._collection
    results = collection.get(include=["metadatas"])

    # 按 doc_id 聚合统计
    docs = {}
    for metadata in results.get("metadatas", []):
        doc_id = metadata.get("doc_id", "")
        if doc_id not in docs:
            docs[doc_id] = {
                "doc_id": doc_id,
                "title": metadata.get("title", ""),
                "source": metadata.get("source", ""),
                "chunk_count": 0
            }
        docs[doc_id]["chunk_count"] += 1

    return list(docs.values())


def get_document_content(doc_id: str) -> dict:
    """
    获取单个知识文档的完整内容（用于编辑回填）。

    通过 doc_id 查询所有切片，按 chunk_index 排序后拼接为完整文本。

    :return: {"doc_id", "title", "source", "content", "chunk_count"} 或 {"success": False}
    """
    collection = knowledge_vector_store._collection
    results = collection.get(
        where={"doc_id": doc_id},
        include=["documents", "metadatas"]
    )

    ids = results.get("ids", [])
    if not ids:
        return {"success": False, "message": f"未找到 doc_id={doc_id} 的文档"}

    metadatas = results.get("metadatas", [])
    documents = results.get("documents", [])

    # 按 chunk_index 排序后拼接
    pairs = list(zip(metadatas, documents))
    pairs.sort(key=lambda p: p[0].get("chunk_index", 0))
    content = "\n".join(doc for _, doc in pairs)

    first_meta = metadatas[0]
    return {
        "doc_id": doc_id,
        "title": first_meta.get("title", ""),
        "source": first_meta.get("source", ""),
        "content": content,
        "chunk_count": len(ids)
    }


def search_knowledge(query: str, k: int = 3) -> list[dict]:
    """检索知识（委托 retriever）"""
    return retrieve(query, k)


def delete_knowledge(doc_id: str) -> dict:
    """
    删除文档：通过 doc_id 删除该文档的所有切片。

    :return: {"success": bool, "deleted_count": int, "doc_id": str}
    """
    collection = knowledge_vector_store._collection

    # 通过 metadata 过滤获取该文档所有切片的 ID
    results = collection.get(where={"doc_id": doc_id}, include=[])
    ids = results.get("ids", [])

    if not ids:
        return {"success": False, "deleted_count": 0, "doc_id": doc_id}

    knowledge_vector_store.delete(ids=ids)

    return {"success": True, "deleted_count": len(ids), "doc_id": doc_id}


def update_knowledge(doc_id: str, title: str, content: str) -> dict:
    """
    更新文档：删除旧切片 → 重新切片 → 写入新切片。

    :return: {"success", "doc_id", "title", "source", "chunk_count", "old_chunk_count"}
    """
    collection = knowledge_vector_store._collection

    # 1. 获取旧文档的 metadata（在删除前保留 source）
    results = collection.get(where={"doc_id": doc_id}, include=["metadatas"])
    ids = results.get("ids", [])

    if not ids:
        return {"success": False, "message": f"未找到 doc_id={doc_id} 的文档"}

    old_source = results["metadatas"][0].get("source", title) if results.get("metadatas") else title

    # 2. 删除旧切片
    knowledge_vector_store.delete(ids=ids)

    # 3. 重新切片写入（复用 doc_id，保持 source 一致）
    chunks = chunk_text(content)

    if not chunks:
        return {"success": False, "message": "内容为空，未写入任何切片"}

    total = len(chunks)
    metadatas = []
    new_ids = []
    for i in range(total):
        metadatas.append({
            "doc_id": doc_id,
            "source": old_source,
            "title": title,
            "chunk_index": i,
            "total_chunks": total
        })
        new_ids.append(f"{doc_id}_chunk_{i}")

    knowledge_vector_store.add_texts(texts=chunks, metadatas=metadatas, ids=new_ids)

    return {
        "success": True,
        "doc_id": doc_id,
        "title": title,
        "source": old_source,
        "chunk_count": total,
        "old_chunk_count": len(ids)
    }


def load_from_directory(dir_path: str) -> dict:
    """
    批量加载目录下所有 .md 文件到知识库。

    :return: {"total_docs", "total_chunks", "details": [...]}
    """
    details = []
    total_docs = 0
    total_chunks = 0

    for filename in sorted(os.listdir(dir_path)):
        if not filename.endswith(".md"):
            continue

        filepath = os.path.join(dir_path, filename)
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()

        result = add_knowledge(
            title=extract_title(content),
            content=content,
            source=filename
        )

        details.append({
            "file": filename,
            "doc_id": result.get("doc_id"),
            "chunk_count": result.get("chunk_count", 0)
        })
        total_docs += 1
        total_chunks += result.get("chunk_count", 0)

    return {
        "total_docs": total_docs,
        "total_chunks": total_chunks,
        "details": details
    }
