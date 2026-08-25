"""
AI 知识库管理 REST API。

端点概览：
  POST   /ai/knowledge              添加知识（切片→Embedding→写入Chroma）
  GET    /ai/knowledge              列出所有知识文档
  GET    /ai/knowledge/search       检索知识（返回命中片段+相似度分数）
  DELETE /ai/knowledge/{doc_id}     删除文档（删除该文档所有切片）
  PUT    /ai/knowledge/{doc_id}     更新文档（删除旧切片+重新切片写入）
  POST   /ai/knowledge/load-dir     批量加载 rag/knowledge/ 目录下所有 .md 文件
"""

import os

from fastapi import APIRouter, HTTPException

from app.schemas.knowledge_schema import (
    KnowledgeAddRequest,
    KnowledgeUpdateRequest,
)
from service.knowledge_service import (
    add_knowledge,
    list_documents,
    search_knowledge,
    delete_knowledge,
    update_knowledge,
    load_from_directory,
)

router = APIRouter()

# 知识文档默认目录：ai-service/rag/knowledge/
_KNOWLEDGE_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "rag", "knowledge"
)


@router.post("")
def add(req: KnowledgeAddRequest):
    """添加知识：文本切片 → Embedding → 写入 Chroma"""
    return add_knowledge(req.title, req.content, req.source)


@router.get("")
def list_all():
    """列出所有知识文档（按 doc_id 聚合）"""
    return list_documents()


@router.get("/search")
def search(query: str, k: int = 3):
    """检索知识：返回命中的知识片段 + 相似度分数"""
    return search_knowledge(query, k)


@router.delete("/{doc_id}")
def delete(doc_id: str):
    """删除文档（删除该文档的所有切片）"""
    result = delete_knowledge(doc_id)
    if not result.get("success"):
        raise HTTPException(status_code=404, detail=f"未找到 doc_id={doc_id} 的文档")
    return result


@router.put("/{doc_id}")
def update(doc_id: str, req: KnowledgeUpdateRequest):
    """更新文档：删除旧切片 + 重新切片写入"""
    result = update_knowledge(doc_id, req.title, req.content)
    if not result.get("success"):
        raise HTTPException(status_code=404, detail=result.get("message", "文档不存在"))
    return result


@router.post("/load-dir")
def load_dir():
    """批量加载 rag/knowledge/ 目录下所有 .md 文件到知识库"""
    return load_from_directory(_KNOWLEDGE_DIR)
