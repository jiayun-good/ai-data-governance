from typing import Optional

from fastapi import APIRouter
from pydantic import BaseModel

from service.rule_service import select_table, generate_rule

router = APIRouter()


class ColumnInfo(BaseModel):
    columnName: str
    dataType: str
    length: Optional[int] = None
    nullable: Optional[bool] = None
    comment: Optional[str] = None


class ChatMessage(BaseModel):
    user: str
    assistant: str


class AnalyzeTableRequest(BaseModel):
    """第一步：分析表请求"""
    description: str
    tables: list[str]
    history: list[ChatMessage] = []
    currentTable: Optional[str] = None


class GenerateRuleRequest(BaseModel):
    """第二步：生成规则请求"""
    description: str
    table: str
    columns: list[ColumnInfo] = []
    history: list[ChatMessage] = []


@router.post("/analyze-table")
def analyze(req: AnalyzeTableRequest):
    """AI 智能选表：有上下文时先检测是否切换，否则直接匹配"""
    history_data = [m.model_dump() for m in req.history]
    return select_table(req.description, req.tables, history_data, req.currentTable)


@router.post("/generate")
def generate(req: GenerateRuleRequest):
    """AI 根据表名 + 字段元数据生成数据质量规则"""
    columns_data = [c.model_dump() for c in req.columns]
    history_data = [m.model_dump() for m in req.history]
    return generate_rule(req.description, req.table, columns_data, history_data)
