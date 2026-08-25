"""
知识库管理模块的请求/响应模型。
"""

from typing import Optional

from pydantic import BaseModel


class KnowledgeAddRequest(BaseModel):
    """添加知识请求"""
    title: str
    content: str
    source: Optional[str] = None


class KnowledgeUpdateRequest(BaseModel):
    """更新知识请求"""
    title: str
    content: str


class KnowledgeSearchRequest(BaseModel):
    """检索知识请求"""
    query: str
    k: int = 3


class KnowledgeChunkVO(BaseModel):
    """命中的知识片段"""
    content: str
    source: Optional[str] = None
    title: Optional[str] = None
    chunk_index: Optional[int] = None
    score: float
