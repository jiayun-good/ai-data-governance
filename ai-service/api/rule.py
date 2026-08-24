from typing import Optional

from fastapi import APIRouter
from pydantic import BaseModel

from service.rule_service import analyze_table, generate_rule

router = APIRouter()


class ColumnInfo(BaseModel):
    columnName: str
    dataType: str
    length: Optional[int] = None
    nullable: Optional[bool] = None
    comment: Optional[str] = None


class AnalyzeTableRequest(BaseModel):
    """第一步：分析表请求"""
    description: str
    tables: list[str]


class GenerateRuleRequest(BaseModel):
    """第二步：生成规则请求"""
    description: str
    table: str
    columns: list[ColumnInfo] = []


@router.post("/analyze-table")
def analyze(req: AnalyzeTableRequest):
    """AI 从表名列表中选出最匹配业务描述的表"""
    return analyze_table(req.description, req.tables)


@router.post("/generate")
def generate(req: GenerateRuleRequest):
    """AI 根据表名 + 字段元数据生成数据质量规则"""
    columns_data = [c.model_dump() for c in req.columns]
    return generate_rule(req.description, req.table, columns_data)
