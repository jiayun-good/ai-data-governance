"""
知识检索模块：从 Chroma 向量库中检索相关知识片段，并格式化为 prompt 上下文。
"""

from vector.chroma_client import knowledge_vector_store


def retrieve(query: str, k: int = 3) -> list[dict]:
    """
    检索与 query 最相关的知识片段。

    :param query: 检索文本（通常是用户的业务描述）
    :param k:     返回的片段数量
    :return:      [{"content": str, "metadata": dict, "score": float}, ...]
    """
    docs_and_scores = knowledge_vector_store.similarity_search_with_score(query, k=k)

    result = []
    for doc, score in docs_and_scores:
        result.append({
            "content": doc.page_content,
            "metadata": doc.metadata,
            "score": float(score)
        })

    return result


def format_context(chunks: list[dict]) -> str:
    """
    将检索结果格式化为 prompt 可用的「参考资料」段落。

    若无检索结果则返回空字符串，不影响原有 prompt 流程。
    """
    if not chunks:
        return ""

    lines = ["\n## 参考资料", "以下是从知识库中检索到的相关资料，请参考：\n"]

    for i, chunk in enumerate(chunks, 1):
        source = chunk["metadata"].get("source", "未知")
        score = chunk.get("score", 0)
        content = chunk["content"]
        lines.append(f"### 资料{i}（来源：{source}，相似度：{score:.2f}）")
        lines.append(content)
        lines.append("")

    return "\n".join(lines)
