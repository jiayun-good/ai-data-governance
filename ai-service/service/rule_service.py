import json
import re

from service.llm_service import chat
from prompt.rule_prompt import (
    build_analyze_table_prompt,
    build_detect_context_switch_prompt,
    build_generate_rule_prompt,
)


def analyze_table(desc: str, tables: list[str]) -> dict:
    """
    让 AI 从表名列表中选出最匹配业务描述的表。

    :param desc:   用户自然语言描述，如 "用户表的名称列不能为空"
    :param tables: 数据源中所有表名，如 ["tb_user", "tb_order", "sys_config"]
    :return:       {"table": "tb_user"}
    """
    tables_text = "\n".join([f"- {t}" for t in tables])
    prompt = build_analyze_table_prompt(desc, tables_text)

    result = chat(prompt)
    json_str = _extract_json(result)
    return json.loads(json_str)


def detect_context_switch(desc: str, current_table: str, tables: list[str]) -> dict:
    """
    检测用户是否想切换到不同的表。

    流程：用户输入 → 读取当前表 → AI 判断是否切换 →
      - 否 → 继续使用当前表
      - 是 → 调用 analyze_table() 重新匹配

    :param desc:          用户新的业务描述
    :param current_table: 上一轮对话中确定的表名
    :param tables:        数据源中所有表名
    :return:              {"table": "表名", "switched": bool}
    """
    prompt = build_detect_context_switch_prompt(desc, current_table)

    result = chat(prompt)
    json_str = _extract_json(result)
    data = json.loads(json_str)

    if not data.get("switched", False):
        return {"table": current_table, "switched": False}

    # 确认切换，调用 analyze_table 重新匹配
    analyze_result = analyze_table(desc, tables)
    return {"table": analyze_result.get("table", ""), "switched": True}


def select_table(desc: str, tables: list[str],
                 history: list[dict] = None,
                 current_table: str = None) -> dict:
    """
    智能表选择入口：根据上下文决定是复用当前表还是重新匹配。

    :param desc:          用户自然语言描述
    :param tables:        数据源中所有表名
    :param history:       对话历史
    :param current_table: 上一轮确定的表名（None 表示首次对话）
    :return:              {"table": "tb_user"}
    """
    if current_table and current_table in tables:
        result = detect_context_switch(desc, current_table, tables)
        return {"table": result["table"]}

    # 首次对话或 currentTable 无效，直接让 AI 匹配
    return analyze_table(desc, tables)


def generate_rule(desc: str, table: str, columns: list[dict], history: list[dict] = None) -> dict:
    """
    第二步：根据已确定的表名 + 字段元数据，生成数据质量规则。

    :param desc:    用户自然语言描述
    :param table:   AI 选出的表名，如 "tb_user"
    :param columns: 该表的字段列表，如 [{"columnName":"name","dataType":"VARCHAR",...}]
    :param history: 对话历史，如 [{"user":"...","assistant":"..."}]
    :return:        规则 JSON dict
    """
    columns_text = _build_columns_text(columns)
    history_text = _build_history_text(history)
    prompt = build_generate_rule_prompt(desc, table, columns_text, history_text)

    result = chat(prompt)
    json_str = _extract_json(result)
    data = json.loads(json_str)

    # 补充 table 字段（由 Java 端已确定，不需要 AI 再猜）
    data["table"] = table
    return data


def _build_history_text(history: list[dict] | None) -> str:
    """将对话历史格式化为 prompt 中的上下文段落"""
    if not history:
        return ""
    lines = ["\n## 之前的对话记录"]
    for msg in history:
        lines.append(f"- 用户：{msg.get('user', '')}")
        lines.append(f"- 助手：{msg.get('assistant', '')}")
    lines.append("\n请参考以上对话记录来理解用户的意图和上下文。\n")
    return "\n".join(lines)


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
