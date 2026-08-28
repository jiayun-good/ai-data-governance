"""
文本切片模块：将长文本切分为有重叠的文本块，供 Embedding 向量化使用。

针对 Markdown 文档按标题层级优先切分，保证语义完整性。
"""

import re

from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.config import settings


_splitter = RecursiveCharacterTextSplitter(
    chunk_size=settings.CHUNK_SIZE,
    chunk_overlap=settings.CHUNK_OVERLAP, #切片之间的重叠长度
    separators=["\n## ", "\n### ", "\n---", "\n\n", "\n", " ", ""],
)


def chunk_text(text: str) -> list[str]:
    """将长文本切分为有重叠的文本块"""
    return _splitter.split_text(text)


def extract_title(text: str) -> str:
    """
    提取 Markdown 文档首个一级标题作为文档标题。
    若无标题则截取前 30 个字符。
    """
    match = re.match(r"^#\s+(.+)$", text.strip(), re.MULTILINE)
    if match:
        return match.group(1).strip()

    # 无标题时取前 30 个字符
    flat = text.strip().replace("\n", " ")
    return flat[:30] + ("..." if len(flat) > 30 else "")
