from fastapi import APIRouter
from pydantic import BaseModel

from service.rule_service import generate_rule

router=APIRouter()



class RuleRequest(BaseModel):

    description:str



@router.post("/generate")
def generate(req:RuleRequest):

    return generate_rule(
        req.description
    )