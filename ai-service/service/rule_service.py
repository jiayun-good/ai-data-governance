from service.llm_service import chat


def generate_rule(desc):
    prompt = f"""

你是一个数据治理专家。

根据业务描述生成数据质量规则。


业务描述：

{desc}


返回JSON:

{{
ruleName:"",
ruleType:"",
table:"",
column:"",
ruleConfig:"",
description:""
}}

"""

    result = chat(prompt)
    rule = QualityRule(**result)
    return rule

