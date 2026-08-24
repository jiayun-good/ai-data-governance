import json
import re

from service.llm_service import chat


def analyze_table(desc: str, tables: list[str]) -> dict:
    """
    第一步：让 AI 从表名列表中选出最匹配业务描述的表。

    :param desc:   用户自然语言描述，如 "用户表的名称列不能为空"
    :param tables: 数据源中所有表名，如 ["tb_user", "tb_order", "sys_config"]
    :return:       {"table": "tb_user"}
    """
    tables_text = "\n".join([f"- {t}" for t in tables])

    prompt = f"""你是一个数据治理专家。根据用户的业务描述，从以下表名列表中选出最相关的一张表。

## 业务描述
{desc}

## 可选表名
{tables_text}

## 要求
1. 只能从上面的表名列表中选择一张，必须使用原始表名，不要修改
2. 根据表名的语义来匹配（例如 "用户表" 可能对应 "tb_user"、"sys_user"、"user" 等）
3. 如果没有匹配的表，table 返回空字符串

请严格按照以下JSON格式返回，不要包含任何其他文字或markdown标记：
{{"table": "表名"}}
"""

    result = chat(prompt)
    json_str = _extract_json(result)
    return json.loads(json_str)


def generate_rule(desc: str, table: str, columns: list[dict]) -> dict:
    """
    第二步：根据已确定的表名 + 字段元数据，生成数据质量规则。

    :param desc:    用户自然语言描述
    :param table:   AI 选出的表名，如 "tb_user"
    :param columns: 该表的字段列表，如 [{"columnName":"name","dataType":"VARCHAR",...}]
    :return:        规则 JSON dict
    """
    columns_text = _build_columns_text(columns)

    prompt = f"""你是一个专业的数据治理专家。请根据用户的业务描述，结合提供的字段元数据，生成一条数据质量规则。

## 业务描述
{desc}

## 表名
{table}

## 字段信息
{columns_text}

## 可用的规则类型(ruleType)
- NOT_NULL：非空校验（检测 NULL 或空字符串）
- UNIQUE：唯一性校验（检测重复值）
- LENGTH：长度校验（字符串字符数的边界）
- RANGE：范围校验（数值型字段的 min/max 边界）
- REGEX：正则校验（正则表达式匹配，如手机号、邮箱等固定格式）
- ENUM：枚举校验（值必须在指定列表中）

## 规则配置(ruleConfig)格式
根据规则类型，ruleConfig 的字段不同：
- NOT_NULL：{{}}
- UNIQUE：{{}}
- LENGTH：{{"minLength": 最小长度, "maxLength": 最大长度}}
- RANGE：{{"min": 最小值, "max": 最大值}}
- REGEX：{{"pattern": "正则表达式"}}
- ENUM：{{"values": ["值1", "值2", "值3"]}}

## 选型优先级（严格按顺序判断，命中即停）
1. 描述涉及"不能为空"、"必填"、"非空" → 必须用 NOT_NULL
2. 描述涉及"不能重复"、"唯一" → 必须用 UNIQUE
3. 描述涉及字符串长度限制（如"名称2到20字"、"长度不超过50"） → 必须用 LENGTH
4. 描述涉及数值范围（年龄、金额、数量等的大小边界） → 必须用 RANGE
5. 描述涉及固定格式（手机号、邮箱、身份证等正则可表达的模式） → 用 REGEX
6. 描述涉及值的范围是有限枚举（如"性别只能是男或女"、"状态只能是启用或禁用"） → 用 ENUM

## 要求
1. column 必须是上面字段信息中实际存在的字段名，不要修改
2. 根据业务语义选择最合适的规则类型
3. 生成简洁明了的规则名称(ruleName)
4. ruleConfig 必须是JSON对象，不是字符串

请严格按照以下JSON格式返回，不要包含任何其他文字或markdown标记：
{{
    "ruleName": "规则名称",
    "ruleType": "规则类型",
    "column": "实际字段名",
    "ruleConfig": {{}},
    "description": "规则描述"
}}
"""

    result = chat(prompt)
    json_str = _extract_json(result)
    data = json.loads(json_str)

    # 补充 table 字段（由 Java 端已确定，不需要 AI 再猜）
    data["table"] = table
    return data


def _build_columns_text(columns: list[dict]) -> str:
    """将字段元数据格式化为可读的 Markdown 表格"""
    if not columns:
        return "（未提供字段信息）"

    lines = [
        "| 字段名 | 类型 | 长度 | 允许为空 | 备注 |",
        "|--------|------|------|----------|------|",
    ]
    for col in columns:
        name = col.get("columnName", "")
        dtype = col.get("dataType", "")
        length = col.get("length", "")
        nullable = "是" if col.get("nullable") else "否"
        comment = col.get("comment", "")
        lines.append(f"| {name} | {dtype} | {length} | {nullable} | {comment} |")

    return "\n".join(lines)


def _extract_json(text: str) -> str:
    """从 AI 返回文本中提取 JSON 字符串"""
    # 尝试匹配 ```json ... ``` 代码块
    match = re.search(r"```(?:json)?\s*\n?(.*?)\n?```", text, re.DOTALL)
    if match:
        return match.group(1).strip()

    # 尝试匹配 { ... } 最外层 JSON
    match = re.search(r"\{.*\}", text, re.DOTALL)
    if match:
        return match.group(0).strip()

    return text.strip()
