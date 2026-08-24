from pydantic import BaseModel


class QualityRule(BaseModel):
    ruleName: str
    ruleType: str
    table: str
    column: str
    ruleConfig: dict
    description: str